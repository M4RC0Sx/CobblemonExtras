package dev.chasem.cobblemonextras.game.poketokens

import dev.chasem.cobblemonextras.lang.ExtrasLang
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.platform.events.ServerPlayerEvent
import dev.chasem.cobblemonextras.CobblemonExtras
import dev.chasem.cobblemonextras.util.ItemBuilder
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.component.CustomData

class MaxIVPokeToken (private val stats: Set<Stat>) : PokeToken(PokeTokenType.MAXIV) {

    override fun getName(): MutableComponent {
        return ExtrasLang.get("token.ivs.name")
    }

    override fun getDescription(): MutableComponent {
        return ExtrasLang.get("token.ivs.lore")
    }

    val CONSTANT_COLORS = arrayOf(ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_AQUA, ChatFormatting.RED, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_RED, ChatFormatting.YELLOW, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GRAY, ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.LIGHT_PURPLE, ChatFormatting.WHITE, ChatFormatting.YELLOW)


    override fun generateItem(amount: Int) : ItemBuilder {
        val builder = super.generateItem(amount)
        val statsSplit = stats.joinToString(",") { it.displayName.string };
        return builder.setCustomData(CustomData.of(
                CompoundTag().apply {
                    this.putString("stats", statsSplit)
                }
        ))
        .addLore(
                arrayOf(
                        Component.literal(""),
                        ExtrasLang.get("token.ivs.label"),
                        *stats.mapIndexed { index, it -> Component.literal("    " + it.displayName.string).withStyle(CONSTANT_COLORS[index]) }.toTypedArray()
//                                .append(Component.literal(stats.joinToString(", ") { it.displayName.string }).withStyle(ChatFormatting.WHITE)),
                        // Append each stat to the lore, and use a differnet color for each stat


                )
        )
        .setCustomModel(CobblemonExtras.config.customModels.IV_TOKEN)
    }

    override fun onUseItem(event: ServerPlayerEvent.RightClickEntity, entity: PokemonEntity) {
        val player = event.player
        val heldItem = player.mainHandItem
        if (heldItem.has(DataComponents.CUSTOM_DATA) && heldItem.get(DataComponents.CUSTOM_DATA)!!.contains("PokeTokenType")) {
            player.playNotifySound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1.0F, 1.0F
            )
            stats.forEach {
                entity.pokemon.setIV(it, 31)
            }
            player.sendSystemMessage(ExtrasLang.get("token.ivs.applied_prefix")
                    .append(Component.literal(entity.pokemon.species.translatedName.string + " ").withStyle(ChatFormatting.YELLOW))
                    .append(
                            stats.joinToString(", ") { it.displayName.string }
                                    .split(", ")
                                    .mapIndexed { index, it -> Component.literal(it).withStyle(CONSTANT_COLORS[index]) }
                                    .reduce { acc, component -> acc.append(ExtrasLang.get("token.ivs.separator")).append(component) }
                    )
                    .append(ExtrasLang.get("token.ivs.applied_suffix"))
            )
        }
    }


}