package com.hp.gatewaysjs.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将运行时创建的网关定义保存到世界存档。
 */
public class DynamicGatewaySavedData extends SavedData {

    public static final String DATA_NAME = "gatewaysjs_dynamic_gateways";

    private final Map<ResourceLocation, GatewayData> gateways = new LinkedHashMap<>();

    public static DynamicGatewaySavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(DynamicGatewaySavedData::new, DynamicGatewaySavedData::load), DATA_NAME);
    }

    public static DynamicGatewaySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
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

    // 将所有动态网关定义写入世界数据文件。
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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

    // 更新已保存的动态网关定义。
    public void put(ResourceLocation id, GatewayData gatewayData) {
        this.gateways.put(id, gatewayData);
        this.setDirty();
    }

    // 查询一个已保存的动态网关定义。
    public String get(ResourceLocation id) {
        GatewayData gatewayData = this.gateways.get(id);
        return gatewayData == null ? null : gatewayData.json();
    }

    public GatewayData getData(ResourceLocation id) {
        return this.gateways.get(id);
    }

    // 返回所有已保存动态网关的快照。
    public Map<ResourceLocation, GatewayData> entries() {
        return new LinkedHashMap<>(this.gateways);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    public record GatewayData(String json, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
    }
}
