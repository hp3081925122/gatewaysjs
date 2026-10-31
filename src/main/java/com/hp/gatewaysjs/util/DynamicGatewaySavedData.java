package com.hp.gatewaysjs.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.hp.gatewaysjs.Gatewaysjs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicGatewaySavedData extends SavedData {

    public static final String DATA_NAME = "gatewaysjs_dynamic_gateways";

    private static final Codec<GatewayEntry> GATEWAY_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GatewayEntry::id),
            Codec.STRING.fieldOf("json").forGetter(GatewayEntry::json),
            Codec.STRING.optionalFieldOf("displayName", "").forGetter(entry -> nullToEmpty(entry.displayName())),
            Codec.STRING.optionalFieldOf("tooltipKey", "").forGetter(entry -> nullToEmpty(entry.tooltipKey())),
            Codec.STRING.optionalFieldOf("tooltipText", "").forGetter(entry -> nullToEmpty(entry.tooltipText())),
            Codec.BOOL.optionalFieldOf("hidePearlInCreativeTab", false).forGetter(GatewayEntry::hidePearlInCreativeTab)
    ).apply(instance, GatewayEntry::new));

    private static final Codec<DynamicGatewaySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GATEWAY_ENTRY_CODEC.listOf().optionalFieldOf("gateways", List.of()).forGetter(DynamicGatewaySavedData::codecEntries)
    ).apply(instance, DynamicGatewaySavedData::fromCodecEntries));

    private static final SavedDataType<DynamicGatewaySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Gatewaysjs.MODID, DATA_NAME), DynamicGatewaySavedData::new, CODEC
    );

    private final Map<Identifier, GatewayData> gateways = new LinkedHashMap<>();

    public static DynamicGatewaySavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private static DynamicGatewaySavedData fromCodecEntries(List<GatewayEntry> entries) {
        DynamicGatewaySavedData data = new DynamicGatewaySavedData();
        for (GatewayEntry entry : entries) {
            try {
                data.gateways.put(Identifier.parse(entry.id()), entry.toData());
            } catch (RuntimeException ignored) {
            }
        }
        return data;
    }

    private List<GatewayEntry> codecEntries() {
        List<GatewayEntry> entries = new ArrayList<>();
        for (Map.Entry<Identifier, GatewayData> entry : gateways.entrySet()) {
            GatewayData data = entry.getValue();
            entries.add(new GatewayEntry(entry.getKey().toString(), data.json(), data.displayName(), data.tooltipKey(), data.tooltipText(), data.hidePearlInCreativeTab()));
        }
        return entries;
    }

    public void put(Identifier id, GatewayData gatewayData) {
        this.gateways.put(id, gatewayData);
        this.setDirty();
    }

    public String get(Identifier id) {
        GatewayData gatewayData = this.gateways.get(id);
        return gatewayData == null ? null : gatewayData.json();
    }

    public GatewayData getData(Identifier id) {
        return this.gateways.get(id);
    }

    public Map<Identifier, GatewayData> entries() {
        return new LinkedHashMap<>(this.gateways);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private record GatewayEntry(String id, String json, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
        private GatewayData toData() {
            return new GatewayData(json, emptyToNull(displayName), emptyToNull(tooltipKey), emptyToNull(tooltipText), hidePearlInCreativeTab);
        }
    }

    public record GatewayData(String json, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
    }
}




