package com.hp.gatewaysjs.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.placebo.codec.CodecMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.reload.DynRegBiMap;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射工具类，用于在服务器运行时动态注册到 DynamicRegistry
 *
 * DynamicRegistry 在 onReload() 后会将 registry 变为 ImmutableBiMap，
 * 本工具类处理了这个问题，允许在运行时动态添加条目。
 */
public class RegistryHelper {

    // 反射缓存
    private static Field registryField;
    private static Field codecsField;
    private static Method bindMethod;
    private static Method unbindMethod;

    /**
     * 在服务器运行时动态注册到 DynamicRegistry
     *
     * 此方法会：
     * 1. 将 ImmutableBiMap 转换为可变的 HashBiMap
     * 2. 如已存在同 ID 条目，先移除旧条目
     * 3. 添加新条目
     * 4. 获取或创建 DynamicHolder 并重新绑定
     * 5. 将 registry 转换回 ImmutableBiMap
     *
     * @param registry 目标注册表
     * @param key 注册ID
     * @param value 要注册的值
     * @return true 如果注册或替换成功
     */
    @SuppressWarnings("unchecked")
    public static <T extends CodecProvider<? super T>> boolean register(
            DynamicRegistry<T> registry,
            ResourceLocation key,
            T value) {
        try {
            // 初始化反射字段
            initReflection();

            // 获取当前 registry map
            BiMap<ResourceLocation, T> currentRegistry = (BiMap<ResourceLocation, T>) registryField.get(registry);

            // 转换为 Placebo 动态注册表使用的可变 Map。
            BiMap<ResourceLocation, T> mutableRegistry = new DynRegBiMap<>();
            mutableRegistry.putAll(currentRegistry);

            // 先移除旧条目，允许同一个网关 ID 刷新。
            mutableRegistry.remove(key);

            // 添加新条目
            mutableRegistry.put(key, value);

            // 转换回不可变视图并设置
            registryField.set(registry, Maps.unmodifiableBiMap(mutableRegistry));

            // 使用 holder() 方法获取或创建 holder
            DynamicHolder<T> holder = registry.holder(key);

            // 刷新可能还指向旧网关的 holder。
            unbindMethod.invoke(holder);
            bindMethod.invoke(holder);

            return true; // 注册成功

        } catch (Exception e) {
            throw new RuntimeException("Failed to register " + key + " to registry", e);
        }
    }

    /**
     * 检查指定ID是否已在注册表中
     */
    public static boolean isRegistered(DynamicRegistry<?> registry, ResourceLocation key) {
        try {
            initReflection();
            BiMap<ResourceLocation, ?> currentRegistry = (BiMap<ResourceLocation, ?>) registryField.get(registry);
            return currentRegistry.containsKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 使用注册表 codec map 将动态注册表条目序列化为 JSON。
     */
    @SuppressWarnings("unchecked")
    public static <T extends CodecProvider<? super T>> String encodeToJson(DynamicRegistry<T> registry, T value) {
        try {
            initReflection();
            CodecMap<T> codecMap = (CodecMap<T>) codecsField.get(registry);
            JsonElement element = codecMap.encodeStart(JsonOps.INSTANCE, value).getOrThrow(IllegalStateException::new);
            return element.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode registry value " + value, e);
        }
    }

    /**
     * 使用注册表 codec map 从 JSON 恢复动态注册表条目。
     */
    @SuppressWarnings("unchecked")
    public static <T extends CodecProvider<? super T>> T decodeFromJson(DynamicRegistry<T> registry, String json) {
        try {
            initReflection();
            CodecMap<T> codecMap = (CodecMap<T>) codecsField.get(registry);
            Pair<T, JsonElement> decoded = codecMap.decode(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(IllegalStateException::new);
            return decoded.getFirst();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode registry value from json", e);
        }
    }

    private static void initReflection() throws NoSuchFieldException, NoSuchMethodException {
        if (registryField == null) {
            registryField = DynamicRegistry.class.getDeclaredField("registry");
            registryField.setAccessible(true);
        }
        if (codecsField == null) {
            codecsField = DynamicRegistry.class.getDeclaredField("codecs");
            codecsField.setAccessible(true);
        }
        if (bindMethod == null) {
            bindMethod = DynamicHolder.class.getDeclaredMethod("bind");
            bindMethod.setAccessible(true);
        }
        if (unbindMethod == null) {
            unbindMethod = DynamicHolder.class.getDeclaredMethod("unbind");
            unbindMethod.setAccessible(true);
        }
    }
}
