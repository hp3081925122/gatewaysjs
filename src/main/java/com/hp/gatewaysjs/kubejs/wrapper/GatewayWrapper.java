package com.hp.gatewaysjs.kubejs.wrapper;

import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Gateway配置的JavaScript包装类
 * 提供对传送门配置的查询功能
 */
public class GatewayWrapper {

    private final Gateway gateway;

    public GatewayWrapper(Gateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 获取原始Gateway对象
     */
    public Gateway getNative() {
        return gateway;
    }

    /**
     * 获取传送门ID
     * @return 资源定位符字符串
     */
    public String getId() {
        ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
        return id != null ? id.toString() : "unknown";
    }

    /**
     * 获取传送门尺寸
     * @return 尺寸名称
     */
    public String getSize() {
        return gateway.size().name();
    }

    /**
     * 获取传送门缩放比例
     * @return 缩放比例
     */
    public float getScale() {
        return gateway.size().getScale();
    }

    /**
     * 获取传送门颜色（十六进制）
     * @return 颜色值
     */
    public int getColor() {
        return gateway.color().getValue();
    }

    /**
     * 获取传送门规则
     * @return 规则包装对象
     */
    public GateRulesWrapper getRules() {
        return new GateRulesWrapper(gateway.rules());
    }

    /**
     * 获取Boss事件设置
     * @return Boss设置包装对象
     */
    public BossSettingsWrapper getBossSettings() {
        return new BossSettingsWrapper(gateway.bossSettings());
    }

    @Override
    public String toString() {
        return "GatewayWrapper{" + "id='" + getId() + '\'' + ", size=" + getSize() + '}';
    }
}
