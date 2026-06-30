# CairnServer

**CairnServer** 是一个开源的 Minecraft 服务端，基于 UntoldStudio 的轻量级 Mixin 字节码编织框架构建，致力于为 Minecraft 提供强大而灵活的插件支持。项目目前处于积极开发阶段，未来将完整支持 Minecraft Java 版。

**CairnServer** is an open-source Minecraft server software built upon UntoldStudio's lightweight Mixin bytecode weaving framework, aiming to deliver powerful and flexible plugin support for Minecraft. The project is under active development and will fully support Minecraft: Java Edition in the future.

## 特性 / Features

- **高效插件系统** – 基于自定义类加载器的插件隔离，支持插件携带自有 Mixin 配置，实现深度定制。
- **现代字节码编织** – 直接集成 UntoldStudio Mixin，无需 Java Agent，避免安全软件误报。
- **轻量设计** – 不依赖庞大框架，专注于核心功能，保持高性能与可维护性。
- **事件驱动架构（开发中）** – 为插件提供简洁的事件总线，方便响应游戏事件。

- **Efficient Plugin System** – Isolated plugin classloading with support for per-plugin Mixin configurations, enabling deep customization.
- **Modern Bytecode Weaving** – Directly integrates UntoldStudio Mixin without Java Agent, avoiding false positives from security software.
- **Lightweight Design** – Focuses on core functionality without heavy frameworks, ensuring high performance and maintainability.
- **Event-Driven Architecture (WIP)** – A clean event bus for plugins to easily respond to in-game events.

## 构建 / Building

CairnServer 使用 Gradle 作为构建系统，需要 JDK 25 及以上版本。

CairnServer uses Gradle as its build system and requires JDK 25 or later.

```bash
git clone https://github.com/UntoldStudio/CairnServer.git
cd CairnServer
./gradlew cairnShadowJar
```

编译产物位于 `build/libs/CairnServer.jar`。

The compiled artifact will be located at `build/libs/CairnServer.jar`.

你也可以直接通过 JitPack 引入项目作为依赖（详情请见 JitPack 徽章，将随第一个稳定版提供）。

You can also consume the project as a dependency via JitPack (details and badge will be available with the first stable release).

## 使用 / Usage

运行 `CairnServer.jar` 即可启动。首次启动会自动下载对应版本的官方服务端，并完成必要的资源提取与 Mixin 初始化。

Simply run `CairnServer.jar` to start. On the first launch, it will automatically download the required official server binaries, extract necessary resources, and initialize the Mixin environment.

```
java -jar CairnServer.jar
```

插件放入 `plugins/` 目录，描述文件为 `cairn-plugin.json`，其中可指定主类和 Mixin 配置。

Place your plugins in the `plugins/` directory. Plugins are described by a `cairn-plugin.json` file, which can specify the main class and optional Mixin configuration.

## 许可证 / License

CairnServer 使用 GNU Lesser General Public License v3 (LGPL v3) 进行许可。  
详见 [LICENSE](LICENSE) 文件。

CairnServer is licensed under the GNU Lesser General Public License v3 (LGPL v3).  
See the [LICENSE](LICENSE) file for details.

## 致谢 / Acknowledgements

- 本项目受益于 **Mixin** 和 **ASM** 生态系统。
- This project benefits from the **Mixin** and **ASM** ecosystems.