package com.playmonumenta.plugins.cosmetics.skills;

import java.util.List;
import org.bukkit.entity.Player;

public interface BuyableCS extends CosmeticSkill {
	// Return true if purchase has succeeded
	boolean attemptPurchase(Player player);

	List<String> getCostDescription();

	CosmeticSkillShopGUI.CSSet getSet();
}
