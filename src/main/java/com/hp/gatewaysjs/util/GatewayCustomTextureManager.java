package com.hp.gatewaysjs.util;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.hp.gatewaysjs.mixin.GatewayEntityAccessor;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.resources.Identifier;

/**
 * 自定义网关纹理管理器。
 * <p>
 * 负责存储和查询每个网关的自定义纹理路径和Boss条渲染配置。
 * 由于 KubeJS 脚本在服务端和客户端都会执行，
 * 因此可以在双方注册，客户端渲染时自动使用自定义纹理。
 * </p>
 */
public class GatewayCustomTextureManager {

    public static final GatewayCustomTextureManager INSTANCE = new GatewayCustomTextureManager();

    private final ConcurrentHashMap<Identifier, GatewayRenderData> textureMap = new ConcurrentHashMap<>();

    private GatewayCustomTextureManager() {}

    /**
     * 设置指定网关的自定义纹理数据
     */
    public void setTextureData(Identifier gateId, GatewayRenderData data) {
        if (data != null && (data.getEntityTexture() != null || data.getBossBarConfig() != null)) {
            textureMap.put(gateId, data);
        }
    }

    /**
     * 移除指定网关的自定义纹理数据
     */
    public void removeTextureData(Identifier gateId) {
        textureMap.remove(gateId);
    }

    /**
     * 获取指定网关的全部自定义纹理数据
     */
    @Nullable
    public GatewayRenderData getTextureData(Identifier gateId) {
        return textureMap.get(gateId);
    }

    /**
     * 获取指定网关的自定义实体纹理
     */
    @Nullable
    public Identifier getEntityTexture(Identifier gateId) {
        GatewayRenderData data = textureMap.get(gateId);
        return data != null ? data.getEntityTexture() : null;
    }

    /**
     * 从网关实体获取自定义实体纹理
     */
    @Nullable
    public Identifier getEntityTexture(GatewayEntity entity) {
        if (!entity.isValid()) return null;
        Identifier gateId = getGatewayId(entity);
        return gateId != null ? getEntityTexture(gateId) : null;
    }

    /**
     * 获取指定网关的自定义Boss条渲染配置
     */
    @Nullable
    public BossBarRenderConfig getBossBarConfig(Identifier gateId) {
        GatewayRenderData data = textureMap.get(gateId);
        return data != null ? data.getBossBarConfig() : null;
    }

    /**
     * 从网关实体获取自定义Boss条渲染配置
     */
    @Nullable
    public BossBarRenderConfig getBossBarConfig(GatewayEntity entity) {
        if (!entity.isValid()) return null;
        Identifier gateId = getGatewayId(entity);
        return gateId != null ? getBossBarConfig(gateId) : null;
    }

    /**
     * 从网关实体中提取网关的 {@link Identifier} ID。
     * <p>
     * 优先通过注册表值反向查找；若查找失败（注册表重建后缓存引用失效），
     * 则通过 {@link GatewayEntityAccessor} 直接获取实体 {@link DynamicHolder} 中保存的ID。
     * </p>
     */
    @Nullable
    private static Identifier getGatewayId(GatewayEntity entity) {
        // 主方案：注册表值反向查找（适用于大多数情况）
        Identifier id = GatewayRegistry.INSTANCE.getKey(entity.getGateway());
        if (id != null) return id;

        // 后备方案：直接获取实体 DynamicHolder 中的 ID
        // 解决 RegistryHelper 替换 registry BiMap 后，旧缓存值引用失效的问题
        DynamicHolder<? extends Gateway> holder = ((GatewayEntityAccessor) entity).getGate();
        return holder.getId();
    }

    /**
     * 清空所有自定义纹理数据（通常在服务器关闭时调用）
     */
    public void clear() {
        textureMap.clear();
    }
}

