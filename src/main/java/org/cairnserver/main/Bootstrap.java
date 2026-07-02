package org.cairnserver.main;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cairnserver.plugin.PluginManager;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Bootstrap {
    private static final String VERSION = "26.1";
    private static final String SERVER_JAR_NAME = "server.jar";
    private static final String TEMP_JAR_NAME = "server.jar.tmp";
    private static final int RETRIES = 5;

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        CairnClassLoader classLoader = (CairnClassLoader) Thread.currentThread().getContextClassLoader();

        Path libPath = Path.of("lib");
        Path serverJarPath = libPath.resolve(SERVER_JAR_NAME);
        Path tempJarPath = libPath.resolve(TEMP_JAR_NAME);

        if (Files.notExists(serverJarPath)) {
            System.out.println("正在下载server.jar...");
            for (int i = 0; i < RETRIES; i++) {
                try {
                    httpDownloadServerJar(tempJarPath, serverJarPath);
                    break;
                } catch (Exception e) {
                    try { Files.deleteIfExists(tempJarPath); } catch (IOException ignored) {}
                    if (i == RETRIES - 1) {
                        System.out.println("下载失败: " + e.getMessage());
                        return;
                    }
                    System.out.println("重试 (" + (i + 1) + "/" + RETRIES + ")");
                    try { Thread.sleep(2000L * (i + 1)); } catch (InterruptedException ignored) {}
                }
            }
        }

        Path outputDir = libPath.resolve("extracted");
        try (JarFile jar = new JarFile(serverJarPath.toFile())) {
            extractJarDir(jar, "META-INF/versions", outputDir.resolve("versions"), classLoader);
            extractJarDir(jar, "META-INF/libraries", outputDir.resolve("libraries"), classLoader);
        } catch (IOException e) {
            System.err.println("启动服务器失败:" + e.getMessage());
            return;
        }

        MixinBootstrap.init();
        MixinEnvironment.init(MixinEnvironment.Phase.DEFAULT);
        Mixins.addConfiguration("mixins.cairnserver.json");
        MixinTransformer transformer = new MixinTransformer();
        MixinEnvironment.gotoPhase(MixinEnvironment.Phase.DEFAULT);
        MixinBootstrap.getPlatform().prepare(CommandLineOptions.defaultArgs());
        classLoader.setTransformer(transformer);

        String mainClassName;
        try (JarFile jar = new JarFile(serverJarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("META-INF/main-class");
            if (entry == null) throw new RuntimeException("找不到META-INF/main-class");
            try (InputStream is = jar.getInputStream(entry)) {
                mainClassName = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            throw new RuntimeException("无法读取META-INF/main-class", e);
        }

        Thread serverThread = new Thread(() -> {
            try {
                Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
                MethodHandle handle = MethodHandles.lookup()
                        .findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class))
                        .asFixedArity();
                handle.invoke((Object) args);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, "ServerMain");
        serverThread.setContextClassLoader(classLoader);

        Path pluginPath = Path.of("plugins");
        try { Files.createDirectories(pluginPath); } catch (IOException e) {
            System.out.println("创建插件文件夹失败: " + e.getMessage());
        }
        PluginManager.init(pluginPath, classLoader);
        PluginManager.registerAllPluginMixinConfigs();
        serverThread.start();
    }

    private static void extractJarDir(JarFile jar, String prefix, Path outputDir, CairnClassLoader classLoader) throws IOException {
        Files.createDirectories(outputDir);
        jar.stream()
                .filter(entry -> entry.getName().startsWith(prefix) && !entry.getName().endsWith("/"))
                .forEach(entry -> {
                    String name = entry.getName().substring(prefix.length() + 1);
                    Path target = outputDir.resolve(name);
                    try {
                        if (Files.notExists(target)) {
                            Files.createDirectories(target.getParent());
                            try (InputStream in = jar.getInputStream(entry)) {
                                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        classLoader.addURL(target.toUri().toURL());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static void httpDownloadServerJar(Path tempJarPath, Path serverJarPath) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> manifestResp = client.send(
                HttpRequest.newBuilder(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
                        .timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (manifestResp.statusCode() != 200) throw new IOException("状态码 " + manifestResp.statusCode());
        JsonObject manifest = JsonParser.parseString(manifestResp.body()).getAsJsonObject();
        String versionUrl = manifest.getAsJsonArray("versions").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(v -> VERSION.equals(v.get("id").getAsString()))
                .findFirst().orElseThrow()
                .get("url").getAsString();
        HttpResponse<String> metaResp = client.send(
                HttpRequest.newBuilder(URI.create(versionUrl)).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (metaResp.statusCode() != 200) throw new IOException("状态码 " + metaResp.statusCode());
        JsonObject meta = JsonParser.parseString(metaResp.body()).getAsJsonObject();
        String downloadUrl = meta.getAsJsonObject("downloads").getAsJsonObject("server").get("url").getAsString();
        Files.deleteIfExists(tempJarPath);
        HttpResponse<Path> downloadResp = client.send(
                HttpRequest.newBuilder(URI.create(downloadUrl)).timeout(Duration.ofMinutes(10)).build(),
                HttpResponse.BodyHandlers.ofFile(tempJarPath));
        if (downloadResp.statusCode() != 200) {
            Files.deleteIfExists(tempJarPath);
            throw new IOException("下载失败，状态码 " + downloadResp.statusCode());
        }
        Files.move(tempJarPath, serverJarPath, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("server.jar 下载完成");
    }
}