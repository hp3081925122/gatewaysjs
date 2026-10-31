package com.hp.gatewaysjs;

import com.hp.gatewaysjs.util.DynamicGatewayManager;
import com.hp.gatewaysjs.network.GatewaysJSNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 */
/**
 * GatewaysJS - Gateway与KubeJS的联动附属模组
 *
 * 功能：
 * 1. 将Gateway事件包装为KubeJS事件，可在JS脚本中监听
 * 2. 提供Gateway工具类，获取所有传送门ID、动态创建传送门等
 * 3. 提供传送门实体包装类，可在事件中修改传送门属性
 *
 * @see com.hp.gatewaysjs.kubejs.GatewaysJSPlugin KubeJS插件类
 * @see com.hp.gatewaysjs.kubejs.event.GatewaysEvents 事件定义
 */
@SuppressWarnings("removal")
@Mod(Gatewaysjs.MODID)
public class Gatewaysjs {

    public static final String MODID = "gatewaysjs";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Gatewaysjs(IEventBus modEventBus) {
        GatewaysJSNetwork.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(DynamicGatewayManager::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(DynamicGatewayManager::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(DynamicGatewayManager::onServerStarted);
        NeoForge.EVENT_BUS.addListener(DynamicGatewayManager::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(DynamicGatewayManager::onServerStopped);

        LOGGER.info("GatewaysJS 加载完成 - Gateway与KubeJS联动附属");
    }

    /**
     * 获取Logger实例
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}

