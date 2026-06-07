declare namespace com.hp.gatewaysjs.kubejs.builder {
    interface GatewayBuilder {
        modifyAllWaves(consumer: (wave: GatewayBuilder.WaveBuilder) => void): this
        addApotheosisAffixReward(rarityId: string): this
    }

    namespace GatewayBuilder {
        interface WaveBuilder {
            addApotheosisBoss(): this
            addApotheosisBoss(bossId: string): this
            addApotheosisAffixReward(rarityId: string): this
        }
    }
}
