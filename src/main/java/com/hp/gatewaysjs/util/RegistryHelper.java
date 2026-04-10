package com.hp.gatewaysjs.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import dev.shadowsoffire.placebo.codec.CodecProvider;
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
    private static Method bindMethod;

    /**
     * 在服务器运行时动态注册到 DynamicRegistry
     *
     * 此方法会：
     * 1. 检查是否已存在，如已存在则跳过注册
     * 2. 将 ImmutableBiMap 转换为可变的 HashBiMap
     * 3. 添加新条目
     * 4. 获取或创建 DynamicHolder 并绑定
     * 5. 将 registry 转换回 ImmutableBiMap
     *
     * @param registry 目标注册表
     * @param key 注册ID
     * @param value 要注册的值
     * @return true 如果新注册成功，false 如果已存在（跳过注册）
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

            // 检查是否已存在 - 如果存在则跳过注册
            if (currentRegistry.containsKey(key)) {
                return false; // 已存在，跳过注册
            }

            // 转换为可变 Map
            BiMap<ResourceLocation, T> mutableRegistry = HashBiMap.create(currentRegistry);

            // 添加新条目
            mutableRegistry.put(key, value);

            // 转换回 ImmutableBiMap 并设置
            registryField.set(registry, ImmutableBiMap.copyOf(mutableRegistry));

            // 使用 holder() 方法获取或创建 holder
            DynamicHolder<T> holder = registry.holder(key);

            // 调用 bind() 方法 - 这会从 registry.getValue(id) 获取值
            // 由于我们已经把值放入 registry，bind 会正确设置 value
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

    private static void initReflection() throws NoSuchFieldException, NoSuchMethodException {
        if (registryField == null) {
            registryField = DynamicRegistry.class.getDeclaredField("registry");
            registryField.setAccessible(true);
        }
        if (bindMethod == null) {
            bindMethod = DynamicHolder.class.getDeclaredMethod("bind");
            bindMethod.setAccessible(true);
        }
    }
}
