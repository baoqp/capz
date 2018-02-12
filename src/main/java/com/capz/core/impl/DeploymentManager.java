
package com.capz.core.impl;


import com.capz.core.AsyncResult;
import com.capz.core.Capsule;
import com.capz.core.CapsuleFactory;
import com.capz.core.CapzInternal;
import com.capz.core.Context;
import com.capz.core.Deployment;
import com.capz.core.DeploymentOptions;
import com.capz.core.Handler;
import com.capz.core.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;


public class DeploymentManager {

    private static final Logger log = LoggerFactory.getLogger(DeploymentManager.class);

    private final CapzInternal capz;
    private final Map<String, Deployment> deployments = new ConcurrentHashMap<>();
    private final Map<String, ClassLoader> classloaders = new WeakHashMap<>();
    private final Map<String, List<CapsuleFactory>> CapsuleFactories = new ConcurrentHashMap<>();
    private final List<CapsuleFactory> defaultFactories = new ArrayList<>();

    public DeploymentManager(CapzInternal capz) {
        this.capz = capz;
        loadCapsuleFactories();
    }

    private void loadCapsuleFactories() {
        Collection<CapsuleFactory> factories = ServiceHelper.loadFactories(CapsuleFactory.class);
        factories.forEach(this::registerCapsuleFactory);
        CapsuleFactory defaultFactory = new JavaCapsuleFactory();
        defaultFactory.init(capz);
        defaultFactories.add(defaultFactory);
    }

    private String generateDeploymentID() {
        return UUID.randomUUID().toString();
    }

    public void deployCapsule(Supplier<Capsule> CapsuleSupplier, DeploymentOptions options, Handler<AsyncResult<String>> completionHandler) {
        if (options.getInstances() < 1) {
            throw new IllegalArgumentException("Can't specify < 1 instances to deploy");
        }
        if (options.isMultiThreaded() && !options.isWorker()) {
            throw new IllegalArgumentException("If multi-threaded then must be worker too");
        }
        if (options.getExtraClasspath() != null) {
            throw new IllegalArgumentException("Can't specify extraClasspath for already created Capsule");
        }
        if (options.getIsolationGroup() != null) {
            throw new IllegalArgumentException("Can't specify isolationGroup for already created Capsule");
        }
        if (options.getIsolatedClasses() != null) {
            throw new IllegalArgumentException("Can't specify isolatedClasses for already createde");
        }
        AbstractContext currentContext = capz.getOrCreateContext();
        ClassLoader cl = getClassLoader(options, currentContext);
        int nbInstances = options.getInstances();
        Set<Capsule> Capsules = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < nbInstances; i++) {
            Capsule Capsule;
            try {
                Capsule = CapsuleSupplier.get();
            } catch (Exception e) {
                completionHandler.handle(Future.failedFuture(e));
                return;
            }
            if (Capsule == null) {
                completionHandler.handle(Future.failedFuture("Supplied Capsule is null"));
                return;
            }
            Capsules.add(Capsule);
        }
        if (Capsules.size() != nbInstances) {
            completionHandler.handle(Future.failedFuture("Same Capsule supplied more than once"));
            return;
        }
        Capsule[] CapsulesArray = Capsules.toArray(new Capsule[Capsules.size()]);
        String CapsuleClass = CapsulesArray[0].getClass().getName();
        doDeploy("java:" + CapsuleClass, generateDeploymentID(), options, currentContext, currentContext, completionHandler, cl, CapsulesArray);
    }

    public void deployCapsule(String identifier,
                              DeploymentOptions options,
                              Handler<AsyncResult<String>> completionHandler) {
        if (options.isMultiThreaded() && !options.isWorker()) {
            throw new IllegalArgumentException("If multi-threaded then must be worker too");
        }
        AbstractContext callingContext = capz.getOrCreateContext();
        ClassLoader cl = getClassLoader(options, callingContext);
        doDeployCapsule(identifier, generateDeploymentID(), options, callingContext, callingContext, cl, completionHandler);
    }

    private void doDeployCapsule(String identifier,
                                 String deploymentID,
                                 DeploymentOptions options,
                                 AbstractContext parentContext,
                                 AbstractContext callingContext,
                                 ClassLoader cl,
                                 Handler<AsyncResult<String>> completionHandler) {
        List<CapsuleFactory> CapsuleFactories = resolveFactories(identifier);
        Iterator<CapsuleFactory> iter = CapsuleFactories.iterator();
        doDeployCapsule(iter, null, identifier, deploymentID, options, parentContext, callingContext, cl, completionHandler);
    }

    private void doDeployCapsule(Iterator<CapsuleFactory> iter,
                                 Throwable prevErr,
                                 String identifier,
                                 String deploymentID,
                                 DeploymentOptions options,
                                 AbstractContext parentContext,
                                 AbstractContext callingContext,
                                 ClassLoader cl,
                                 Handler<AsyncResult<String>> completionHandler) {
        if (iter.hasNext()) {
            CapsuleFactory CapsuleFactory = iter.next();
            Future<String> fut = Future.future();
            if (CapsuleFactory.requiresResolve()) {
                try {
                    CapsuleFactory.resolve(identifier, options, cl, fut);
                } catch (Exception e) {
                    try {
                        fut.fail(e);
                    } catch (Exception ignore) {
                        // Too late
                    }
                }
            } else {
                fut.complete(identifier);
            }
            fut.setHandler(ar -> {
                Throwable err;
                if (ar.succeeded()) {
                    String resolvedName = ar.result();
                    if (!resolvedName.equals(identifier)) {
                        try {
                            deployCapsule(resolvedName, options, completionHandler);
                        } catch (Exception e) {
                            completionHandler.handle(Future.failedFuture(e));
                        }
                        return;
                    } else {
                        if (CapsuleFactory.blockingCreate()) {
                            capz.<Capsule[]>executeBlocking(createFut -> {
                                try {
                                    Capsule[] Capsules = createCapsules(CapsuleFactory, identifier, options.getInstances(), cl);
                                    createFut.complete(Capsules);
                                } catch (Exception e) {
                                    createFut.fail(e);
                                }
                            }, res -> {
                                if (res.succeeded()) {
                                    doDeploy(identifier, deploymentID, options, parentContext, callingContext, completionHandler, cl, res.result());
                                } else {
                                    // Try the next one
                                    doDeployCapsule(iter, res.cause(), identifier, deploymentID, options, parentContext, callingContext, cl, completionHandler);
                                }
                            });
                            return;
                        } else {
                            try {
                                Capsule[] Capsules = createCapsules(CapsuleFactory, identifier, options.getInstances(), cl);
                                doDeploy(identifier, deploymentID, options, parentContext, callingContext, completionHandler, cl, Capsules);
                                return;
                            } catch (Exception e) {
                                err = e;
                            }
                        }
                    }
                } else {
                    err = ar.cause();
                }
                // Try the next one
                doDeployCapsule(iter, err, identifier, deploymentID, options, parentContext, callingContext, cl, completionHandler);
            });
        } else {
            if (prevErr != null) {
                // Report failure if there are no more factories to try otherwise try the next one
                reportFailure(prevErr, callingContext, completionHandler);
            } else {
                // not handled or impossible ?
            }
        }
    }

    private Capsule[] createCapsules(CapsuleFactory CapsuleFactory, String identifier, int instances, ClassLoader cl) throws Exception {
        Capsule[] Capsules = new Capsule[instances];
        for (int i = 0; i < instances; i++) {
            Capsules[i] = CapsuleFactory.createCapsule(identifier, cl);
            if (Capsules[i] == null) {
                throw new NullPointerException("CapsuleFactory::createCapsule returned null");
            }
        }
        return Capsules;
    }

    private String getSuffix(int pos, String str) {
        if (pos + 1 >= str.length()) {
            throw new IllegalArgumentException("Invalid name: " + str);
        }
        return str.substring(pos + 1);
    }

    public void undeployCapsule(String deploymentID, Handler<AsyncResult<Void>> completionHandler) {
        Deployment deployment = deployments.get(deploymentID);
        Context currentContext = capz.getOrCreateContext();
        if (deployment == null) {
            reportFailure(new IllegalStateException("Unknown deployment"), currentContext, completionHandler);
        } else {
            deployment.undeploy(completionHandler);
        }
    }

    public Set<String> deployments() {
        return Collections.unmodifiableSet(deployments.keySet());
    }

    public Deployment getDeployment(String deploymentID) {
        return deployments.get(deploymentID);
    }

    public void undeployAll(Handler<AsyncResult<Void>> completionHandler) {
        // TODO timeout if it takes too long - e.g. async stop Capsule fails to call future

        // We only deploy the top level Capsules as the children will be undeployed when the parent is
        Set<String> deploymentIDs = new HashSet<>();
        for (Map.Entry<String, Deployment> entry : deployments.entrySet()) {
            if (!entry.getValue().isChild()) {
                deploymentIDs.add(entry.getKey());
            }
        }
        if (!deploymentIDs.isEmpty()) {
            AtomicInteger count = new AtomicInteger(0);
            for (String deploymentID : deploymentIDs) {
                undeployCapsule(deploymentID, ar -> {
                    if (ar.failed()) {
                        // Log but carry on regardless
                        log.error("Undeploy failed", ar.cause());
                    }
                    if (count.incrementAndGet() == deploymentIDs.size()) {
                        completionHandler.handle(Future.succeededFuture());
                    }
                });
            }
        } else {
            Context context = capz.getOrCreateContext();
            context.runOnContext(v -> completionHandler.handle(Future.succeededFuture()));
        }
    }

    public void registerCapsuleFactory(CapsuleFactory factory) {
        String prefix = factory.prefix();
        if (prefix == null) {
            throw new IllegalArgumentException("factory.prefix() cannot be null");
        }
        List<CapsuleFactory> facts = CapsuleFactories.get(prefix);
        if (facts == null) {
            facts = new ArrayList<>();
            CapsuleFactories.put(prefix, facts);
        }
        if (facts.contains(factory)) {
            throw new IllegalArgumentException("Factory already registered");
        }
        facts.add(factory);
        // Sort list in ascending order
        facts.sort((fact1, fact2) -> fact1.order() - fact2.order());
        factory.init(capz);
    }

    public void unregisterCapsuleFactory(CapsuleFactory factory) {
        String prefix = factory.prefix();
        if (prefix == null) {
            throw new IllegalArgumentException("factory.prefix() cannot be null");
        }
        List<CapsuleFactory> facts = CapsuleFactories.get(prefix);
        boolean removed = false;
        if (facts != null) {
            if (facts.remove(factory)) {
                removed = true;
            }
            if (facts.isEmpty()) {
                CapsuleFactories.remove(prefix);
            }
        }
        if (!removed) {
            throw new IllegalArgumentException("factory isn't registered");
        }
    }

    public Set<CapsuleFactory> CapsuleFactories() {
        Set<CapsuleFactory> facts = new HashSet<>();
        for (List<CapsuleFactory> list : CapsuleFactories.values()) {
            facts.addAll(list);
        }
        return facts;
    }

    private List<CapsuleFactory> resolveFactories(String identifier) {
    /*
      We resolve the Capsule factory list to use as follows:
      1. We look for a prefix in the identifier.
      E.g. the identifier might be "js:app.js" <-- the prefix is "js"
      If it exists we use that to lookup the Capsule factory list
      2. We look for a suffix (like a file extension),
      E.g. the identifier might be just "app.js"
      If it exists we use that to lookup the factory list
      3. If there is no prefix or suffix OR there is no match then defaults will be used
    */
        List<CapsuleFactory> factoryList = null;
        int pos = identifier.indexOf(':');
        String lookup = null;
        if (pos != -1) {
            // Infer factory from prefix, e.g. "java:" or "js:"
            lookup = identifier.substring(0, pos);
        } else {
            // Try and infer name from extension
            pos = identifier.lastIndexOf('.');
            if (pos != -1) {
                lookup = getSuffix(pos, identifier);
            } else {
                // No prefix, no extension - use defaults
                factoryList = defaultFactories;
            }
        }
        if (factoryList == null) {
            factoryList = CapsuleFactories.get(lookup);
            if (factoryList == null) {
                factoryList = defaultFactories;
            }
        }
        return factoryList;
    }

    /**
     * <strong>IMPORTANT</strong> - Isolation groups are not supported on Java 9+ because the application classloader is not
     * an URLClassLoader anymore. Thus we can't extract the list of jars to configure the IsolatedClassLoader.
     */
    private ClassLoader getClassLoader(DeploymentOptions options, AbstractContext parentContext) {
        String isolationGroup = options.getIsolationGroup();
        ClassLoader cl;
        if (isolationGroup == null) {
            cl = getCurrentClassLoader();
        } else {
            // IMPORTANT - Isolation groups are not supported on Java 9+, because the system classloader is not an URLClassLoader
            // anymore. Thus we can't extract the paths from the classpath and isolate the loading.
            synchronized (this) {
                cl = classloaders.get(isolationGroup);
                if (cl == null) {
                    ClassLoader current = getCurrentClassLoader();
                    if (!(current instanceof URLClassLoader)) {
                        throw new IllegalStateException("Current classloader must be URLClassLoader");
                    }
                    List<URL> urls = new ArrayList<>();
                    // Add any extra URLs to the beginning of the classpath
                    List<String> extraClasspath = options.getExtraClasspath();
                    if (extraClasspath != null) {
                        for (String pathElement : extraClasspath) {
                            File file = new File(pathElement);
                            try {
                                URL url = file.toURI().toURL();
                                urls.add(url);
                            } catch (MalformedURLException e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    }
                    // And add the URLs of the Vert.x classloader
                    URLClassLoader urlc = (URLClassLoader) current;
                    urls.addAll(Arrays.asList(urlc.getURLs()));

                    // Create an isolating cl with the urls
                    cl = new IsolatingClassLoader(urls.toArray(new URL[urls.size()]), getCurrentClassLoader(),
                            options.getIsolatedClasses());
                    classloaders.put(isolationGroup, cl);
                }
            }
        }
        return cl;
    }

    private ClassLoader getCurrentClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }


    private <T> void reportFailure(Throwable t, Context context, Handler<AsyncResult<T>> completionHandler) {
        if (completionHandler != null) {
            reportResult(context, completionHandler, Future.failedFuture(t));
        } else {
            log.error(t.getMessage(), t);
        }
    }

    private <T> void reportSuccess(T result, Context context, Handler<AsyncResult<T>> completionHandler) {
        if (completionHandler != null) {
            reportResult(context, completionHandler, Future.succeededFuture(result));
        }
    }

    private <T> void reportResult(Context context, Handler<AsyncResult<T>> completionHandler, AsyncResult<T> result) {
        context.runOnContext(v -> {
            try {
                completionHandler.handle(result);
            } catch (Throwable t) {
                log.error("Failure in calling handler", t);
                throw t;
            }
        });
    }

    private void doDeploy(String identifier, String deploymentID, DeploymentOptions options,
                          Context parentContext,
                          Context callingContext,
                          Handler<AsyncResult<String>> completionHandler,
                          ClassLoader tccl, Capsule... Capsules) {

        String poolName = options.getWorkerPoolName();

        Deployment parent = parentContext.getDeployment();
        DeploymentImpl deployment = new DeploymentImpl(parent, deploymentID, identifier, options);

        AtomicInteger deployCount = new AtomicInteger();
        AtomicBoolean failureReported = new AtomicBoolean();
        for (Capsule Capsule : Capsules) {
            WorkerExecutorImpl workerExec = poolName != null ? capz.createSharedWorkerExecutor(poolName, options.getWorkerPoolSize(), options.getMaxWorkerExecuteTime()) : null;
            WorkerPool pool = workerExec != null ? workerExec.getPool() : null;
            AbstractContext context = options.isWorker() ? capz.createWorkerContext(options.isMultiThreaded(), deploymentID, pool, conf, tccl) :
                    capz.createEventLoopContext(deploymentID, pool, conf, tccl);
            if (workerExec != null) {
                context.addCloseHook(workerExec);
            }
            context.setDeployment(deployment);
            deployment.addCapsule(new CapsuleHolder(Capsule, context));
            context.runOnContext(v -> {
                try {
                    Capsule.init(capz, context);
                    Future<Void> startFuture = Future.future();
                    Capsule.start(startFuture);
                    startFuture.setHandler(ar -> {
                        if (ar.succeeded()) {
                            if (parent != null) {
                                if (parent.addChild(deployment)) {
                                    deployment.child = true;
                                } else {
                                    // Orphan
                                    deployment.undeploy(null);
                                    return;
                                }
                            }
                            capzMetrics metrics = capz.metricsSPI();
                            if (metrics != null) {
                                metrics.CapsuleDeployed(Capsule);
                            }
                            deployments.put(deploymentID, deployment);
                            if (deployCount.incrementAndGet() == Capsules.length) {
                                reportSuccess(deploymentID, callingContext, completionHandler);
                            }
                        } else if (failureReported.compareAndSet(false, true)) {
                            deployment.rollback(callingContext, completionHandler, context, ar.cause());
                        }
                    });
                } catch (Throwable t) {
                    if (failureReported.compareAndSet(false, true))
                        deployment.rollback(callingContext, completionHandler, context, t);
                }
            });
        }
    }

    static class CapsuleHolder {
        final Capsule Capsule;
        final AbstractContext context;

        CapsuleHolder(Capsule Capsule, AbstractContext context) {
            this.Capsule = Capsule;
            this.context = context;
        }
    }

    private class DeploymentImpl implements Deployment {

        private static final int ST_DEPLOYED = 0, ST_UNDEPLOYING = 1, ST_UNDEPLOYED = 2;

        private final Deployment parent;
        private final String deploymentID;
        private final String CapsuleIdentifier;
        private final List<CapsuleHolder> Capsules = new CopyOnWriteArrayList<>();
        private final Set<Deployment> children = new ConcurrentHashSet<>();
        private final DeploymentOptions options;
        private int status = ST_DEPLOYED;
        private volatile boolean child;

        private DeploymentImpl(Deployment parent, String deploymentID, String CapsuleIdentifier, DeploymentOptions options) {
            this.parent = parent;
            this.deploymentID = deploymentID;
            this.CapsuleIdentifier = CapsuleIdentifier;
            this.options = options;
        }

        public void addCapsule(CapsuleHolder holder) {
            Capsules.add(holder);
        }

        private synchronized void rollback(AbstractContext callingContext, Handler<AsyncResult<String>> completionHandler, AbstractContext context, Throwable cause) {
            if (status == ST_DEPLOYED) {
                status = ST_UNDEPLOYING;
                doUndeployChildren(callingContext, childrenResult -> {
                    synchronized (DeploymentImpl.this) {
                        status = ST_UNDEPLOYED;
                    }
                    if (childrenResult.failed()) {
                        reportFailure(cause, callingContext, completionHandler);
                    } else {
                        context.runCloseHooks(closeHookAsyncResult -> reportFailure(cause, callingContext, completionHandler));
                    }
                });
            }
        }

        @Override
        public void undeploy(Handler<AsyncResult<Void>> completionHandler) {
            AbstractContext currentContext = capz.getOrCreateContext();
            doUndeploy(currentContext, completionHandler);
        }

        private synchronized void doUndeployChildren(AbstractContext undeployingContext, Handler<AsyncResult<Void>> completionHandler) {
            if (!children.isEmpty()) {
                final int size = children.size();
                AtomicInteger childCount = new AtomicInteger();
                boolean undeployedSome = false;
                for (Deployment childDeployment : new HashSet<>(children)) {
                    undeployedSome = true;
                    childDeployment.doUndeploy(undeployingContext, ar -> {
                        children.remove(childDeployment);
                        if (ar.failed()) {
                            reportFailure(ar.cause(), undeployingContext, completionHandler);
                        } else if (childCount.incrementAndGet() == size) {
                            // All children undeployed
                            completionHandler.handle(Future.succeededFuture());
                        }
                    });
                }
                if (!undeployedSome) {
                    // It's possible that children became empty before iterating
                    completionHandler.handle(Future.succeededFuture());
                }
            } else {
                completionHandler.handle(Future.succeededFuture());
            }
        }

        public synchronized void doUndeploy(AbstractContext undeployingContext, Handler<AsyncResult<Void>> completionHandler) {
            if (status == ST_UNDEPLOYED) {
                reportFailure(new IllegalStateException("Already undeployed"), undeployingContext, completionHandler);
                return;
            }
            if (!children.isEmpty()) {
                status = ST_UNDEPLOYING;
                doUndeployChildren(undeployingContext, ar -> {
                    if (ar.failed()) {
                        reportFailure(ar.cause(), undeployingContext, completionHandler);
                    } else {
                        doUndeploy(undeployingContext, completionHandler);
                    }
                });
            } else {
                status = ST_UNDEPLOYED;
                AtomicInteger undeployCount = new AtomicInteger();
                int numToUndeploy = Capsules.size();
                for (CapsuleHolder CapsuleHolder : Capsules) {
                    AbstractContext context = CapsuleHolder.context;
                    context.runOnContext(v -> {
                        Future<Void> stopFuture = Future.future();
                        AtomicBoolean failureReported = new AtomicBoolean();
                        stopFuture.setHandler(ar -> {
                            deployments.remove(deploymentID);

                            /*context.runCloseHooks(ar2 -> {
                                if (ar2.failed()) {
                                    // Log error but we report success anyway
                                    log.error("Failed to run close hook", ar2.cause());
                                }
                                if (ar.succeeded() && undeployCount.incrementAndGet() == numToUndeploy) {
                                    reportSuccess(null, undeployingContext, completionHandler);
                                } else if (ar.failed() && !failureReported.get()) {
                                    failureReported.set(true);
                                    reportFailure(ar.cause(), undeployingContext, completionHandler);
                                }
                            });*/
                        });
                        try {
                            CapsuleHolder.Capsule.stop(stopFuture);
                        } catch (Throwable t) {
                            stopFuture.fail(t);
                        } finally {
                            // Remove the deployment from any parents
                            if (parent != null) {
                                parent.removeChild(this);
                            }
                        }
                    });
                }
            }
        }

        @Override
        public String CapsuleIdentifier() {
            return CapsuleIdentifier;
        }

        @Override
        public DeploymentOptions deploymentOptions() {
            return options;
        }

        @Override
        public synchronized boolean addChild(Deployment deployment) {
            if (status == ST_DEPLOYED) {
                children.add(deployment);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void removeChild(Deployment deployment) {
            children.remove(deployment);
        }

        @Override
        public Set<Capsule> getCapsules() {
            Set<Capsule> verts = new HashSet<>();
            for (CapsuleHolder holder : Capsules) {
                verts.add(holder.Capsule);
            }
            return verts;
        }

        @Override
        public boolean isChild() {
            return child;
        }

        @Override
        public String deploymentID() {
            return deploymentID;
        }

    }

}
