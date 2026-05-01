package com.hp.gatewaysjs.util;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * 存储自定义网关渲染数据（实体材质路径和Boss条材质路径）
 */
public class GatewayRenderData {

    @Nullable
    private final ResourceLocation entityTexture;

    @Nullable
    private final ResourceLocation bossBarTexture;

    public GatewayRenderData(@Nullable ResourceLocation entityTexture, @Nullable ResourceLocation bossBarTexture) {
        this.entityTexture = entityTexture;
        this.bossBarTexture = bossBarTexture;
    }

    /**
     * @return 自定义实体材质路径，未设置返回null
     */
    @Nullable
    public ResourceLocation getEntityTexture() {
        return entityTexture;
    }

    /**
     * @return 自定义Boss条材质路径，未设置返回null
     */
    @Nullable
    public ResourceLocation getBossBarTexture() {
        return bossBarTexture;
    }
}
