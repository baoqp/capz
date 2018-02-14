package com.capz.core.impl.capsule;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Map<String, ByteArrayOutputStream> compiledClasses = new HashMap<>();
    private final PackageHelper helper;

    public MemoryFileManager(ClassLoader classLoader, JavaFileManager fileManager) {
        super(fileManager);
        helper = new PackageHelper(classLoader);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, final String className,
                                               JavaFileObject.Kind kind, FileObject sibling) {
        try {
            return new SimpleJavaFileObject(new URI(""), kind) {
                public OutputStream openOutputStream()  {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    compiledClasses.put(className, outputStream);
                    return outputStream;
                }
            };
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getCompiledClass(String name) {
        ByteArrayOutputStream bytes = compiledClasses.get(name);
        if (bytes == null) {
            return null;
        }
        return bytes.toByteArray();
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof CustomJavaFileObject) {
            return ((CustomJavaFileObject) file).binaryName();
        } else {
            return super.inferBinaryName(location, file);
        }
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
                                         boolean recurse) throws IOException {
        if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
            return helper.find(packageName);
        }
        return super.list(location, packageName, kinds, recurse);
    }
}
