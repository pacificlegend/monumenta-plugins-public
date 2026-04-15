package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;

public interface DelvesCS extends ItemBuyableCS {
	int STRAND_PER_DELVE_SKIN = 64;

	@Override
	default Map<NamespacedKey, Integer> getItemCost() {
		Map<NamespacedKey, Integer> cost = new HashMap<>();
		cost.put(Constants.Loot.TWISTED_STRAND, STRAND_PER_DELVE_SKIN);
		cost.put(Constants.Loot.TWISTED_PIGMENT, PIGMENT_PER_SKIN);
		return cost;
	}

	@Override
	default List<String> getCostDescription() {
		return List.of(PIGMENT_PER_SKIN + " Twisted Pigments and", STRAND_PER_DELVE_SKIN + " Twisted Strands");
	}

	@Override
	default CosmeticSkillShopGUI.CSSet getSet() {
		return CosmeticSkillShopGUI.CSSet.DELVE;
	}
}
