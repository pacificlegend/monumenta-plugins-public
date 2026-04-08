package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.InventoryUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ClearSpecialLore {
	public static void register() {
		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		new CommandAPICommand("clearspeciallore")
			.withPermission("monumenta.command.clearspeciallore")
			.withArguments(playerArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				if (player != null) {
					ItemStack[] inventoryItems = player.getInventory().getStorageContents();
					for (ItemStack item : inventoryItems) {
						if (InventoryUtils.containsSpecialLore(item)) {
							item.setAmount(0);
						}
					}
				}
		}).register();
	}
}
