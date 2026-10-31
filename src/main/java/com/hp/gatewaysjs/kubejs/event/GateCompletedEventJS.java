package com.hp.gatewaysjs.kubejs.event;

import com.hp.gatewaysjs.kubejs.wrapper.GatewayEntityWrapper;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.resources.Identifier;

/**
 * 传送门完成事件JS包装类
 */
public class GateCompletedEventJS implements KubeEvent {

    private final GateEvent.Completed event;
    private GatewayEntityWrapper entityWrapper;

    public GateCompletedEventJS(GateEvent.Completed event) {
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
     * 获取传送门类型名称
     * @return 传送门类型的可读名称
     */
    public String getGatewayName() {
        return event.getEntity().getCustomName() != null
            ? event.getEntity().getCustomName().getString()
            : getGatewayId();
    }
}

