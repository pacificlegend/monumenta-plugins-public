package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;

public interface HexfallCS extends ItemBuyableCS {
	int SAPLING_PER_HEXFALL_SKIN = 1;

	@Override
	default Map<NamespacedKey, Integer> getItemCost() {
		Map<NamespacedKey, Integer> cost = new HashMap<>();
		cost.put(Constants.Loot.LIFEROOT_SAPLING, SAPLING_PER_HEXFALL_SKIN);
		cost.put(Constants.Loot.TWISTED_PIGMENT, PIGMENT_PER_SKIN);
		return cost;
	}

	@Override
	default List<String> getCostDescription() {
		return List.of(PIGMENT_PER_SKIN + " Twisted Pigments and", SAPLING_PER_HEXFALL_SKIN + " Liferoot Sapling");
	}

	@Override
	default CosmeticSkillShopGUI.CSSet getSet() {
		return CosmeticSkillShopGUI.CSSet.HEXFALL;
	}
}
