package com.hp.gatewaysjs.mixin;

import com.hp.gatewaysjs.util.DynamicGatewayManager;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores saved dynamic gateway definitions before gateway entities validate their registry holder.
 */
@Mixin(GatewayEntity.class)
public class GatewayEntityPersistenceMixin {

    // Restores a missing runtime gateway from world save data before the original load logic checks it.
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void gatewaysjs$restoreDynamicGateway(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("gate")) {
            DynamicGatewayManager.ensureGatewayAvailable(ResourceLocation.parse(tag.getString("gate")));
        }
    }
}
