package dev.chasem.cobblemonextras.commands

import com.cobblemon.mod.common.Cobblemon
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import dev.chasem.cobblemonextras.CobblemonExtras
import dev.chasem.cobblemonextras.lang.ExtrasLang
import dev.chasem.cobblemonextras.permissions.CobblemonExtrasPermissions
import dev.chasem.cobblemonextras.util.PokemonUtility
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

class PokeShoutAll {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
                Commands.literal("pokeshoutall")
                        .requires { src: CommandSourceStack? -> CobblemonExtrasPermissions.checkPermission(src, CobblemonExtras.permissions.POKESHOUT_ALL_PERMISSION) }
                        .executes { ctx: CommandContext<CommandSourceStack> -> this.execute(ctx) }
        )
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        val player: ServerPlayer = ctx.source.player
            ?: run {
                ctx.source.sendFailure(ExtrasLang.get("common.players_only"))
                return 1
            }

        val party = Cobblemon.storage.getParty(player)
        val newLine = Component.literal("\n")
        val toSend = ExtrasLang.get("pokeshoutall.header")
                .append(player.displayName!!.copy().withStyle(ExtrasLang.style("pokeshoutall.player_style")))
                .append(ExtrasLang.get("pokeshoutall.separator"))

        // Every slot is printed, empty ones included: a gap in the list is
        // information too, and skipping them would make slot numbers lie.
        for (i in 0..5) {
            toSend.append(newLine).append(ExtrasLang.get("pokeshoutall.slot", i + 1))
            val pokemon = party.get(i)
            if (pokemon == null) {
                toSend.append(ExtrasLang.get("pokeshoutall.empty"))
                continue
            }
            toSend.append(pokemon.species.translatedName.copy().withStyle(ExtrasLang.style("pokeshoutall.species_style")))
            if (pokemon.shiny) {
                toSend.append(ExtrasLang.get("pokeshoutall.shiny"))
            }
            PokemonUtility.getHoverText(toSend, pokemon)
        }
        ctx.source.server.playerList.players.forEach { it.sendSystemMessage(toSend) }
        return 1
    }
}
