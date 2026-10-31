package com.hp.gatewaysjs.kubejs.wrapper;

import dev.shadowsoffire.gateways.gate.GateRules;

/**
 * GateRules的JavaScript包装类
 */
public class GateRulesWrapper {

    private final GateRules rules;

    public GateRulesWrapper(GateRules rules) {
        this.rules = rules;
    }

    /**
     * 获取生成范围
     */
    public double getSpawnRange() {
        return rules.spawnRange();
    }

    /**
     * 获取牵引范围
     */
    public double getLeashRange() {
        return rules.leashRange();
    }

    /**
     * 是否允许丢弃
     */
    public boolean isAllowDiscarding() {
        return rules.allowDiscarding();
    }

    /**
     * 是否允许跨维度
     */
    public boolean isAllowDimChange() {
        return rules.allowDimChange();
    }

    /**
     * 是否只允许玩家伤害
     */
    public boolean isPlayerDamageOnly() {
        return rules.playerDamageOnly();
    }

    /**
     * 失败时是否移除实体
     */
    public boolean isRemoveOnFailure() {
        return rules.removeOnFailure();
    }

    /**
     * 超出范围是否失败
     */
    public boolean isFailOnOutOfBounds() {
        return rules.failOnOutOfBounds();
    }

    /**
     * 获取传送门最小间距
     */
    public double getSpacing() {
        return rules.spacing();
    }

    /**
     * 获取跟随范围加成
     */
    public double getFollowRangeBoost() {
        return rules.followRangeBoost();
    }

    /**
     * 获取默认掉落几率
     */
    public float getDefaultDropChance() {
        return rules.defaultDropChance();
    }
}

