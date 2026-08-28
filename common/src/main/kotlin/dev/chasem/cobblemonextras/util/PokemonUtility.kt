package dev.chasem.cobblemonextras.util

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.lang
import dev.chasem.cobblemonextras.lang.ExtrasLang
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import java.lang.Boolean
import kotlin.arrayOf

object PokemonUtility {
    /**
     * Builds the chat line's hover buttons: [Stats], [EVs], [IVs], [Moves].
     *
     * Every label, colour and separator comes from ExtrasLang, so the whole
     * thing can be translated and restyled from a file. The pieces that stay
     * components — the species name, the nature, the ability — do so on purpose:
     * they are translatable, and flattening them here would freeze every player
     * into the server's language. Their colour still comes from the file, via
     * ExtrasLang.style.
     */
    fun getHoverText(toSend: MutableComponent, pokemon: Pokemon): Component {
        val newLine = Component.literal("\n")

        val statsHoverText = Component.literal("").withStyle(Style.EMPTY)
        if (pokemon.shiny) {
            statsHoverText.append(ExtrasLang.get("hover.stats.shiny"))
        }
        statsHoverText.append(pokemon.species.translatedName.copy().withStyle(ExtrasLang.style("hover.stats.species_style")))
        statsHoverText.append(newLine)
        if (pokemon.nickname != null) {
            statsHoverText.append(ExtrasLang.get("hover.stats.nickname", pokemon.nickname!!.string))
            statsHoverText.append(newLine)
        }
        statsHoverText.append(ExtrasLang.get("hover.stats.level", pokemon.level))
        statsHoverText.append(newLine)
        statsHoverText.append(
            ExtrasLang.get("hover.stats.nature")
                .append(lang(pokemon.nature.displayName.replace("cobblemon.", "")).withStyle(ExtrasLang.style("hover.stats.nature_style")))
        )
        statsHoverText.append(newLine)
        statsHoverText.append(
            ExtrasLang.get("hover.stats.ability")
                .append(lang(pokemon.ability.displayName.replace("cobblemon.", "")).withStyle(ExtrasLang.style("hover.stats.ability_style")))
        )
        statsHoverText.append(newLine)
        statsHoverText.append(ExtrasLang.get("hover.stats.form", pokemon.form.name))

        toSend.append(hoverButton("hover.button.stats", statsHoverText))

        // EVs cap at 510 across all six, so the percentage is of that total.
        val allEvs = (pokemon.evs.getOrDefault(Stats.HP) + pokemon.evs.getOrDefault(Stats.ATTACK)
                + pokemon.evs.getOrDefault(Stats.DEFENCE) + pokemon.evs.getOrDefault(Stats.SPECIAL_ATTACK)
                + pokemon.evs.getOrDefault(Stats.SPECIAL_DEFENCE) + pokemon.evs.getOrDefault(Stats.SPEED)).toDouble()
        val evPercent = Math.round((allEvs / 510.0) * 10000).toDouble() / 100

        val evsHoverText = Component.literal("").withStyle(Style.EMPTY)
        evsHoverText.append(ExtrasLang.get("hover.evs.title", evPercent))
        appendStatLines(evsHoverText, newLine, "hover.evs", pokemon.evs::getOrDefault)
        toSend.append(hoverButton("hover.button.evs", evsHoverText))

        // IVs cap at 31 each, 186 across the six.
        val allIvs = pokemon.ivs.getOrDefault(Stats.HP) + pokemon.ivs.getOrDefault(Stats.ATTACK) +
                pokemon.ivs.getOrDefault(Stats.DEFENCE) + pokemon.ivs.getOrDefault(Stats.SPECIAL_ATTACK) +
                pokemon.ivs.getOrDefault(Stats.SPECIAL_DEFENCE) + pokemon.ivs.getOrDefault(Stats.SPEED)
        val ivPercent = Math.round((allIvs / 186.0) * 10000).toDouble() / 100

        val ivsHoverText = Component.literal("").withStyle(Style.EMPTY)
        ivsHoverText.append(ExtrasLang.get("hover.ivs.title", ivPercent))
        appendStatLines(ivsHoverText, newLine, "hover.ivs", pokemon.ivs::getOrDefault)
        toSend.append(hoverButton("hover.button.ivs", ivsHoverText))

        val movesHoverText = Component.literal("").withStyle(Style.EMPTY)
        movesHoverText.append(ExtrasLang.get("hover.moves.title"))
        val moveKeys = listOf("hover.moves.one", "hover.moves.two", "hover.moves.three", "hover.moves.four")
        for (i in moveKeys.indices) {
            val name = if (pokemon.moveSet.getMoves().size >= i + 1) {
                pokemon.moveSet[i]!!.displayName.string
            } else {
                ExtrasLang.raw("hover.moves.empty")
            }
            movesHoverText.append(newLine).append(ExtrasLang.get(moveKeys[i], name))
        }
        toSend.append(hoverButton("hover.button.moves", movesHoverText))

        return toSend
    }

    /** A clickable-looking label whose whole purpose is to carry a hover panel. */
    private fun hoverButton(labelKey: String, hover: Component): MutableComponent {
        val label = ExtrasLang.get(labelKey)
        label.style = label.style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
        return label
    }

    /**
     * The six stat lines, in one place.
     *
     * EVs and IVs print the same six labels in the same order and differ only in
     * which map they read, so they share this rather than sixteen near-identical
     * lines that drift apart the first time somebody renames a stat.
     */
    private fun appendStatLines(
        target: MutableComponent,
        newLine: Component,
        prefix: String,
        value: (com.cobblemon.mod.common.api.pokemon.stats.Stat) -> Int
    ) {
        val rows = listOf(
            "hp" to Stats.HP,
            "attack" to Stats.ATTACK,
            "defence" to Stats.DEFENCE,
            "special_attack" to Stats.SPECIAL_ATTACK,
            "special_defence" to Stats.SPECIAL_DEFENCE,
            "speed" to Stats.SPEED
        )
        for ((suffix, stat) in rows) {
            target.append(newLine).append(ExtrasLang.get("$prefix.$suffix", value(stat)))
        }
    }

    /**
     * The Pokemon as an inventory item, for the /pokesee and /compsee menus.
     *
     * Same rule as the chat hover: labels and colours come from the language
     * file, while the ball, nature, ability and species names stay components
     * so each client reads them in its own language.
     */
    fun pokemonToItem(pokemon: Pokemon): ItemStack {
        val ivs = pokemon.ivs
        val evs = pokemon.evs
        val lore = mutableListOf<Component>()

        lore.add(pokemon.caughtBall.item().defaultInstance.displayName.copy().withStyle(ExtrasLang.style("item.ball_style")))
        lore.add(ExtrasLang.get("item.level", pokemon.level))
        lore.add(ExtrasLang.get("item.nickname", pokemon.nickname?.string ?: ExtrasLang.raw("item.no_nickname")))
        lore.add(ExtrasLang.get("item.nature").append(lang(pokemon.nature.displayName.replace("cobblemon.", "")).withStyle(ExtrasLang.style("item.nature_style"))))
        lore.add(ExtrasLang.get("item.ability").append(lang(pokemon.ability.displayName.replace("cobblemon.", "")).withStyle(ExtrasLang.style("item.ability_style"))))

        lore.add(ExtrasLang.get("item.ivs_header"))
        lore.add(ExtrasLang.get("item.stats_line_one", ivs.getOrDefault(Stats.HP), ivs.getOrDefault(Stats.ATTACK), ivs.getOrDefault(Stats.DEFENCE)))
        lore.add(ExtrasLang.get("item.stats_line_two", ivs.getOrDefault(Stats.SPECIAL_ATTACK), ivs.getOrDefault(Stats.SPECIAL_DEFENCE), ivs.getOrDefault(Stats.SPEED)))

        lore.add(ExtrasLang.get("item.evs_header"))
        lore.add(ExtrasLang.get("item.stats_line_one", evs.getOrDefault(Stats.HP), evs.getOrDefault(Stats.ATTACK), evs.getOrDefault(Stats.DEFENCE)))
        lore.add(ExtrasLang.get("item.stats_line_two", evs.getOrDefault(Stats.SPECIAL_ATTACK), evs.getOrDefault(Stats.SPECIAL_DEFENCE), evs.getOrDefault(Stats.SPEED)))

        lore.add(ExtrasLang.get("item.moves_header"))
        for (i in 0..3) {
            val name = if (pokemon.moveSet.getMoves().size >= i + 1) {
                pokemon.moveSet[i]!!.displayName.string
            } else {
                ExtrasLang.raw("hover.moves.empty")
            }
            lore.add(ExtrasLang.get("item.move", name))
        }
        lore.add(ExtrasLang.get("item.form", pokemon.form.name))

        val name = pokemon.species.translatedName.copy().withStyle(ExtrasLang.style("item.name_style"))
        if (pokemon.shiny) {
            name.append(ExtrasLang.get("item.name_shiny"))
        }

        return ItemBuilder(PokemonItem.from(pokemon, 1))
            .hideAdditional()
            .addLore(lore.toTypedArray())
            .setCustomName(name)
            .build()
    }
}