package org.cairnserver.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cairnserver.main.CairnClassLoader;
import org.spongepowered.asm.mixin.Mixins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginManager {
    private static Path pluginsDir;
    private static CairnClassLoader classLoader;
    private static final List<CairnPlugin> loadedPlugins = new ArrayList<>();

    public static void init(Path newPluginsDir, CairnClassLoader newClassLoader){
        pluginsDir = newPluginsDir;
        classLoader = newClassLoader;
    }

    public static void registerAllPluginMixinConfigs() {
        if (Files.notExists(pluginsDir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jar : stream) {
                JsonObject config = readPluginConfig(jar);
                if (config.has("mixinConfig")) {
                    Mixins.addConfiguration(config.get("mixinConfig").getAsString());
                }
            }
        } catch (IOException e) {
            System.err.println("扫描插件 Mixin 配置失败: " + e.getMessage());
        }
    }

    public static void enableAllPlugins() {
        if (Files.notExists(pluginsDir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.jar")) {
            for (Path jar : stream) {
                try {
                    loadPlugin(jar);
                } catch (Exception e) {
                    System.err.println("启用插件失败 " + jar.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("扫描plugins目录失败: " + e.getMessage());
        }
    }

    private static void loadPlugin(Path jarPath) throws Exception {
        JsonObject config = readPluginConfig(jarPath);
        String mainClassName = config.get("mainClass").getAsString();

        URL[] urls = { jarPath.toUri().toURL() };
        PluginClassLoader loader = new PluginClassLoader(urls, classLoader);
        Class<?> clazz = loader.loadClass(mainClassName);
        Object instance = clazz.getDeclaredConstructor().newInstance();

        if (instance instanceof CairnPlugin plugin) {
            plugin.onEnable();
            loadedPlugins.add(plugin);
            System.out.println("已启用插件: " + plugin.getName());
        } else {
            throw new RuntimeException("插件主类必须实现 CairnPlugin 接口");
        }
    }

    public static void disableAllPlugins() {
        for (CairnPlugin plugin : loadedPlugins) {
            try {
                plugin.onDisable();
            } catch (Exception e) {
                System.err.println("禁用插件失败 " + plugin.getName() + ": " + e.getMessage());
            }
        }
        loadedPlugins.clear();
    }

    private static JsonObject readPluginConfig(Path jar) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            JarEntry entry = jf.getJarEntry("cairn-plugin.json");
            if (entry == null) throw new RuntimeException("缺少 cairn-plugin.json");
            try (InputStream in = jf.getInputStream(entry)) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return JsonParser.parseString(json).getAsJsonObject();
            }
        }
    }
}
