declare namespace com.hp.gatewaysjs.kubejs.builder {
    type JavaList<T> = T[] | java.util.List<T>
    type ItemStack = net.minecraft.world.item.ItemStack
    type MobEffect = net.minecraft.world.effect.MobEffect
    type Attribute = net.minecraft.world.entity.ai.attributes.Attribute
    type Level = net.minecraft.world.level.Level
    type Player = net.minecraft.world.entity.player.Player
    type BlockPos = net.minecraft.core.BlockPos

    interface GatewayBuilder {
        pos(x: number, y: number, z: number): this
        pos(blockPos: BlockPos): this
        summoner(player: Player): this
        name(name: string): this
        tooltipKey(key: string): this
        tooltipText(text: string): this
        hidePearlInCreativeTab(): this
        showPearlInCreativeTab(show: boolean): this
        asNormalGateway(): this
        asEndlessGateway(): this
        asApotheosisTieredGateway(tierId: "haven" | "frontier" | "ascent" | "summit" | "pinnacle" | string): this
        size(sizeName: "small" | "medium" | "large" | string): this
        color(hexColor: number): this
        setColor(hexColor: number): this
        addAttribute(attribute: Attribute, value: number): this
        addAttribute(attribute: Attribute, value: number, operation: GatewayBuilder.AttributeOperation): this
        addEffect(effect: MobEffect, amplifier: number): this
        addEffect(effect: MobEffect, amplifier: number, chance: number): this
        addEffect(effect: MobEffect, amplifier: number, chance: number, ambient: boolean, visible: boolean): this
        addGearSet(gearSetId: string): this
        addModifierJson(modifierJson: string): this
        setDescription(description: string): this
        setNbt(nbtString: string): this
        addNbt(nbtString: string): this
        finalizeSpawn(finalizeSpawn: boolean): this
        entityTexture(texturePath: string): this
        bossBarTexture(texturePath: string): this
        bossBar(texturePath: string, bgU: number, bgV: number, fillU: number, fillV: number, barWidth: number, barHeight: number, texWidth: number, texHeight: number): this
        bossBar(texturePath: string, progressTexturePath: string): this
        bossBar(texturePath: string, progressTexturePath: string, bgU: number, bgV: number, fillU: number, fillV: number, barWidth: number, barHeight: number, texWidth: number, texHeight: number): this
        addWave(): GatewayBuilder.WaveBuilder
        addWave(consumer: (wave: GatewayBuilder.WaveBuilder) => void): this
        modifyAllWaves(consumer: (wave: GatewayBuilder.WaveBuilder) => void): this
        addReward(itemId: string, count: number): this
        addReward(itemId: string, count: number, nbtString: string): this
        addStackReward(itemId: string, count: number): this
        addStackReward(itemId: string, count: number, nbtString: string): this
        addStackListReward(stacks: JavaList<ItemStack>): this
        addStackListReward(stacksJson: string): this
        addRewardJson(rewardJson: string): this
        addLootTableReward(lootTableId: string, rolls: number): this
        addLootTableReward(lootTableId: string, rolls: number, description: string): this
        addEntityLootReward(entityId: string, rolls: number): this
        addEntityLootReward(entityId: string, nbtString: string, rolls: number): this
        addExperienceReward(experience: number): this
        addExperienceReward(experience: number, orbSize: number): this
        addXpReward(experience: number): this
        addXpReward(experience: number, orbSize: number): this
        addCommandReward(command: string, description: string): this
        addSummonReward(entityId: string, count: number): this
        addSummonReward(entityId: string, nbtString: string, count: number): this
        addChancedReward(rewardJson: string, chance: number): this
        addApotheosisAffixReward(rarityId: string): this
        addApotheosisAffixItemReward(...rarityIds: string[]): this
        addApotheosisGemReward(...purityIds: string[]): this
        addApotheosisTrueRandomGemReward(...purityIds: string[]): this
        addFailureJson(failureJson: string): this
        addCommandFailure(command: string, description: string): this
        addSummonFailure(entityId: string, count: number): this
        addSummonFailure(entityId: string, nbtString: string, count: number): this
        addExplosionFailure(strength: number, fire: boolean, blockDamage: boolean): this
        addMobEffectFailure(effect: MobEffect, duration: number): this
        addMobEffectFailure(effect: MobEffect, duration: number, amplifier: number): this
        addChancedFailure(failureJson: string, chance: number): this
        addEndlessModifier(): this
        addEndlessModifier(consumer: (modifier: GatewayBuilder.EndlessModifierBuilder) => void): this
        spawnAlgorithm(algorithm: "open_field" | "inward_spiral" | string): this
        soundtrack(soundId: string): this
        rules(rules: dev.shadowsoffire.gateways.gate.GateRules): this
        rules(spawnRange: number, leashRange: number, allowDiscarding: boolean, allowDimChange: boolean, playerDamageOnly: boolean, removeMobsOnFailure: boolean, failOnOutOfBounds: boolean, spacing: number, followRangeBoost: number, defaultDropChance: number): this
        rules(spawnRange: number, leashRange: number, allowDiscarding: boolean, allowDimChange: boolean, playerDamageOnly: boolean, removeMobsOnFailure: boolean, failOnOutOfBounds: boolean, spacing: number, followRangeBoost: number, defaultDropChance: number, requiresNearbyPlayer: boolean, lives: number): this
        spawnRange(spawnRange: number): this
        leashRange(leashRange: number): this
        allowDiscarding(allowDiscarding: boolean): this
        allowDimChange(allowDimChange: boolean): this
        playerDamageOnly(playerDamageOnly: boolean): this
        removeMobsOnFailure(removeMobsOnFailure: boolean): this
        removeOnFailure(removeOnFailure: boolean): this
        failOnOutOfBounds(failOnOutOfBounds: boolean): this
        spacing(spacing: number): this
        followRangeBoost(followRangeBoost: number): this
        defaultDropChance(defaultDropChance: number): this
        requiresNearbyPlayer(requiresNearbyPlayer: boolean): this
        lives(lives: number): this
        bossEvent(mode: "boss_bar" | "name_plate" | string, fog: boolean): this
        bossEventMode(mode: "boss_bar" | "name_plate" | string): this
        bossEventFog(fog: boolean): this
        bossEventAsBar(): this
        bossEventAsNamePlate(): this
        exists(): boolean
        register(): this
        spawn(level: Level): dev.shadowsoffire.gateways.entity.GatewayEntity | null
        getGatewayId(): net.minecraft.resources.ResourceLocation
    }

    namespace GatewayBuilder {
        type AttributeOperation = "addition" | "multiply_base" | "multiply_total"

        interface WaveBuilder {
            addEntity(entityId: string, count: number): WaveEntityBuilder
            addEntityJson(entityJson: string): this
            addApotheosisBoss(): this
            addApotheosisBoss(bossId: string): this
            addApotheosisInvader(invaderId: string, count: number): this
            addApotheosisInvader(invaderId: string, count: number, description: string): this
            addApotheosisTrueRandomInvader(count: number): this
            addApotheosisTrueRandomInvader(count: number, description: string): this
            addApotheosisElite(eliteId: string, entityId: string): this
            addApotheosisElite(eliteId: string, entityId: string, description: string, count: number): this
            addAttribute(attribute: Attribute, value: number): this
            addAttribute(attribute: Attribute, value: number, operation: AttributeOperation): this
            addEffect(effect: MobEffect, amplifier: number): this
            addEffect(effect: MobEffect, amplifier: number, chance: number): this
            addEffect(effect: MobEffect, amplifier: number, chance: number, ambient: boolean, visible: boolean): this
            addGearSet(gearSetId: string): this
            addModifierJson(modifierJson: string): this
            addApotheosisAffixModifier(affixChance: number, ...rarityIds: string[]): this
            addApotheosisPassenger(passengerEntityJson: string): this
            maxTime(ticks: number): this
            setupTime(ticks: number): this
            addReward(itemId: string, count: number): this
            addReward(itemId: string, count: number, nbtString: string): this
            addStackReward(itemId: string, count: number): this
            addStackReward(itemId: string, count: number, nbtString: string): this
            addStackListReward(stacks: JavaList<ItemStack>): this
            addStackListReward(stacksJson: string): this
            addRewardJson(rewardJson: string): this
            addLootTableReward(lootTableId: string, rolls: number): this
            addLootTableReward(lootTableId: string, rolls: number, description: string): this
            addEntityLootReward(entityId: string, rolls: number): this
            addEntityLootReward(entityId: string, nbtString: string, rolls: number): this
            addExperienceReward(experience: number): this
            addExperienceReward(experience: number, orbSize: number): this
            addXpReward(experience: number): this
            addXpReward(experience: number, orbSize: number): this
            addCommandReward(command: string, description: string): this
            addSummonReward(entityId: string, count: number): this
            addSummonReward(entityId: string, nbtString: string, count: number): this
            addChancedReward(rewardJson: string, chance: number): this
            addApotheosisAffixReward(rarityId: string): this
            addApotheosisAffixItemReward(...rarityIds: string[]): this
            addApotheosisGemReward(...purityIds: string[]): this
            addApotheosisTrueRandomGemReward(...purityIds: string[]): this
            buildWave(): GatewayBuilder
        }

        interface WaveEntityBuilder {
            modify(consumer: (entity: EntityModifierBuilder) => void): WaveBuilder
        }

        interface EntityModifierBuilder {
            addAttribute(attribute: Attribute, value: number): this
            addAttribute(attribute: Attribute, value: number, operation: AttributeOperation): this
            addEffect(effect: MobEffect, amplifier: number): this
            addEffect(effect: MobEffect, amplifier: number, chance: number): this
            addEffect(effect: MobEffect, amplifier: number, chance: number, ambient: boolean, visible: boolean): this
            addGearSet(gearSetId: string): this
            addModifierJson(modifierJson: string): this
            setDescription(description: string): this
            setNbt(nbtString: string): this
            addNbt(nbtString: string): this
            finalizeSpawn(finalizeSpawn: boolean): this
        }

        interface EndlessModifierBuilder {
            applicationModeJson(json: string): this
            maxTimeDelta(ticks: number): this
            setupTimeDelta(ticks: number): this
            addEntityJson(entityJson: string): this
            addRewardJson(rewardJson: string): this
            addModifierJson(modifierJson: string): this
            buildModifier(): GatewayBuilder
        }
    }
}

declare const Gateway: {
    getAllGatewayIds(): string[]
    getAllGatewayIds(sortBy: "size" | "id" | "none" | string): string[]
    exists(gatewayId: string): boolean
    getGateway(gatewayId: string): com.hp.gatewaysjs.kubejs.wrapper.GatewayWrapper | null
    getGatewayType(gatewayId: string): "normal" | "endless" | "unknown" | string
    createGateway(level: net.minecraft.world.level.Level, gatewayId: string, x: number, y: number, z: number, player: net.minecraft.world.entity.player.Player | null): boolean
    createGatewayAtPos(level: net.minecraft.world.level.Level, gatewayId: string, pos: net.minecraft.core.BlockPos, player: net.minecraft.world.entity.player.Player | null): boolean
    createPearlItem(gatewayId: string): net.minecraft.world.item.ItemStack
    getGatewayCount(): number
    getNormalGatewayIds(): string[]
    getEndlessGatewayIds(): string[]
    getGatewaysBySize(size: string): string[]
    builder(id: string): com.hp.gatewaysjs.kubejs.builder.GatewayBuilder
    customBuilder(id: string): com.hp.gatewaysjs.kubejs.builder.GatewayBuilder
    endlessBuilder(id: string): com.hp.gatewaysjs.kubejs.builder.GatewayBuilder
    apotheosisTieredBuilder(id: string, tierId: string): com.hp.gatewaysjs.kubejs.builder.GatewayBuilder
    registerJson(id: string, gatewayJson: string): boolean
    registerJson(id: string, gatewayJson: string, displayName: string): boolean
    hidePearlInCreativeTab(gatewayId: string): void
    showPearlInCreativeTab(gatewayId: string): void
    setEntityTexture(gatewayId: string, texturePath: string): void
    setBossBarTexture(gatewayId: string, texturePath: string): void
    setBossBarTexture(gatewayId: string, texturePath: string, bgU: number, bgV: number, fillU: number, fillV: number, barWidth: number, barHeight: number, texWidth: number, texHeight: number): void
    setBossBarTexture(gatewayId: string, texturePath: string, progressTexturePath: string): void
    setBossBarTexture(gatewayId: string, texturePath: string, progressTexturePath: string, bgU: number, bgV: number, fillU: number, fillV: number, barWidth: number, barHeight: number, texWidth: number, texHeight: number): void
}
