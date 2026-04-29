package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.guis.DailiesGui;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class DailiesGuiCommand {
	public static void register() {
		new CommandAPICommand("dailies")
			.withPermission("monumenta.command.dailies")
			.executes((sender, args) -> {
				// TODO: Is there anywhere that we'd need to restrict this GUI being opened at?
				Player player = CommandUtils.getPlayerFromSender(sender);
				new DailiesGui(player, 1).open(); // defaults to region 1
			})
			.withSubcommand(
				new CommandAPICommand("r1")
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						new DailiesGui(player, 1).open();
					})
			).withSubcommand(
				new CommandAPICommand("r2")
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						if (PlayerUtils.hasUnlockedIsles(player)) {
							new DailiesGui(player, 2).open();
						} else {
							player.sendMessage(Component.text("You don't have access to this region!", NamedTextColor.RED));
						}
					})
			).withSubcommand(
				new CommandAPICommand("r3")
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						if (PlayerUtils.hasUnlockedRing(player)) {
							new DailiesGui(player, 3).open();
						} else {
							player.sendMessage(Component.text("You don't have access to this region!", NamedTextColor.RED));
						}
					})
			).register();
	}
}
