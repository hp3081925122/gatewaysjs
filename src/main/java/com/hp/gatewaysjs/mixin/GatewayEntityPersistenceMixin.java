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
 * 在网关实体校验注册表 holder 前恢复已保存的动态网关定义。
 */
@Mixin(GatewayEntity.class)
public class GatewayEntityPersistenceMixin {

    // 在原加载逻辑检查前，从世界存档恢复缺失的运行时网关。
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"), remap = false)
    private void gatewaysjs$restoreDynamicGateway(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("gate")) {
            DynamicGatewayManager.ensureGatewayAvailable(ResourceLocation.parse(tag.getString("gate")));
        }
    }
}
