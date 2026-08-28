package dev.chasem.cobblemonextras.commands

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import dev.chasem.cobblemonextras.CobblemonExtras
import dev.chasem.cobblemonextras.lang.ExtrasLang
import dev.chasem.cobblemonextras.permissions.CobblemonExtrasPermissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.world.level.entity.EntityTypeTest

class PokeKill {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
                Commands.literal("pokekill")
                        .requires { src: CommandSourceStack? -> CobblemonExtrasPermissions.checkPermission(src, CobblemonExtras.permissions.POKEKILL_PERMISSION) }
                        .executes { ctx: CommandContext<CommandSourceStack> -> this.execute(ctx) }
        )
    }

    /**
     * Removes wild Pokemon from every world.
     *
     * Three things decide whether a Pokemon is left alone, and each is here for
     * a reason:
     *
     *  - ownerUUID != null. NOT 'owner != null'. getOwner() resolves the UUID to
     *    a player who is CURRENTLY ONLINE and returns null otherwise, so testing
     *    it would treat every offline player's Pokemon as wild and delete them.
     *    The UUID is on the entity and does not care who is connected.
     *
     *  - tethering != null. A Pokemon tied to a pasture block belongs to
     *    somebody even though it is standing in the world.
     *
     *  - isBattling. Removing a combatant mid-turn leaves the battle in a state
     *    nothing else expects.
     *
     *  - shiny, legendary, mythical or ultra beast. These are not ownership
     *    checks, they are worth checks: a wild shiny is somebody's afternoon and
     *    a legendary may be the only one on the server. Trimming entity count is
     *    never worth erasing one by accident.
     *
     *    Mythical is a separate flag from legendary in Cobblemon, not a subset —
     *    Mew and Celebi answer false to isLegendary() — which is exactly the
     *    kind of gap that turns a cleanup command into an incident.
     *
     * The entities are collected per world before anything is removed: mutating
     * a world's entity list while iterating it is asking for trouble.
     */
    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        var removed = 0
        var worlds = 0

        for (level in ctx.source.server.allLevels) {
            worlds++
            val wild = level.getEntities(EntityTypeTest.forClass(PokemonEntity::class.java)) { candidate ->
                val pokemon = candidate.pokemon
                candidate.ownerUUID == null &&
                    candidate.tethering == null &&
                    !candidate.isBattling &&
                    !pokemon.shiny &&
                    !pokemon.isLegendary() &&
                    !pokemon.isMythical() &&
                    !pokemon.isUltraBeast()
            }
            for (pokemon in wild) {
                // discard(), not kill(): this is a cleanup, so no death animation,
                // no drops and no experience. kill() would also run the death
                // path on an entity we are about to remove anyway.
                pokemon.discard()
                removed++
            }
        }

        ctx.source.sendSystemMessage(ExtrasLang.get("pokekill.killed", removed, worlds))
        return 1
    }
}
