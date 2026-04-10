package com.hp.gatewaysjs.kubejs;

import com.hp.gatewaysjs.kubejs.binding.GatewayAPI;
import com.hp.gatewaysjs.kubejs.event.GatewaysEvents;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/**
 * GatewaysJS 的 KubeJS 插件类
 * 负责注册事件、绑定工具类、暴露类到JavaScript环境
 */
public class GatewaysJSPlugin extends KubeJSPlugin {

    /**
     * 插件初始化时调用
     * 注册Forge事件监听器，监听Gateway事件并转换为KubeJS事件
     */
    @Override
    public void init() {
        // 注册Forge事件监听器，将Gateway事件桥接到KubeJS
        GatewayEventHandler.register();
    }

    /**
     * 注册KubeJS事件组
     */
    @Override
    public void registerEvents() {
        // 注册Gateways事件组
        GatewaysEvents.GROUP.register();
    }

    /**
     * 注册JavaScript绑定（全局变量/工具类）
     */
    @Override
    public void registerBindings(BindingsEvent event) {
        // 注册Gateway工具类，在JS中通过 Gateway.xxx 访问
        event.add("Gateway", GatewayAPI.class);
    }

    /**
     * 注册允许在JavaScript中访问的类
     */
    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        // 允许访问包装类
        filter.allow("com.hp.gatewaysjs.kubejs.wrapper");
        filter.allow("com.hp.gatewaysjs.kubejs.binding");
        filter.allow("com.hp.gatewaysjs.kubejs.builder");

        // 允许访问Gateway核心类（只读访问）
        filter.allow("dev.shadowsoffire.gateways.entity.GatewayEntity");
        filter.allow("dev.shadowsoffire.gateways.gate.Gateway");
        filter.allow("dev.shadowsoffire.gateways.gate.Wave");
        filter.allow("dev.shadowsoffire.gateways.gate.WaveEntity");
        filter.allow("dev.shadowsoffire.gateways.gate.Reward");
        filter.allow("dev.shadowsoffire.gateways.gate.GateRules");
        filter.allow("dev.shadowsoffire.gateways.gate.GatewayRegistry");
        filter.allow("dev.shadowsoffire.gateways.event.GateEvent");
    }
}
