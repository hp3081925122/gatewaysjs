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

    private final Map<ResourceLocation, String> gateways = new LinkedHashMap<>();

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

            data.gateways.put(ResourceLocation.parse(entry.getString("id")), entry.getString("json"));
        }

        return data;
    }

    // Saves every dynamic gateway definition into the world data file.
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();

        for (Map.Entry<ResourceLocation, String> entry : this.gateways.entrySet()) {
            CompoundTag gatewayTag = new CompoundTag();
            gatewayTag.putString("id", entry.getKey().toString());
            gatewayTag.putString("json", entry.getValue());
            entries.add(gatewayTag);
        }

        tag.put("gateways", entries);
        return tag;
    }

    // Updates a saved dynamic gateway definition.
    public void put(ResourceLocation id, String json) {
        this.gateways.put(id, json);
        this.setDirty();
    }

    // Looks up one saved dynamic gateway definition.
    public String get(ResourceLocation id) {
        return this.gateways.get(id);
    }

    // Returns a snapshot of all saved dynamic gateways.
    public Map<ResourceLocation, String> entries() {
        return new LinkedHashMap<>(this.gateways);
    }
}
