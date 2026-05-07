package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.guis.DungeonsGui;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class DungeonsGuiCommand {
	public static void register() {
		new CommandAPICommand("dungeons")
			.withPermission("monumenta.command.dungeons")
			.executes((sender, args) -> {
				// TODO: Is there anywhere that we'd need to restrict this GUI being opened at?
				Player player = CommandUtils.getPlayerFromSender(sender);
				int region = Region.getRegionNumber(ServerProperties.getRegion(player));
				if ((region == 2 && !PlayerUtils.hasUnlockedIsles(player))
					|| (region == 3 && !PlayerUtils.hasUnlockedRing(player))) {
					new DungeonsGui(player, 1).open(); // opens to region 1 if player is in a region they don't have access to, ex: playerplots
				} else {
					new DungeonsGui(player, region).open(); // opens to player's region
				}
			})
			.withSubcommand(
				new CommandAPICommand("r1")
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						new DungeonsGui(player, 1).open();
					})
			).withSubcommand(
				new CommandAPICommand("r2")
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						if (PlayerUtils.hasUnlockedIsles(player)) {
							new DungeonsGui(player, 2).open();
						} else {
							player.sendMessage(Component.text("You don't have access to this region!", NamedTextColor.RED));
						}
					})
			).withSubcommand(
				new CommandAPICommand("r3")
					.executes((sender, args) -> {
						Player player = CommandUtils.getPlayerFromSender(sender);
						if (PlayerUtils.hasUnlockedRing(player)) {
							new DungeonsGui(player, 3).open();
						} else {
							player.sendMessage(Component.text("You don't have access to this region!", NamedTextColor.RED));
						}
					})
			).register();
	}
}
