package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.utils.InventoryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemBuyableCS extends BuyableCS {
	int PIGMENT_PER_SKIN = 3;

	Map<NamespacedKey, Integer> getItemCost();

	@Override
	default boolean attemptPurchase(Player player) {
		Map<NamespacedKey, Integer> requiredItems = getItemCost();
		List<ItemStack> items = new ArrayList<>();
		for (Map.Entry<NamespacedKey, Integer> entry : requiredItems.entrySet()) {
			ItemStack item = InventoryUtils.getItemFromLootTable(player, entry.getKey());
			if (item == null) {
				return false;
			}

			// TODO feature: wallet integration?
			if (!player.getInventory().containsAtLeast(item, entry.getValue())) {
				player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
				player.sendMessage(Component.text("You don't have enough items to buy this cosmetic skill!", NamedTextColor.RED));
				return false;
			}

			item.setAmount(entry.getValue());
			items.add(item);
		}

		for (ItemStack item : items) {
			player.getInventory().removeItem(item);
		}
		return true;
	}
}
