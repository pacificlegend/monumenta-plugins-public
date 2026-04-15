package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;

public interface DepthsCS extends ItemBuyableCS {
	int GEODE_PER_DEPTHS_SKIN = 2 * 64;
	int TALISMAN_PER_DEPTHS_SKIN = 1;
	String TALISMAN_LOOTTABLE_FOLDER = "epic:r2/depths/utility/";

	String TALISMAN_FLAME = "flamecaller_talisman";
	String TALISMAN_FROST = "frostborn_talisman";
	String TALISMAN_WIND = "windwalker_talisman";
	String TALISMAN_EARTH = "earthbound_talisman";
	String TALISMAN_DAWN = "dawnbringer_talisman";
	String TALISMAN_SHADOW = "shadowdancer_talisman";
	String TALISMAN_STEEL = "steelsage_talisman";
	String EXTRA_GEODES = "extra_geodes";

	String getToken();

	@Override
	default Map<NamespacedKey, Integer> getItemCost() {
		Map<NamespacedKey, Integer> cost = new HashMap<>();
		String token = getToken();
		boolean isExtraGeodes = token.equals(EXTRA_GEODES);

		cost.put(Constants.Loot.VOIDSTAINED_GEODE, isExtraGeodes ? GEODE_PER_DEPTHS_SKIN + 64 : GEODE_PER_DEPTHS_SKIN);
		cost.put(Constants.Loot.TWISTED_PIGMENT, PIGMENT_PER_SKIN);
		if (!isExtraGeodes) {
			NamespacedKey talismanLoot = NamespacedKeyUtils.fromString(TALISMAN_LOOTTABLE_FOLDER + token);
			cost.put(talismanLoot, TALISMAN_PER_DEPTHS_SKIN);
		}
		return cost;
	}

	@Override
	default List<String> getCostDescription() {
		String token = getToken();
		boolean isExtraGeodes = token.equals(EXTRA_GEODES);
		if (isExtraGeodes) {
			return List.of(
				PIGMENT_PER_SKIN + " Twisted Pigments and",
				GEODE_PER_DEPTHS_SKIN + 64 + " Voidstained Geodes");
		} else {
			String talismanName = StringUtils.capitalizeWords(token.replace('_', ' '));
			return List.of(
				PIGMENT_PER_SKIN + " Twisted Pigments,",
				TALISMAN_PER_DEPTHS_SKIN + " " + talismanName + " and",
				GEODE_PER_DEPTHS_SKIN + " Voidstained Geodes");
		}
	}

	@Override
	default CosmeticSkillShopGUI.CSSet getSet() {
		return CosmeticSkillShopGUI.CSSet.DEPTHS;
	}
}
