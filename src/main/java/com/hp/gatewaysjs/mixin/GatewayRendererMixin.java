package com.hp.gatewaysjs.mixin;

import com.hp.gatewaysjs.util.GatewayCustomTextureManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.shadowsoffire.gateways.client.GatewayRenderState;
import dev.shadowsoffire.gateways.client.GatewayRenderer;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GatewayRenderer.class)
public class GatewayRendererMixin {

    @Unique
    private static final ThreadLocal<Identifier> gatewaysjs$customTexture = new ThreadLocal<>();

    @Inject(method = "extractRenderState(Ldev/shadowsoffire/gateways/entity/GatewayEntity;Ldev/shadowsoffire/gateways/client/GatewayRenderState;F)V", at = @At("TAIL"), remap = false)
    private void gatewaysjs$extractCustomTexture(GatewayEntity entity, GatewayRenderState state, float partialTick, CallbackInfo ci) {
        Identifier custom = GatewayCustomTextureManager.INSTANCE.getEntityTexture(entity);
        ((GatewayRenderStateMixin) (Object) state).gatewaysjs$customTexture = custom;
    }

    @Inject(method = "submit(Ldev/shadowsoffire/gateways/client/GatewayRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), remap = false)
    private void gatewaysjs$beginSubmit(GatewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        gatewaysjs$customTexture.set(((GatewayRenderStateMixin) (Object) state).gatewaysjs$customTexture);
    }

    @Inject(method = "submit(Ldev/shadowsoffire/gateways/client/GatewayRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("RETURN"), remap = false)
    private void gatewaysjs$endSubmit(GatewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        gatewaysjs$customTexture.remove();
    }

    @Redirect(method = "submit(Ldev/shadowsoffire/gateways/client/GatewayRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/RenderType;", ordinal = 0), remap = false)
    private RenderType gatewaysjs$redirectEntityTexture(Identifier texture) {
        Identifier custom = gatewaysjs$customTexture.get();
        return RenderTypes.entityCutout(custom != null ? custom : texture);
    }
}

