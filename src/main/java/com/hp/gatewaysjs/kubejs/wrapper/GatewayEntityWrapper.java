package com.hp.gatewaysjs.kubejs.wrapper;

import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.gate.Wave;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * GatewayEntity的JavaScript包装类
 * 提供对传送门实体的各种操作和查询功能
 */
@SuppressWarnings("removal")
public class GatewayEntityWrapper {

    private final GatewayEntity entity;

    public GatewayEntityWrapper(GatewayEntity entity) {
        this.entity = entity;
    }

    /**
     * 获取原始的GatewayEntity对象
     * @return GatewayEntity实例
     */
    public GatewayEntity getNative() {
        return entity;
    }

    // ============== 基本信息 ==============

    /**
     * 获取传送门的资源定位符ID
     * @return 传送门ID，如 "minecraft:example_gate"
     */
    public String getGatewayId() {
        Gateway gateway = entity.getGateway();
        if (gateway != null) {
            ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
            return id != null ? id.toString() : "unknown";
        }
        return "unknown";
    }

    /**
     * 获取传送门的尺寸
     * @return 尺寸名称 (SMALL/MEDIUM/LARGE)
     */
    public String getSize() {
        Gateway gateway = entity.getGateway();
        return gateway != null ? gateway.size().name() : "UNKNOWN";
    }

    /**
     * 获取实体的UUID
     * @return UUID字符串
     */
    public String getUuid() {
        return entity.getUUID().toString();
    }

    /**
     * 获取实体的实体ID
     * @return 实体ID
     */
    public int getId() {
        return entity.getId();
    }

    // ============== 位置信息 ==============

    /**
     * 获取X坐标
     * @return X坐标
     */
    public double getX() {
        return entity.getX();
    }

    /**
     * 获取Y坐标
     * @return Y坐标
     */
    public double getY() {
        return entity.getY();
    }

    /**
     * 获取Z坐标
     * @return Z坐标
     */
    public double getZ() {
        return entity.getZ();
    }

    /**
     * 获取方块位置
     * @return 方块位置字符串 "x, y, z"
     */
    public String getBlockPos() {
        BlockPos pos = entity.blockPosition();
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /**
     * 获取维度ID
     * @return 维度ID，如 "minecraft:overworld"
     */
    public String getDimension() {
        return entity.level().dimension().location().toString();
    }

    // ============== 波次信息 ==============

    /**
     * 获取当前波次索引（从0开始）
     * @return 当前波次
     */
    public int getWave() {
        return entity.getWave();
    }

    /**
     * 获取当前波次剩余敌人数量
     * @return 敌人数量
     */
    public int getActiveEnemies() {
        return entity.getActiveEnemies();
    }

    /**
     * 判断当前波次是否激活
     * @return true表示波次激活
     */
    public boolean isWaveActive() {
        return entity.isWaveActive();
    }

    /**
     * 获取当前波次已激活的时间（tick）
     * @return 激活时间
     */
    public int getTicksActive() {
        return entity.getTicksActive();
    }

    /**
     * 获取当前波次的最大持续时间
     * @return 最大时间（tick）
     */
    public int getMaxWaveTime() {
        return entity.getMaxWaveTime();
    }

    /**
     * 获取当前波次的准备时间
     * @return 准备时间（tick）
     */
    public int getSetupTime() {
        return entity.getSetupTime();
    }

    /**
     * 判断传送门是否已完成
     * @return true表示已完成
     */
    public boolean isCompleted() {
        return entity.isCompleted();
    }

    /**
     * 获取当前波次对象（只读）
     * @return 当前波次的包装对象
     */
    public WaveWrapper getCurrentWave() {
        Wave wave = entity.getCurrentWave();
        return wave != null ? new WaveWrapper(wave) : null;
    }

    // ============== 召唤者信息 ==============

    /**
     * 获取召唤者玩家对象
     * @return 玩家对象，可能为null
     */
    public Player getSummoner() {
        return entity.summonerOrClosest();
    }

    /**
     * 获取召唤者名称
     * @return 召唤者名称
     */
    public String getSummonerName() {
        Player player = getSummoner();
        return player != null ? player.getName().getString() : "Unknown";
    }

    /**
     * 获取召唤者UUID
     * @return UUID字符串，可能为null
     */
    public String getSummonerUuid() {
        Player summoner = entity.summonerOrClosest();
        return summoner != null ? summoner.getUUID().toString() : null;
    }

    // ============== 动态修改方法 ==============

    /**
     * 立即生成物品（不进入队列）
     * @param itemId 物品ID
     * @param count 数量
     */
    public void spawnItem(String itemId, int count) {
        if (entity.level() instanceof ServerLevel) {
            ItemStack stack = new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId))
            );
            stack.setCount(count);
            entity.spawnItem(stack);
        }
    }

    /**
     * 立即生成完成奖励物品（无生命周期限制，位置更精确）
     * @param itemId 物品ID
     * @param count 数量
     */
    public void spawnCompletionItem(String itemId, int count) {
        if (entity.level() instanceof ServerLevel) {
            ItemStack stack = new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new ResourceLocation(itemId))
            );
            stack.setCount(count);
            entity.spawnCompletionItem(stack);
        }
    }

    /**
     * 播放音效
     * @param soundId 音效ID，如 "minecraft:entity.player.levelup"
     */
    public void playSound(String soundId) {
        if (entity.level() instanceof ServerLevel) {
            net.minecraft.sounds.SoundEvent sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(soundId));
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    /**
     * 生成粒子效果
     */
    public void spawnParticles() {
        entity.spawnParticle(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
            dev.shadowsoffire.gateways.net.ParticleMessage.Type.SPAWNED);
    }

    /**
     * 获取世界对象
     * @return Level对象
     */
    public net.minecraft.world.level.Level getLevel() {
        return entity.level();
    }

    /**
     * 获取服务器世界对象
     * @return ServerLevel对象，客户端返回null
     */
    public ServerLevel getServerLevel() {
        return entity.level() instanceof ServerLevel sl ? sl : null;
    }

    /**
     * 移动传送门到新位置
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     */
    public void teleport(double x, double y, double z) {
        entity.setPos(x, y, z);
    }

    /**
     * 设置自定义名称
     * @param name 名称文本
     */
    public void setCustomName(String name) {
        entity.setCustomName(net.minecraft.network.chat.Component.literal(name));
    }

    /**
     * 获取自定义名称
     * @return 自定义名称
     */
    public String getCustomName() {
        return entity.getCustomName() != null ? entity.getCustomName().getString() : null;
    }

    // ============== 工具方法 ==============

    /**
     * 判断传送门是否有效（配置已绑定）
     * @return true表示有效
     */
    public boolean isValid() {
        return entity.isValid();
    }

    /**
     * 移除传送门实体
     */
    public void discard() {
        entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    /**
     * 获取传送门配置对象
     * @return Gateway包装对象
     */
    public GatewayWrapper getGateway() {
        Gateway gateway = entity.getGateway();
        return gateway != null ? new GatewayWrapper(gateway) : null;
    }

    @Override
    public String toString() {
        return "GatewayEntityWrapper{" +
            "id=" + getGatewayId() +
            ", wave=" + getWave() +
            ", enemies=" + getActiveEnemies() +
            ", pos=" + getBlockPos() +
            '}';
    }
}
