package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.integrations.ChestSortIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class Butterfingers extends Ability {
	private static final String SCOREBOARD = "Butterfingers";
	private static final int POINT_COST = 2;

	public static final AbilityInfo<Butterfingers> INFO =
		new SnowPerkGui.SnowPerkInfo<>(Butterfingers.class, "Butterfingers", Butterfingers::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.EGG)
			.description(getDescription());

	public Butterfingers(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void playerPickupItemEvent(EntityPickupItemEvent event) {
		ItemStack item = event.getItem().getItemStack();
		if (event.getEntity() instanceof Player player && item.getType().equals(Material.SPLASH_POTION)) {
			event.setCancelled(true);
			event.getItem().remove();

			ThrownPotion potion = EntityUtils.spawnSplashPotion(player, item, true);
			PotionUtils.mimicSplashPotionEffect(player, potion);
			PotionUtils.splashPotionParticlesAndSound(player, potion.getPotionMeta().getColor());

			mPlayer.playSound(mPlayer, Sound.ENTITY_GLOW_ITEM_FRAME_REMOVE_ITEM, SoundCategory.PLAYERS, 1f, 1f);
		}
	}

	@Override
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		Inventory inventory = event.getInventory();
		if (event.getPlayer() instanceof Player && inventory.getLocation() != null && inventory.getType().equals(InventoryType.CHEST)) {
			ChestSortIntegration.sortInventory(inventory);
			mPlayer.playSound(mPlayer, Sound.UI_LOOM_TAKE_RESULT, SoundCategory.PLAYERS, 1f, 1.25f);
		}
	}

	public static Description<Butterfingers> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("When you pick up a splash potion,")
			.addLine("throw it onto the ground instead.")
			.addLine()
			.addLine("When you open a chest, automatically")
			.addLine("sort its contents.")
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
