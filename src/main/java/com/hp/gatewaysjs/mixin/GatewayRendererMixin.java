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
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GatewayRenderer.class)
public class GatewayRendererMixin {

    private static final ThreadLocal<Identifier> gatewaysjs$customTexture = new ThreadLocal<>();
    private static final Map<GatewayRenderState, Identifier> gatewaysjs$stateTextures = Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(method = "extractRenderState(Ldev/shadowsoffire/gateways/entity/GatewayEntity;Ldev/shadowsoffire/gateways/client/GatewayRenderState;F)V", at = @At("TAIL"), remap = false)
    private void gatewaysjs$extractCustomTexture(GatewayEntity entity, GatewayRenderState state, float partialTick, CallbackInfo ci) {
        gatewaysjs$stateTextures.put(state, GatewayCustomTextureManager.INSTANCE.getEntityTexture(entity));
    }

    @Inject(method = "submit(Ldev/shadowsoffire/gateways/client/GatewayRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), remap = false)
    private void gatewaysjs$beginSubmit(GatewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        gatewaysjs$customTexture.set(gatewaysjs$stateTextures.get(state));
    }

    @Inject(method = "submit(Ldev/shadowsoffire/gateways/client/GatewayRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("RETURN"), remap = false)
    private void gatewaysjs$endSubmit(GatewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        gatewaysjs$customTexture.remove();
        gatewaysjs$stateTextures.remove(state);
    }
    @ModifyArg(method = "submit(Ldev/shadowsoffire/gateways/client/GatewayRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V"), index = 1, remap = false)
    private RenderType gatewaysjs$redirectEntityTexture(RenderType renderType) {
        Identifier custom = gatewaysjs$customTexture.get();
        return custom != null ? RenderTypes.entityCutout(custom) : renderType;
    }
}






