package com.hp.gatewaysjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hp.gatewaysjs.util.GatewayCustomTextureManager;

import dev.shadowsoffire.gateways.client.GatewayRenderer;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * 混入 {@link GatewayRenderer}，为每个网关实体提供自定义纹理支持。
 * <p>
 * 在 {@link GatewayRenderer#getTextureLocation(GatewayEntity)} 中注入，
 * 如果该网关有自定义纹理则返回自定义纹理，否则使用默认纹理。
 * </p>
 */
@Mixin(GatewayRenderer.class)
public class GatewayRendererMixin {

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true, remap = false)
    private void gatewaysjs$getTextureLocation(GatewayEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation custom = GatewayCustomTextureManager.INSTANCE.getEntityTexture(entity);
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
