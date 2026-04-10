package com.hp.gatewaysjs.kubejs.event;

import com.hp.gatewaysjs.kubejs.wrapper.GatewayEntityWrapper;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * 传送门失败事件JS包装类
 */
public class GateFailedEventJS extends EventJS {

    private final GateEvent.Failed event;
    private GatewayEntityWrapper entityWrapper;

    public GateFailedEventJS(GateEvent.Failed event) {
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
     * 获取失败原因
     * @return 失败原因枚举名称
     */
    public String getFailureReason() {
        return event.getEntity().getFailureReason() != null
            ? event.getEntity().getFailureReason().name()
            : "UNKNOWN";
    }

    /**
     * 获取失败原因的本地化描述
     * @return 失败原因的本地化文本
     */
    public String getFailureReasonText() {
        return event.getEntity().getFailureReason() != null
            ? event.getEntity().getFailureReason().getMsg().getString()
            : "Unknown failure reason";
    }
}
