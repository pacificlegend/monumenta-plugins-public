package com.playmonumenta.plugins.cosmetics.skills.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SoulBurstCS extends RampageCS {
	public static final String NAME = "Soul Burst";

	@Override
	public Material getDisplayItem() {
		return Material.LIGHT_BLUE_CARPET;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"You feel your heart twist and splinter.",
			"Such strength comes at a great cost."
		);
	}

	@Override
	public void onHitMob(Player player, LivingEntity mob) {
		Location location = LocationUtils.getHalfHeightLocation(mob);
		new PartialParticle(Particle.CRIT_MAGIC, location).count(24).extra(0.1).spawnAsPlayerActive(player);
	}

	@Override
	public void onStackGain(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.35f, 0.8f);

		Location pLoc = player.getLocation();
		pLoc.setPitch(pLoc.getPitch() - 90);
		Vector direction = pLoc.getDirection();
		loc.setDirection(direction);
		ParticleUtils.drawParticleCircleExplosion(player, loc.clone().add(direction), 0, 1, 0, 0, 75, 1.75f, true, 0, -0.4, Particle.CRIT_MAGIC);

		Vector up = new Vector(0, 1, 0);
		for (int i = 0; i < 3; i++) {
			Vector dir = VectorUtils.randomHorizontalUnitVector();
			dir.setY(Math.abs(dir.getY()));
			Vector right = up.clone().crossProduct(dir);
			double random = FastUtils.randomDoubleInRange(0, 1000);

			new PPParametric(Particle.SOUL_FIRE_FLAME, loc, (progress, packagedValues) -> {
				double m1 = Math.pow(FastUtils.sin(random + progress * 4 * Math.E), 3) + Math.pow(FastUtils.cos(random + progress * 4), 3);
				double m2 = Math.pow(FastUtils.sin(random + progress * 4 * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * 4), 3);
				Vector upComponent = up.clone().multiply(m1 * 2 * (1 - progress));
				Vector rightComponent = right.clone().multiply(m2 * 2 * (1 - progress));
				packagedValues.location(loc.clone().add(dir.clone().multiply(3.5 * progress)));
				packagedValues.offset(
					upComponent.getX() + rightComponent.getX(),
					upComponent.getY() + rightComponent.getY(),
					upComponent.getZ() + rightComponent.getZ()
				);
			}).count(20).delay(10).extra(0.05).directionalMode(true).spawnAsEntityActive(player);
		}
	}

	@Override
	public void onCast(Player player, Location loc, World world, double radius) {
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 0.6f, 0.7f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.5f, 1.2f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.7f, 0.6f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.9f, 0.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.5f, 0.1f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.5f, 0.7f);

		new BukkitRunnable() {
			double mRadius = 0;

			@Override
			public void run() {
				mRadius++;

				new PPCircle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.1, 0), mRadius)
					.countPerMeter(3)
					.delta(1, 0, 0)
					.directionalMode(true)
					.rotateDelta(true)
					.extraRange(0, radius / 10)
					.spawnAsPlayerActive(player);

				new PPCircle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.15, 0), mRadius)
					.countPerMeter(3)
					.delta(0, 1, 0)
					.directionalMode(true)
					.extraRange(0, radius / 10)
					.spawnAsBoss();

				new PPCircle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.15, 0), mRadius)
					.countPerMeter(1)
					.delta(0, 1, 0)
					.directionalMode(true)
					.extraRange(0, radius / 18)
					.spawnAsBoss();

				if (mRadius >= radius) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		Vector up = new Vector(0, 1, 0);
		Location heartLoc = LocationUtils.getHalfHeightLocation(player);

		new PartialParticle(Particle.FLASH, heartLoc).minimumCount(1).spawnAsPlayerActive(player);

		for (int i = 0; i < 10; i++) {
			double distance = radius * 1.25;
			int delay = (int) (2 * distance);
			Vector dir = VectorUtils.randomUnitVector();
			Vector right = new Vector(0, 1, 0).crossProduct(dir);
			if (right.lengthSquared() <= 0) {
				right.add(new Vector(1, 0, 0).crossProduct(dir));
			}
			double random = FastUtils.randomDoubleInRange(0, 1000);

			new PPParametric(Particle.DUST_COLOR_TRANSITION, heartLoc, (progress, packagedValues) -> {
				double m1 = Math.pow(FastUtils.sin(random + progress * 4 * Math.E), 3) + Math.pow(FastUtils.cos(random + progress * 4), 3);
				double m2 = Math.pow(FastUtils.sin(random + progress * 4 * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * 4), 3);
				double multiplier = Math.sqrt(1 - progress);
				Vector upComponent = up.clone().multiply(m1 * multiplier);
				Vector rightComponent = right.clone().multiply(m2 * multiplier);
				packagedValues.location(heartLoc.clone().add(dir.clone().multiply(distance * progress)).add(upComponent).add(rightComponent));
				Color startColor = ParticleUtils.getTransition(Color.RED, Color.MAROON, progress);
				packagedValues.data(new Particle.DustTransition(startColor, Color.fromRGB(0x6b0000), 1.7f - (float) progress));
			}).count(delay * 14).delay(delay).data(new Particle.DustTransition(Color.RED, Color.RED, 1)).spawnAsEntityActive(player);
		}
	}

	@Override
	public void tick(Player player, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.SOUL, LocationUtils.getHalfHeightLocation(player))
			.count(6)
			.delta(0.2, 0.6, 0.2)
			.extra(0.1)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void loseEffect(Player player) {
		Location playerLocation = player.getLocation().add(0, 0.1, 0);
		AbilityUtils.playPassiveAbilitySound(playerLocation, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 0.5f);
		AbilityUtils.playPassiveAbilitySound(playerLocation, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.4f, 1.1f);


		new PPCircle(Particle.SMOKE_NORMAL, playerLocation, 0.5)
			.countPerMeter(8)
			.delta(1, 0, 0)
			.rotateDelta(true)
			.directionalMode(true)
			.extra(0.3)
			.spawnAsPlayerActive(player);
	}
}
