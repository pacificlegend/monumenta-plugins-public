package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
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
import org.jetbrains.annotations.Nullable;

public class AmalgamatedNightmaresCS extends UnstableAmalgamCS {
	public static final String NAME = "Amalgamated Nightmares";

	@Override
	public Material getDisplayItem() {
		return Material.RED_CANDLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A red candle sits in the dark, a menacing silhouette inches forward.",
			"Awakening in a cold sweat, the restless nightmares repeat over and over."
		);
	}

	@Override
	public String summonedMobName() {
		return "NightmareCaller";
	}

	@Override
	public void periodicEffects(Player caster, Location loc, double radius, int ticks, int duration) {
		new PPCircle(Particle.SMALL_FLAME, loc.clone().add(0, 0.15, 0), radius)
			.countPerMeter(0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1, 0, 0)
			.extra(0.1)
			.spawnAsPlayerActive(caster);
		if (ticks % 10 == 0) {
			loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.6f, 1.0f);
			new PPCircle(Particle.SMALL_FLAME, loc.clone().add(0, 0.15, 0), radius)
				.countPerMeter(3)
				.delta(0, 1, 0)
				.directionalMode(true)
				.extra(0.1)
				.spawnAsPlayerActive(caster);
		}
	}

	@Override
	public void explodeEffects(Player caster, Location loc, double radius) {
		new PPLightning(Particle.DUST_COLOR_TRANSITION, loc, 16, 2, 0)
			.count(2)
			.data(new Particle.DustTransition(Color.BLACK, Color.GRAY, 2.4f))
			.hopsPerBlock(1)
			.duration(2)
			.spawnAsPlayerActive(caster);

		new PPCircle(Particle.SQUID_INK, loc.clone().add(0, 0.2, 0), 0.5)
			.count(60)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1.4, 0, 0)
			.extra(radius / 10)
			.spawnAsPlayerActive(caster);

		new PPCircle(Particle.FLAME, loc.clone().add(0, 0.1, 0), 0.5)
			.count(60)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1, 0, 0)
			.extra(radius / 10)
			.spawnAsPlayerActive(caster);

		new PartialParticle(Particle.FLASH, loc.clone().add(0, 0.3, 0)).minimumCount(1).spawnAsPlayerActive(caster);

		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_HURT, SoundCategory.PLAYERS, 0.7f, 0.6f);
	}

	@Override
	public void unstableMobEffects(Player caster, LivingEntity mob) {
		new PPPillar(Particle.REDSTONE, mob.getLocation(), mob.getHeight())
			.count(12)
			.data(new Particle.DustOptions(Color.MAROON, 0.9f))
			.delta(mob.getWidth() / 2)
			.spawnAsPlayerActive(caster);
	}

	@Override
	public void unstableMobDeath(Player caster, LivingEntity mob) {
		new PartialParticle(Particle.SOUL_FIRE_FLAME, LocationUtils.getHalfHeightLocation(mob))
			.count(25)
			.extra(0.1)
			.spawnAsPlayerActive(caster);
	}
}
