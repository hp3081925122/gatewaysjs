package com.hp.gatewaysjs.util;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * 自定义网关纹理管理器。
 * <p>
 * 负责存储和查询每个网关的自定义纹理路径。
 * 由于 KubeJS 脚本在服务端和客户端都会执行，
 * 因此可以在双方注册，客户端渲染时自动使用自定义纹理。
 * </p>
 */
public class GatewayCustomTextureManager {

    public static final GatewayCustomTextureManager INSTANCE = new GatewayCustomTextureManager();

    private final ConcurrentHashMap<ResourceLocation, GatewayRenderData> textureMap = new ConcurrentHashMap<>();

    private GatewayCustomTextureManager() {}

    /**
     * 设置指定网关的自定义纹理数据
     */
    public void setTextureData(ResourceLocation gateId, GatewayRenderData data) {
        if (data != null && (data.getEntityTexture() != null || data.getBossBarTexture() != null)) {
            textureMap.put(gateId, data);
        }
    }

    /**
     * 移除指定网关的自定义纹理数据
     */
    public void removeTextureData(ResourceLocation gateId) {
        textureMap.remove(gateId);
    }

    /**
     * 获取指定网关的全部自定义纹理数据
     */
    @Nullable
    public GatewayRenderData getTextureData(ResourceLocation gateId) {
        return textureMap.get(gateId);
    }

    /**
     * 获取指定网关的自定义实体纹理
     */
    @Nullable
    public ResourceLocation getEntityTexture(ResourceLocation gateId) {
        GatewayRenderData data = textureMap.get(gateId);
        return data != null ? data.getEntityTexture() : null;
    }

    /**
     * 从网关实体获取自定义实体纹理
     */
    @Nullable
    public ResourceLocation getEntityTexture(GatewayEntity entity) {
        if (!entity.isValid()) return null;
        ResourceLocation gateId = GatewayRegistry.INSTANCE.getKey(entity.getGateway());
        return gateId != null ? getEntityTexture(gateId) : null;
    }

    /**
     * 获取指定网关的自定义Boss条纹理
     */
    @Nullable
    public ResourceLocation getBossBarTexture(ResourceLocation gateId) {
        GatewayRenderData data = textureMap.get(gateId);
        return data != null ? data.getBossBarTexture() : null;
    }

    /**
     * 从网关实体获取自定义Boss条纹理
     */
    @Nullable
    public ResourceLocation getBossBarTexture(GatewayEntity entity) {
        if (!entity.isValid()) return null;
        ResourceLocation gateId = GatewayRegistry.INSTANCE.getKey(entity.getGateway());
        return gateId != null ? getBossBarTexture(gateId) : null;
    }

    /**
     * 清空所有自定义纹理数据（通常在服务器关闭时调用）
     */
    public void clear() {
        textureMap.clear();
    }
}
