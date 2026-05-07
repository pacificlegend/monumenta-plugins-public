package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;

public class DamageTraceCommand {
	public static final String TAG = "DevDamageTracerTag";

	public static void register() {

		new CommandAPICommand("damagetrace")
			.withPermission("monumenta.command.tracedamage")
			.withSubcommand(new CommandAPICommand("start")
				.executes((sender, args) -> {
					if (sender instanceof Player player) {
						player.sendMessage("Damage tracing started (deal or take damage).");
						player.addScoreboardTag(TAG);
					}
				}))
			.withSubcommand(new CommandAPICommand("stop")
				.executes((sender, args) -> {
					if (sender instanceof Player player) {
						player.sendMessage("Damage tracing stopped.");
						player.removeScoreboardTag(TAG);
					}
				}))
			.register();
	}
}
