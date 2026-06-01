package com.hp.gatewaysjs.mixin;

import com.hp.gatewaysjs.util.DynamicGatewayManager;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

/**
 * 过滤创造物品栏中自动生成的传送门珍珠。
 */
@Mixin(value = GatePearlItem.class)
public class GatePearlItemCreativeTabMixin {

    @Inject(method = "fillItemCategory", at = @At("HEAD"), cancellable = true, remap = false)
    private void gatewaysjs$filterHiddenPearls(CreativeModeTab tab, CreativeModeTab.Output output, CallbackInfo ci) {
        GatewayRegistry.INSTANCE.getValues().stream()
            .sorted(Comparator
                .comparing(Gateway::size)
                .thenComparing(gateway -> GatewayRegistry.INSTANCE.getKey(gateway).toString()))
            .forEach(gateway -> {
                ResourceLocation id = GatewayRegistry.INSTANCE.getKey(gateway);
                if (id == null || DynamicGatewayManager.isPearlHiddenInCreativeTab(id)) {
                    return;
                }

                ItemStack stack = new ItemStack((GatePearlItem) (Object) this);
                GatePearlItem.setGate(stack, gateway);
                output.accept(stack);
            });
        ci.cancel();
    }
}
