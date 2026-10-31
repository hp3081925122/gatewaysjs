package com.hp.gatewaysjs.util;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

/**
 * 存储自定义网关渲染数据（实体材质路径和Boss条渲染配置）
 */
public class GatewayRenderData {

    @Nullable
    private final Identifier entityTexture;

    @Nullable
    private final BossBarRenderConfig bossBarConfig;

    /**
     * @param entityTexture 自定义实体纹理路径（null=使用默认）
     * @param bossBarConfig Boss条渲染配置（null=使用默认）
     */
    public GatewayRenderData(@Nullable Identifier entityTexture, @Nullable BossBarRenderConfig bossBarConfig) {
        this.entityTexture = entityTexture;
        this.bossBarConfig = bossBarConfig;
    }

    /**
     * @return 自定义实体材质路径，未设置返回null
     */
    @Nullable
    public Identifier getEntityTexture() {
        return entityTexture;
    }

    /**
     * @return 自定义Boss条渲染配置，未设置返回null
     */
    @Nullable
    public BossBarRenderConfig getBossBarConfig() {
        return bossBarConfig;
    }
}

