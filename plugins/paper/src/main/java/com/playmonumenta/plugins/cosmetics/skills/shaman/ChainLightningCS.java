package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ChainLightningCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CHAIN_LIGHTNING;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	public void chainLightningCast(Player player, LivingEntity prevTarget, LivingEntity target, int i) {
		if (i == 0) {
			lightning(player, prevTarget, target);
		} else {
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> lightning(player, prevTarget, target), i);
		}
	}

	private void lightning(Player player, LivingEntity prevTarget, LivingEntity target) {
		new PPLightning(Particle.END_ROD, LocationUtils.getEntityCenter(target), LocationUtils.getEntityCenter(prevTarget), 0.4, 0.21)
			.hopsPerBlock(0.8)
			.minimumHops(4)
			.count(6)
			.duration(1)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.CRIT, LocationUtils.getEntityCenter(target), 0.01)
			.count(20)
			.directionalMode(true)
			.rotateDelta(true)
			.deltaVariance(false, false, true, false, true, true)
			.delta(0.9, 0.8, 0.1)
			.extra(1)
			.distanceFalloff(16)
			.spawnAsPlayerActive(player);
	}

	public void chainLightningSound(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 0.3f, 2f);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 2f, 3);
	}
}
