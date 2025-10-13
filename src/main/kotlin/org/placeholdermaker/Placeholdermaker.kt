package org.placeholdermaker

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.apache.logging.log4j.LogManager

class Placeholdermaker : ModInitializer {

    private val logger = LogManager.getLogger("PlaceholderMaker")

    override fun onInitialize() {
        PlaceholderLoader.loadPlaceholders()
        PlaceholderRegistrar.registerPlaceholders()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("pm")
                    .then(
                        CommandManager.literal("reload")
                            .requires { source: ServerCommandSource ->
                                Permissions.check(source, "pm.reload", 2)
                            }
                            .executes { context: CommandContext<ServerCommandSource> ->
                                reloadConfig(context)
                            }
                    )
            )
        }
    }

    private fun reloadConfig(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        try {
            PlaceholderLoader.loadPlaceholders()
            PlaceholderRegistrar.registerPlaceholders()
            source.sendFeedback({ Text.literal("§aPlaceholderMaker config reloaded successfully.") }, false)
            logger.info("Configuration reloaded successfully.")
            return Command.SINGLE_SUCCESS
        } catch (e: Exception) {
            source.sendError(Text.literal("§cFailed to reload Blanket Conditional Text config. Check console for details."))
            logger.error("Failed to reload configuration:", e)
            return 0
        }
    }
}
