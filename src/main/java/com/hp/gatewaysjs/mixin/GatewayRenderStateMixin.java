package com.hp.gatewaysjs.mixin;

import dev.shadowsoffire.gateways.client.GatewayRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GatewayRenderState.class)
public class GatewayRenderStateMixin {

    @Unique
    public Identifier gatewaysjs$customTexture;
}
