package com.hp.gatewaysjs.mixin;

import com.hp.gatewaysjs.util.DynamicGatewayManager;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
    private void gatewaysjs$filterHiddenPearls(CreativeModeTab tab, BuildCreativeModeTabContentsEvent event, CallbackInfo ci) {
        GatewayRegistry.INSTANCE.getValues().stream()
            .sorted(Comparator
                .comparing(Gateway::size)
                .thenComparing(gateway -> {
                    Identifier id = GatewayRegistry.INSTANCE.getKey(gateway);
                    return id == null ? "" : id.toString();
                }))
            .forEach(gateway -> {
                Identifier id = GatewayRegistry.INSTANCE.getKey(gateway);
                if (id == null || DynamicGatewayManager.isPearlHiddenInCreativeTab(id)) {
                    return;
                }

                ItemStack stack = new ItemStack((GatePearlItem) (Object) this);
                GatePearlItem.setGate(stack, gateway);
                event.accept(stack);
            });
        ci.cancel();
    }
}

