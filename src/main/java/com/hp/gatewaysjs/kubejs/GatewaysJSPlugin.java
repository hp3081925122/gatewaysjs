package com.hp.gatewaysjs.kubejs;

import com.hp.gatewaysjs.kubejs.binding.GatewayAPI;
import com.hp.gatewaysjs.kubejs.event.GatewaysEvents;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/**
 * GatewaysJS 的 KubeJS 插件类
 * 负责注册事件、绑定工具类、暴露类到JavaScript环境
 */
public class GatewaysJSPlugin implements KubeJSPlugin {

    /**
     * 插件初始化时调用
     * 注册NeoForge事件监听器，监听Gateway事件并转换为KubeJS事件
     */
    @Override
    public void init() {
        GatewayEventHandler.register();
    }

    /**
     * 注册KubeJS事件组
     */
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GatewaysEvents.GROUP);
    }

    /**
     * 注册JavaScript绑定（全局变量/工具类）
     */
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("Gateway", GatewayAPI.class);
    }

    /**
     * 注册允许在JavaScript中访问的类
     */
    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.hp.gatewaysjs.kubejs.wrapper");
        filter.allow("com.hp.gatewaysjs.kubejs.binding");
        filter.allow("com.hp.gatewaysjs.kubejs.builder");

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
