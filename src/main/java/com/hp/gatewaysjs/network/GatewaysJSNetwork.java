package com.hp.gatewaysjs.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GatewaysJSNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private GatewaysJSNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GatewaysJSNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                SyncDynamicGatewayMetadataPacket.TYPE,
                SyncDynamicGatewayMetadataPacket.STREAM_CODEC,
                SyncDynamicGatewayMetadataPacket::handle
        );
    }
}
