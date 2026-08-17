package com.hp.gatewaysjs.util;

import com.google.common.collect.ImmutableMap;
import com.hp.gatewaysjs.Gatewaysjs;
import com.hp.gatewaysjs.network.GatewaysJSNetwork;
import com.hp.gatewaysjs.network.SyncDynamicGatewayMetadataPacket;
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
    private static final Map<ResourceLocation, String> TOOLTIP_KEYS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, String> TOOLTIP_TEXTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Boolean> HIDDEN_CREATIVE_PEARLS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Gateway> PRE_SERVER_GATEWAYS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, String> PRE_SERVER_DISPLAY_NAMES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, String> PRE_SERVER_TOOLTIP_KEYS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, String> PRE_SERVER_TOOLTIP_TEXTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Boolean> PRE_SERVER_HIDDEN_CREATIVE_PEARLS = new LinkedHashMap<>();
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
        return register(id, gateway, null, null, null, false);
    }

    public static boolean register(ResourceLocation id, Gateway gateway, String displayName) {
        return register(id, gateway, displayName, null, null, false);
    }

    public static boolean register(ResourceLocation id, Gateway gateway, String displayName, String tooltipKey, String tooltipText) {
        return register(id, gateway, displayName, tooltipKey, tooltipText, false);
    }

    public static boolean register(ResourceLocation id, Gateway gateway, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        updateMetadata(id, displayName, tooltipKey, tooltipText, hidePearlInCreativeTab);
        if (server == null) {
            cachePreServerRegistration(id, gateway, displayName, tooltipKey, tooltipText, hidePearlInCreativeTab);
            return registerInternal(id, gateway, null, false, false);
        }

        return registerInternal(id, gateway, server, true, true);
    }

    public static String getDisplayName(ResourceLocation id) {
        return DISPLAY_NAMES.get(id);
    }

    public static String getTooltipKey(ResourceLocation id) {
        return TOOLTIP_KEYS.get(id);
    }

    public static String getTooltipText(ResourceLocation id) {
        return TOOLTIP_TEXTS.get(id);
    }

    public static boolean isPearlHiddenInCreativeTab(ResourceLocation id) {
        return HIDDEN_CREATIVE_PEARLS.getOrDefault(id, false);
    }

    public static void setPearlHiddenInCreativeTab(ResourceLocation id, boolean hidden) {
        if (hidden) {
            HIDDEN_CREATIVE_PEARLS.put(id, true);
        }
        else {
            HIDDEN_CREATIVE_PEARLS.remove(id);
        }
    }

    public static Map<ResourceLocation, SyncDynamicGatewayMetadataPacket.Entry> copyMetadata() {
        Map<ResourceLocation, SyncDynamicGatewayMetadataPacket.Entry> entries = new LinkedHashMap<>();
        DYNAMIC_GATEWAYS.keySet().forEach(id -> addMetadataEntry(entries, id));
        DISPLAY_NAMES.keySet().forEach(id -> addMetadataEntry(entries, id));
        TOOLTIP_KEYS.keySet().forEach(id -> addMetadataEntry(entries, id));
        TOOLTIP_TEXTS.keySet().forEach(id -> addMetadataEntry(entries, id));
        HIDDEN_CREATIVE_PEARLS.keySet().forEach(id -> addMetadataEntry(entries, id));
        return entries;
    }

    private static void addMetadataEntry(Map<ResourceLocation, SyncDynamicGatewayMetadataPacket.Entry> entries, ResourceLocation id) {
        entries.put(id, new SyncDynamicGatewayMetadataPacket.Entry(
                DISPLAY_NAMES.get(id),
                TOOLTIP_KEYS.get(id),
                TOOLTIP_TEXTS.get(id),
                isPearlHiddenInCreativeTab(id)
        ));
    }

    public static void applyClientMetadata(Map<ResourceLocation, SyncDynamicGatewayMetadataPacket.Entry> entries) {
        entries.forEach((id, entry) -> updateMetadata(
                id,
                entry.displayName(),
                entry.tooltipKey(),
                entry.tooltipText(),
                entry.hidePearlInCreativeTab()
        ));
    }

    // Restores persisted dynamic gateways as early as the server lifecycle allows.
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        restoreAll(event.getServer());
        reapplyPreServerRegistrations(event.getServer());
    }

    // Retries restoration once the server is fully started if early restoration had no world access yet.
    public static void onServerStarted(ServerStartedEvent event) {
        if (!restoredForCurrentServer) {
            restoreAll(event.getServer());
            reapplyPreServerRegistrations(event.getServer());
        }

        if (!DYNAMIC_GATEWAYS.isEmpty()) {
            syncToAll();
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
        TOOLTIP_KEYS.clear();
        TOOLTIP_TEXTS.clear();
        HIDDEN_CREATIVE_PEARLS.clear();
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

        try {
            Gateway gateway = RegistryHelper.decodeFromJson(GatewayRegistry.INSTANCE, json);
            registerInternal(id, gateway, server, false, false);
        } catch (RuntimeException exception) {
            // 无效的动态传送门只记录日志并跳过，保留存档数据以便缺失模组恢复后自动加载。
            Gatewaysjs.getLogger().error("Skipping dynamic gateway {} because its saved definition could not be restored.", id, exception);
        }
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
        DISPLAY_NAMES.clear();
        TOOLTIP_KEYS.clear();
        TOOLTIP_TEXTS.clear();
        HIDDEN_CREATIVE_PEARLS.clear();

        for (Map.Entry<ResourceLocation, DynamicGatewaySavedData.GatewayData> entry : DynamicGatewaySavedData.get(server).entries().entrySet()) {
            DynamicGatewaySavedData.GatewayData gatewayData = entry.getValue();
            try {
                Gateway gateway = RegistryHelper.decodeFromJson(GatewayRegistry.INSTANCE, gatewayData.json());
                updateMetadata(entry.getKey(), gatewayData.displayName(), gatewayData.tooltipKey(), gatewayData.tooltipText(), gatewayData.hidePearlInCreativeTab());
                registerInternal(entry.getKey(), gateway, server, false, false);
            } catch (RuntimeException exception) {
                // 无效的动态传送门只记录日志并跳过，避免一个缺失实体阻塞整个服务器启动。
                Gatewaysjs.getLogger().error("Skipping dynamic gateway {} because its saved definition could not be restored.", entry.getKey(), exception);
            }
        }

        restoredForCurrentServer = true;
    }

    // Performs the shared registry/cache/save/sync bookkeeping for runtime gateways.
    private static boolean registerInternal(ResourceLocation id, Gateway gateway, MinecraftServer server, boolean persistToSave, boolean syncClients) {
        boolean registered = RegistryHelper.register(GatewayRegistry.INSTANCE, id, gateway);
        DYNAMIC_GATEWAYS.put(id, gateway);

        if (persistToSave && server != null && server.overworld() != null) {
            DynamicGatewaySavedData.get(server).put(id, new DynamicGatewaySavedData.GatewayData(
                    RegistryHelper.encodeToJson(GatewayRegistry.INSTANCE, gateway),
                    DISPLAY_NAMES.get(id),
                    TOOLTIP_KEYS.get(id),
                    TOOLTIP_TEXTS.get(id),
                    isPearlHiddenInCreativeTab(id)
            ));
        }

        if (syncClients && server != null) {
            syncToAll();
        }

        return registered;
    }

    private static void updateMetadata(ResourceLocation id, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
        putOrRemove(DISPLAY_NAMES, id, displayName);
        putOrRemove(TOOLTIP_KEYS, id, tooltipKey);
        putOrRemove(TOOLTIP_TEXTS, id, tooltipText);
        if (hidePearlInCreativeTab) {
            HIDDEN_CREATIVE_PEARLS.put(id, true);
        }
        else {
            HIDDEN_CREATIVE_PEARLS.remove(id);
        }
    }

    private static void cachePreServerRegistration(ResourceLocation id, Gateway gateway, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
        PRE_SERVER_GATEWAYS.put(id, gateway);
        putOrRemove(PRE_SERVER_DISPLAY_NAMES, id, displayName);
        putOrRemove(PRE_SERVER_TOOLTIP_KEYS, id, tooltipKey);
        putOrRemove(PRE_SERVER_TOOLTIP_TEXTS, id, tooltipText);
        if (hidePearlInCreativeTab) {
            PRE_SERVER_HIDDEN_CREATIVE_PEARLS.put(id, true);
        }
        else {
            PRE_SERVER_HIDDEN_CREATIVE_PEARLS.remove(id);
        }
    }

    private static void reapplyPreServerRegistrations(MinecraftServer server) {
        if (server == null || server.overworld() == null || PRE_SERVER_GATEWAYS.isEmpty()) {
            return;
        }

        PRE_SERVER_GATEWAYS.forEach((id, gateway) -> {
            updateMetadata(
                id,
                PRE_SERVER_DISPLAY_NAMES.get(id),
                PRE_SERVER_TOOLTIP_KEYS.get(id),
                PRE_SERVER_TOOLTIP_TEXTS.get(id),
                PRE_SERVER_HIDDEN_CREATIVE_PEARLS.getOrDefault(id, false)
            );
            registerInternal(id, gateway, server, true, false);
        });
    }

    private static void putOrRemove(Map<ResourceLocation, String> map, ResourceLocation id, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(id, value);
        }
        else {
            map.remove(id);
        }
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
        GatewaysJSNetwork.CHANNEL.send(target, SyncDynamicGatewayMetadataPacket.create());
    }
}
