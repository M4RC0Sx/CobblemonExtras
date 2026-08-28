package dev.chasem.cobblemonextras.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import dev.chasem.cobblemonextras.CobblemonExtras
import dev.chasem.cobblemonextras.lang.ExtrasLang
import dev.chasem.cobblemonextras.permissions.CobblemonExtrasPermissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

class ItemShout {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
                Commands.literal("itemshout")
                        .requires { src: CommandSourceStack? -> CobblemonExtrasPermissions.checkPermission(src, CobblemonExtras.permissions.ITEMSHOUT_PERMISSION) }
                        .executes { ctx: CommandContext<CommandSourceStack> -> this.execute(ctx) }
        )
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        val player: ServerPlayer = ctx.source.player
            ?: run {
                ctx.source.sendFailure(ExtrasLang.get("common.players_only"))
                return 1
            }

        val heldItem: ItemStack = player.mainHandItem
        // The item's own name and its hover panel are vanilla's: they are
        // translatable and rendered by each client, which is exactly what a
        // shout wants. Only the wrapping text is ours.
        val hoverable = heldItem.displayName.copy().withStyle(
            heldItem.displayName.style
                .applyTo(ExtrasLang.style("itemshout.item_style"))
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackInfo(heldItem)))
        )

        val toSend = ExtrasLang.get("itemshout.header")
                .append(player.displayName!!.copy().withStyle(ExtrasLang.style("itemshout.player_style")))
                .append(ExtrasLang.get("itemshout.separator"))
                .append(hoverable)
        ctx.source.server.playerList.players.forEach { it.sendSystemMessage(toSend) }
        return 1
    }
}
