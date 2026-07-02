package org.cairnserver.main;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        // 收集运行时 classpath 中的 jar 和目录
        List<URL> urls = new ArrayList<>();
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            urls.add(new File(path).toURI().toURL());
        }

        CairnClassLoader classLoader = new CairnClassLoader(urls.toArray(new URL[0]), Main.class.getClassLoader(), null);
        Thread.currentThread().setContextClassLoader(classLoader);

        Class<?> bootstrapClass = Class.forName("org.cairnserver.main.Bootstrap", true, classLoader);
        bootstrapClass.getMethod("main", String[].class).invoke(null, (Object) args);
    }
}