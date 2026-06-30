package org.cairnserver.plugin;

import org.cairnserver.main.CairnClassLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL[] urls, CairnClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException ignored) {}
        String path = name.replace('.', '/').concat(".class");
        try (InputStream in = getResourceAsStream(path)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] bytes = in.readAllBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}
