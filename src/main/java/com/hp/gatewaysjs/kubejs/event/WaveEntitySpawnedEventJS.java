package com.hp.gatewaysjs.kubejs.event;

import com.hp.gatewaysjs.kubejs.wrapper.GatewayEntityWrapper;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * 波次实体生成事件JS包装类
 */
public class WaveEntitySpawnedEventJS implements KubeEvent {

    private final GateEvent.WaveEntitySpawned event;
    private GatewayEntityWrapper entityWrapper;

    public WaveEntitySpawnedEventJS(GateEvent.WaveEntitySpawned event) {
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
            Identifier id = GatewayRegistry.INSTANCE.getKey(gateway);
            return id != null ? id.toString() : "unknown";
        }
        return "unknown";
    }

    /**
     * 获取当前波次索引
     * @return 当前波次
     */
    public int getWave() {
        return event.getEntity().getWave();
    }

    /**
     * 获取生成的波次实体
     * @return 生成的活体实体
     */
    public LivingEntity getWaveEntity() {
        return event.getWaveEntity();
    }

    /**
     * 获取生成实体的类型ID
     * @return 实体类型ID，如 "minecraft:zombie"
     */
    public String getWaveEntityId() {
        LivingEntity entity = event.getWaveEntity();
        return entity != null
            ? entity.getType().builtInRegistryHolder().key().identifier().toString()
            : "unknown";
    }

    /**
     * 获取生成实体的显示名称
     * @return 实体显示名称
     */
    public String getWaveEntityName() {
        LivingEntity entity = event.getWaveEntity();
        return entity != null
            ? entity.getName().getString()
            : "Unknown Entity";
    }
}


