package org.cairnserver.mixin.server;

import net.minecraft.server.Main;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {
    @Shadow @Final
    private static Logger LOGGER;

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Eula;<init>(Ljava/nio/file/Path;)V"))
    private static void onEulaNew(String[] args, CallbackInfo info) {
        LOGGER.info("Cairn Server正在启动");
    }
}
