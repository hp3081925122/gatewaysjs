package com.hp.gatewaysjs.mixin;

import com.hp.gatewaysjs.Gatewaysjs;
import dev.shadowsoffire.apotheosis.adventure.boss.ApothBoss;
import dev.shadowsoffire.apotheosis.adventure.boss.BossRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.adventure.compat.GatewaysCompat$BossWaveEntity", remap = false)
public abstract class ApotheosisBossWaveEntityMixin {

    @Shadow
    @Final
    private Optional<ResourceLocation> bossId;

    @Unique
    private boolean gatewaysjs$missingBossLogged;

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void gatewaysjs$safeBossDescription(CallbackInfoReturnable<MutableComponent> cir) {
        if (this.bossId.isEmpty()) {
            return;
        }

        ResourceLocation id = this.bossId.get();
        ApothBoss boss = BossRegistry.INSTANCE.getValue(id);
        if (boss == null) {
            if (!this.gatewaysjs$missingBossLogged) {
                Gatewaysjs.getLogger().warn(
                        "Apotheosis boss {} is unavailable in the client registry; using fallback tooltip.", id);
                this.gatewaysjs$missingBossLogged = true;
            }
            cir.setReturnValue(Component.translatable("gatewaysjs.tooltip.apotheosis.boss_unavailable"));
            return;
        }

        cir.setReturnValue(Component.translatable("misc.apotheosis.boss",
                Component.translatable(boss.getEntity().getDescriptionId())));
    }
}
