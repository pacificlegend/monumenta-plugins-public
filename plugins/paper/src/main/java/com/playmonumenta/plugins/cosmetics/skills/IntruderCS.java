package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;

public interface IntruderCS extends ItemBuyableCS {
	int PSYCHE_PER_INTRUDER_SKIN = 24;

	@Override
	default Map<NamespacedKey, Integer> getItemCost() {
		Map<NamespacedKey, Integer> cost = new HashMap<>();
		cost.put(Constants.Loot.FRACTURED_PSYCHE, PSYCHE_PER_INTRUDER_SKIN);
		cost.put(Constants.Loot.TWISTED_PIGMENT, PIGMENT_PER_SKIN);
		return cost;
	}

	@Override
	default List<String> getCostDescription() {
		return List.of(PIGMENT_PER_SKIN + " Twisted Pigments and", PSYCHE_PER_INTRUDER_SKIN + " Fractured Psyche");
	}

	@Override
	default CosmeticSkillShopGUI.CSSet getSet() {
		return CosmeticSkillShopGUI.CSSet.INTRUDER;
	}
}
