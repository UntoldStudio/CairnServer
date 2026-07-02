package org.cairnserver.main;

import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class CairnClassLoader extends URLClassLoader {
    private IMixinTransformer transformer;

    public CairnClassLoader(URL[] urls, ClassLoader parent, IMixinTransformer transformer) {
        super(urls, parent);
        this.transformer = transformer;
    }

    public void setTransformer(IMixinTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                if (name.startsWith("java.")
                        || name.equals("org.cairnserver.main.CairnClassLoader")
                        || name.startsWith("org.spongepowered.asm.")
                        || name.startsWith("org.objectweb.asm.")) {
                    c = getParent().loadClass(name);
                } else {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        c = getParent().loadClass(name);
                    }
                }
            }
            if (resolve) resolveClass(c);
            return c;
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("net.minecraft.")) {
            try {
                String path = name.replace('.', '/').concat(".class");
                InputStream is = getResourceAsStream(path);
                if (is == null) throw new ClassNotFoundException(name);
                byte[] originalBytes = is.readAllBytes();
                is.close();
                byte[] transformedBytes = transformer.transformClassBytes(name, name, originalBytes);
                return defineClass(name, transformedBytes, 0, transformedBytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
        return super.findClass(name);
    }
}