package com.hp.gatewaysjs.network;

import com.hp.gatewaysjs.util.DynamicGatewayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncDynamicGatewayMetadataPacket(Map<ResourceLocation, Entry> entries) {

    public static SyncDynamicGatewayMetadataPacket create() {
        return new SyncDynamicGatewayMetadataPacket(DynamicGatewayManager.copyMetadata());
    }

    public static void encode(SyncDynamicGatewayMetadataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entries.size());
        packet.entries.forEach((id, entry) -> {
            buffer.writeResourceLocation(id);
            buffer.writeUtf(entry.displayName() == null ? "" : entry.displayName());
            buffer.writeUtf(entry.tooltipKey() == null ? "" : entry.tooltipKey());
            buffer.writeUtf(entry.tooltipText() == null ? "" : entry.tooltipText());
            buffer.writeBoolean(entry.hidePearlInCreativeTab());
        });
    }

    public static SyncDynamicGatewayMetadataPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, Entry> entries = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            String displayName = emptyToNull(buffer.readUtf());
            String tooltipKey = emptyToNull(buffer.readUtf());
            String tooltipText = emptyToNull(buffer.readUtf());
            boolean hidePearlInCreativeTab = buffer.readBoolean();
            entries.put(id, new Entry(displayName, tooltipKey, tooltipText, hidePearlInCreativeTab));
        }
        return new SyncDynamicGatewayMetadataPacket(entries);
    }

    public static void handle(SyncDynamicGatewayMetadataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DynamicGatewayManager.applyClientMetadata(packet.entries));
        context.setPacketHandled(true);
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    public record Entry(String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
    }
}
