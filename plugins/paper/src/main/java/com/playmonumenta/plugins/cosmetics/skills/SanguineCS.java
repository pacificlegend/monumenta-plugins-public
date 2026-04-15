package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public interface SanguineCS extends LockableCS, ItemBuyableCS {

	String GALLERY_COMPLETE_SCB = "GallerySanguineHallsEasterEgg";
	int CANVAS_PER_GALLERY_SKIN = 2 * 64;

	@Override
	default boolean isUnlocked(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, GALLERY_COMPLETE_SCB).orElse(0) >= 1
			|| player.getGameMode() == GameMode.CREATIVE;
	}

	@Override
	default String getLockDesc() {
		// I feel like this message is a bit misleading but don't know what it should be instead and it's been this forever
		return "Complete Sanguine Halls to unlock!";
	}

	@Override
	default Map<NamespacedKey, Integer> getItemCost() {
		Map<NamespacedKey, Integer> cost = new HashMap<>();
		cost.put(Constants.Loot.TORN_CANVAS, CANVAS_PER_GALLERY_SKIN);
		cost.put(Constants.Loot.TWISTED_PIGMENT, PIGMENT_PER_SKIN);
		return cost;
	}

	@Override
	default List<String> getCostDescription() {
		return List.of(PIGMENT_PER_SKIN + " Twisted Pigments and", CANVAS_PER_GALLERY_SKIN + " Torn Canvases");
	}

	@Override
	default CosmeticSkillShopGUI.CSSet getSet() {
		return CosmeticSkillShopGUI.CSSet.SANGUINE;
	}
}
