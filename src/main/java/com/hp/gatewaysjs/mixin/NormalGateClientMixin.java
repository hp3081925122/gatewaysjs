package com.hp.gatewaysjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hp.gatewaysjs.util.BossBarRenderConfig;
import com.hp.gatewaysjs.util.GatewayCustomTextureManager;

import dev.shadowsoffire.gateways.client.NormalGateClient;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

/**
 * 混入 {@link NormalGateClient}，为Boss条渲染提供完全自定义的纹理支持。
 */
@Mixin(NormalGateClient.class)
public class NormalGateClientMixin {

    private static final ThreadLocal<BossBarRenderConfig> CUSTOM_BOSS_BAR_CONFIG = new ThreadLocal<>();

    @Inject(method = "renderBossBar", at = @At("HEAD"), remap = false)
    private static void gatewaysjs$captureBossBarConfig(GatewayEntity gateEntity, Object guiGfx,
            int x, int y, boolean isInWorld, CallbackInfo ci) {
        BossBarRenderConfig config = GatewayCustomTextureManager.INSTANCE.getBossBarConfig(gateEntity);
        CUSTOM_BOSS_BAR_CONFIG.set(config);
    }

    @Inject(method = "renderBossBar", at = @At("RETURN"), remap = false)
    private static void gatewaysjs$cleanupBossBarConfig(CallbackInfo ci) {
        CUSTOM_BOSS_BAR_CONFIG.remove();
    }

    /**
     * 渲染 Boss 条背景。
     */
    @Redirect(
        method = "renderBossBar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"),
        remap = false)
    private static void gatewaysjs$redirectBackground(GuiGraphicsExtractor gfx, RenderPipeline pipeline, Identifier tex,
            int x, int y, int width, int height, int color) {
        BossBarRenderConfig config = CUSTOM_BOSS_BAR_CONFIG.get();
        if (config != null && width == 182 && height == 5) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, config.texture(), x, y, (int) config.bgU(), (int) config.bgV(),
                    config.barWidth(), config.barHeight(),
                    config.texWidth(), config.texHeight());
        } else {
            gfx.blitSprite(pipeline, tex, x, y, width, height, color);
        }
    }

    /**
     * 渲染 Boss 条进度。
     */
    @Redirect(
        method = "renderBossBar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V"),
        remap = false)
    private static void gatewaysjs$redirectProgress(GuiGraphicsExtractor gfx, RenderPipeline pipeline, Identifier tex,
            int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height, int color) {
        BossBarRenderConfig config = CUSTOM_BOSS_BAR_CONFIG.get();
        if (config != null && spriteWidth == 182 && spriteHeight == 5 && height == 5) {
            int scaledWidth = Math.max(0, Math.round(width * (config.barWidth() / 182.0F)));
            gfx.blit(RenderPipelines.GUI_TEXTURED, config.getActualProgressTexture(), x, y, (int) config.fillU(), (int) config.fillV(),
                    scaledWidth, config.barHeight(),
                    config.texWidth(), config.texHeight());
        } else {
            gfx.blitSprite(pipeline, tex, spriteWidth, spriteHeight, u, v, x, y, width, height, color);
        }
    }
}



