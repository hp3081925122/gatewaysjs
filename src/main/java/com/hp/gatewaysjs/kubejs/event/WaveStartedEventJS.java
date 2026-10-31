package com.hp.gatewaysjs.kubejs.event;

import com.hp.gatewaysjs.kubejs.wrapper.GatewayEntityWrapper;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.resources.Identifier;

/**
 * 波次开始事件JS包装类
 */
public class WaveStartedEventJS implements KubeEvent {

    private final GateEvent.WaveStarted event;
    private GatewayEntityWrapper entityWrapper;

    public WaveStartedEventJS(GateEvent.WaveStarted event) {
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
     * 获取当前波次索引（从0开始）
     * @return 当前波次
     */
    public int getWave() {
        return event.getEntity().getWave();
    }

    /**
     * 获取当前波次的敌人数量
     * @return 敌人数量
     */
    public int getActiveEnemies() {
        return event.getEntity().getActiveEnemies();
    }
}

