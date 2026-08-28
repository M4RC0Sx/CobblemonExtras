package dev.chasem.cobblemonextras.game.poketokens

import dev.chasem.cobblemonextras.lang.ExtrasLang
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.platform.events.ServerPlayerEvent
import dev.chasem.cobblemonextras.CobblemonExtras
import dev.chasem.cobblemonextras.util.ItemBuilder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

class ShinyPokeToken : PokeToken(PokeTokenType.SHINY) {

    override fun generateItem(amount: Int): ItemBuilder {
        val builder = super.generateItem(amount)
        builder
            .setCustomModel(CobblemonExtras.config.customModels.SHINY_TOKEN)
        return builder
    }

    override fun getName(): MutableComponent {
        return ExtrasLang.get("token.shiny.name")
    }

    override fun getDescription(): MutableComponent {
        return ExtrasLang.get("token.shiny.lore")
    }

    override fun onUseItem(event: ServerPlayerEvent.RightClickEntity, entity: PokemonEntity) {
        val player = event.player
        entity.pokemon.shiny = true
        player.playNotifySound(
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1.0F, 1.0F
        )
        player.sendSystemMessage(ExtrasLang.get("token.shiny.applied"))
    }


}