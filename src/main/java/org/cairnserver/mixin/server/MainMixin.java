package org.cairnserver.mixin.server;

import net.minecraft.server.Main;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.cairnserver.util.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(Main.class)
public class MainMixin {
    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Eula;<init>(Ljava/nio/file/Path;)V"))
    private static void onEulaNew(String[] args, CallbackInfo info) {
        Logger.info("Cairn Server正在启动");
    }

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V", shift = At.Shift.AFTER, ordinal = 0))
    private static void afterEulaNotIf(CallbackInfo callbackInfo){
        Logger.warn("你需要先同意Mojang EULA才能启动服务器");
    }

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;bootStrap()V", shift = At.Shift.AFTER))
    private static void customizeLogger(CallbackInfo ci) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig rootLogger = config.getRootLogger();

        Map<String, Appender> appenders = rootLogger.getAppenders();
        List<String> consoleAppenderNames = new ArrayList<>();
        for (Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getValue() instanceof ConsoleAppender) {
                consoleAppenderNames.add(entry.getKey());
            }
        }
        for (String name : consoleAppenderNames) {
            Appender appender = rootLogger.getAppenders().get(name);
            if (appender != null) {
                appender.stop();
                rootLogger.removeAppender(name);
            }
        }

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(
                        "%style{[%d{HH:mm:ss}]}{blue} " +
                                "%style{[%t/%p]}{green} " +
                                "%style{(%cairnSource)}{cyan} " +
                                "%highlight{%msg%n%xEx}" +
                                "{FATAL=bright_red, ERROR=bright_red, WARN=bright_yellow, " +
                                "INFO=bright_white, DEBUG=bright_black, TRACE=bright_black}"
                )
                .withConfiguration(config)
                .withDisableAnsi(false)
                .build();

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setName("Console")
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .build();
        consoleAppender.start();

        rootLogger.addAppender(consoleAppender, Level.INFO, null);

        ctx.updateLoggers();
    }
}
