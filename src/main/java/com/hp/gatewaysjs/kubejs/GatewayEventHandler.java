package com.hp.gatewaysjs.kubejs;

import com.hp.gatewaysjs.kubejs.event.GateOpenedEventJS;
import com.hp.gatewaysjs.kubejs.event.GateCompletedEventJS;
import com.hp.gatewaysjs.kubejs.event.GateFailedEventJS;
import com.hp.gatewaysjs.kubejs.event.WaveStartedEventJS;
import com.hp.gatewaysjs.kubejs.event.WaveEndedEventJS;
import com.hp.gatewaysjs.kubejs.event.WaveEntitySpawnedEventJS;
import com.hp.gatewaysjs.kubejs.event.GatewaysEvents;
import dev.shadowsoffire.gateways.event.GateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge事件处理器
 * 监听Gateway模组的Forge事件，转换为KubeJS事件
 */
public class GatewayEventHandler {

    /**
     * 注册事件监听器到Forge事件总线
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(GatewayEventHandler.class);
    }

    /**
     * 监听传送门开启事件
     */
    @SubscribeEvent
    public static void onGateOpened(GateEvent.Opened event) {
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
}
