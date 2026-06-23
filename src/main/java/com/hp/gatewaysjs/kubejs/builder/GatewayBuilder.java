package com.hp.gatewaysjs.kubejs.builder;

import com.hp.gatewaysjs.compat.ApotheosisGatewayCompat;
import com.hp.gatewaysjs.util.BossBarRenderConfig;
import com.hp.gatewaysjs.util.DynamicGatewayManager;
import com.hp.gatewaysjs.util.GatewayCustomTextureManager;
import com.hp.gatewaysjs.util.GatewayRenderData;
import com.hp.gatewaysjs.util.RegistryHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.*;
import dev.shadowsoffire.gateways.gate.endless.ApplicationMode;
import dev.shadowsoffire.gateways.gate.endless.EndlessGateway;
import dev.shadowsoffire.gateways.gate.endless.EndlessModifier;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import dev.shadowsoffire.placebo.json.ChancedEffectInstance;
import dev.shadowsoffire.placebo.json.RandomAttributeModifier;
import dev.shadowsoffire.gateways.gate.SpawnAlgorithms.SpawnAlgorithm;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

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

    public enum GatewayKind {
        normal,
        endless,
        apotheosis_tiered
    }

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
    private GatewayKind gatewayKind = GatewayKind.normal;
    
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
    @Nullable
    private Holder<SoundEvent> soundtrack;
    @Nullable
    private String tieredTierId;
    private final List<EndlessModifierBuilder> endlessModifierBuilders = new ArrayList<>();

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
        return new GatewayBuilder(ResourceLocation.parse(id), false);
    }

    /**
     * 创建自定义传送门的 Builder
     * @param id 自定义传送门的唯一ID（如 "mymod:custom_gate"）
     */
    public static GatewayBuilder custom(String id) {
        return new GatewayBuilder(ResourceLocation.parse(id), true);
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

    public GatewayBuilder asNormalGateway() {
        this.gatewayKind = GatewayKind.normal;
        return this;
    }

    public GatewayBuilder asEndlessGateway() {
        this.gatewayKind = GatewayKind.endless;
        return this;
    }

    public GatewayBuilder asApotheosisTieredGateway(String tierId) {
        ApotheosisGatewayCompat.checkLoaded();
        this.gatewayKind = GatewayKind.apotheosis_tiered;
        this.tieredTierId = tierId;
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

    public GatewayBuilder addModifierJson(String modifierJson) {
        globalModifiers.add(parseWaveModifier(modifierJson));
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
        this.entityTexture = ResourceLocation.parse(texturePath);
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
        this.bossBarConfig = new BossBarRenderConfig(ResourceLocation.parse(texturePath));
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
            ResourceLocation.parse(texturePath),
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
            ResourceLocation.parse(texturePath),
            ResourceLocation.parse(progressTexturePath));
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
            ResourceLocation.parse(texturePath),
            ResourceLocation.parse(progressTexturePath),
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
        this.rewards.add(new Reward.StackReward(createItemStack(itemId, count, null)));
        return this;
    }

    public GatewayBuilder addReward(String itemId, int count, String nbtString) {
        this.rewards.add(new Reward.StackReward(createItemStack(itemId, count, nbtString)));
        return this;
    }

    public GatewayBuilder addStackReward(String itemId, int count) {
        return addReward(itemId, count);
    }

    public GatewayBuilder addStackReward(String itemId, int count, String nbtString) {
        return addReward(itemId, count, nbtString);
    }

    public GatewayBuilder addStackListReward(List<ItemStack> stacks) {
        this.rewards.add(new Reward.StackListReward(List.copyOf(stacks)));
        return this;
    }

    public GatewayBuilder addStackListReward(String stacksJson) {
        this.rewards.add(createStackListReward(stacksJson));
        return this;
    }

    /**
     * 添加原始奖励JSON，支持Gateways数据包中的全部奖励类型
     */
    public GatewayBuilder addRewardJson(String rewardJson) {
        this.rewards.add(parseReward(rewardJson));
        return this;
    }

    /**
     * 添加战利品表奖励（传送门完成时发放）
     */
    public GatewayBuilder addLootTableReward(String lootTableId, int rolls) {
        return addLootTableReward(lootTableId, rolls, null);
    }

    /**
     * 添加战利品表奖励（传送门完成时发放）
     */
    public GatewayBuilder addLootTableReward(String lootTableId, int rolls, String description) {
        this.rewards.add(new Reward.LootTableReward(ResourceLocation.parse(lootTableId), rolls, description));
        return this;
    }

    /**
     * 添加实体掉落奖励（传送门完成时发放）
     */
    public GatewayBuilder addEntityLootReward(String entityId, int rolls) {
        return addEntityLootReward(entityId, null, rolls);
    }

    /**
     * 添加带NBT的实体掉落奖励（传送门完成时发放）
     */
    public GatewayBuilder addEntityLootReward(String entityId, String nbtString, int rolls) {
        this.rewards.add(new Reward.EntityLootReward(resolveEntityType(entityId), parseNullableNbt(nbtString), rolls));
        return this;
    }

    /**
     * 添加经验奖励（传送门完成时发放）
     */
    public GatewayBuilder addExperienceReward(int experience) {
        return addExperienceReward(experience, 0);
    }

    /**
     * 添加经验奖励（传送门完成时发放）
     */
    public GatewayBuilder addExperienceReward(int experience, int orbSize) {
        this.rewards.add(new Reward.ExperienceReward(experience, orbSize));
        return this;
    }

    /**
     * addExperienceReward 的别名
     */
    public GatewayBuilder addXpReward(int experience) {
        return addExperienceReward(experience);
    }

    /**
     * addExperienceReward 的别名
     */
    public GatewayBuilder addXpReward(int experience, int orbSize) {
        return addExperienceReward(experience, orbSize);
    }

    /**
     * 添加命令奖励（传送门完成时执行）
     */
    public GatewayBuilder addCommandReward(String command, String description) {
        this.rewards.add(new Reward.CommandReward(command, description));
        return this;
    }

    /**
     * 添加召唤实体奖励（传送门完成时生成）
     */
    public GatewayBuilder addSummonReward(String entityId, int count) {
        return addSummonReward(entityId, null, count);
    }

    /**
     * 添加带NBT的召唤实体奖励（传送门完成时生成）
     */
    public GatewayBuilder addSummonReward(String entityId, String nbtString, int count) {
        this.rewards.add(new Reward.SummonReward(createStandardWaveEntity(entityId, nbtString, count)));
        return this;
    }

    /**
     * 添加概率奖励（传送门完成时发放）
     */
    public GatewayBuilder addChancedReward(String rewardJson, double chance) {
        this.rewards.add(new Reward.ChancedReward(parseReward(rewardJson), (float) chance));
        return this;
    }

    /**
     * 添加Apotheosis词缀物品奖励（传送门完成时发放）
     */
    public GatewayBuilder addApotheosisAffixReward(String rarityId) {
        this.rewards.add(ApotheosisGatewayCompat.createAffixItemReward(rarityId));
        return this;
    }

    public GatewayBuilder addApotheosisAffixItemReward(String... rarityIds) {
        this.rewards.add(ApotheosisGatewayCompat.createAffixItemReward(rarityIds));
        return this;
    }

    public GatewayBuilder addApotheosisGemReward(String... purityIds) {
        this.rewards.add(ApotheosisGatewayCompat.createGemReward(purityIds));
        return this;
    }

    public GatewayBuilder addApotheosisTrueRandomGemReward(String... purityIds) {
        this.rewards.add(ApotheosisGatewayCompat.createTrueRandomGemReward(purityIds));
        return this;
    }

    public GatewayBuilder soundtrack(String soundId) {
        this.soundtrack = resolveSound(soundId);
        return this;
    }

    public GatewayBuilder addEndlessModifier() {
        asEndlessGateway();
        endlessModifierBuilders.add(new EndlessModifierBuilder(this));
        return this;
    }

    public GatewayBuilder addEndlessModifier(Consumer<EndlessModifierBuilder> consumer) {
        asEndlessGateway();
        EndlessModifierBuilder builder = new EndlessModifierBuilder(this);
        consumer.accept(builder);
        endlessModifierBuilders.add(builder);
        return this;
    }

    /**
     * 添加原始失败惩罚JSON，支持Gateways数据包中的全部失败惩罚类型
     */
    public GatewayBuilder addFailureJson(String failureJson) {
        this.failures.add(parseFailure(failureJson));
        return this;
    }

    /**
     * 添加命令失败惩罚
     */
    public GatewayBuilder addCommandFailure(String command, String description) {
        this.failures.add(new Failure.CommandFailure(command, description));
        return this;
    }

    /**
     * 添加召唤实体失败惩罚
     */
    public GatewayBuilder addSummonFailure(String entityId, int count) {
        return addSummonFailure(entityId, null, count);
    }

    /**
     * 添加带NBT的召唤实体失败惩罚
     */
    public GatewayBuilder addSummonFailure(String entityId, String nbtString, int count) {
        this.failures.add(new Failure.SummonFailure(createStandardWaveEntity(entityId, nbtString, count)));
        return this;
    }

    /**
     * 添加爆炸失败惩罚
     */
    public GatewayBuilder addExplosionFailure(double strength, boolean fire, boolean blockDamage) {
        this.failures.add(new Failure.ExplosionFailure((float) strength, fire, blockDamage));
        return this;
    }

    /**
     * 添加药水效果失败惩罚
     */
    public GatewayBuilder addMobEffectFailure(MobEffect effect, int duration) {
        return addMobEffectFailure(effect, duration, 0);
    }

    /**
     * 添加药水效果失败惩罚
     */
    public GatewayBuilder addMobEffectFailure(MobEffect effect, int duration, int amplifier) {
        if (effect == null) {
            throw new IllegalArgumentException("Effect cannot be null");
        }
        this.failures.add(new Failure.MobEffectFailure(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), duration, amplifier));
        return this;
    }

    /**
     * 添加概率失败惩罚
     */
    public GatewayBuilder addChancedFailure(String failureJson, double chance) {
        this.failures.add(new Failure.ChancedFailure(parseFailure(failureJson), (float) chance));
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
        this.rules = createRules(spawnRange, leashRange, allowDiscarding, allowDimChange, playerDamageOnly, removeMobsOnFailure, failOnOutOfBounds, spacing, followRangeBoost, defaultDropChance);
        return this;
    }

    /**
     * 设置完整传送门规则
     */
    public GatewayBuilder rules(double spawnRange, double leashRange, boolean allowDiscarding,
                                boolean allowDimChange, boolean playerDamageOnly, boolean removeMobsOnFailure,
                                boolean failOnOutOfBounds, double spacing, double followRangeBoost,
                                double defaultDropChance, boolean requiresNearbyPlayer, int lives) {
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
            (float) defaultDropChance,
            requiresNearbyPlayer,
            lives
        );
        return this;
    }

    /**
     * 设置怪物生成范围
     */
    public GatewayBuilder spawnRange(double spawnRange) {
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
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
        this.rules = createRules(
            rules.spawnRange(),
            rules.leashRange(),
            rules.allowDiscarding(),
            rules.allowDimChange(),
            rules.playerDamageOnly(),
            rules.removeOnFailure(),
            rules.failOnOutOfBounds(),
            rules.spacing(),
            rules.followRangeBoost(),
            defaultDropChance
        );
        return this;
    }

    /**
     * 设置是否需要附近玩家维持传送门
     */
    public GatewayBuilder requiresNearbyPlayer(boolean requiresNearbyPlayer) {
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
            rules.defaultDropChance(),
            requiresNearbyPlayer,
            rules.lives()
        );
        return this;
    }

    /**
     * 设置传送门生命次数，-1 表示不限制
     */
    public GatewayBuilder lives(int lives) {
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
            rules.defaultDropChance(),
            rules.requiresNearbyPlayer(),
            lives
        );
        return this;
    }

    public GatewayBuilder bossEvent(String mode, boolean fog) {
        this.bossSettings = new BossEventSettings(parseBossEventMode(mode), fog);
        return this;
    }

    public GatewayBuilder bossEventMode(String mode) {
        this.bossSettings = new BossEventSettings(parseBossEventMode(mode), bossSettings.fog());
        return this;
    }

    public GatewayBuilder bossEventFog(boolean fog) {
        this.bossSettings = new BossEventSettings(bossSettings.mode(), fog);
        return this;
    }

    public GatewayBuilder bossEventAsBar() {
        return bossEventMode("boss_bar");
    }

    public GatewayBuilder bossEventAsNamePlate() {
        return bossEventMode("name_plate");
    }

    private GateRules createRules(double spawnRange, double leashRange, boolean allowDiscarding,
                                  boolean allowDimChange, boolean playerDamageOnly, boolean removeOnFailure,
                                  boolean failOnOutOfBounds, double spacing, double followRangeBoost,
                                  double defaultDropChance) {
        return new GateRules(
            spawnRange,
            leashRange,
            allowDiscarding,
            allowDimChange,
            playerDamageOnly,
            removeOnFailure,
            failOnOutOfBounds,
            spacing,
            followRangeBoost,
            (float) defaultDropChance,
            rules.requiresNearbyPlayer(),
            rules.lives()
        );
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

        Gateway gateway;
        if (gatewayKind == GatewayKind.endless) {
            List<EndlessModifier> builtModifiers = new ArrayList<>();
            for (EndlessModifierBuilder builder : endlessModifierBuilders) {
                builtModifiers.add(builder.build());
            }
            gateway = new EndlessGateway(
                size,
                color,
                waves.getFirst(),
                builtModifiers,
                failures,
                spawnAlgo,
                rules,
                bossSettings,
                soundtrackOrDefault()
            );
        }
        else if (gatewayKind == GatewayKind.apotheosis_tiered) {
            gateway = createApotheosisTieredGateway(waves);
        }
        else {
            gateway = new NormalGateway(
                size,
                color,
                waves,
                rewards,
                failures,
                spawnAlgo,
                rules,
                bossSettings
            );
        }

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
                    player = FakePlayerFactory.getMinecraft(serverLevel);
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
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
            if (type == null) {
                throw new IllegalArgumentException("Unknown entity type: " + entityId);
            }

            EntityConfig entity = new EntityConfig(type, count);
            entities.add(entity);
            return new WaveEntityBuilder(this, entity);
        }

        /**
         * 添加原始实体JSON，支持Gateways数据包中的全部波次实体类型
         */
        public WaveBuilder addEntityJson(String entityJson) {
            customEntities.add(parseWaveEntity(entityJson));
            return this;
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

        public WaveBuilder addApotheosisInvader(String invaderId, int count) {
            customEntities.add(ApotheosisGatewayCompat.createInvaderWaveEntity(invaderId, count, null));
            return this;
        }

        public WaveBuilder addApotheosisInvader(String invaderId, int count, String description) {
            customEntities.add(ApotheosisGatewayCompat.createInvaderWaveEntity(invaderId, count, description));
            return this;
        }

        public WaveBuilder addApotheosisTrueRandomInvader(int count) {
            customEntities.add(ApotheosisGatewayCompat.createTrueRandomInvaderWaveEntity(count, null));
            return this;
        }

        public WaveBuilder addApotheosisTrueRandomInvader(int count, String description) {
            customEntities.add(ApotheosisGatewayCompat.createTrueRandomInvaderWaveEntity(count, description));
            return this;
        }

        public WaveBuilder addApotheosisElite(String eliteId, String entityId) {
            return addApotheosisElite(eliteId, entityId, null, 1);
        }

        public WaveBuilder addApotheosisElite(String eliteId, String entityId, String description, int count) {
            String baseEntityJson = "{\"type\":\"gateways:standard\",\"entity\":\"" + ResourceLocation.parse(entityId) + "\",\"count\":" + count + "}";
            customEntities.add(ApotheosisGatewayCompat.createEliteWaveEntity(eliteId, baseEntityJson, description));
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

        public WaveBuilder addModifierJson(String modifierJson) {
            modifiers.add(parseWaveModifier(modifierJson));
            return this;
        }

        public WaveBuilder addApotheosisAffixModifier(float affixChance, String... rarityIds) {
            modifiers.add(ApotheosisGatewayCompat.createAffixWaveModifier(affixChance, rarityIds));
            return this;
        }

        public WaveBuilder addApotheosisPassenger(String passengerEntityJson) {
            modifiers.add(ApotheosisGatewayCompat.createPassengerWaveModifier(passengerEntityJson));
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
            this.rewards.add(new Reward.StackReward(createItemStack(itemId, count, null)));
            return this;
        }

        public WaveBuilder addReward(String itemId, int count, String nbtString) {
            this.rewards.add(new Reward.StackReward(createItemStack(itemId, count, nbtString)));
            return this;
        }

        public WaveBuilder addStackReward(String itemId, int count) {
            return addReward(itemId, count);
        }

        public WaveBuilder addStackReward(String itemId, int count, String nbtString) {
            return addReward(itemId, count, nbtString);
        }

        public WaveBuilder addStackListReward(List<ItemStack> stacks) {
            this.rewards.add(new Reward.StackListReward(List.copyOf(stacks)));
            return this;
        }

        public WaveBuilder addStackListReward(String stacksJson) {
            this.rewards.add(createStackListReward(stacksJson));
            return this;
        }

        /**
         * 添加原始奖励JSON，支持Gateways数据包中的全部奖励类型
         */
        public WaveBuilder addRewardJson(String rewardJson) {
            this.rewards.add(parseReward(rewardJson));
            return this;
        }

        /**
         * 添加战利品表奖励到波次
         */
        public WaveBuilder addLootTableReward(String lootTableId, int rolls) {
            return addLootTableReward(lootTableId, rolls, null);
        }

        /**
         * 添加战利品表奖励到波次
         */
        public WaveBuilder addLootTableReward(String lootTableId, int rolls, String description) {
            this.rewards.add(new Reward.LootTableReward(ResourceLocation.parse(lootTableId), rolls, description));
            return this;
        }

        /**
         * 添加实体掉落奖励到波次
         */
        public WaveBuilder addEntityLootReward(String entityId, int rolls) {
            return addEntityLootReward(entityId, null, rolls);
        }

        /**
         * 添加带NBT的实体掉落奖励到波次
         */
        public WaveBuilder addEntityLootReward(String entityId, String nbtString, int rolls) {
            this.rewards.add(new Reward.EntityLootReward(resolveEntityType(entityId), parseNullableNbt(nbtString), rolls));
            return this;
        }

        /**
         * 添加经验奖励到波次
         */
        public WaveBuilder addExperienceReward(int experience) {
            return addExperienceReward(experience, 0);
        }

        /**
         * 添加经验奖励到波次
         */
        public WaveBuilder addExperienceReward(int experience, int orbSize) {
            this.rewards.add(new Reward.ExperienceReward(experience, orbSize));
            return this;
        }

        /**
         * addExperienceReward 的别名
         */
        public WaveBuilder addXpReward(int experience) {
            return addExperienceReward(experience);
        }

        /**
         * addExperienceReward 的别名
         */
        public WaveBuilder addXpReward(int experience, int orbSize) {
            return addExperienceReward(experience, orbSize);
        }

        /**
         * 添加命令奖励到波次
         */
        public WaveBuilder addCommandReward(String command, String description) {
            this.rewards.add(new Reward.CommandReward(command, description));
            return this;
        }

        /**
         * 添加召唤实体奖励到波次
         */
        public WaveBuilder addSummonReward(String entityId, int count) {
            return addSummonReward(entityId, null, count);
        }

        /**
         * 添加带NBT的召唤实体奖励到波次
         */
        public WaveBuilder addSummonReward(String entityId, String nbtString, int count) {
            this.rewards.add(new Reward.SummonReward(createStandardWaveEntity(entityId, nbtString, count)));
            return this;
        }

        /**
         * 添加概率奖励到波次
         */
        public WaveBuilder addChancedReward(String rewardJson, double chance) {
            this.rewards.add(new Reward.ChancedReward(parseReward(rewardJson), (float) chance));
            return this;
        }

        /**
         * 添加Apotheosis词缀物品奖励到波次
         */
        public WaveBuilder addApotheosisAffixReward(String rarityId) {
            this.rewards.add(ApotheosisGatewayCompat.createAffixItemReward(rarityId));
            return this;
        }

        public WaveBuilder addApotheosisAffixItemReward(String... rarityIds) {
            this.rewards.add(ApotheosisGatewayCompat.createAffixItemReward(rarityIds));
            return this;
        }

        public WaveBuilder addApotheosisGemReward(String... purityIds) {
            this.rewards.add(ApotheosisGatewayCompat.createGemReward(purityIds));
            return this;
        }

        public WaveBuilder addApotheosisTrueRandomGemReward(String... purityIds) {
            this.rewards.add(ApotheosisGatewayCompat.createTrueRandomGemReward(purityIds));
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

                return new StandardWaveEntity(
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

            public EntityModifierBuilder addModifierJson(String modifierJson) {
                entity.modifiers.add(parseWaveModifier(modifierJson));
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
                    return AttributeModifier.Operation.ADD_VALUE;
                }

                return switch (operation) {
                    case addition -> AttributeModifier.Operation.ADD_VALUE;
                    case multiply_base -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case multiply_total -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                };
            }

            public static WaveModifier.AttributeModifier createAttributeModifier(Attribute attribute, Number value, AttributeOperation operation) {
                if (attribute == null) {
                    throw new IllegalArgumentException("Attribute cannot be null");
                }

                AttributeModifier.Operation op = parseAttributeOperation(operation);
                return new WaveModifier.AttributeModifier(
                    new RandomAttributeModifier(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), op, StepFunction.constant(value.floatValue()))
                );
            }

            public static WaveModifier.EffectModifier createEffectModifier(MobEffect effect, Number amplifier, Number chance, boolean ambient, boolean visible) {
                if (effect == null) {
                    throw new IllegalArgumentException("Effect cannot be null");
                }

                return new WaveModifier.EffectModifier(
                    new ChancedEffectInstance(
                        chance.floatValue(),
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                        StepFunction.constant(amplifier.floatValue()),
                        ambient,
                        visible
                    )
                );
            }

            public static WaveModifier.GearSetModifier createGearSetModifier(String gearSetId) {
                return WaveModifier.GearSetModifier.create(ResourceLocation.parse(gearSetId));
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
    private static CompoundTag parseNullableNbt(@Nullable String nbtString) {
        return nbtString == null || nbtString.isBlank() ? null : parseNbt(nbtString);
    }

    private static ItemStack createItemStack(String itemId, int count, @Nullable String nbtString) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)));
        stack.setCount(count);
        CompoundTag tag = parseNullableNbt(nbtString);
        if (tag != null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    private static Reward createStackListReward(String stacksJson) {
        JsonElement json = JsonParser.parseString(stacksJson);
        JsonObject rewardJson;
        if (json.isJsonArray()) {
            rewardJson = new JsonObject();
            rewardJson.addProperty("type", "gateways:stack_list");
            rewardJson.add("stacks", json);
        } else if (json.isJsonObject()) {
            rewardJson = json.getAsJsonObject();
            if (!rewardJson.has("type")) {
                rewardJson.addProperty("type", "gateways:stack_list");
            }
        } else {
            throw new IllegalArgumentException("无法解码Gateways物品列表奖励JSON。");
        }
        return parseReward(rewardJson);
    }

    private static BossEventSettings.Mode parseBossEventMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "name_plate", "nameplate", "name" -> BossEventSettings.Mode.NAME_PLATE;
            case "boss_bar", "bar", "bossbar" -> BossEventSettings.Mode.BOSS_BAR;
            default -> throw new IllegalArgumentException("Unknown boss event mode: " + mode);
        };
    }

    private static EntityType<?> resolveEntityType(String entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
        if (type == null) {
            throw new IllegalArgumentException("Unknown entity type: " + entityId);
        }
        return type;
    }

    private static WaveEntity createStandardWaveEntity(String entityId, @Nullable String nbtString, int count) {
        return new StandardWaveEntity(
            resolveEntityType(entityId),
            Optional.empty(),
            Optional.ofNullable(parseNullableNbt(nbtString)),
            List.of(),
            true,
            count
        );
    }

    private static Reward parseReward(String rewardJson) {
        return parseReward(JsonParser.parseString(rewardJson));
    }

    private static Reward parseReward(JsonElement json) {
        return Reward.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Gateways奖励JSON。"));
    }

    private static Failure parseFailure(String failureJson) {
        JsonElement json = JsonParser.parseString(failureJson);
        return Failure.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Gateways失败惩罚JSON。"));
    }

    private static WaveEntity parseWaveEntity(String entityJson) {
        JsonElement json = JsonParser.parseString(entityJson);
        return WaveEntity.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Gateways波次实体JSON。"));
    }

    private static WaveModifier parseWaveModifier(String modifierJson) {
        JsonElement json = JsonParser.parseString(modifierJson);
        return WaveModifier.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Gateways波次修饰器JSON。"));
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

    private Gateway createApotheosisTieredGateway(List<Wave> waves) {
        ApotheosisGatewayCompat.checkLoaded();
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:tiered");
        JsonObject settings = new JsonObject();
        settings.addProperty("tier", parseRequiredTierId());
        settings.addProperty("size", size.name().toLowerCase());
        settings.addProperty("color", String.format("#%06X", color.getValue() & 0xFFFFFF));
        if (soundtrack != null) {
            settings.addProperty("soundtrack", BuiltInRegistries.SOUND_EVENT.getKey(soundtrack.value()).toString());
        }
        json.add("settings", settings);
        json.add("waves", parseJsonArray(waves, Wave.CODEC, "无法编码Tiered网关波次。"));
        json.add("rewards", parseJsonArray(rewards, Reward.CODEC, "无法编码Tiered网关奖励。"));
        json.add("failures", parseJsonArray(failures, Failure.CODEC, "无法编码Tiered网关失败惩罚。"));
        json.add("rules", GateRules.CODEC.encodeStart(JsonOps.INSTANCE, rules).getOrThrow(IllegalStateException::new));
        json.add("boss_event", BossEventSettings.CODEC.encodeStart(JsonOps.INSTANCE, bossSettings).getOrThrow(IllegalStateException::new));
        return RegistryHelper.decodeFromJson(GatewayRegistry.INSTANCE, json.toString());
    }

    private Holder<SoundEvent> soundtrackOrDefault() {
        return soundtrack != null ? soundtrack : BuiltInRegistries.SOUND_EVENT.wrapAsHolder(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace("music.creative")));
    }

    private String parseRequiredTierId() {
        if (tieredTierId == null || tieredTierId.isBlank()) {
            throw new IllegalStateException("Apotheosis tiered gateway requires a tier id.");
        }
        return switch (tieredTierId.toLowerCase()) {
            case "haven", "frontier", "ascent", "summit", "pinnacle" -> tieredTierId.toLowerCase();
            default -> throw new IllegalArgumentException("Unknown Apotheosis world tier: " + tieredTierId);
        };
    }

    private static Holder<SoundEvent> resolveSound(String soundId) {
        ResourceLocation id = ResourceLocation.parse(soundId);
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound == null) {
            throw new IllegalArgumentException("Unknown sound event: " + soundId);
        }
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
    }

    private static <T> com.google.gson.JsonArray parseJsonArray(List<T> values, com.mojang.serialization.Codec<T> codec, String errorMessage) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (T value : values) {
            array.add(codec.encodeStart(JsonOps.INSTANCE, value)
                .resultOrPartial(message -> {
                    throw new IllegalArgumentException(message);
                })
                .orElseThrow(() -> new IllegalArgumentException(errorMessage)));
        }
        return array;
    }

    public static class EndlessModifierBuilder {
        private final GatewayBuilder parent;
        private final List<WaveEntity> entities = new ArrayList<>();
        private final List<Reward> rewards = new ArrayList<>();
        private final List<WaveModifier> modifiers = new ArrayList<>();
        private String applicationModeJson = "{\"type\":\"gateways:after_wave\",\"wave\":1}";
        private int maxWaveTime;
        private int setupTime;

        EndlessModifierBuilder(GatewayBuilder parent) {
            this.parent = parent;
        }

        public EndlessModifierBuilder applicationModeJson(String json) {
            this.applicationModeJson = json;
            return this;
        }

        public EndlessModifierBuilder maxTimeDelta(int ticks) {
            this.maxWaveTime = ticks;
            return this;
        }

        public EndlessModifierBuilder setupTimeDelta(int ticks) {
            this.setupTime = ticks;
            return this;
        }

        public EndlessModifierBuilder addEntityJson(String entityJson) {
            this.entities.add(parseWaveEntity(entityJson));
            return this;
        }

        public EndlessModifierBuilder addRewardJson(String rewardJson) {
            this.rewards.add(parseReward(rewardJson));
            return this;
        }

        public EndlessModifierBuilder addModifierJson(String modifierJson) {
            this.modifiers.add(parseWaveModifier(modifierJson));
            return this;
        }

        public GatewayBuilder buildModifier() {
            return parent;
        }

        EndlessModifier build() {
            return new EndlessModifier(parseApplicationMode(applicationModeJson), entities, rewards, modifiers, maxWaveTime, setupTime);
        }
    }

    private static ApplicationMode parseApplicationMode(String applicationModeJson) {
        JsonElement json = JsonParser.parseString(applicationModeJson);
        return ApplicationMode.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Endless应用模式JSON。"));
    }
}
