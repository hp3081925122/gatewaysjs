package com.hp.gatewaysjs.kubejs.binding;

import com.hp.gatewaysjs.kubejs.builder.GatewayBuilder;
import com.hp.gatewaysjs.kubejs.wrapper.GatewayWrapper;
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
            ResourceLocation loc = new ResourceLocation(gatewayId);
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
            ResourceLocation loc = new ResourceLocation(gatewayId);
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
            ResourceLocation loc = new ResourceLocation(gatewayId);
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
            ResourceLocation loc = new ResourceLocation(gatewayId);
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
                    summoner = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(serverLevel);
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
            ResourceLocation loc = new ResourceLocation(gatewayId);
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
     *     .addWave()
     *         .addEntity("minecraft:zombie", 3)
     *         .maxTime(1200)      // 60秒
     *         .addReward("minecraft:iron_ingot", 2)
     *         .buildWave()
     *     .addWave()
     *         .addEntity("minecraft:skeleton", 2)
     *         .addEntity("minecraft:spider", 1)
     *         .maxTime(1500)
     *         .buildWave()
     *     .addReward("minecraft:diamond", 1)  // 完成奖励
     *     .register()            // 注册传送门
     *     .pos(x, y, z)
     *     .spawn(level);         // 生成传送门
     * </pre>
     */
    static GatewayBuilder customBuilder(String id) {
        return GatewayBuilder.custom(id);
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
        ResourceLocation id = new ResourceLocation(gatewayId);
        ResourceLocation tex = new ResourceLocation(texturePath);
        GatewayRenderData existing = GatewayCustomTextureManager.INSTANCE.getTextureData(id);
        if (existing != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(tex, existing.getBossBarTexture()));
        } else {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(tex, null));
        }
    }

    /**
     * 为指定ID的网关设置自定义Boss条纹理。
     * <p>
     * 纹理布局必须与原版 {@code textures/gui/bars.png} 一致。
     * </p>
     * <p>
     * JavaScript示例:
     * <pre>
     * Gateway.setBossBarTexture("mymod:my_gate", "mymod:textures/gui/my_boss_bar.png");
     * </pre>
     * </p>
     * @param gatewayId  网关ID
     * @param texturePath Boss条纹理资源路径
     */
    static void setBossBarTexture(String gatewayId, String texturePath) {
        ResourceLocation id = new ResourceLocation(gatewayId);
        ResourceLocation tex = new ResourceLocation(texturePath);
        GatewayRenderData existing = GatewayCustomTextureManager.INSTANCE.getTextureData(id);
        if (existing != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(existing.getEntityTexture(), tex));
        } else {
            GatewayCustomTextureManager.INSTANCE.setTextureData(id,
                new GatewayRenderData(null, tex));
        }
    }
}
