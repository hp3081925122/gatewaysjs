package com.hp.gatewaysjs.util;

import com.google.common.collect.ImmutableMap;
import com.hp.gatewaysjs.Gatewaysjs;
import com.hp.gatewaysjs.network.SyncDynamicGatewayMetadataPacket;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.dynreg.RegistryCallback;
import dev.shadowsoffire.placebo.dynreg.DynRegPayloads;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跟踪运行时创建的网关，并负责保存和同步。
 */
public final class DynamicGatewayManager {

    private static final Identifier GATEWAY_REGISTRY_PATH = Identifier.fromNamespaceAndPath("gatewaysjs", "gateways");
    private static final Map<Identifier, Gateway> DYNAMIC_GATEWAYS = new LinkedHashMap<>();
    private static final Map<Identifier, String> DISPLAY_NAMES = new LinkedHashMap<>();
    private static final Map<Identifier, String> TOOLTIP_KEYS = new LinkedHashMap<>();
    private static final Map<Identifier, String> TOOLTIP_TEXTS = new LinkedHashMap<>();
    private static final Map<Identifier, Boolean> HIDDEN_CREATIVE_PEARLS = new LinkedHashMap<>();
    private static final Map<Identifier, Gateway> PRE_SERVER_GATEWAYS = new LinkedHashMap<>();
    private static final Map<Identifier, String> PRE_SERVER_DISPLAY_NAMES = new LinkedHashMap<>();
    private static final Map<Identifier, String> PRE_SERVER_TOOLTIP_KEYS = new LinkedHashMap<>();
    private static final Map<Identifier, String> PRE_SERVER_TOOLTIP_TEXTS = new LinkedHashMap<>();
    private static final Map<Identifier, Boolean> PRE_SERVER_HIDDEN_CREATIVE_PEARLS = new LinkedHashMap<>();
    private static boolean bootstrapped = false;
    private static boolean restoredForCurrentServer = false;

    private DynamicGatewayManager() {
    }

    // 将运行时缓存重新接回 Placebo 重载流程。
    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        GatewayRegistry.INSTANCE.addCallback(RegistryCallback.reloadOnly(registry -> {
            if (DYNAMIC_GATEWAYS.isEmpty()) {
                return;
            }

            ImmutableMap.copyOf(DYNAMIC_GATEWAYS).forEach((id, gateway) -> RegistryHelper.register(registry, id, gateway));
        }));
        bootstrapped = true;
    }

    // 注册运行时网关，写入世界存档，并同步到客户端。
    public static boolean register(Identifier id, Gateway gateway) {
        return register(id, gateway, null, null, null, false);
    }

    public static boolean register(Identifier id, Gateway gateway, String displayName) {
        return register(id, gateway, displayName, null, null, false);
    }

    public static boolean register(Identifier id, Gateway gateway, String displayName, String tooltipKey, String tooltipText) {
        return register(id, gateway, displayName, tooltipKey, tooltipText, false);
    }

    public static boolean register(Identifier id, Gateway gateway, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        updateMetadata(id, displayName, tooltipKey, tooltipText, hidePearlInCreativeTab);
        if (server == null) {
            cachePreServerRegistration(id, gateway, displayName, tooltipKey, tooltipText, hidePearlInCreativeTab);
            return registerInternal(id, gateway, null, false, false);
        }

        return registerInternal(id, gateway, server, true, true);
    }

    public static String getDisplayName(Identifier id) {
        return DISPLAY_NAMES.get(id);
    }

    public static String getTooltipKey(Identifier id) {
        return TOOLTIP_KEYS.get(id);
    }

    public static String getTooltipText(Identifier id) {
        return TOOLTIP_TEXTS.get(id);
    }

    public static boolean isPearlHiddenInCreativeTab(Identifier id) {
        return HIDDEN_CREATIVE_PEARLS.getOrDefault(id, false);
    }

    public static void setPearlHiddenInCreativeTab(Identifier id, boolean hidden) {
        if (hidden) {
            HIDDEN_CREATIVE_PEARLS.put(id, true);
        }
        else {
            HIDDEN_CREATIVE_PEARLS.remove(id);
        }
    }

    public static Map<Identifier, SyncDynamicGatewayMetadataPacket.Entry> copyMetadata() {
        Map<Identifier, SyncDynamicGatewayMetadataPacket.Entry> entries = new LinkedHashMap<>();
        DYNAMIC_GATEWAYS.keySet().forEach(id -> addMetadataEntry(entries, id));
        DISPLAY_NAMES.keySet().forEach(id -> addMetadataEntry(entries, id));
        TOOLTIP_KEYS.keySet().forEach(id -> addMetadataEntry(entries, id));
        TOOLTIP_TEXTS.keySet().forEach(id -> addMetadataEntry(entries, id));
        HIDDEN_CREATIVE_PEARLS.keySet().forEach(id -> addMetadataEntry(entries, id));
        return entries;
    }

    private static void addMetadataEntry(Map<Identifier, SyncDynamicGatewayMetadataPacket.Entry> entries, Identifier id) {
        entries.put(id, new SyncDynamicGatewayMetadataPacket.Entry(
                DISPLAY_NAMES.get(id),
                TOOLTIP_KEYS.get(id),
                TOOLTIP_TEXTS.get(id),
                isPearlHiddenInCreativeTab(id)
        ));
    }

    public static void applyClientMetadata(Map<Identifier, SyncDynamicGatewayMetadataPacket.Entry> entries) {
        entries.forEach((id, entry) -> updateMetadata(
                id,
                entry.displayName(),
                entry.tooltipKey(),
                entry.tooltipText(),
                entry.hidePearlInCreativeTab()
        ));
    }

    // 在服务端生命周期允许时尽早恢复已保存的动态网关。
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        bootstrap();
        restoreAll(event.getServer());
        reapplyPreServerRegistrations(event.getServer());
    }

    // 如果早期恢复时还没有世界访问权限，则在服务端启动完成后重试。
    public static void onServerStarted(ServerStartedEvent event) {
        if (!restoredForCurrentServer) {
            restoreAll(event.getServer());
            reapplyPreServerRegistrations(event.getServer());
        }

        if (!DYNAMIC_GATEWAYS.isEmpty()) {
            syncToAll();
        }
    }

    // 向注册表重建后加入的玩家同步动态网关。
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !DYNAMIC_GATEWAYS.isEmpty()) {
            syncToPlayer(player);
        }
    }

    // 清理服务端作用域缓存，避免不同存档之间串数据。
    public static void onServerStopped(ServerStoppedEvent event) {
        DYNAMIC_GATEWAYS.clear();
        DISPLAY_NAMES.clear();
        TOOLTIP_KEYS.clear();
        TOOLTIP_TEXTS.clear();
        HIDDEN_CREATIVE_PEARLS.clear();
        restoredForCurrentServer = false;
    }

    // 在网关实体加载时校验 holder 前，确保已保存的动态网关可用。
    public static void ensureGatewayAvailable(Identifier id) {
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

    // 向所有客户端同步注册表内容。
    public static void syncToAll() {
        sendRegistryToAll();
        PacketDistributor.sendToAllPlayers(SyncDynamicGatewayMetadataPacket.create());
    }

    // 向单个客户端同步注册表内容。
    public static void syncToPlayer(ServerPlayer player) {
        sendRegistryToPlayer(player);
        PacketDistributor.sendToPlayer(player, SyncDynamicGatewayMetadataPacket.create());
    }

    // 从世界存档恢复所有动态网关。
    private static void restoreAll(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return;
        }

        DYNAMIC_GATEWAYS.clear();
        DISPLAY_NAMES.clear();
        TOOLTIP_KEYS.clear();
        TOOLTIP_TEXTS.clear();
        HIDDEN_CREATIVE_PEARLS.clear();

        for (Map.Entry<Identifier, DynamicGatewaySavedData.GatewayData> entry : DynamicGatewaySavedData.get(server).entries().entrySet()) {
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

    // 处理运行时网关共用的注册、缓存、保存和同步逻辑。
    private static boolean registerInternal(Identifier id, Gateway gateway, MinecraftServer server, boolean persistToSave, boolean syncClients) {
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

    private static void updateMetadata(Identifier id, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
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

    private static void cachePreServerRegistration(Identifier id, Gateway gateway, String displayName, String tooltipKey, String tooltipText, boolean hidePearlInCreativeTab) {
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

    private static void putOrRemove(Map<Identifier, String> map, Identifier id, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(id, value);
        }
        else {
            map.remove(id);
        }
    }

    private static void sendRegistryToAll() {
        PacketDistributor.sendToAllPlayers(new DynRegPayloads.Start(GATEWAY_REGISTRY_PATH));
        GatewayRegistry.INSTANCE.getKeys().forEach(id -> {
            Gateway gateway = GatewayRegistry.INSTANCE.getValue(id);
            if (gateway != null) {
                PacketDistributor.sendToAllPlayers(new DynRegPayloads.Content<>(GATEWAY_REGISTRY_PATH, id, gateway));
            }
        });
        PacketDistributor.sendToAllPlayers(new DynRegPayloads.End(GATEWAY_REGISTRY_PATH));
    }

    private static void sendRegistryToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new DynRegPayloads.Start(GATEWAY_REGISTRY_PATH));
        GatewayRegistry.INSTANCE.getKeys().forEach(id -> {
            Gateway gateway = GatewayRegistry.INSTANCE.getValue(id);
            if (gateway != null) {
                PacketDistributor.sendToPlayer(player, new DynRegPayloads.Content<>(GATEWAY_REGISTRY_PATH, id, gateway));
            }
        });
        PacketDistributor.sendToPlayer(player, new DynRegPayloads.End(GATEWAY_REGISTRY_PATH));
    }
}


