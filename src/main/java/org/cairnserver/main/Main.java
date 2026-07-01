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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
    private static final String VERSION = "26.1";
    private static final String SERVER_JAR_PARENT_PATH = "lib";
    private static final String SERVER_JAR_NAME = "server.jar";
    private static final String TEMP_JAR_NAME = "server.jar.tmp";
    private static final int NETWORK_RETRIES_COUNT = 5;
    private static Path serverJarPath;
    private static Path tempJarPath;

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        Path libPath = Path.of(SERVER_JAR_PARENT_PATH);
        try {
            Files.createDirectories(libPath);
        } catch (IOException e) {
            System.err.println("尝试创建lib目录时发生IO异常:" + e.getMessage());
        }
        serverJarPath = libPath.resolve(SERVER_JAR_NAME);
        tempJarPath = libPath.resolve(TEMP_JAR_NAME);
        if (Files.notExists(serverJarPath)) {
            System.out.println("正在下载server.jar...");
            for (int i = 0; i < NETWORK_RETRIES_COUNT; i++) {
                try {
                    httpDownloadServerJar();
                    break;
                } catch (Exception e) {
                    try {
                        Files.deleteIfExists(tempJarPath);
                    } catch (IOException ignored) {}
                    if (i == NETWORK_RETRIES_COUNT - 1) {
                        System.out.println("请求Minecraft服务API时发生错误,请尝试手动下载" + SERVER_JAR_NAME + "并移动到本服务器JAR根目录下的lib/server.jar,错误:" + e.getMessage());
                        return;
                    }
                    System.out.println("下载失败,重试中(" + (i + 1) + "/" + NETWORK_RETRIES_COUNT + "):" + e.getMessage());
                    try {
                        Thread.sleep(2000L * (i + 1));
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        List<URL> extractedJars = new ArrayList<>();
        Path outputDir = libPath.resolve("extracted");
        try (JarFile jar = new JarFile(serverJarPath.toFile())) {
            extractedJars.addAll(extractDir(jar, "META-INF/versions", outputDir.resolve("versions")));
            extractedJars.addAll(extractDir(jar, "META-INF/libraries", outputDir.resolve("libraries")));
        } catch (IOException e) {
            System.err.println("启动服务器失败:" + e.getMessage());
        }

        MixinBootstrap.init();
        MixinEnvironment.init(MixinEnvironment.Phase.DEFAULT);
        Mixins.addConfiguration("mixins.cairnserver.json");

        MixinTransformer transformer = new MixinTransformer();
        URL selfUrl = Main.class.getProtectionDomain().getCodeSource().getLocation();
        List<URL> allUrls = new ArrayList<>(extractedJars);
        allUrls.add(selfUrl);
        URL[] urls = allUrls.toArray(new URL[0]);
        ClassLoader parent = Main.class.getClassLoader().getParent();

        CairnClassLoader classLoader = new CairnClassLoader(urls, parent, transformer);

        MixinEnvironment.gotoPhase(MixinEnvironment.Phase.DEFAULT);
        MixinBootstrap.getPlatform().prepare(CommandLineOptions.defaultArgs());

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
                handle.invoke((Object)args);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, "ServerMain");
        serverThread.setContextClassLoader(classLoader);
        PluginManager.init(Path.of("plugins"), classLoader);
        PluginManager.registerAllPluginMixinConfigs();
        serverThread.start();
    }

    private static List<URL> extractDir(JarFile jar, String prefix, Path outputDir) throws IOException {
        List<URL> urls = new ArrayList<>();
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
                        urls.add(target.toUri().toURL());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return urls;
    }

    private static void httpDownloadServerJar() throws IOException, InterruptedException {
        try (HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build()) {
            HttpResponse<String> manifestResp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (manifestResp.statusCode() != 200) {
                throw new IOException("获取版本清单失败,状态码:" + manifestResp.statusCode());
            }
            JsonObject manifest = JsonParser.parseString(manifestResp.body()).getAsJsonObject();

            String versionMetaUrl = manifest.getAsJsonArray("versions").asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(v -> VERSION.equals(v.get("id").getAsString()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("找不到" + VERSION + "版本信息"))
                    .get("url").getAsString();

            HttpResponse<String> metaResp = httpClient.send(
                    HttpRequest.newBuilder(URI.create(versionMetaUrl))
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (metaResp.statusCode() != 200) {
                throw new IOException("获取版本元数据失败,状态码:" + metaResp.statusCode());
            }
            JsonObject meta = JsonParser.parseString(metaResp.body()).getAsJsonObject();
            String serverDownloadUrl = meta.getAsJsonObject("downloads")
                    .getAsJsonObject("server")
                    .get("url").getAsString();

            Files.deleteIfExists(tempJarPath);
            HttpResponse<Path> downloadResp = httpClient.send(
                    HttpRequest.newBuilder(URI.create(serverDownloadUrl))
                            .timeout(Duration.ofMinutes(10))
                            .build(),
                    HttpResponse.BodyHandlers.ofFile(tempJarPath)
            );
            if (downloadResp.statusCode() != 200) {
                Files.deleteIfExists(tempJarPath);
                throw new IOException("下载server.jar失败,HTTP状态码:" + downloadResp.statusCode());
            }

            Files.move(tempJarPath, serverJarPath, StandardCopyOption.ATOMIC_MOVE);

            System.out.println("下载" + SERVER_JAR_NAME + "成功!");
        }
    }
}