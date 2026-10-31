package com.hp.gatewaysjs.kubejs.event;

import com.hp.gatewaysjs.kubejs.wrapper.GatewayEntityWrapper;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.resources.Identifier;

/**
 * 传送门开启事件JS包装类
 *
 * JavaScript使用示例:
 * <pre>
 * GatewaysEvents.opened(event => {
 *     console.info("传送门开启!");
 *     console.info("传送门ID: " + event.gatewayId);
 *     console.info("召唤者: " + event.entity.summonerName);
 *     console.info("位置: " + event.entity.position);
 * });
 *
 * // 按传送门ID过滤
 * GatewaysEvents.opened("minecraft:example_gate", event => {
 *     // 只处理特定传送门
 * });
 * </pre>
 */
public class GateOpenedEventJS implements KubeEvent {

    private final GateEvent.Opened event;
    private GatewayEntityWrapper entityWrapper;

    public GateOpenedEventJS(GateEvent.Opened event) {
        this.event = event;
    }

    /**
     * 获取传送门实体包装对象
     * @return 传送门实体包装
     */
    public GatewayEntityWrapper getEntity() {
        if (entityWrapper == null) {
            entityWrapper = new GatewayEntityWrapper(event.getEntity());
        }
        return entityWrapper;
    }

    /**
     * 获取传送门ID
     * @return 传送门的资源定位符
     */
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

