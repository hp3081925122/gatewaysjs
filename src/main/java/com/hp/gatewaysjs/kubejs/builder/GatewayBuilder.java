package com.hp.gatewaysjs.kubejs.builder;

import com.hp.gatewaysjs.util.RegistryHelper;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.*;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 动态传送门创建工具
 * 
 * 支持两种模式：
 * 1. 生成已注册的传送门（使用ID）
 * 2. 注册并生成自定义传送门
 * 
 * 示例用法：
 * <pre>
 * // 方式1：生成已注册的传送门
 * GatewayBuilder.create("gateways:test_gate")
 *     .pos(x, y, z)
 *     .summoner(player)
 *     .spawn(level);
 * 
 * // 方式2：创建自定义传送门
 * GatewayBuilder.custom("mymod:custom_gate")
 *     .color(0xFF0000)  // 红色
 *     .size("small")    // 小型传送门
 *     .addWave()
 *         .addEntity("minecraft:zombie", 3)
 *         .maxTime(1200)
 *         .setupTime(60)
 *         .buildWave()
 *     .addReward("minecraft:diamond", 1)
 *     .register()  // 注册传送门
 *     .pos(x, y, z)
 *     .spawn(level);
 * </pre>
 */
@SuppressWarnings("removal")
public class GatewayBuilder {

    private final ResourceLocation gatewayId;
    private final boolean isCustom;
    
    // 生成位置
    private double x, y, z;
    private Player summoner;
    private String customName;  // 自定义名称
    
    // 自定义传送门属性
    private Gateway.Size size = Gateway.Size.SMALL;
    private TextColor color = TextColor.fromRgb(0xFFFFFF);
    private final List<WaveBuilder> waveBuilders = new ArrayList<>();
    private final List<Reward> rewards = new ArrayList<>();
    private final List<Failure> failures = new ArrayList<>();
    private SpawnAlgorithm spawnAlgo = SpawnAlgorithms.OPEN_FIELD;
    private GateRules rules = GateRules.DEFAULT;
    private BossEventSettings bossSettings = BossEventSettings.DEFAULT;

    private GatewayBuilder(ResourceLocation id, boolean isCustom) {
        this.gatewayId = id;
        this.isCustom = isCustom;
    }

    /**
     * 创建已注册传送门的 Builder
     * @param id 传送门ID（必须已在数据包中注册）
     */
    public static GatewayBuilder create(String id) {
        return new GatewayBuilder(new ResourceLocation(id), false);
    }

    /**
     * 创建自定义传送门的 Builder
     * @param id 自定义传送门的唯一ID（如 "mymod:custom_gate"）
     */
    public static GatewayBuilder custom(String id) {
        return new GatewayBuilder(new ResourceLocation(id), true);
    }

    // ==================== 通用方法 ====================

    /**
     * 设置生成位置
     */
    public GatewayBuilder pos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * 设置生成位置（BlockPos）
     */
    public GatewayBuilder pos(BlockPos blockPos) {
        this.x = blockPos.getX() + 0.5;
        this.y = blockPos.getY();
        this.z = blockPos.getZ() + 0.5;
        return this;
    }

    /**
     * 设置召唤者
     */
    public GatewayBuilder summoner(Player player) {
        this.summoner = player;
        return this;
    }

    /**
     * 设置传送门的自定义名称
     * @param name 显示名称（直接显示，不需要汉化键）
     */
    public GatewayBuilder name(String name) {
        this.customName = name;
        return this;
    }

    // ==================== 自定义传送门配置 ====================

    /**
     * 设置传送门大小：small, medium, large
     */
    public GatewayBuilder size(String sizeName) {
        switch (sizeName.toLowerCase()) {
            case "small" -> this.size = Gateway.Size.SMALL;
            case "medium" -> this.size = Gateway.Size.MEDIUM;
            case "large" -> this.size = Gateway.Size.LARGE;
            default -> throw new IllegalArgumentException("Unknown size: " + sizeName);
        }
        return this;
    }

    /**
     * 设置传送门大小
     */
    public GatewayBuilder size(Gateway.Size size) {
        this.size = size;
        return this;
    }

    /**
     * 设置传送门颜色（十六进制）
     * @param hexColor 十六进制颜色值（如 0xFF0000 为红色）
     */
    public GatewayBuilder color(int hexColor) {
        this.color = TextColor.fromRgb(hexColor & 0xFFFFFF);
        return this;
    }

    /**
     * 添加一个波次
     */
    public WaveBuilder addWave() {
        WaveBuilder builder = new WaveBuilder(this);
        waveBuilders.add(builder);
        return builder;
    }

    /**
     * 添加物品奖励（传送门完成时发放）
     */
    public GatewayBuilder addReward(String itemId, int count) {
        ItemStack stack = new ItemStack(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId))
        );
        stack.setCount(count);
        this.rewards.add(new Reward.StackReward(stack));
        return this;
    }

    /**
     * 设置生成算法：open_field 或 inward_spiral
     */
    public GatewayBuilder spawnAlgorithm(String algorithm) {
        if ("inward_spiral".equalsIgnoreCase(algorithm)) {
            this.spawnAlgo = SpawnAlgorithms.INWARD_SPIRAL;
        } else {
            this.spawnAlgo = SpawnAlgorithms.OPEN_FIELD;
        }
        return this;
    }

    // ==================== 注册与生成 ====================

    /**
     * 检查传送门是否已注册
     */
    public boolean exists() {
        DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(gatewayId);
        return holder.isBound();
    }

    /**
     * 注册自定义传送门（仅对 custom() 创建的 Builder 有效）
     * 如果传送门已注册，则跳过注册
     * @return this，便于链式调用
     */
    public GatewayBuilder register() {
        if (!isCustom) {
            throw new IllegalStateException("Cannot register a non-custom gateway. Use GatewayBuilder.custom() instead.");
        }

        // 如果已注册则跳过
        if (exists()) {
            return this;
        }

        // 构建波次列表
        List<Wave> waves = new ArrayList<>();
        for (WaveBuilder wb : waveBuilders) {
            waves.add(wb.build());
        }

        if (waves.isEmpty()) {
            throw new IllegalStateException("Gateway must have at least one wave!");
        }

        // 创建 NormalGateway
        NormalGateway gateway = new NormalGateway(
            size,
            color,
            waves,
            rewards,
            failures,
            spawnAlgo,
            rules,
            bossSettings
        );

        // 使用反射工具类注册（如果已存在会返回 false）
        RegistryHelper.register(GatewayRegistry.INSTANCE, gatewayId, gateway);

        return this;
    }

    /**
     * 在指定世界生成传送门实体
     * @return 成功返回 GatewayEntity 实例，失败返回 null
     */
    public GatewayEntity spawn(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        try {
            DynamicHolder<Gateway> holder = GatewayRegistry.INSTANCE.holder(gatewayId);
            if (!holder.isBound()) {
                return null;
            }

            Gateway gate = holder.get();

            Player player = summoner;
            if (player == null) {
                player = serverLevel.getNearestPlayer(x, y, z, 50, false);
                if (player == null) {
                    player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(serverLevel);
                }
            }

            GatewayEntity entity = gate.createEntity(level, player);
            entity.setPos(x, y, z);

            if (customName != null && !customName.isEmpty()) {
                entity.setCustomName(net.minecraft.network.chat.Component.literal(customName));
            }

            int yAdjust = 0;
            while (yAdjust < 4) {
                if (!level.noCollision(entity)) {
                    entity.setPos(entity.getX(), entity.getY() + 1, entity.getZ());
                    yAdjust++;
                } else {
                    break;
                }
            }

            if (!level.noCollision(entity)) {
                return null;  // 位置被占用，生成失败
            }

            level.addFreshEntity(entity);
            entity.onGateCreated();

            return entity;  // ← 返回实体

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取传送门ID
     */
    public ResourceLocation getGatewayId() {
        return gatewayId;
    }

    // ==================== 波次构建器 ====================

    /**
     * 波次构建器
     */
    public static class WaveBuilder {
        private final GatewayBuilder parent;
        private final List<WaveEntity> entities = new ArrayList<>();
        private final List<WaveModifier> modifiers = Collections.emptyList();
        private final List<Reward> rewards = new ArrayList<>();
        private int maxWaveTime = 1200;  // 默认60秒
        private int setupTime = 60;      // 默认3秒

        WaveBuilder(GatewayBuilder parent) {
            this.parent = parent;
        }

        /**
         * 添加实体到波次
         * @param entityId 实体类型ID（如 "minecraft:zombie"）
         * @param count 数量
         */
        public WaveBuilder addEntity(String entityId, int count) {
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entityId));
            if (type == null) {
                throw new IllegalArgumentException("Unknown entity type: " + entityId);
            }
            
            WaveEntity waveEntity = new WaveEntity.StandardWaveEntity(
                type,
                Optional.empty(),
                Optional.empty(),
                Collections.emptyList(),
                true,
                count
            );
            entities.add(waveEntity);
            return this;
        }

        /**
         * 设置波次最大时间（tick）
         */
        public WaveBuilder maxTime(int ticks) {
            this.maxWaveTime = ticks;
            return this;
        }

        /**
         * 设置下一波准备时间（tick）
         */
        public WaveBuilder setupTime(int ticks) {
            this.setupTime = ticks;
            return this;
        }

        /**
         * 添加波次奖励
         */
        public WaveBuilder addReward(String itemId, int count) {
            ItemStack stack = new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId))
            );
            stack.setCount(count);
            this.rewards.add(new Reward.StackReward(stack));
            return this;
        }

        /**
         * 完成波次构建，返回父 Builder
         */
        public GatewayBuilder buildWave() {
            return parent;
        }

        Wave build() {
            return new Wave(entities, modifiers, rewards, maxWaveTime, setupTime);
        }
    }
}
