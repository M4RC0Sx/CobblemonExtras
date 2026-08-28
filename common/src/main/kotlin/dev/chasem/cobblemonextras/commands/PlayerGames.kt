package dev.chasem.cobblemonextras.commands

import dev.chasem.cobblemonextras.lang.ExtrasLang
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer

class PlayerGames {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
                Commands.literal("playergames")
                        .executes { ctx: CommandContext<CommandSourceStack> -> this.execute(ctx) }
        )
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        if (ctx.getSource().getPlayer() != null) {
            val player: ServerPlayer = ctx.getSource().getPlayer()!!
            val hoverable = ExtrasLang.get("playergames.title").withStyle(
                    Style.EMPTY.withUnderlined(true)
                            .withColor(ChatFormatting.LIGHT_PURPLE)
                            .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.player.games/en-US/creator-hub"))
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ExtrasLang.get("playergames.hover")))
            )
            player.sendSystemMessage(ExtrasLang.get("playergames.text").append(hoverable))
        } else {
            ctx.getSource().sendFailure(ExtrasLang.get("common.players_only"))
        }
        return 1
    }
}
