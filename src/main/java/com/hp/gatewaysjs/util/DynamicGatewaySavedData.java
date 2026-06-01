package com.hp.gatewaysjs.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores runtime-created gateway definitions in the world save.
 */
public class DynamicGatewaySavedData extends SavedData {

    public static final String DATA_NAME = "gatewaysjs_dynamic_gateways";

    private final Map<ResourceLocation, GatewayData> gateways = new LinkedHashMap<>();

    public static DynamicGatewaySavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(DynamicGatewaySavedData::load, DynamicGatewaySavedData::new, DATA_NAME);
    }

    public static DynamicGatewaySavedData load(CompoundTag tag) {
        DynamicGatewaySavedData data = new DynamicGatewaySavedData();
        ListTag entries = tag.getList("gateways", Tag.TAG_COMPOUND);

        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.contains("id", Tag.TAG_STRING) || !entry.contains("json", Tag.TAG_STRING)) {
                continue;
            }

            ResourceLocation id = ResourceLocation.parse(entry.getString("id"));
            String displayName = entry.contains("displayName", Tag.TAG_STRING) ? entry.getString("displayName") : null;
            String tooltipKey = entry.contains("tooltipKey", Tag.TAG_STRING) ? entry.getString("tooltipKey") : null;
            String tooltipText = entry.contains("tooltipText", Tag.TAG_STRING) ? entry.getString("tooltipText") : null;
            boolean hidePearlInCreativeTab = entry.getBoolean("hidePearlInCreativeTab");
            data.gateways.put(id, new GatewayData(entry.getString("json"), emptyToNull(displayName), emptyToNull(tooltipKey), emptyToNull(tooltipText), hidePearlInCreativeTab));
        }

        return data;
    }

    // Saves every dynamic gateway definition into the world data file.
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();

        for (Map.Entry<ResourceLocation, GatewayData> entry : this.gateways.entrySet()) {
            CompoundTag gatewayTag = new CompoundTag();
            gatewayTag.putString("id", entry.getKey().toString());
            gatewayTag.putString("json", entry.getValue().json());
            if (entry.getValue().displayName() != null) {
                gatewayTag.putString("displayName", entry.getValue().displayName());
            }
            if (entry.getValue().tooltipKey() != null) {
                gatewayTag.putString("tooltipKey", entry.getValue().tooltipKey());
            }
            if (entry.getValue().tooltipText() != null) {
                gatewayTag.putString("tooltipText", entry.getValue().tooltipText());
            }
            if (entry.getValue().hidePearlInCreativeTab()) {
                gatewayTag.putBoolean("hidePearlInCreativeTab", true);
            }
            entries.add(gatewayTag);
        }

        tag.put("gateways", entries);
        return tag;
    }

    // Updates a saved dynamic gateway definition.
    public void put(ResourceLocation id, GatewayData gatewayData) {
        this.gateways.put(id, gatewayData);
        this.setDirty();
    }

    // Looks up one saved dynamic gateway definition.
    public String get(ResourceLocation id) {
        GatewayData gatewayData = this.gateways.get(id);
        return gatewayData == null ? null : gatewayData.json();
    }

    public GatewayData getData(ResourceLocation id) {
        return this.gateways.get(id);
    }

    // Returns a snapshot of all saved dynamic gateways.
    public Map<ResourceLocation, GatewayData> entries() {
        return new LinkedHashMap<>(this.gateways);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    public record GatewayData(String json, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
    }
}
