package com.hp.gatewaysjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hp.gatewaysjs.util.GatewayCustomTextureManager;

import dev.shadowsoffire.gateways.client.NormalGateClient;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 混入 {@link NormalGateClient}，为Boss条渲染提供自定义纹理支持。
 * <p>
 * 原版Boss条使用写死的 {@code textures/gui/bars.png}，
 * 此混入允许每个网关使用不同的Boss条纹理。
 * </p>
 * <p>
 * 实现方式：在 {@link NormalGateClient#renderBossBar} 的 HEAD 处捕获自定义纹理
 * 到 {@link ThreadLocal}，然后通过 {@link Redirect} 将所有 {@code blit} 调用
 * 重定向到使用自定义纹理的版本。
 * </p>
 */
@OnlyIn(Dist.CLIENT)
@Mixin(NormalGateClient.class)
public class NormalGateClientMixin {

    private static final ThreadLocal<ResourceLocation> CUSTOM_BOSS_BAR_TEXTURE = new ThreadLocal<>();

    /**
     * 在 renderBossBar 执行前，将当前实体对应的自定义Boss条纹理存入ThreadLocal。
     */
    @Inject(method = "renderBossBar", at = @At("HEAD"), remap = false)
    private static void gatewaysjs$captureBossBarTexture(GatewayEntity gateEntity, Object guiGfx,
            int x, int y, boolean isInWorld, CallbackInfo ci) {
        ResourceLocation custom = GatewayCustomTextureManager.INSTANCE.getBossBarTexture(gateEntity);
        CUSTOM_BOSS_BAR_TEXTURE.set(custom);
    }

    /**
     * 在 renderBossBar 执行完毕后清理ThreadLocal。
     */
    @Inject(method = "renderBossBar", at = @At("RETURN"), remap = false)
    private static void gatewaysjs$cleanupBossBarTexture(CallbackInfo ci) {
        CUSTOM_BOSS_BAR_TEXTURE.remove();
    }

    /**
     * 将 renderBossBar 中所有匹配的 blit 调用重定向，替换纹理参数。
     * <p>
     * 如果有自定义纹理，则使用自定义纹理调用原方法；否则使用原始纹理。
     * </p>
     */
    @Redirect(
        method = "renderBossBar",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"),
        remap = true)
    private static void gatewaysjs$redirectBlit(GuiGraphics gfx, ResourceLocation tex,
            int x, int y, float u, float v, int w, int h, int tw, int th) {
        ResourceLocation custom = CUSTOM_BOSS_BAR_TEXTURE.get();
        gfx.blit(custom != null ? custom : tex, x, y, u, v, w, h, tw, th);
    }
}
