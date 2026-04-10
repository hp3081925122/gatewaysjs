package com.hp.gatewaysjs.kubejs.wrapper;

import dev.shadowsoffire.gateways.gate.Wave;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveEntity.StandardWaveEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave的JavaScript包装类
 */
public class WaveWrapper {

    private final Wave wave;

    public WaveWrapper(Wave wave) {
        this.wave = wave;
    }

    /**
     * 获取原始Wave对象
     */
    public Wave getNative() {
        return wave;
    }

    /**
     * 获取最大波次时间
     */
    public int getMaxWaveTime() {
        return wave.maxWaveTime();
    }

    /**
     * 获取准备时间
     */
    public int getSetupTime() {
        return wave.setupTime();
    }

    /**
     * 获取实体数量
     */
    public int getEntityCount() {
        return wave.entities().size();
    }

    /**
     * 获取奖励数量
     */
    public int getRewardCount() {
        return wave.rewards().size();
    }

    /**
     * 获取修改器数量
     */
    public int getModifierCount() {
        return wave.modifiers().size();
    }

    /**
     * 获取所有波次实体类型ID列表
     * 注意：仅支持 StandardWaveEntity 类型
     */
    public List<String> getEntityTypes() {
        List<String> types = new ArrayList<>();
        for (WaveEntity we : wave.entities()) {
            if (we instanceof StandardWaveEntity swe) {
                // StandardWaveEntity 的 type 字段是 protected，需要通过描述获取
                types.add(swe.getDescription().getString());
            } else {
                types.add("unknown");
            }
        }
        return types;
    }

    /**
     * 获取所有波次实体的数量列表
     */
    public List<Integer> getEntityCounts() {
        List<Integer> counts = new ArrayList<>();
        for (WaveEntity we : wave.entities()) {
            counts.add(we.getCount());
        }
        return counts;
    }

    @Override
    public String toString() {
        return "WaveWrapper{" +
            "maxWaveTime=" + getMaxWaveTime() +
            ", setupTime=" + getSetupTime() +
            ", entities=" + getEntityCount() +
            ", rewards=" + getRewardCount() +
            '}';
    }
}
