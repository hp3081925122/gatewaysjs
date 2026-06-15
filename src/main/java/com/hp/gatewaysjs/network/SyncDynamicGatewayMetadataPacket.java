package com.hp.gatewaysjs.network;

import com.hp.gatewaysjs.Gatewaysjs;
import com.hp.gatewaysjs.util.DynamicGatewayManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;

public record SyncDynamicGatewayMetadataPacket(Map<ResourceLocation, Entry> entries) implements CustomPacketPayload {

    public static final Type<SyncDynamicGatewayMetadataPacket> TYPE = CustomPacketPayload.createType(Gatewaysjs.MODID + ":sync_dynamic_gateway_metadata");
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDynamicGatewayMetadataPacket> STREAM_CODEC = CustomPacketPayload.codec(
            SyncDynamicGatewayMetadataPacket::encode,
            SyncDynamicGatewayMetadataPacket::decode
    );

    public static SyncDynamicGatewayMetadataPacket create() {
        return new SyncDynamicGatewayMetadataPacket(DynamicGatewayManager.copyMetadata());
    }

    public static void encode(SyncDynamicGatewayMetadataPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entries.size());
        packet.entries.forEach((id, entry) -> {
            buffer.writeResourceLocation(id);
            buffer.writeUtf(entry.displayName() == null ? "" : entry.displayName());
            buffer.writeUtf(entry.tooltipKey() == null ? "" : entry.tooltipKey());
            buffer.writeUtf(entry.tooltipText() == null ? "" : entry.tooltipText());
            buffer.writeBoolean(entry.hidePearlInCreativeTab());
        });
    }

    public static SyncDynamicGatewayMetadataPacket decode(RegistryFriendlyByteBuf buffer) {
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

    public static void handle(SyncDynamicGatewayMetadataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DynamicGatewayManager.applyClientMetadata(packet.entries));
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
    }
}
