package com.hp.gatewaysjs.kubejs.wrapper;

import dev.shadowsoffire.gateways.gate.Reward;

/**
 * Reward的JavaScript包装类
 */
public class RewardWrapper {

    private final Reward reward;

    public RewardWrapper(Reward reward) {
        this.reward = reward;
    }

    /**
     * 获取原始Reward对象
     */
    public Reward getNative() {
        return reward;
    }
}


