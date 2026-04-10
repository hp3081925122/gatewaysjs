package com.hp.gatewaysjs;

import com.mojang.logging.LogUtils;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * 后续修改如果已经注册了传送门珍珠，则删除已注册传送门珍珠，然后再注册新的
 * rules部分还没塞
 * 动态构建传送门珍珠返回传送门珍珠实体实例
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

    public Gatewaysjs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册到Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("GatewaysJS 加载完成 - Gateway与KubeJS联动附属");
    }

    /**
     * 获取Logger实例
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}
