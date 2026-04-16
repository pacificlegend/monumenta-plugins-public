package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SharpshooterCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SHARPSHOOTER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TARGET;
	}

	public void hitEffect(Player player, LivingEntity enemy) {
		player.playSound(enemy.getLocation(), Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 0.6f, 1.4f);
		player.playSound(enemy.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.6f, 0.8f);
		player.playSound(enemy.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.6f, 2f);

	}

	public void arrowSave(Player player) {
		player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.3f, 1.0f);
	}

	public void stackCount(Player player, int stacks) {

	}
}
