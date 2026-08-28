package dev.chasem.cobblemonextras.commands

import dev.chasem.cobblemonextras.lang.ExtrasLang
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import dev.chasem.cobblemonextras.CobblemonExtras
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer

class Showcase {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
                Commands.literal("showcase")
                        .then(Commands.literal("off")
                                .executes { ctx: CommandContext<CommandSourceStack> -> this.toggle(ctx, false) })
                        .then(Commands.literal("on")
                                .executes { ctx: CommandContext<CommandSourceStack> -> this.toggle(ctx, true) })
                        .executes { ctx: CommandContext<CommandSourceStack> -> this.execute(ctx) }
        )
    }

    private fun toggle(ctx: CommandContext<CommandSourceStack>, enable: Boolean): Int {
        if (ctx.getSource().getPlayer() != null) {
            val player: ServerPlayer = ctx.getSource().getPlayer()!!

            if(!CobblemonExtras.config.showcase.isShowcaseEnabled) {
                player.sendSystemMessage(ExtrasLang.get("showcase.disabled"));
                return 1
            }

            player.sendSystemMessage(ExtrasLang.get("showcase.toggling"))
            CobblemonExtras.showcaseService.togglePlayerPublic(player, enable)
        } else {
            ctx.getSource().sendFailure(ExtrasLang.get("common.players_only"))
        }
        return 1
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        if (ctx.getSource().getPlayer() != null) {
            val player: ServerPlayer = ctx.getSource().getPlayer()!!
            val hoverable = ExtrasLang.get("showcase.here").withStyle(
                    Style.EMPTY.withUnderlined(true)
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://cobblemonextras.com/"))
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ExtrasLang.get("showcase.hover")))
            )
            player.sendSystemMessage(ExtrasLang.get("showcase.text").append(hoverable))
        } else {
            ctx.getSource().sendFailure(ExtrasLang.get("common.players_only"))
        }
        return 1
    }
}
