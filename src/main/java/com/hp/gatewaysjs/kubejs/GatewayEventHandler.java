package com.hp.gatewaysjs.kubejs;

import com.hp.gatewaysjs.mixin.GatewayEntityAccessor;
import com.hp.gatewaysjs.kubejs.event.GateOpenedEventJS;
import com.hp.gatewaysjs.kubejs.event.GateCompletedEventJS;
import com.hp.gatewaysjs.kubejs.event.GateFailedEventJS;
import com.hp.gatewaysjs.kubejs.event.WaveStartedEventJS;
import com.hp.gatewaysjs.kubejs.event.WaveEndedEventJS;
import com.hp.gatewaysjs.kubejs.event.WaveEntitySpawnedEventJS;
import com.hp.gatewaysjs.kubejs.event.GatewaysEvents;
import com.hp.gatewaysjs.util.DynamicGatewayManager;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.event.GateEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge事件处理器
 * 监听Gateway模组的NeoForge事件，转换为KubeJS事件
 */
public class GatewayEventHandler {

    /**
     * 注册事件监听器到NeoForge事件总线
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(GatewayEventHandler.class);
    }

    /**
     * 监听传送门开启事件
     */
    @SubscribeEvent
    public static void onGateOpened(GateEvent.Opened event) {
        applyDynamicDisplayName(event.getEntity());
        if (GatewaysEvents.OPENED.hasListeners()) {
            GatewaysEvents.OPENED.post(new GateOpenedEventJS(event));
        }
    }

    /**
     * 监听传送门完成事件
     */
    @SubscribeEvent
    public static void onGateCompleted(GateEvent.Completed event) {
        if (GatewaysEvents.COMPLETED.hasListeners()) {
            GatewaysEvents.COMPLETED.post(new GateCompletedEventJS(event));
        }
    }

    /**
     * 监听传送门失败事件
     */
    @SubscribeEvent
    public static void onGateFailed(GateEvent.Failed event) {
        if (GatewaysEvents.FAILED.hasListeners()) {
            GatewaysEvents.FAILED.post(new GateFailedEventJS(event));
        }
    }

    /**
     * 监听波次开始事件
     */
    @SubscribeEvent
    public static void onWaveStarted(GateEvent.WaveStarted event) {
        applyDynamicDisplayName(event.getEntity());
        if (GatewaysEvents.WAVE_STARTED.hasListeners()) {
            GatewaysEvents.WAVE_STARTED.post(new WaveStartedEventJS(event));
        }
    }

    /**
     * 监听波次结束事件
     */
    @SubscribeEvent
    public static void onWaveEnded(GateEvent.WaveEnd event) {
        if (GatewaysEvents.WAVE_ENDED.hasListeners()) {
            GatewaysEvents.WAVE_ENDED.post(new WaveEndedEventJS(event));
        }
    }

    /**
     * 监听波次实体生成事件
     */
    @SubscribeEvent
    public static void onWaveEntitySpawned(GateEvent.WaveEntitySpawned event) {
        if (GatewaysEvents.WAVE_ENTITY_SPAWNED.hasListeners()) {
            GatewaysEvents.WAVE_ENTITY_SPAWNED.post(new WaveEntitySpawnedEventJS(event));
        }
    }

    private static void applyDynamicDisplayName(GatewayEntity entity) {
        Identifier id = ((GatewayEntityAccessor) entity).getGate().getId();
        String displayName = DynamicGatewayManager.getDisplayName(id);
        if (displayName == null || displayName.isEmpty()) {
            return;
        }

        Component name = Component.literal(displayName);
        entity.setCustomName(name);
    }
}

