package com.hp.gatewaysjs.kubejs.wrapper;

import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveEntity.StandardWaveEntity;

/**
 * WaveEntity的JavaScript包装类
 */
public class WaveEntityWrapper {

    private final WaveEntity waveEntity;

    public WaveEntityWrapper(WaveEntity waveEntity) {
        this.waveEntity = waveEntity;
    }

    /**
     * 获取原始WaveEntity对象
     */
    public WaveEntity getNative() {
        return waveEntity;
    }

    /**
     * 获取数量
     */
    public int getCount() {
        return waveEntity.getCount();
    }

    /**
     * 获取描述文本
     */
    public String getDescription() {
        return waveEntity.getDescription().getString();
    }

    /**
     * 是否需要完成生成
     */
    public boolean shouldFinalizeSpawn() {
        return waveEntity.shouldFinalizeSpawn();
    }
}
