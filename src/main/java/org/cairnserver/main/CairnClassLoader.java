package org.cairnserver.main;

import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class CairnClassLoader extends URLClassLoader {
    private final IMixinTransformer transformer;
    public CairnClassLoader(URL[] urls, ClassLoader parent, IMixinTransformer transformer){
        super(urls, parent);
        this.transformer = transformer;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("net.minecraft.") || name.equals("net.minecraft")) {
            try {
                String path = name.replace('.', '/').concat(".class");
                InputStream is = getResourceAsStream(path);
                if (is == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] originalBytes = is.readAllBytes();
                is.close();
                byte[] transformedBytes = transformer.transformClassBytes(name, name, originalBytes);
                return defineClass(name, transformedBytes, 0, transformedBytes.length);
            } catch (IOException e){
                throw new ClassNotFoundException(name, e);
            }
        }
        return super.findClass(name);
    }
}
