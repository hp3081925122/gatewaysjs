package com.hp.gatewaysjs.compat;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

public final class ApotheosisGatewayCompat {

    private static final String APOTHEOSIS = "apotheosis";
    private static final String REQUIRED_ERROR = "使用Apotheosis网关Boss或词缀奖励需要安装Apotheosis。";

    private ApotheosisGatewayCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(APOTHEOSIS);
    }

    public static void checkLoaded() {
        if (!isLoaded()) {
            throw new IllegalStateException(REQUIRED_ERROR);
        }
    }

    public static WaveEntity createBossWaveEntity() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:boss");
        return decodeWaveEntity(json);
    }

    public static WaveEntity createBossWaveEntity(String bossId) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:boss");
        json.addProperty("boss", new ResourceLocation(bossId).toString());
        return decodeWaveEntity(json);
    }

    public static Reward createAffixReward(String rarityId) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:affix");
        json.addProperty("rarity", new ResourceLocation(rarityId).toString());
        return decodeReward(json);
    }

    private static WaveEntity decodeWaveEntity(JsonObject json) {
        checkLoaded();
        return WaveEntity.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Apotheosis Boss波次实体。"));
    }

    private static Reward decodeReward(JsonObject json) {
        checkLoaded();
        return Reward.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Apotheosis词缀奖励。"));
    }
}
