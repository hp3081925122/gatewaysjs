package com.hp.gatewaysjs.network;

import com.hp.gatewaysjs.Gatewaysjs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GatewaysJSNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Gatewaysjs.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private GatewaysJSNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncDynamicGatewayMetadataPacket.class, id++)
                .encoder(SyncDynamicGatewayMetadataPacket::encode)
                .decoder(SyncDynamicGatewayMetadataPacket::decode)
                .consumerMainThread(SyncDynamicGatewayMetadataPacket::handle)
                .add();
    }
}
