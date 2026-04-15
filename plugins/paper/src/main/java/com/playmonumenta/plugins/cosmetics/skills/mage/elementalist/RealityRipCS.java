package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.cosmetics.skills.IntruderCS;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RealityRipCS extends ElementalSpiritCS implements IntruderCS {
	public static final String NAME = "Reality Rip";

	@Override
	public Material getDisplayItem() {
		return Material.GRAY_GLAZED_TERRACOTTA;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The elements of nature brought to the very extreme.",
			"Channel a burning rage and rip reality apart."
		);
	}

	@Override
	public void fireSpiritActivate(World world, Player player, Location loc, Location endLoc, Vector dir, double hitbox) {
		new PPCircle(Particle.SQUID_INK, loc.clone().add(0, 0.5, 0), 1)
			.count(30)
			.delta(0.4, 0, 0)
			.rotateDelta(true)
			.directionalMode(true)
			.extra(1)
			.spawnAsPlayerActive(player);

		Location start = loc.clone().add(0, 1, 0).add(dir.clone().multiply(0.66));

		start.setDirection(dir);
		start.setPitch(start.getPitch() - 90);
		Vector up = start.getDirection();
		Vector right = up.clone().crossProduct(dir);
		double distance = loc.distance(endLoc);
		int delay = Math.max(1, (int) (Math.sqrt(distance) * 2));
		double random = FastUtils.randomDoubleInRange(0, 1000);

		new PPParametric(Particle.CRIT, start, (progress, packagedValues) -> {
			double m1 = Math.pow(FastUtils.sin(random + progress * distance), 3) + Math.pow(FastUtils.cos(random + progress * distance * Math.E), 3);
			double m2 = Math.pow(FastUtils.sin(random + progress * distance * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * distance), 3);
			Vector upComponent = up.clone().multiply(m1 * hitbox / 3);
			Vector rightComponent = right.clone().multiply(m2 * hitbox / 2);
			packagedValues.location(start.clone().add(dir.clone().multiply(distance * progress)).add(upComponent).add(rightComponent));
		}).count(delay * 12).delay(delay).extra(0.02).spawnAsEntityActive(player);

		new PPParametric(Particle.SQUID_INK, start, (progress, packagedValues) -> {
			double m1 = Math.pow(FastUtils.sin(random + progress * distance), 3) + Math.pow(FastUtils.cos(random + progress * distance * Math.E), 3);
			double m2 = Math.pow(FastUtils.sin(random + progress * distance * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * distance), 3);
			Vector upComponent = up.clone().multiply(m1 * hitbox / 1.5);
			Vector rightComponent = right.clone().multiply(m2 * hitbox);
			packagedValues.location(start.clone().add(dir.clone().multiply(distance * progress)));
			packagedValues.offset(
				upComponent.getX() + rightComponent.getX(),
				upComponent.getY() + rightComponent.getY(),
				upComponent.getZ() + rightComponent.getZ()
			);
		}).count(delay * 8).delay(delay).directionalMode(true).extra(0.1 * hitbox).spawnAsEntityActive(player);

		new PPParametric(Particle.DUST_COLOR_TRANSITION, start, (progress, packagedValues) -> {
			double m1 = Math.pow(FastUtils.sin(random + progress * distance), 3) - Math.pow(FastUtils.cos(random + progress * distance * Math.E), 3);
			double m2 = Math.pow(FastUtils.sin(random + progress * distance * Math.E), 3) + Math.pow(FastUtils.cos(random + progress * distance), 3);
			Vector upComponent = up.clone().multiply(m1 * hitbox / 3);
			Vector rightComponent = right.clone().multiply(m2 * hitbox / 2);
			packagedValues.location(start.clone().add(dir.clone().multiply(distance * progress)).add(upComponent).add(rightComponent));
		}).count(delay * 4).delay(delay).data(new Particle.DustTransition(Color.GRAY, Color.BLACK, 1.8f)).spawnAsEntityActive(player);

		world.playSound(start, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(start, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(start, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 0.66f);
		world.playSound(start, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(start, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 2.0f, 0.5f);
	}

	@Override
	public void fireSpiritTravel(Player player, Location loc, double hitbox) {
		if (FastUtils.randomDoubleInRange(0, hitbox) > 0.4) {
			return;
		}
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.2f, 1.0f);
	}

	@Override
	public void iceSpiritPulse(Player player, World world, Location loc, double size) {
		for (int i = 0; i < 2; i++) {
			Vector dir = VectorUtils.randomUnitVector();
			Location start = loc.clone().subtract(dir.clone().multiply(size));

			start.setDirection(dir);
			start.setPitch(start.getPitch() - 90);
			Vector up = start.getDirection();
			Vector right = up.clone().crossProduct(dir);
			double distance = size * 2;
			int delay = 4;
			double random = FastUtils.randomDoubleInRange(0, 1000);

			loc.setDirection(up);
			ParticleUtils.drawParticleCircleExplosion(player, loc, 0, 0.1, 0, 0, 90, (float) (size / 1.5), true, 0, Particle.CRIT_MAGIC);

			new PPParametric(Particle.END_ROD, start, (progress, packagedValues) -> {
				double m1 = Math.pow(FastUtils.sin(random + progress * distance), 3) + Math.pow(FastUtils.cos(random + progress * distance * Math.E), 3);
				double m2 = Math.pow(FastUtils.sin(random + progress * distance * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * distance), 3);
				Vector upComponent = up.clone().multiply(m1 / 3);
				Vector rightComponent = right.clone().multiply(m2 / 3);
				packagedValues.location(start.clone().add(dir.clone().multiply(distance * progress)).add(upComponent).add(rightComponent));
				packagedValues.offset(
					upComponent.getX() + rightComponent.getX(),
					upComponent.getY() + rightComponent.getY(),
					upComponent.getZ() + rightComponent.getZ()
				);
			}).count(delay * 12).delay(delay).directionalMode(true).extra(0.01).spawnAsEntityActive(player);

			new PPParametric(Particle.CLOUD, start, (progress, packagedValues) -> {
				double m1 = Math.pow(FastUtils.sin(random + progress * distance), 3) + Math.pow(FastUtils.cos(random + progress * distance * Math.E), 3);
				double m2 = Math.pow(FastUtils.sin(random + progress * distance * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * distance), 3);
				Vector upComponent = up.clone().multiply(m1 / 8);
				Vector rightComponent = right.clone().multiply(m2 / 8);
				packagedValues.location(start.clone().add(dir.clone().multiply(distance * progress)).add(upComponent).add(rightComponent));
				packagedValues.offset(
					upComponent.getX() + rightComponent.getX(),
					upComponent.getY() + rightComponent.getY(),
					upComponent.getZ() + rightComponent.getZ()
				);
			}).count(delay * 4).delay(delay).directionalMode(true).extra(0.5).spawnAsEntityActive(player);

			new PPParametric(Particle.CLOUD, start, (progress, packagedValues) -> {
				double m1 = Math.pow(FastUtils.sin(random + progress * distance), 3) + Math.pow(FastUtils.cos(random + progress * distance * Math.E), 3);
				double m2 = Math.pow(FastUtils.sin(random + progress * distance * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * distance), 3);
				Vector upComponent = up.clone().multiply(m1 / 8);
				Vector rightComponent = right.clone().multiply(m2 / 8);
				packagedValues.location(start.clone().add(dir.clone().multiply(distance * progress)).add(upComponent).add(rightComponent));
				packagedValues.offset(
					upComponent.getX() + rightComponent.getX(),
					upComponent.getY() + rightComponent.getY(),
					upComponent.getZ() + rightComponent.getZ()
				);
			}).count(delay * 4).delay(delay).directionalMode(true).extra(-0.5).spawnAsEntityActive(player);
		}

		world.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(player.getLocation(), Sound.ENTITY_BREEZE_HURT, SoundCategory.PLAYERS, 0.8f, 1.5f);
		world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1.5f, 1.0f);
		world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.1f);
		world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 1.2f, 0.8f);
	}

	@Override
	public AbstractPartialParticle<?> getFirePeriodicParticle(Player player) {
		return new PPPeriodic(Particle.REDSTONE, player.getLocation()).data(new Particle.DustOptions(Color.BLACK, 1.2f)).extra(1);
	}

	@Override
	public AbstractPartialParticle<?> getIcePeriodicParticle(Player player) {
		return new PPPeriodic(Particle.WHITE_SMOKE, player.getLocation()).count(1);
	}
}
