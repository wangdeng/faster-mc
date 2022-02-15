package com.wangdeng.fastermc.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilSocialInteractionsService;
import com.wangdeng.fastermc.FasterMc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(YggdrasilSocialInteractionsService.class)
public class YggdrasilSocialInteractionsServiceInject {
    @Shadow
    private boolean serversAllowed;

    @Shadow
    private boolean realmsAllowed;

    @Shadow
    private boolean chatAllowed;
    @Shadow
    private boolean telemetryAllowed;

    @Inject(method = "checkPrivileges", at = @At(value = "HEAD"), cancellable = true)
    private void checkPrivilegesOffline(CallbackInfo ci) {
        FasterMc.LOGGER.info("checkPrivilegesOffline start");
        this.serversAllowed=true;
        this.realmsAllowed=true;
        this.chatAllowed=true;
        this.telemetryAllowed=true;
        ci.cancel();
    }
}
