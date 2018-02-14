package com.capz.core.impl;

import com.capz.core.Capsule;
import com.capz.core.CapsuleFactory;
import com.capz.core.impl.capsule.CompilingClassLoader;

/**
 * @author Bao Qingping
 */
public class JavaCapsuleFactory implements CapsuleFactory {

    @Override
    public String prefix() {
        return "java";
    }

    @Override
    public Capsule createCapsule(String capsuleName, ClassLoader classLoader) throws Exception {
        capsuleName = CapsuleFactory.removePrefix(capsuleName);
        Class clazz;
        if (capsuleName.endsWith(".java")) {
            CompilingClassLoader compilingLoader = new CompilingClassLoader(classLoader, capsuleName);
            String className = compilingLoader.resolveMainClassName();
            clazz = compilingLoader.loadClass(className);
        } else {
            clazz = classLoader.loadClass(capsuleName);
        }
        return (Capsule) clazz.newInstance();
    }
}
