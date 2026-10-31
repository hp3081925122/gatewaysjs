package com.hp.gatewaysjs.compat;

import com.google.gson.JsonObject;
import com.hp.gatewaysjs.Gatewaysjs;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.gateways.gate.Reward;
import dev.shadowsoffire.gateways.gate.WaveEntity;
import dev.shadowsoffire.gateways.gate.WaveModifier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.Arrays;

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
        Gatewaysjs.getLogger().debug("Mapping the legacy Apotheosis boss gateway API to a random invader.");
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:invader");
        return decodeWaveEntity(json);
    }

    public static WaveEntity createBossWaveEntity(String bossId) {
        Gatewaysjs.getLogger().debug("Mapping the legacy Apotheosis boss gateway API to invader {}.", bossId);
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:invader");
        json.addProperty("invader", ResourceLocation.parse(bossId).toString());
        return decodeWaveEntity(json);
    }

    public static Reward createAffixReward(String rarityId) {
        return createAffixItemReward(rarityId);
    }

    public static Reward createAffixItemReward(String... rarityIds) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:affix_item");
        json.add("rarities", buildStringArray(rarityIds));
        return decodeReward(json);
    }

    public static Reward createGemReward(String... purityIds) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:gem");
        json.add("purities", buildPurityArray(purityIds));
        return decodeReward(json);
    }

    public static Reward createTrueRandomGemReward(String... purityIds) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:true_random_gem");
        json.add("purities", buildPurityArray(purityIds));
        return decodeReward(json);
    }

    public static WaveEntity createInvaderWaveEntity(String invaderId, int count, String description) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:invader");
        json.addProperty("invader", ResourceLocation.parse(invaderId).toString());
        json.addProperty("count", count);
        if (description != null && !description.isBlank()) {
            json.addProperty("desc", description);
        }
        return decodeWaveEntity(json);
    }

    public static WaveEntity createTrueRandomInvaderWaveEntity(int count, String description) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:true_random_invader");
        json.addProperty("count", count);
        if (description != null && !description.isBlank()) {
            json.addProperty("desc", description);
        }
        return decodeWaveEntity(json);
    }

    public static WaveEntity createEliteWaveEntity(String eliteId, String baseEntityJson, String description) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:elite");
        json.add("base_entity", com.google.gson.JsonParser.parseString(baseEntityJson));
        json.addProperty("elite", ResourceLocation.parse(eliteId).toString());
        if (description != null && !description.isBlank()) {
            json.addProperty("desc", description);
        }
        return decodeWaveEntity(json);
    }

    public static WaveModifier createAffixWaveModifier(float affixChance, String... rarityIds) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:affix");
        json.addProperty("affix_chance", affixChance);
        if (rarityIds != null && rarityIds.length > 0) {
            json.add("rarities", buildStringArray(rarityIds));
        }
        return decodeWaveModifier(json);
    }

    public static WaveModifier createPassengerWaveModifier(String entityJson) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "apotheosis:passenger");
        json.add("entity", com.google.gson.JsonParser.parseString(entityJson));
        return decodeWaveModifier(json);
    }

    private static WaveEntity decodeWaveEntity(JsonObject json) {
        checkLoaded();
        return WaveEntity.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Apotheosis Boss波次实体。"));
    }

    private static WaveModifier decodeWaveModifier(JsonObject json) {
        checkLoaded();
        return WaveModifier.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Apotheosis波次修饰器。"));
    }

    private static Reward decodeReward(JsonObject json) {
        checkLoaded();
        return Reward.CODEC.parse(JsonOps.INSTANCE, json)
            .resultOrPartial(message -> {
                throw new IllegalArgumentException(message);
            })
            .orElseThrow(() -> new IllegalArgumentException("无法解码Apotheosis词缀奖励。"));
    }

    private static com.google.gson.JsonArray buildPurityArray(String... values) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        if (values == null) {
            return array;
        }
        Arrays.stream(values)
            .filter(value -> value != null && !value.isBlank())
            .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
            .map(value -> value.startsWith(APOTHEOSIS + ":") ? value.substring(APOTHEOSIS.length() + 1) : value)
            .forEach(array::add);
        return array;
    }

    private static com.google.gson.JsonArray buildStringArray(String... values) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        if (values == null) {
            return array;
        }
        Arrays.stream(values)
            .filter(value -> value != null && !value.isBlank())
            .map(ResourceLocation::parse)
            .map(ResourceLocation::toString)
            .forEach(array::add);
        return array;
    }
}
