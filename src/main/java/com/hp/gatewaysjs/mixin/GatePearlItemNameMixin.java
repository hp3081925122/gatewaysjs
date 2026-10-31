package com.hp.gatewaysjs.mixin;

import com.hp.gatewaysjs.util.DynamicGatewayManager;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.item.GatePearlItem;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让动态网关的自定义名称同步应用到手持传送门珍珠的显示名。
 */
@Mixin(value = GatePearlItem.class)
public class GatePearlItemNameMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true, remap = false)
    private void gatewaysjs$useDynamicDisplayName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            return;
        }

        DynamicHolder<Gateway> holder = GatePearlItem.getGate(stack);
        Identifier id = holder.getId();
        String displayName = DynamicGatewayManager.getDisplayName(id);
        if (displayName == null || displayName.isEmpty()) {
            return;
        }

        cir.setReturnValue(Component.literal(displayName));
    }
}

