package com.hp.gatewaysjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hp.gatewaysjs.util.BossBarRenderConfig;
import com.hp.gatewaysjs.util.GatewayCustomTextureManager;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.shadowsoffire.gateways.client.NormalGateClient;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 混入 {@link NormalGateClient}，为Boss条渲染提供完全自定义的纹理支持。
 */
@OnlyIn(Dist.CLIENT)
@Mixin(NormalGateClient.class)
public class NormalGateClientMixin {

    private static final ThreadLocal<BossBarRenderConfig> CUSTOM_BOSS_BAR_CONFIG = new ThreadLocal<>();

    // NormalGateClient.renderBossBar 中硬编码的原始 v 坐标：
    // 背景使用 v=6*5*2=60，填充使用 v=6*5*2+5=65
    private static final float BG_V_ORIGINAL = 60.0F;
    private static final float FILL_V_ORIGINAL = 65.0F;

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
     * 通过原始 blit 调用的 v 坐标区分背景(v=60)和填充(v=65)。
     */
    @Redirect(
        method = "renderBossBar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"),
        remap = true)
    private static void gatewaysjs$redirectBlit(GuiGraphics gfx, ResourceLocation tex,
            int x, int y, float u, float v, int w, int h, int tw, int th) {
        BossBarRenderConfig config = CUSTOM_BOSS_BAR_CONFIG.get();
        if (config != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (v == BG_V_ORIGINAL) {
                gfx.blit(config.texture(), x, y, config.bgU(), config.bgV(),
                        config.barWidth(), config.barHeight(),
                        config.texWidth(), config.texHeight());
            } else if (v == FILL_V_ORIGINAL) {
                gfx.blit(config.getActualProgressTexture(), x, y, config.fillU(), config.fillV(),
                        w, config.barHeight(),
                        config.texWidth(), config.texHeight());
            } else {
                gfx.blit(tex, x, y, u, v, w, h, tw, th);
            }
        } else {
            gfx.blit(tex, x, y, u, v, w, h, tw, th);
        }
    }
}
