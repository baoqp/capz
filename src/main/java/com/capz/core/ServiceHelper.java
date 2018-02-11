package com.capz.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

// SPI加载工具类
public class ServiceHelper {

    public static <T> T loadFactory(Class<T> clazz) {
        T factory = loadFactoryOrNull(clazz);
        if (factory == null) {
            throw new IllegalStateException("Cannot find META-INF/services/" + clazz.getName() + " on classpath");
        }
        return factory;
    }

    public static <T> T loadFactoryOrNull(Class<T> clazz) {
        Collection<T> collection = loadFactories(clazz);
        if (!collection.isEmpty()) {
            return collection.iterator().next();
        } else {
            return null;
        }
    }


    public static <T> Collection<T> loadFactories(Class<T> clazz) {
        return loadFactories(clazz, null);
    }

    public static <T> Collection<T> loadFactories(Class<T> clazz, ClassLoader classLoader) {
        List<T> list = new ArrayList<>();
        //ServiceLoader加载在META-INF/services文件中定义的类
        ServiceLoader<T> factories;
        if (classLoader != null) {
            factories = ServiceLoader.load(clazz, classLoader);
        } else {
            // this is equivalent to:
            // ServiceLoader.load(clazz, TCCL);
            factories = ServiceLoader.load(clazz);
        }
        if (factories.iterator().hasNext()) {
            factories.iterator().forEachRemaining(list::add);
            return list;
        } else {
            // By default ServiceLoader.load uses the TCCL, this may not be enough in environment dealing with
            // classloaders differently such as OSGi. So we should try to use the  classloader having loaded this
            // class. In OSGi it would be the bundle exposing vert.x and so have access to all its classes.
            factories = ServiceLoader.load(clazz, ServiceHelper.class.getClassLoader());
            if (factories.iterator().hasNext()) {
                factories.iterator().forEachRemaining(list::add);
                return list;
            } else {
                return Collections.emptyList();
            }
        }
    }
}
