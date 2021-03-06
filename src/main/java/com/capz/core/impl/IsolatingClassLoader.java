package com.capz.core.impl;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IsolatingClassLoader extends URLClassLoader {

    private List<String> isolatedClasses;

    public IsolatingClassLoader(URL[] urls, ClassLoader parent, List<String> isolatedClasses) {
        super(urls, parent);
        this.isolatedClasses = isolatedClasses;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (isIsolatedClass(name)) {
                    // We don't want to load Vert.x (or Vert.x dependency) classes from an isolating loader
                    if (isCapzOrSystemClass(name)) {
                        try {
                            c = getParent().loadClass(name);
                        } catch (ClassNotFoundException e) {
                            // Fall through
                        }
                    }
                    if (c == null) {
                        // Try and load with this classloader
                        try {
                            c = findClass(name);
                        } catch (ClassNotFoundException e) {
                            // Now try with parent
                            c = getParent().loadClass(name);
                        }
                    }
                    if (resolve) {
                        resolveClass(c);
                    }
                } else {
                    // Parent first
                    c = super.loadClass(name, resolve);
                }
            }
            return c;
        }
    }

    private boolean isIsolatedClass(String name) {
        if (isolatedClasses != null) {
            for (String isolated : isolatedClasses) {
                if (isolated.endsWith(".*")) {
                    String isolatedPackage = isolated.substring(0, isolated.length() - 1);
                    String paramPackage = name.substring(0, name.lastIndexOf('.') + 1);
                    if (paramPackage.startsWith(isolatedPackage)) {
                        // Matching package
                        return true;
                    }
                } else if (isolated.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }



    @Override
    public URL getResource(String name) {

        // First check this classloader
        URL url = findResource(name);

        // Then try the parent if not found
        if (url == null) {
            url = super.getResource(name);
        }

        return url;
    }


    @Override
    public Enumeration<URL> getResources(String name) throws IOException {

        // First get resources from this classloader
        List<URL> resources = Collections.list(findResources(name));

        // Then add resources from the parent
        if (getParent() != null) {
            Enumeration<URL> parentResources = getParent().getResources(name);
            if (parentResources.hasMoreElements()) {
                resources.addAll(Collections.list(parentResources));
            }
        }

        return Collections.enumeration(resources);
    }

    private boolean isCapzOrSystemClass(String name) {
        return
                name.startsWith("java.") ||
                        name.startsWith("javax.") ||
                        name.startsWith("sun.*") ||
                        name.startsWith("com.sun.") ||
                        name.startsWith("com.capz.core") ||
                        name.startsWith("io.netty.") ||
                        name.startsWith("com.alibaba.fastjson");
    }
}