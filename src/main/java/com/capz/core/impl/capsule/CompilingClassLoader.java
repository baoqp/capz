package com.capz.core.impl.capsule;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CompilingClassLoader extends ClassLoader {

    private static final Logger log = LoggerFactory.getLogger(CompilingClassLoader.class);

    private static final String JAVA_COMPILER_OPTIONS_PROP_NAME = "capz.javaCompilerOptions";
    private final static List<String> COMPILER_OPTIONS;

    static {
        String props = System.getProperty(JAVA_COMPILER_OPTIONS_PROP_NAME);
        if (props != null) {
            String[] array = props.split(",");
            List<String> compilerProps = new ArrayList<>(array.length);

            for (String prop : array) {
                compilerProps.add(prop.trim());
            }
            COMPILER_OPTIONS = Collections.unmodifiableList(compilerProps);
        } else {
            COMPILER_OPTIONS = null;
        }
    }

    private final JavaSourceContext javaSourceContext;
    private final MemoryFileManager fileManager;

    public CompilingClassLoader(ClassLoader loader, String sourceName) {
        super(loader);
        URL resource = getResource(sourceName);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + sourceName);
        }
        //Need to urldecode it too, since bug in JDK URL class which does not url decode it, so if it contains spaces you are screwed
        File sourceFile;
        try {
            sourceFile = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to decode " + e.getMessage());
        }
        if (!sourceFile.canRead()) {
            throw new RuntimeException("File not found: " + sourceFile.getAbsolutePath() + " current dir is: " + new File(".").getAbsolutePath());
        }

        this.javaSourceContext = new JavaSourceContext(sourceFile);

        try {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
            if (javaCompiler == null) {
                throw new RuntimeException("Unable to detect java compiler, make sure you're using a JDK not a JRE!");
            }
            StandardJavaFileManager standardFileManager = javaCompiler.getStandardFileManager(
                    null, null, null);

            standardFileManager.setLocation(StandardLocation.SOURCE_PATH,
                    Collections.singleton(javaSourceContext.getSourceRoot())); // java源文件所在目录

            fileManager = new MemoryFileManager(loader, standardFileManager);

            // TODO - this needs to be fixed so it can compile classes from the classpath otherwise can't include
            // other .java resources from other modules

            JavaFileObject javaFile = standardFileManager.getJavaFileForInput(
                    StandardLocation.SOURCE_PATH, resolveMainClassName(), Kind.SOURCE);

            JavaCompiler.CompilationTask task = javaCompiler.getTask(
                    null, fileManager, diagnostics, COMPILER_OPTIONS, null, Collections.singleton(javaFile));

            boolean valid = task.call();
            if (valid) {
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    String code = d.getCode();
                    if (code == null || (!code.startsWith("compiler.warn.annotation.method.not.found") &&
                            !"compiler.warn.proc.processor.incompatible.source.version".equals(code))) {
                        log.info(d.toString());
                    }
                }
            } else {
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    log.warn(d.toString());
                }
                throw new RuntimeException("Compilation failed!");
            }
        } catch (Exception e) {
            throw new RuntimeException("Compilation failed", e);
        }
    }

    public String resolveMainClassName() {
        return javaSourceContext.getClassName();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = getClassBytes(name);
        if (bytecode == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytecode, 0, bytecode.length);
    }

    public byte[] getClassBytes(String name) {
        return fileManager.getCompiledClass(name);
    }
}
