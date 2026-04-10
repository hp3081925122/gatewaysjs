package com.hp.gatewaysjs.kubejs.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * Gateways事件组定义
 * 所有Gateway相关事件都通过此事件组注册
 *
 * JavaScript使用示例:
 * <pre>
 * // 监听传送门开启事件
 * GatewaysEvents.opened(event => {
 *     console.info("传送门开启: " + event.gatewayId);
 * });
 *
 * // 监听波次开始事件
 * GatewaysEvents.waveStarted(event => {
 *     console.info("波次开始: " + event.wave);
 * });
 * </pre>
 */
public interface GatewaysEvents {

    /**
     * 事件组 - GatewaysEvents
     */
    EventGroup GROUP = EventGroup.of("GatewaysEvents");

    /**
     * 传送门开启事件
     * 当传送门被成功开启时触发
     */
    EventHandler OPENED = GROUP.server("opened", () -> GateOpenedEventJS.class);

    /**
     * 传送门完成事件
     * 当传送门挑战成功完成时触发
     */
    EventHandler COMPLETED = GROUP.server("completed", () -> GateCompletedEventJS.class);

    /**
     * 传送门失败事件
     * 当传送门挑战失败时触发
     */
    EventHandler FAILED = GROUP.server("failed", () -> GateFailedEventJS.class);

    /**
     * 波次开始事件
     * 当一波怪物开始生成时触发
     */
    EventHandler WAVE_STARTED = GROUP.server("waveStarted", () -> WaveStartedEventJS.class);

    /**
     * 波次结束事件
     * 当一波怪物被全部清除时触发
     */
    EventHandler WAVE_ENDED = GROUP.server("waveEnded", () -> WaveEndedEventJS.class);

    /**
     * 波次实体生成事件
     * 当波次中生成单个实体时触发
     */
    EventHandler WAVE_ENTITY_SPAWNED = GROUP.server("waveEntitySpawned", () -> WaveEntitySpawnedEventJS.class);
}
