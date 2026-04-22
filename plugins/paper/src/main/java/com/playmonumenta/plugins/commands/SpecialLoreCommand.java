package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpecialLoreCommand {
	public static void register() {
		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		new CommandAPICommand("speciallore")
			.withPermission("monumenta.command.speciallore")
			.withSubcommand(
				new CommandAPICommand("clear")
					.withArguments(playerArg)
					.executes((sender, args) -> {
						Player player = args.getByArgument(playerArg);
						if (player != null) {
							ItemStack[] playerItems = player.getInventory().getContents();
							for (ItemStack item : playerItems) {
								// Clear all special lore tagged items from the specified player's inventory.
								if (InventoryUtils.containsSpecialLore(item)) {
									item.setAmount(0);
								}
							}
						}
					})
				)
			.withSubcommand(
				new CommandAPICommand("assignworld")
					.withArguments(playerArg)
					.executes((sender, args) -> {
						Player player = args.getByArgument(playerArg);
						if (player != null) {
							ItemStack[] playerItems = player.getInventory().getContents();
							for (ItemStack item : playerItems) {
								// Assign world to all items on the player that do not already have one assigned.
								if (InventoryUtils.containsSpecialLore(item) && !ItemStatUtils.hasAssignedWorld(item)) {
									ItemStatUtils.addAssignedWorld(item, player.getWorld().getName());
								}
							}
						}
					})
				)
			.register();
	}
}
