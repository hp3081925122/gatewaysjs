package com.hp.gatewaysjs.kubejs.event;

import com.hp.gatewaysjs.kubejs.wrapper.GatewayEntityWrapper;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * 波次结束事件JS包装类
 */
@SuppressWarnings("removal")
public class WaveEndedEventJS implements KubeEvent {

    private final GateEvent.WaveEnd event;
    private GatewayEntityWrapper entityWrapper;

    public WaveEndedEventJS(GateEvent.WaveEnd event) {
        this.event = event;
    }

    public GatewayEntityWrapper getEntity() {
        if (entityWrapper == null) {
            entityWrapper = new GatewayEntityWrapper(event.getEntity());
        }
        return entityWrapper;
    }

    public String getGatewayId() {
        GatewayEntity entity = event.getEntity();
        Gateway gateway = entity.getGateway();
        if (gateway != null) {
            ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
            return id != null ? id.toString() : "unknown";
        }
        return "unknown";
    }

    /**
     * 获取刚完成的波次索引（从0开始）
     * @return 已完成的波次
     */
    public int getWave() {
        return event.getEntity().getWave();
    }

    /**
     * 直接在传送门实体位置生成掉落物
     *
     * @param itemId 物品ID，如 "minecraft:diamond"
     * @param count 数量
     */
    public void addReward(String itemId, int count) {
        GatewayEntity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel serverLevel) {
            ItemStack stack = new ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId))
            );
            stack.setCount(count);
            // 直接在世界中生成物品实体
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                serverLevel,
                entity.getX(),
                entity.getY() + 1.0,
                entity.getZ(),
                stack
            );
            serverLevel.addFreshEntity(itemEntity);
        }
    }
}
