package com.hp.gatewaysjs.kubejs.builder;

import com.hp.gatewaysjs.compat.ApotheosisGatewayCompat;
import com.hp.gatewaysjs.util.BossBarRenderConfig;
import com.hp.gatewaysjs.util.DynamicGatewayManager;
import com.hp.gatewaysjs.util.GatewayCustomTextureManager;
import com.hp.gatewaysjs.util.GatewayRenderData;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.*;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.placebo.json.ChancedEffectInstance;
import dev.shadowsoffire.placebo.json.GearSetRegistry;
import dev.shadowsoffire.placebo.json.RandomAttributeModifier;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
 *     .addAttribute("minecraft:generic.max_health", 20) // 所有波次实体都加生命
 *     .addEffect("minecraft:strength", 1)               // 所有波次实体都加力量
 *     .addNbt("{CanPickUpLoot:1b}")                     // 所有波次实体都能捡装备
 *     .addWave(wave -> {
 *         wave.addEntity("minecraft:zombie", 3).modify(entity -> {
 *             entity.addAttribute("minecraft:generic.max_health", 20);
 *             entity.addEffect("minecraft:strength", 1);
 *         });
 *         wave.addAttribute("minecraft:generic.attack_damage", 4);
 *         wave.addEffect("minecraft:speed", 0);
 *         wave.maxTime(1200);
 *         wave.setupTime(60);
 *     })
 *     .addWave(wave -> {
 *         wave.addEntity("minecraft:skeleton", 2).modify(entity -> {
 *             entity.setNbt("{CanPickUpLoot:1b}");
 *         });
 *         wave.addGearSet("placebo:example");
 *         wave.maxTime(800);
 *         wave.setupTime(40);
 *     })
 *     .addReward("minecraft:diamond", 1)
 *     .register()  // 注册传送门
 *     .pos(x, y, z)
 *     .spawn(level);
 * </pre>
 */
@SuppressWarnings("removal")
public class GatewayBuilder {

    /**
     * 属性修饰操作类型
     */
    public enum AttributeOperation {
        addition,
        multiply_base,
        multiply_total
    }

    private final ResourceLocation gatewayId;
    private final boolean isCustom;
    
    // 生成位置
    private double x, y, z;
    private Player summoner;
    private String customName;  // 自定义名称
    private String tooltipKey;  // JEI珍珠提示汉化键
    private String tooltipText; // JEI珍珠提示文本
    private boolean hidePearlInCreativeTab;
    
    // 自定义传送门属性
    private Gateway.Size size = Gateway.Size.SMALL;
    private TextColor color = TextColor.fromRgb(0xFFFFFF);
    private final List<WaveBuilder> waveBuilders = new ArrayList<>();
    private final List<Consumer<WaveBuilder>> globalWaveModifiers = new ArrayList<>();
    private final List<Reward> rewards = new ArrayList<>();
    private final List<Failure> failures = new ArrayList<>();
    private final List<WaveModifier> globalModifiers = new ArrayList<>();
    private String globalDescription;
    private CompoundTag globalNbt;
    @Nullable
    private Boolean globalFinalizeSpawn;
    private SpawnAlgorithm spawnAlgo = SpawnAlgorithms.OPEN_FIELD;
    private GateRules rules = GateRules.DEFAULT;
    private BossEventSettings bossSettings = BossEventSettings.DEFAULT;

    // 自定义纹理（仅客户端渲染用）
    @Nullable
    private ResourceLocation entityTexture;
    @Nullable
    private BossBarRenderConfig bossBarConfig;

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

    /**
     * 设置 JEI 珍珠提示的汉化键
     */
    public GatewayBuilder tooltipKey(String key) {
        this.tooltipKey = key;
        return this;
    }

    /**
     * 设置 JEI 珍珠提示的直接显示文本
     */
    public GatewayBuilder tooltipText(String text) {
        this.tooltipText = text;
        return this;
    }

    /**
     * 在创造物品栏中隐藏这个传送门对应的珍珠
     */
    public GatewayBuilder hidePearlInCreativeTab() {
        this.hidePearlInCreativeTab = true;
        return this;
    }

    /**
     * 控制这个传送门对应的珍珠是否显示在创造物品栏中
     */
    public GatewayBuilder showPearlInCreativeTab(boolean show) {
        this.hidePearlInCreativeTab = !show;
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
     * 设置传送门颜色（十六进制）
     * @param hexColor 十六进制颜色值（如 0xFF0000 为红色）
     */
    public GatewayBuilder color(int hexColor) {
        this.color = TextColor.fromRgb(hexColor & 0xFFFFFF);
        return this;
    }

    public GatewayBuilder color(double hexColor) {
        return color((int) hexColor);
    }

    public GatewayBuilder color(Number hexColor) {
        return color(hexColor.intValue());
    }

    public GatewayBuilder setColor(int hexColor) {
        return color(hexColor);
    }

    public GatewayBuilder setColor(double hexColor) {
        return color(hexColor);
    }

    public GatewayBuilder setColor(Number hexColor) {
        return color(hexColor);
    }

    /**
     * 为所有波次里的全部实体添加属性修饰，默认使用 addition
     */
    public GatewayBuilder addAttribute(Attribute attribute, Number value) {
        return addAttribute(attribute, value, AttributeOperation.addition);
    }

    /**
     * 为所有波次里的全部实体添加属性修饰
     */
    public GatewayBuilder addAttribute(Attribute attribute, Number value, AttributeOperation operation) {
        globalModifiers.add(WaveBuilder.EntityModifierBuilder.createAttributeModifier(attribute, value, operation));
        return this;
    }

    /**
     * 为所有波次里的全部实体添加状态效果，默认 100% 概率、非环境粒子、显示粒子
     */
    public GatewayBuilder addEffect(MobEffect effect, Number amplifier) {
        return addEffect(effect, amplifier, 1.0F, false, true);
    }

    /**
     * 为所有波次里的全部实体添加状态效果
     */
    public GatewayBuilder addEffect(MobEffect effect, Number amplifier, Number chance) {
        return addEffect(effect, amplifier, chance, false, true);
    }

    /**
     * 为所有波次里的全部实体添加状态效果
     */
    public GatewayBuilder addEffect(MobEffect effect, Number amplifier, Number chance, boolean ambient, boolean visible) {
        globalModifiers.add(WaveBuilder.EntityModifierBuilder.createEffectModifier(effect, amplifier, chance, ambient, visible));
        return this;
    }

    /**
     * 为所有波次里的全部实体添加装备套装修饰
     */
    public GatewayBuilder addGearSet(String gearSetId) {
        globalModifiers.add(WaveBuilder.EntityModifierBuilder.createGearSetModifier(gearSetId));
        return this;
    }

    /**
     * 设置所有波次实体的默认描述文本，实体单独设置时会覆盖这里
     */
    public GatewayBuilder setDescription(String description) {
        this.globalDescription = description;
        return this;
    }

    /**
     * 设置所有波次实体的默认 NBT，实体单独设置时会合并并优先覆盖这里
     */
    public GatewayBuilder setNbt(String nbtString) {
        this.globalNbt = parseNbt(nbtString);
        return this;
    }

    /**
     * 合并所有波次实体的默认 NBT，可多次调用
     */
    public GatewayBuilder addNbt(String nbtString) {
        CompoundTag tag = parseNbt(nbtString);
        if (this.globalNbt == null) {
            this.globalNbt = tag;
        } else {
            this.globalNbt.merge(tag);
        }
        return this;
    }

    /**
     * 设置所有波次实体是否执行原版初始化，实体单独设置时会覆盖这里
     */
    public GatewayBuilder finalizeSpawn(boolean finalizeSpawn) {
        this.globalFinalizeSpawn = finalizeSpawn;
        return this;
    }

    // ==================== 自定义纹理 ====================

    /**
     * 设置网关实体的自定义纹理路径。
     * <p>
     * 纹理应放置在资源包中，路径格式如 {@code "mymod:textures/entity/my_gate.png"}。
     * 纹理高度应为宽度的9倍（因为渲染器会按9帧动画切分）。
     * </p>
     * @param texturePath 纹理的资源路径
     */
    public GatewayBuilder entityTexture(String texturePath) {
        this.entityTexture = new ResourceLocation(texturePath);
        return this;
    }

    /**
     * 设置网关Boss条的自定义纹理路径（使用默认UV布局）。
     * <p>
     * 默认UV布局和原版 {@code textures/gui/bars.png} 一致：
     * 背景在(u=0, v=60)，填充在(u=0, v=65)，Boss条宽182高5，纹理尺寸256x256。
     * </p>
     * @param texturePath Boss条纹理的资源路径
     */
    public GatewayBuilder bossBarTexture(String texturePath) {
        this.bossBarConfig = new BossBarRenderConfig(new ResourceLocation(texturePath));
        return this;
    }

    /**
     * 设置网关Boss条的自定义纹理和完整渲染参数。
     * <p>
     * 适用于纹理布局不同于原版 {@code textures/gui/bars.png} 的情况。
     * 例如，如果某个模组的Boss条纹理在(u=0, v=0)处为背景、(u=0, v=20)处为填充、宽度200、高度20、纹理尺寸512x512：
     * <pre>
     * .bossBar("mymod:textures/gui/my_boss_bar.png", 0, 0, 0, 20, 200, 20, 512, 512)
     * </pre>
     * </p>
     * @param texturePath Boss条纹理的资源路径
     * @param bgU         背景部分在纹理中的u坐标
     * @param bgV         背景部分在纹理中的v坐标
     * @param fillU       进度填充部分在纹理中的u坐标
     * @param fillV       进度填充部分在纹理中的v坐标
     * @param barWidth    Boss条的绘制宽度（像素）
     * @param barHeight   Boss条的绘制高度（像素）
     * @param texWidth    纹理的总宽度（像素）
     * @param texHeight   纹理的总高度（像素）
     */
    public GatewayBuilder bossBar(String texturePath, double bgU, double bgV, double fillU, double fillV,
                                  double barWidth, double barHeight, double texWidth, double texHeight) {
        this.bossBarConfig = new BossBarRenderConfig(
            new ResourceLocation(texturePath),
            (float) bgU, (float) bgV, (float) fillU, (float) fillV,
            (int) barWidth, (int) barHeight, (int) texWidth, (int) texHeight);
        return this;
    }

    /**
     * 双纹理模式——分别指定外表和进度两张纹理（使用默认UV布局）。
     * <p>
     * 默认UV布局和原版 {@code textures/gui/bars.png} 一致。
     * </p>
     * @param texturePath        外表纹理（空血条背景）
     * @param progressTexturePath 进度纹理（血条填充）
     */
    public GatewayBuilder bossBar(String texturePath, String progressTexturePath) {
        this.bossBarConfig = new BossBarRenderConfig(
            new ResourceLocation(texturePath),
            new ResourceLocation(progressTexturePath));
        return this;
    }

    /**
     * 双纹理模式——分别指定外表和进度两张纹理，并自定义完整UV参数。
     * <p>
     * 例如灾变模格的风气Boss条：
     * <pre>
     * .bossBar("cataclysm:textures/gui/boss_bar/leviathan_bar_base.png",
     *          "cataclysm:textures/gui/boss_bar/leviathan_bar_overlay.png",
     *          0, 0, 0, 5, 182, 16, 256, 16)
     * </pre>
     * </p>
     * @param texturePath        外表纹理（空血条背景）
     * @param progressTexturePath 进度纹理（血条填充）
     * @param bgU         背景部分在纹理中的u坐标
     * @param bgV         背景部分在纹理中的v坐标
     * @param fillU       进度填充部分在纹理中的u坐标
     * @param fillV       进度填充部分在纹理中的v坐标
     * @param barWidth    Boss条的绘制宽度（像素）
     * @param barHeight   Boss条的绘制高度（像素）
     * @param texWidth    纹理的总宽度（像素）
     * @param texHeight   纹理的总高度（像素）
     */
    public GatewayBuilder bossBar(String texturePath, String progressTexturePath,
                                  double bgU, double bgV, double fillU, double fillV,
                                  double barWidth, double barHeight, double texWidth, double texHeight) {
        this.bossBarConfig = new BossBarRenderConfig(
            new ResourceLocation(texturePath),
            new ResourceLocation(progressTexturePath),
            (float) bgU, (float) bgV, (float) fillU, (float) fillV,
            (int) barWidth, (int) barHeight, (int) texWidth, (int) texHeight);
        return this;
    }

    /**
     * 添加一个波次
     */
    public WaveBuilder addWave() {
        WaveBuilder builder = new WaveBuilder(this);
        waveBuilders.add(builder);
        for (Consumer<WaveBuilder> modifier : globalWaveModifiers) {
            modifier.accept(builder);
        }
        return builder;
    }

    /**
     * 添加一个波次，并在回调中直接配置波次内容
     * <p>
     * 示例：
     * <pre>
 * .addWave(wave -> {
 *     wave.addEntity("minecraft:zombie", 3).modify(entity -> {
 *         entity.addAttribute("minecraft:generic.max_health", 20);
 *     });
     *     wave.addAttribute("minecraft:generic.attack_damage", 4);
     *     wave.addEffect("minecraft:speed", 0);
     *     wave.maxTime(1200);
     *     wave.setupTime(60);
     * })
     * </pre>
     * </p>
     */
    public GatewayBuilder addWave(Consumer<WaveBuilder> consumer) {
        WaveBuilder builder = addWave();
        consumer.accept(builder);
        return this;
    }

    /**
     * 对所有波次应用统一配置
     */
    public GatewayBuilder modifyAllWaves(Consumer<WaveBuilder> consumer) {
        globalWaveModifiers.add(consumer);
        for (WaveBuilder builder : waveBuilders) {
            consumer.accept(builder);
        }
        return this;
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
     * 添加Apotheosis词缀物品奖励（传送门完成时发放）
     */
    public GatewayBuilder addApotheosisAffixReward(String rarityId) {
        this.rewards.add(ApotheosisGatewayCompat.createAffixReward(rarityId));
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

    /**
     * 设置传送门规则
     */
    public GatewayBuilder rules(GateRules rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Rules cannot be null");
        }
        this.rules = rules;
        return this;
    }

    /**
     * 设置完整传送门规则
     */
    public GatewayBuilder rules(double spawnRange, double leashRange, boolean allowDiscarding,
                                boolean allowDimChange, boolean playerDamageOnly, boolean removeMobsOnFailure,
                                boolean failOnOutOfBounds, double spacing, double followRangeBoost,
                                double defaultDropChance) {
        this.rules = new GateRules(
            spawnRange,
            leashRange,
            allowDiscarding,
            allowDimChange,
            playerDamageOnly,
            removeMobsOnFailure,
            failOnOutOfBounds,
            spacing,
            followRangeBoost,
            (float) defaultDropChance
        );
        return this;
    }

    /**
     * 设置怪物生成范围
     */
    public GatewayBuilder spawnRange(double spawnRange) {
        this.rules = new GateRules(
            spawnRange,
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置怪物牵引范围
     */
    public GatewayBuilder leashRange(double leashRange) {
        this.rules = new GateRules(
            rules.spawnRange(),
            leashRange,
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置是否允许丢弃的实体计入击杀
     */
    public GatewayBuilder allowDiscarding(boolean allowDiscarding) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            allowDiscarding,
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置是否允许跨维度实体计入击杀
     */
    public GatewayBuilder allowDimChange(boolean allowDimChange) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            allowDimChange,
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置波次实体是否只能被玩家伤害
     */
    public GatewayBuilder playerDamageOnly(boolean playerDamageOnly) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            playerDamageOnly,
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置传送门失败时是否移除怪物
     */
    public GatewayBuilder removeMobsOnFailure(boolean removeMobsOnFailure) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            removeMobsOnFailure,
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * removeMobsOnFailure 的别名
     */
    public GatewayBuilder removeOnFailure(boolean removeOnFailure) {
        return removeMobsOnFailure(removeOnFailure);
    }

    /**
     * 设置怪物超出牵引范围时是否让传送门失败
     */
    public GatewayBuilder failOnOutOfBounds(boolean failOnOutOfBounds) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            failOnOutOfBounds,
            rules.spacing(),
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置传送门之间的最小间距
     */
    public GatewayBuilder spacing(double spacing) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            spacing,
            rules.followRangeBoost(),
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置怪物跟随范围加成
     */
    public GatewayBuilder followRangeBoost(double followRangeBoost) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            followRangeBoost,
            rules.defaultDropChance()
        );
        return this;
    }

    /**
     * 设置默认装备掉落概率
     */
    public GatewayBuilder defaultDropChance(double defaultDropChance) {
        this.rules = new GateRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            (float) defaultDropChance
        );
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
     * 如果传送门已注册，则替换旧注册，刷新状态
     * @return this，便于链式调用
     */
    public GatewayBuilder register() {
        if (!isCustom) {
            throw new IllegalStateException("Cannot register a non-custom gateway. Use GatewayBuilder.custom() instead.");
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

        // 使用反射工具类注册，同 ID 已存在时会替换旧注册并刷新 holder
        DynamicGatewayManager.register(gatewayId, gateway, customName, tooltipKey, tooltipText, hidePearlInCreativeTab);

        // 如果有自定义纹理，存入纹理管理器
        if (entityTexture != null || bossBarConfig != null) {
            GatewayCustomTextureManager.INSTANCE.setTextureData(gatewayId,
                new GatewayRenderData(entityTexture, bossBarConfig));
        }

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
        private final List<EntityConfig> entities = new ArrayList<>();
        private final List<WaveEntity> customEntities = new ArrayList<>();
        private final List<WaveModifier> modifiers = new ArrayList<>();
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
        public WaveEntityBuilder addEntity(String entityId, int count) {
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entityId));
            if (type == null) {
                throw new IllegalArgumentException("Unknown entity type: " + entityId);
            }

            EntityConfig entity = new EntityConfig(type, count);
            entities.add(entity);
            return new WaveEntityBuilder(this, entity);
        }

        /**
         * 添加随机Apotheosis Boss到波次
         */
        public WaveBuilder addApotheosisBoss() {
            customEntities.add(ApotheosisGatewayCompat.createBossWaveEntity());
            return this;
        }

        /**
         * 添加指定Apotheosis Boss到波次
         */
        public WaveBuilder addApotheosisBoss(String bossId) {
            customEntities.add(ApotheosisGatewayCompat.createBossWaveEntity(bossId));
            return this;
        }

        /**
         * 为本波所有实体添加属性修饰，默认使用 addition
         */
        public WaveBuilder addAttribute(Attribute attribute, Number value) {
            return addAttribute(attribute, value, AttributeOperation.addition);
        }

        /**
         * 为本波所有实体添加属性修饰
         */
        public WaveBuilder addAttribute(Attribute attribute, Number value, AttributeOperation operation) {
            modifiers.add(EntityModifierBuilder.createAttributeModifier(attribute, value, operation));
            return this;
        }

        /**
         * 为本波所有实体添加状态效果，默认 100% 概率、非环境粒子、显示粒子
         */
        public WaveBuilder addEffect(MobEffect effect, Number amplifier) {
            return addEffect(effect, amplifier, 1.0F, false, true);
        }

        /**
         * 为本波所有实体添加状态效果
         */
        public WaveBuilder addEffect(MobEffect effect, Number amplifier, Number chance) {
            return addEffect(effect, amplifier, chance, false, true);
        }

        /**
         * 为本波所有实体添加状态效果
         */
        public WaveBuilder addEffect(MobEffect effect, Number amplifier, Number chance, boolean ambient, boolean visible) {
            modifiers.add(EntityModifierBuilder.createEffectModifier(effect, amplifier, chance, ambient, visible));
            return this;
        }

        /**
         * 为本波所有实体添加装备套装修饰
         */
        public WaveBuilder addGearSet(String gearSetId) {
            modifiers.add(EntityModifierBuilder.createGearSetModifier(gearSetId));
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
         * 添加Apotheosis词缀物品奖励到波次
         */
        public WaveBuilder addApotheosisAffixReward(String rarityId) {
            this.rewards.add(ApotheosisGatewayCompat.createAffixReward(rarityId));
            return this;
        }

        /**
         * 完成波次构建，返回父 Builder
         */
        public GatewayBuilder buildWave() {
            return parent;
        }

        Wave build() {
            List<WaveModifier> builtModifiers = new ArrayList<>(parent.globalModifiers);
            builtModifiers.addAll(modifiers);

            List<WaveEntity> builtEntities = new ArrayList<>();
            for (EntityConfig entity : entities) {
                builtEntities.add(entity.build(parent));
            }
            builtEntities.addAll(customEntities);
            return new Wave(builtEntities, builtModifiers, rewards, maxWaveTime, setupTime);
        }

        private static class EntityConfig {
            private final EntityType<?> type;
            private final int count;
            private final List<WaveModifier> modifiers = new ArrayList<>();
            private String description;
            private CompoundTag nbt;
            @Nullable
            private Boolean finalizeSpawn;

            private EntityConfig(EntityType<?> type, int count) {
                this.type = type;
                this.count = count;
            }

            private WaveEntity build(GatewayBuilder gateway) {
                CompoundTag builtNbt = mergeNbt(gateway.globalNbt, nbt);
                String builtDescription = description != null ? description : gateway.globalDescription;
                boolean builtFinalizeSpawn = finalizeSpawn != null ? finalizeSpawn : gateway.globalFinalizeSpawn != null ? gateway.globalFinalizeSpawn : true;

                return new WaveEntity.StandardWaveEntity(
                    type,
                    Optional.ofNullable(builtDescription),
                    Optional.ofNullable(builtNbt),
                    List.copyOf(modifiers),
                    builtFinalizeSpawn,
                    count
                );
            }
        }

        /**
         * 单个波次实体的构建入口
         */
        public static class WaveEntityBuilder {
            private final WaveBuilder parent;
            private final EntityConfig entity;

            private WaveEntityBuilder(WaveBuilder parent, EntityConfig entity) {
                this.parent = parent;
                this.entity = entity;
            }

            /**
             * 修改刚添加的实体配置
             */
            public WaveBuilder modify(Consumer<EntityModifierBuilder> consumer) {
                consumer.accept(new EntityModifierBuilder(entity));
                return parent;
            }
        }

        /**
         * 单个实体的详细配置器
         */
        public static class EntityModifierBuilder {
            private final EntityConfig entity;

            private EntityModifierBuilder(EntityConfig entity) {
                this.entity = entity;
            }

            /**
             * 添加属性修饰，默认使用 addition
             */
            public EntityModifierBuilder addAttribute(Attribute attribute, Number value) {
                return addAttribute(attribute, value, AttributeOperation.addition);
            }

            /**
             * 添加属性修饰
             * @param attribute 属性ID，如 "minecraft:generic.max_health"
             * @param value 修饰值
             * @param operation 操作类型，可用值：addition、multiply_base、multiply_total
             */
            public EntityModifierBuilder addAttribute(Attribute attribute, Number value, AttributeOperation operation) {
                entity.modifiers.add(createAttributeModifier(attribute, value, operation));
                return this;
            }

            /**
             * 添加状态效果，默认 100% 概率、非环境粒子、显示粒子
             */
            public EntityModifierBuilder addEffect(MobEffect effect, Number amplifier) {
                return addEffect(effect, amplifier, 1.0F, false, true);
            }

            /**
             * 添加状态效果
             */
            public EntityModifierBuilder addEffect(MobEffect effect, Number amplifier, Number chance) {
                return addEffect(effect, amplifier, chance, false, true);
            }

            /**
             * 添加状态效果
             * @param effect 效果ID，如 "minecraft:strength"
             * @param amplifier 效果等级，0 表示 1 级
             * @param chance 生效概率，范围 0~1
             * @param ambient 是否为环境效果
             * @param visible 是否显示粒子
             */
            public EntityModifierBuilder addEffect(MobEffect effect, Number amplifier, Number chance, boolean ambient, boolean visible) {
                entity.modifiers.add(createEffectModifier(effect, amplifier, chance, ambient, visible));
                return this;
            }

            /**
             * 添加装备套装修饰
             */
            public EntityModifierBuilder addGearSet(String gearSetId) {
                entity.modifiers.add(createGearSetModifier(gearSetId));
                return this;
            }

            /**
             * 设置实体描述文本
             */
            public EntityModifierBuilder setDescription(String description) {
                entity.description = description;
                return this;
            }

            /**
             * 设置实体NBT
             */
            public EntityModifierBuilder setNbt(String nbtString) {
                entity.nbt = parseNbt(nbtString);
                return this;
            }

            /**
             * 合并实体NBT，可多次调用
             */
            public EntityModifierBuilder addNbt(String nbtString) {
                CompoundTag tag = parseNbt(nbtString);
                if (entity.nbt == null) {
                    entity.nbt = tag;
                } else {
                    entity.nbt.merge(tag);
                }
                return this;
            }

            /**
             * 设置是否执行实体原版初始化
             */
            public EntityModifierBuilder finalizeSpawn(boolean finalizeSpawn) {
                entity.finalizeSpawn = finalizeSpawn;
                return this;
            }

            private static AttributeModifier.Operation parseAttributeOperation(AttributeOperation operation) {
                if (operation == null) {
                    return AttributeModifier.Operation.ADDITION;
                }

                return switch (operation) {
                    case addition -> AttributeModifier.Operation.ADDITION;
                    case multiply_base -> AttributeModifier.Operation.MULTIPLY_BASE;
                    case multiply_total -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                };
            }

            public static WaveModifier.AttributeModifier createAttributeModifier(Attribute attribute, Number value, AttributeOperation operation) {
                if (attribute == null) {
                    throw new IllegalArgumentException("Attribute cannot be null");
                }

                AttributeModifier.Operation op = parseAttributeOperation(operation);
                return new WaveModifier.AttributeModifier(
                    new RandomAttributeModifier(attribute, op, StepFunction.constant(value.floatValue()))
                );
            }

            public static WaveModifier.EffectModifier createEffectModifier(MobEffect effect, Number amplifier, Number chance, boolean ambient, boolean visible) {
                if (effect == null) {
                    throw new IllegalArgumentException("Effect cannot be null");
                }

                return new WaveModifier.EffectModifier(
                    new ChancedEffectInstance(
                        chance.floatValue(),
                        effect,
                        StepFunction.constant(amplifier.floatValue()),
                        ambient,
                        visible
                    )
                );
            }

            public static WaveModifier.GearSetModifier createGearSetModifier(String gearSetId) {
                return new WaveModifier.GearSetModifier(
                    GearSetRegistry.INSTANCE.holder(new ResourceLocation(gearSetId))
                );
            }
        }
    }

    private static CompoundTag parseNbt(String nbtString) {
        try {
            return TagParser.parseTag(nbtString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid entity NBT: " + nbtString, e);
        }
    }

    @Nullable
    private static CompoundTag mergeNbt(@Nullable CompoundTag base, @Nullable CompoundTag override) {
        if (base == null && override == null) {
            return null;
        }

        CompoundTag merged = base == null ? new CompoundTag() : base.copy();
        if (override != null) {
            merged.merge(override);
        }
        return merged;
    }
}
