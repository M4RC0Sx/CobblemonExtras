package dev.chasem.cobblemonextras.commands

import com.cobblemon.mod.common.Cobblemon
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.chasem.cobblemonextras.CobblemonExtras
import dev.chasem.cobblemonextras.lang.ExtrasLang
import dev.chasem.cobblemonextras.permissions.CobblemonExtrasPermissions
import dev.chasem.cobblemonextras.util.PokemonUtility
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.level.ServerPlayer

class PokeShout {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
                Commands.literal("pokeshout")
                        .requires { src: CommandSourceStack? -> CobblemonExtrasPermissions.checkPermission(src, CobblemonExtras.permissions.POKESHOUT_PERMISSION) }
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6)).executes { ctx: CommandContext<CommandSourceStack> -> this.execute(ctx) })
        )
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        val player: ServerPlayer = ctx.source.player
            ?: run {
                ctx.source.sendFailure(ExtrasLang.get("common.players_only"))
                return 1
            }

        val slot: Int = ctx.getArgument<Int>("slot", Int::class.java)
        val pokemon = Cobblemon.storage.getParty(player).get(slot - 1)
        if (pokemon == null) {
            ctx.source.sendFailure(ExtrasLang.get("pokeshout.empty_slot"))
            return 1
        }

        // The player's display name stays a component rather than being flattened
        // into the header: it may carry a rank prefix or colour that a plain
        // string would throw away.
        val toSend = ExtrasLang.get("pokeshout.header")
                .append(player.displayName!!.copy().withStyle(ExtrasLang.style("pokeshout.player_style")))
                .append(ExtrasLang.get("pokeshout.separator"))
                .append(pokemon.species.translatedName.copy().withStyle(ExtrasLang.style("pokeshout.species_style")))
        if (pokemon.shiny) {
            toSend.append(ExtrasLang.get("pokeshout.shiny"))
        }
        PokemonUtility.getHoverText(toSend, pokemon)
        ctx.source.server.playerList.players.forEach { it.sendSystemMessage(toSend) }
        return 1
    }
}
