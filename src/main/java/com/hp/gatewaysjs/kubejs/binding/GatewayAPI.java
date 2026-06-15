package com.hp.gatewaysjs.kubejs.binding;

import com.hp.gatewaysjs.kubejs.builder.GatewayBuilder;
import com.hp.gatewaysjs.kubejs.wrapper.GatewayWrapper;
import com.hp.gatewaysjs.util.BossBarRenderConfig;
import com.hp.gatewaysjs.util.DynamicGatewayManager;
import com.hp.gatewaysjs.util.GatewayCustomTextureManager;
import com.hp.gatewaysjs.util.GatewayRenderData;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gateway工具类 - 在JavaScript中通过 Gateway.xxx 访问
 *
 * JavaScript使用示例:
 * <pre>
 * // 获取所有传送门ID
 * let ids = Gateway.getAllGatewayIds();
 * console.info("共有 " + ids.length + " 个传送门");
 * ids.forEach(id => console.info(id));
 *
 * // 检查传送门是否存在
 * if (Gateway.exists("minecraft:example_gate")) {
 *     console.info("传送门存在!");
 * }
 *
 * // 获取传送门配置
 * let gate = Gateway.getGateway("minecraft:example_gate");
 * if (gate) {
 *     console.info("尺寸: " + gate.size);
 *     console.info("缩放: " + gate.scale);
 * }
 *
 * // 动态创建传送门
 * let level = event.level; // ServerLevel
 * let player = event.player; // Player
 * let pos = event.block.pos; // BlockPos
 *
 * Gateway.createGateway(level, "minecraft:example_gate", pos, player);
 * </pre>
 */
@SuppressWarnings("removal")
public interface GatewayAPI {

    /**
     * 获取所有已注册的传送门ID列表
     * 返回按尺寸和ID排序的传送门ID
     *
     * @return 传送门ID字符串列表
     */
    static List<String> getAllGatewayIds() {
        List<String> ids = new ArrayList<>();
        GatewayRegistry.INSTANCE.getValues().stream()
            .sorted(Comparator
                .comparing(Gateway::size)
                .thenComparing(g -> GatewayRegistry.INSTANCE.getKey(g).toString()))
            .forEach(gate -> {
                ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gate);
                ids.add(id != null ? id.toString() : "unknown");
            });
        return ids;
    }

    /**
     * 获取所有传送门ID列表（按指定排序）
     *
     * @param sortBy 排序方式: "size"（按尺寸）, "id"（按ID）, "none"（不排序）
     * @return 传送门ID字符串列表
     */
    static List<String> getAllGatewayIds(String sortBy) {
        List<Gateway> gates = new ArrayList<>(GatewayRegistry.INSTANCE.getValues());

        switch (sortBy.toLowerCase()) {
            case "size":
                gates.sort(Comparator.comparing(Gateway::size));
                break;
            case "id":
                gates.sort(Comparator.comparing(g -> GatewayRegistry.INSTANCE.getKey(g).toString()));
                break;
            case "none":
            default:
                // 保持原始顺序
                break;
        }

        List<String> ids = new ArrayList<>();
        for (Gateway gate : gates) {
            ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gate);
            ids.add(id != null ? id.toString() : "unknown");
        }
        return ids;
    }

    /**
     * 检查指定ID的传送门是否存在
     *
     * @param gatewayId 传送门ID，如 "minecraft:example_gate"
     * @return true表示存在
     */
    static boolean exists(String gatewayId) {
        try {
            ResourceLocation loc = ResourceLocation.parse(gatewayId);
            DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(loc);
            return holder.isBound();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取指定ID的传送门配置
     *
     * @param gatewayId 传送门ID
     * @return 传送门包装对象，不存在则返回null
     */
    static GatewayWrapper getGateway(String gatewayId) {
        try {
            ResourceLocation loc = ResourceLocation.parse(gatewayId);
            DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(loc);
            if (holder.isBound()) {
                return new GatewayWrapper(holder.get());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 获取传送门类型
     *
     * @param gatewayId 传送门ID
     * @return 类型名称: "normal", "endless", "unknown"
     */
    static String getGatewayType(String gatewayId) {
        try {
            ResourceLocation loc = ResourceLocation.parse(gatewayId);
            DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(loc);
            if (holder.isBound()) {
                Gateway gate = holder.get();
                if (gate instanceof NormalGateway) {
                    return "normal";
                } else if (gate instanceof EndlessGateway) {
                    return "endless";
                }
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    /**
     * 动态创建传送门实体
     *
     * @param level     世界对象（必须是ServerLevel）
     * @param gatewayId 传送门ID
     * @param x         X坐标
     * @param y         Y坐标
     * @param z         Z坐标
     * @param player    召唤者玩家（可为null，将使用假玩家）
     * @return 创建成功返回true，失败返回false
     */
    static boolean createGateway(Level level, String gatewayId, double x, double y, double z, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        try {
            ResourceLocation loc = ResourceLocation.parse(gatewayId);
            DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(loc);

            if (!holder.isBound()) {
                return false;
            }

            Gateway gate = holder.get();

            // 获取或创建召唤者
            Player summoner = player;
            if (summoner == null) {
                summoner = serverLevel.getNearestPlayer(x, y, z, 50, false);
                if (summoner == null) {
                    summoner = FakePlayerFactory.getMinecraft(serverLevel);
                }
            }

            // 创建传送门实体
            GatewayEntity entity = gate.createEntity(level, summoner);
            entity.setPos(x, y, z);

            // 检查碰撞并自动调整位置
            int yAdjust = 0;
            while (yAdjust < 4) {
                if (!level.noCollision(entity)) {
                    entity.setPos(entity.getX(), entity.getY() + 1, entity.getZ());
                    yAdjust++;
                } else {
                    break;
                }
            }

            // 最终碰撞检测
            if (!level.noCollision(entity)) {
                return false;
            }

            // 生成实体
            level.addFreshEntity(entity);
            entity.onGateCreated();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 动态创建传送门实体（使用BlockPos）
     *
     * @param level     世界对象
     * @param gatewayId 传送门ID
     * @param pos       方块位置
     * @param player    召唤者玩家
     * @return 创建成功返回true
     */
    static boolean createGatewayAtPos(Level level, String gatewayId, BlockPos pos, Player player) {
        return createGateway(level, gatewayId, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
    }

    /**
     * 创建传送门珍珠物品栈
     *
     * @param gatewayId 传送门ID
     * @return 物品栈JSON表示，用于give命令等
     */
    static String createPearlItem(String gatewayId) {
        try {
            ResourceLocation loc = ResourceLocation.parse(gatewayId);
            DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(loc);

            if (!holder.isBound()) {
                return "minecraft:air";
            }

            // 返回带有NBT的物品命令格式
            return "gateways:gate_pearl{gateway:\"" + gatewayId + "\"}";
        } catch (Exception e) {
            return "minecraft:air";
        }
    }

    /**
     * 获取传送门总数
     *
     * @return 已注册的传送门数量
     */
    static int getGatewayCount() {
        return GatewayRegistry.INSTANCE.getValues().size();
    }

    /**
     * 获取所有普通传送门ID
     *
     * @return 普通传送门ID列表
     */
    static List<String> getNormalGatewayIds() {
        List<String> ids = new ArrayList<>();
        for (Gateway gate : GatewayRegistry.INSTANCE.getValues()) {
            if (gate instanceof NormalGateway) {
                ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gate);
                ids.add(id != null ? id.toString() : "unknown");
            }
        }
        return ids;
    }

    /**
     * 获取所有无尽传送门ID
     *
     * @return 无尽传送门ID列表
     */
    static List<String> getEndlessGatewayIds() {
        List<String> ids = new ArrayList<>();
        for (Gateway gate : GatewayRegistry.INSTANCE.getValues()) {
            if (gate instanceof EndlessGateway) {
                ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gate);
                ids.add(id != null ? id.toString() : "unknown");
            }
        }
        return ids;
    }

    /**
     * 按尺寸获取传送门ID列表
     *
     * @param size 尺寸名称: "SMALL", "MEDIUM", "LARGE"
     * @return 对应尺寸的传送门ID列表
     */
    static List<String> getGatewaysBySize(String size) {
        List<String> ids = new ArrayList<>();
        Gateway.Size targetSize;
        try {
            targetSize = Gateway.Size.valueOf(size.toUpperCase());
        } catch (Exception e) {
            return ids;
        }

        for (Gateway gate : GatewayRegistry.INSTANCE.getValues()) {
            if (gate.size() == targetSize) {
                ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gate);
                ids.add(id != null ? id.toString() : "unknown");
            }
        }
        return ids;
    }

    // ==================== Builder 工具方法 ====================

    /**
     * 创建一个 GatewayBuilder 用于生成已注册的传送门
     *
     * @param id 传送门ID，必须是已注册的传送门ID
     * @return GatewayBuilder 对象
     * 
     * JavaScript 示例:
     * <pre>
     * Gateway.builder("gateways:example_gate")
     *     .pos(player.x, player.y, player.z)
     *     .summoner(player)
     *     .spawn(event.level);
     * </pre>
     */
    static GatewayBuilder builder(String id) {
        return GatewayBuilder.create(id);
    }

    /**
     * 创建一个自定义传送门构建器
     * 可以自定义波次、实体、奖励等
     *
     * @param id 自定义传送门的唯一ID（如 "mymod:custom_gate"）
     * @return GatewayBuilder 对象
     * 
     * JavaScript 示例:
     * <pre>
     * // 创建并注册自定义传送门
     * Gateway.customBuilder("mymod:test_gate")
     *     .color(0xFF0000)        // 红色
     *     .size("small")          // 小型传送门
     *     .addAttribute("minecraft:generic.max_health", 20) // 所有波次实体都加生命
     *     .addEffect("minecraft:strength", 1)               // 所有波次实体都加力量
     *     .addNbt("{CanPickUpLoot:1b}")                     // 所有波次实体都能捡装备
     *     .addWave(wave => {
     *         wave.addEntity("minecraft:zombie", 3).modify(entity => {
     *             entity.addAttribute("minecraft:generic.max_health", 20)
     *             entity.addEffect("minecraft:strength", 1)
     *         })
     *         wave.maxTime(1200)      // 60秒
     *         wave.addReward("minecraft:iron_ingot", 2)
     *     })
     *     .addWave(wave => {
     *         wave.addEntity("minecraft:skeleton", 2).modify(entity => {
     *             entity.setNbt("{CanPickUpLoot:1b}")
     *         })
     *         wave.addEntity("minecraft:spider", 1)
     *         wave.maxTime(1500)
     *     })
     *     .addReward("minecraft:diamond", 1)  // 完成奖励
     *     .register()            // 注册传送门
     *     .pos(x, y, z)
     *     .spawn(level);         // 生成传送门
     * </pre>
     */
    static GatewayBuilder customBuilder(String id) {
        return GatewayBuilder.custom(id);
    }

    static boolean registerJson(String id, String gatewayJson) {
        ResourceLocation loc = ResourceLocation.parse(id);
        Gateway gateway = com.hp.gatewaysjs.util.RegistryHelper.decodeFromJson(GatewayRegistry.INSTANCE, gatewayJson);
        return DynamicGatewayManager.register(loc, gateway);
    }

    static boolean registerJson(String id, String gatewayJson, String displayName) {
        ResourceLocation loc = ResourceLocation.parse(id);
        Gateway gateway = com.hp.gatewaysjs.util.RegistryHelper.decodeFromJson(GatewayRegistry.INSTANCE, gatewayJson);
        return DynamicGatewayManager.register(loc, gateway, displayName);
    }

    /**
     * 在创造物品栏中隐藏指定传送门对应的珍珠
     */
    static void hidePearlInCreativeTab(String gatewayId) {
        DynamicGatewayManager.setPearlHiddenInCreativeTab(ResourceLocation.parse(gatewayId), true);
    }

    /**
     * 恢复指定传送门珍珠在创造物品栏中的显示
     */
    static void showPearlInCreativeTab(String gatewayId) {
        DynamicGatewayManager.setPearlHiddenInCreativeTab(ResourceLocation.parse(gatewayId), false);
    }

    // ==================== 自定义纹理设置 ====================

    /**
     * 为指定ID的网关设置自定义实体纹理。
     * <p>
     * JavaScript示例:
     * <pre>
     * Gateway.setEntityTexture("mymod:my_gate", "mymod:textures/entity/my_gate.png");
     * </pre>
     * </p>
     * @param gatewayId  网关ID
     * @param texturePath 纹理资源路径（如 "mymod:textures/entity/my_gate.png"）
     */
    static void setEntityTexture(String gatewayId, String texturePath) {
        ResourceLocation id = ResourceLocation.parse(gatewayId);
        ResourceLocation tex = ResourceLocation.parse(texturePath);
        GatewayRenderData existing = GatewayCustomTextureManager.INSTANCE.getTextureData(id);
        if (existing != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(tex, existing.getBossBarConfig()));
        } else {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(tex, null));
        }
    }

    /**
     * 为指定ID的网关设置自定义Boss条纹理（使用默认UV布局）。
     * <p>
     * 单纹理模式：外表（空背景）和进度（填充）共用同一张纹理图，通过不同的 UV 坐标区分。
     * </p>
     * <p>
     * 默认UV布局和原版 {@code textures/gui/bars.png} 一致：
     * <ul>
     *   <li>背景：从纹理的 (u=0, v=60) 处裁剪</li>
     *   <li>进度：从纹理的 (u=0, v=65) 处裁剪</li>
     *   <li>血条尺寸：宽 182px，高 5px</li>
     *   <li>纹理总尺寸：256×256 px</li>
     * </ul>
     * 渲染时先画背景（整条底色），再在上面叠加动态缩放的进度条（随血量从满宽逐步缩至0）。
     * </p>
     * <p>
     * JavaScript示例:
     * <pre>
     * // 使用原版 bars.png，表现与原版完全一致
     * Gateway.setBossBarTexture("mymod:my_gate", "minecraft:textures/gui/bars.png");
     * </pre>
     * </p>
     * @param gatewayId  网关ID
     * @param texturePath Boss条纹理资源路径（建议使用 "minecraft:textures/gui/bars.png"）
     */
    static void setBossBarTexture(String gatewayId, String texturePath) {
        ResourceLocation id = ResourceLocation.parse(gatewayId);
        BossBarRenderConfig config = new BossBarRenderConfig(ResourceLocation.parse(texturePath));
        GatewayRenderData existing = GatewayCustomTextureManager.INSTANCE.getTextureData(id);
        if (existing != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(existing.getEntityTexture(), config));
        } else {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(null, config));
        }
    }

    /**
     * 为指定ID的网关设置自定义Boss条纹理和完整渲染参数（单纹理模式）。
     * <p>
     * 外表和进度共用同一张纹理，但允许自定义 UV 坐标来选择纹理中的任意区域。
     * 适用于纹理布局不符合原版 bars.png 规范，或想用同一纹理中不同区域做背景/进度的情况。
     * </p>
     *
     * <h3>参数说明</h3>
     * <p>
     * 坐标系统：Minecraft 使用 (u, v) 从纹理图片中裁剪需要的部分。
     * u 为水平方向（从左到右），v 为垂直方向（从上到下），(0,0) 在左上角，单位是像素。
     * </p>
     * <p>
     * <b>渲染流程：</b>
     * <ol>
     *   <li>从 (bgU, bgV) 裁剪出 barWidth×barHeight 的区域作为底色背景</li>
     *   <li>从 (fillU, fillV) 裁剪，宽度随 Boss 血量动态缩放，叠加在背景上</li>
     * </ol>
     * </p>
     *
     * <p>
     * JavaScript示例:
     * <pre>
     * // 使用原版 bars.png 自定义UV——选择不同的血条风格
     * // bars.png 包含多行血条：v=0(粉色凋灵), v=10(紫色末影龙), v=60(默认暗色)
     * Gateway.setBossBarTexture("mymod:my_gate", "minecraft:textures/gui/bars.png",
     *     0, 0,   // bgU=0,    bgV=0    → 从纹理左上角裁剪背景（粉色凋灵条）
     *     0, 5,   // fillU=0,  fillV=5  → 从 (0,5) 裁剪填充（紧挨着背景下一行）
     *     182, 5, // barWidth=182, barHeight=5 → 血条渲染尺寸（宽182高5）
     *     256, 256); // texWidth=256, texHeight=256 → 纹理原图实际尺寸
     * </pre>
     * </p>
     * @param gatewayId   网关ID
     * @param texturePath Boss条纹理资源路径
     * @param bgU         背景裁剪的起始u坐标（像素），0表示从纹理最左边开始
     * @param bgV         背景裁剪的起始v坐标（像素），0表示从纹理最上边开始
     * @param fillU       进度裁剪的起始u坐标（像素），通常与bgU相同
     * @param fillV       进度裁剪的起始v坐标（像素），通常与bgV错开一行以使用不同的贴图区域
     * @param barWidth    Boss条在屏幕上的绘制宽度（像素），原版为182
     * @param barHeight   Boss条在屏幕上的绘制高度（像素），原版为5
     * @param texWidth    纹理图片的总宽度（像素），用于UV坐标归一化
     * @param texHeight   纹理图片的总高度（像素），用于UV坐标归一化
     */
    static void setBossBarTexture(String gatewayId, String texturePath,
                                  double bgU, double bgV, double fillU, double fillV,
                                  double barWidth, double barHeight, double texWidth, double texHeight) {
        ResourceLocation id = ResourceLocation.parse(gatewayId);
        BossBarRenderConfig config = new BossBarRenderConfig(
            ResourceLocation.parse(texturePath),
            (float) bgU, (float) bgV, (float) fillU, (float) fillV,
            (int) barWidth, (int) barHeight, (int) texWidth, (int) texHeight);
        GatewayRenderData existing = GatewayCustomTextureManager.INSTANCE.getTextureData(id);
        if (existing != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(existing.getEntityTexture(), config));
        } else {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(null, config));
        }
    }

    /**
     * 双纹理模式——为指定ID的网关分别设置外表和进度纹理（使用默认UV布局）。
     * <p>
     * 外表纹理是空血条背景，进度纹理是血条填充，两者可以是两张独立的图片。
     * 默认 UV 布局和原版 {@code textures/gui/bars.png} 一致：
     * <ul>
     *   <li>背景：从纹理的 (u=0, v=60) 处裁剪</li>
     *   <li>进度：从纹理的 (u=0, v=65) 处裁剪</li>
     *   <li>血条尺寸：宽 182px，高 5px</li>
     *   <li>纹理总尺寸：256×256 px</li>
     * </ul>
     * </p>
     * <p>
     * JavaScript示例:
     * <pre>
     * // 双纹理：外表和进度使用相同纹理（bars.png），通过UV区分
     * Gateway.setBossBarTexture("mymod:my_gate",
     *     "minecraft:textures/gui/bars.png",
     *     "minecraft:textures/gui/bars.png");
     * </pre>
     * </p>
     * @param gatewayId         网关ID
     * @param texturePath       外表纹理（空血条背景）
     * @param progressTexturePath 进度纹理（血条填充）
     */
    static void setBossBarTexture(String gatewayId, String texturePath, String progressTexturePath) {
        ResourceLocation id = ResourceLocation.parse(gatewayId);
        BossBarRenderConfig config = new BossBarRenderConfig(
            ResourceLocation.parse(texturePath),
            ResourceLocation.parse(progressTexturePath));
        GatewayRenderData existing = GatewayCustomTextureManager.INSTANCE.getTextureData(id);
        if (existing != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(existing.getEntityTexture(), config));
        } else {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(null, config));
        }
    }

    /**
     * 双纹理模式——为指定ID的网关分别设置外表和进度纹理，并自定义完整UV参数。
     * <p>
     * 这是最灵活的重载，允许对背景和进度使用两张独立的纹理，并精确控制每张贴图的裁剪区域。
     * 适合使用模组提供的专用 Boss 条纹理（通常是两张分开的图片）。
     * </p>
     *
     * <h3>参数详细说明</h3>
     * <p>
     * <b>纹理坐标系统（UV）：</b>Minecraft 使用 (u, v) 从纹理图片中裁剪需要的部分。
     * u 表示水平方向（从左到右），v 表示垂直方向（从上到下），(0, 0) 是纹理的左上角，单位是像素。
     * </p>
     *
     * <table border="1">
     *   <caption>参数作用说明</caption>
     *   <tr><th>参数</th><th>说明</th></tr>
     *   <tr><td>{@code texturePath}</td>
     *       <td><b>外表纹理</b>——空血条的背景贴图，玩家看到的 Boss 条底色/底框。</td></tr>
     *   <tr><td>{@code progressTexturePath}</td>
     *       <td><b>进度纹理</b>——血条的填充贴图，会随 Boss 血量百分比被裁剪（从左到右收缩）。</td></tr>
     *   <tr><td>{@code bgU}, {@code bgV}</td>
     *       <td><b>背景裁剪坐标</b>——从外表纹理中裁剪背景的起始位置。(0,0) 表示从纹理左上角开始。</td></tr>
     *   <tr><td>{@code fillU}, {@code fillV}</td>
     *       <td><b>进度裁剪坐标</b>——从进度纹理中裁剪填充的起始位置。可与bgU/bgV相同（两张纹理布局一致时）。</td></tr>
     *   <tr><td>{@code barWidth}</td>
     *       <td><b>血条绘制宽度</b>（像素）——Boss 条在屏幕上渲染的总宽度，原版为182。</td></tr>
     *   <tr><td>{@code barHeight}</td>
     *       <td><b>血条绘制高度</b>（像素）——Boss 条在屏幕上渲染的总高度，原版为5。</td></tr>
     *   <tr><td>{@code texWidth}</td>
     *       <td><b>纹理图片总宽度</b>（像素）——用于将 UV 像素坐标归一化为 0.0~1.0 的纹理坐标。</td></tr>
     *   <tr><td>{@code texHeight}</td>
     *       <td><b>纹理图片总高度</b>（像素）——同上。</td></tr>
     * </table>
     *
     * <h3>纹理资源路径说明</h3>
     * <p>
     * 路径格式为 {@code "命名空间:路径/文件.png"}，对应 {@code assets/命名空间/textures/路径/文件.png}。
     * 可使用原版或其它模组的纹理（无需额外依赖）：
     * <ul>
     *   <li>{@code minecraft:textures/gui/bars.png} —— 原版 Boss 条纹理，256×256，
     *       背景在 (u=0, v=60)，进度在 (u=0, v=65)，血条宽182高5。</li>
     *   <li>{@code gateways:textures/entity/gateway.png} —— Gateways 模组的传送门实体纹理。</li>
     *   <li>也可使用任意资源包或模组中的纹理。</li>
     * </ul>
     * </p>
     *
     * <h3>JavaScript 示例</h3>
     * <pre>
     * // 使用原版 bars.png 作为外表和进度，自定义UV选择粉色凋灵风格血条
     * Gateway.setBossBarTexture("mymod:my_gate",
     *     "minecraft:textures/gui/bars.png",
     *     "minecraft:textures/gui/bars.png",
     *     0, 0,   // bgU=0,   bgV=0   → 从 (0,0) 裁剪背景（粉色凋灵条）
     *     0, 5,   // fillU=0, fillV=5 → 从 (0,5) 裁剪填充
     *     182, 5, // barWidth=182, barHeight=5
     *     256, 256); // texWidth=256, texHeight=256
     * </pre>
     *
     * <h3>工作原理</h3>
     * <p>
     * 渲染时先绘制背景（从 texturePath 的 bgU/bgV 裁剪出整条底色），
     * 再绘制进度（从 progressTexturePath 的 fillU/fillV 裁剪，宽度随血量动态缩放）。
     * 两张纹理的 texWidth 和 texHeight 必须相同，因为共用同一套归一化坐标。
     * </p>
     *
     * @param gatewayId          网关ID
     * @param texturePath        外表纹理（空血条背景）
     * @param progressTexturePath 进度纹理（血条填充）
     * @param bgU         背景裁剪的起始u坐标（像素），0表示从纹理最左边开始
     * @param bgV         背景裁剪的起始v坐标（像素），0表示从纹理最上边开始
     * @param fillU       进度裁剪的起始u坐标（像素），通常与bgU相同
     * @param fillV       进度裁剪的起始v坐标（像素），通常与bgV错开一行以使用不同的贴图区域
     * @param barWidth    Boss条在屏幕上的绘制宽度（像素），原版为182
     * @param barHeight   Boss条在屏幕上的绘制高度（像素），原版为5
     * @param texWidth    纹理图片的总宽度（像素），用于UV坐标归一化
     * @param texHeight   纹理图片的总高度（像素），用于UV坐标归一化
     */
    static void setBossBarTexture(String gatewayId, String texturePath, String progressTexturePath,
                                  double bgU, double bgV, double fillU, double fillV,
                                  double barWidth, double barHeight, double texWidth, double texHeight) {
        ResourceLocation id = ResourceLocation.parse(gatewayId);
        BossBarRenderConfig config = new BossBarRenderConfig(
            ResourceLocation.parse(texturePath),
            ResourceLocation.parse(progressTexturePath),
            (float) bgU, (float) bgV, (float) fillU, (float) fillV,
            (int) barWidth, (int) barHeight, (int) texWidth, (int) texHeight);
        GatewayRenderData existing = GatewayCustomTextureManager.INSTANCE.getTextureData(id);
        if (existing != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(existing.getEntityTexture(), config));
        } else {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(null, config));
        }
    }
}
