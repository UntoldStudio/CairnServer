package org.cairnserver.mixin.server;

import net.minecraft.server.MinecraftServer;
import org.cairnserver.event.EventBus;
import org.cairnserver.event.events.server.ServerStartedEvent;
import org.cairnserver.event.events.server.ServerStopingEvent;
import org.cairnserver.plugin.PluginManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Unique
    private static boolean cairn$pluginsLoaded = false;

    @Inject(method = "runServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;isReady:Z", opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void onServerReady(CallbackInfo callbackInfo) {
        if (!cairn$pluginsLoaded) {
            cairn$pluginsLoaded = true;
            PluginManager.enableAllPlugins();

            EventBus.post(new ServerStartedEvent());
        }
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onServerStop(CallbackInfo callbackInfo) {
        EventBus.post(new ServerStopingEvent());
        PluginManager.disableAllPlugins();
    }
}
