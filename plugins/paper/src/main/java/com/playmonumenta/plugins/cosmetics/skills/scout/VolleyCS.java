package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class VolleyCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.VOLLEY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ARROW;
	}

	public void volleyEffect(Player player) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1f);
		world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1.33f);
	}

	public void volleyMultishotParticle(Player player, int t, int level, int totalTime) {
		float yaw = player.getYaw() + 90;
		if (t % 4 == 0) {
			for (int i = -level; i <= level; i++) {
				double angleOffset = yaw + 10.0 * i;

				Vector vec = new Vector(
					FastUtils.cosDeg(angleOffset),
					-0.2,
					FastUtils.sinDeg(angleOffset)
				);

				Location particleLoc = LocationUtils.getHalfHeightLocation(player).add(vec.clone().multiply(0.4));
				new PartialParticle(Particle.CRIT_MAGIC, particleLoc)
					.count(2)
					.directionalMode(true)
					.delta(vec.getX(), 0.3, vec.getZ())
					.extraRange(1.0, 1.3)
					.spawnAsPlayerActive(player);
			}
		}
	}

	public void volleyHit(Player player, LivingEntity enemy) {

	}
}
