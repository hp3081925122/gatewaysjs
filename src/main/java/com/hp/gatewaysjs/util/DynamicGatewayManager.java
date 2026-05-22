package com.hp.gatewaysjs.util;

import com.google.common.collect.ImmutableMap;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.Placebo;
import dev.shadowsoffire.placebo.reload.RegistryCallback;
import dev.shadowsoffire.placebo.reload.ReloadListenerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks runtime-created gateways, syncs them to clients, and persists them in the world save.
 */
public final class DynamicGatewayManager {

    private static final String GATEWAY_REGISTRY_PATH = "gateways";
    private static final Map<ResourceLocation, Gateway> DYNAMIC_GATEWAYS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, String> DISPLAY_NAMES = new LinkedHashMap<>();
    private static boolean restoredForCurrentServer = false;

    private DynamicGatewayManager() {
    }

    // Hooks the runtime cache back into Placebo reloads.
    public static void bootstrap() {
        GatewayRegistry.INSTANCE.addCallback(RegistryCallback.reloadOnly(registry -> {
            if (DYNAMIC_GATEWAYS.isEmpty()) {
                return;
            }

            ImmutableMap.copyOf(DYNAMIC_GATEWAYS).forEach((id, gateway) -> RegistryHelper.register(registry, id, gateway));
        }));
    }

    // Registers a runtime gateway, writes it into world save data, and syncs it to clients.
    public static boolean register(ResourceLocation id, Gateway gateway) {
        return register(id, gateway, null);
    }

    public static boolean register(ResourceLocation id, Gateway gateway, String displayName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (displayName != null && !displayName.isEmpty()) {
            DISPLAY_NAMES.put(id, displayName);
        } else {
            DISPLAY_NAMES.remove(id);
        }
        return registerInternal(id, gateway, server, true, server != null);
    }

    public static String getDisplayName(ResourceLocation id) {
        return DISPLAY_NAMES.get(id);
    }

    // Restores persisted dynamic gateways as early as the server lifecycle allows.
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        restoreAll(event.getServer());
    }

    // Retries restoration once the server is fully started if early restoration had no world access yet.
    public static void onServerStarted(ServerStartedEvent event) {
        if (!restoredForCurrentServer) {
            restoreAll(event.getServer());
        }
    }

    // Syncs dynamic gateways to players that join after the registry was rebuilt.
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !DYNAMIC_GATEWAYS.isEmpty()) {
            syncToPlayer(player);
        }
    }

    // Clears server-scoped runtime cache so separate saves do not leak gateways into each other.
    public static void onServerStopped(ServerStoppedEvent event) {
        DYNAMIC_GATEWAYS.clear();
        DISPLAY_NAMES.clear();
        restoredForCurrentServer = false;
    }

    // Makes sure a saved dynamic gateway exists before a gateway entity validates its holder during load.
    public static void ensureGatewayAvailable(ResourceLocation id) {
        if (RegistryHelper.isRegistered(GatewayRegistry.INSTANCE, id)) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.overworld() == null) {
            return;
        }

        String json = DynamicGatewaySavedData.get(server).get(id);
        if (json == null || json.isEmpty()) {
            return;
        }

        Gateway gateway = RegistryHelper.decodeFromJson(GatewayRegistry.INSTANCE, json);
        registerInternal(id, gateway, server, false, false);
    }

    // Syncs the registry contents to all connected clients.
    public static void syncToAll() {
        sync(PacketDistributor.ALL.noArg());
    }

    // Syncs the registry contents to a single client.
    public static void syncToPlayer(ServerPlayer player) {
        sync(PacketDistributor.PLAYER.with(() -> player));
    }

    // Restores all persisted dynamic gateways from the world save.
    private static void restoreAll(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return;
        }

        DYNAMIC_GATEWAYS.clear();

        for (Map.Entry<ResourceLocation, String> entry : DynamicGatewaySavedData.get(server).entries().entrySet()) {
            Gateway gateway = RegistryHelper.decodeFromJson(GatewayRegistry.INSTANCE, entry.getValue());
            registerInternal(entry.getKey(), gateway, server, false, false);
        }

        restoredForCurrentServer = true;
    }

    // Performs the shared registry/cache/save/sync bookkeeping for runtime gateways.
    private static boolean registerInternal(ResourceLocation id, Gateway gateway, MinecraftServer server, boolean persistToSave, boolean syncClients) {
        boolean registered = RegistryHelper.register(GatewayRegistry.INSTANCE, id, gateway);
        DYNAMIC_GATEWAYS.put(id, gateway);

        if (persistToSave && server != null && server.overworld() != null) {
            DynamicGatewaySavedData.get(server).put(id, RegistryHelper.encodeToJson(GatewayRegistry.INSTANCE, gateway));
        }

        if (syncClients && server != null) {
            syncToAll();
        }

        return registered;
    }

    // Uses Placebo's native reload packets so clients rebuild the same dynamic registry entries.
    private static void sync(PacketDistributor.PacketTarget target) {
        Placebo.CHANNEL.send(target, new ReloadListenerPacket.Start(GATEWAY_REGISTRY_PATH));
        GatewayRegistry.INSTANCE.getKeys().forEach(id -> {
            Gateway gateway = GatewayRegistry.INSTANCE.getValue(id);
            if (gateway != null) {
                Placebo.CHANNEL.send(target, new ReloadListenerPacket.Content<>(GATEWAY_REGISTRY_PATH, id, gateway));
            }
        });
        Placebo.CHANNEL.send(target, new ReloadListenerPacket.End(GATEWAY_REGISTRY_PATH));
    }
}
