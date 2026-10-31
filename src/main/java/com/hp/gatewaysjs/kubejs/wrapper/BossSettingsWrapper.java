package com.hp.gatewaysjs.kubejs.wrapper;

import dev.shadowsoffire.gateways.gate.BossEventSettings;

/**
 * BossEventSettings的JavaScript包装类
 */
public class BossSettingsWrapper {

    private final BossEventSettings settings;

    public BossSettingsWrapper(BossEventSettings settings) {
        this.settings = settings;
    }

    /**
     * 是否显示为Boss血条
     */
    public boolean isDrawAsBar() {
        return settings.drawAsBar();
    }

    /**
     * 是否生成雾效
     */
    public boolean hasFog() {
        return settings.fog();
    }
}


