package com.hp.gatewaysjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.placebo.reload.DynamicHolder;

/**
 * 访问器混入，暴露 {@link GatewayEntity} 中受保护的 {@code gate} 字段。
 * <p>
 * 通过 {@link DynamicHolder#getId()} 可以直接获取网关的 {@link net.minecraft.resources.ResourceLocation} ID，
 * 避免通过 {@link dev.shadowsoffire.gateways.gate.GatewayRegistry GatewayRegistry} 的 {@code getKey()} 做值反向查找，
 * 从而解决注册表重建后值引用失效导致的自定义纹理配置查找不到的问题。
 * </p>
 */
@Mixin(value = GatewayEntity.class, remap = false)
public interface GatewayEntityAccessor {

    @Accessor(value = "gate", remap = false)
    DynamicHolder<? extends Gateway> getGate();
}
