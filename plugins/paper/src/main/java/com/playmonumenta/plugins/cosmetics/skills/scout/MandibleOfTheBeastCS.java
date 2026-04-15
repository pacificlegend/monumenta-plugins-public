package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.scout.SteelTrap;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MandibleOfTheBeastCS extends SteelTrapCS {
	public static final String NAME = "Mandible of the Beast";

	@Override
	public Material getDisplayItem() {
		return Material.QUARTZ;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The earth hungers for flesh.",
			"In time, it's desire will be met."
		);
	}

	@Override
	public Material getThrownItem() {
		return Material.CRIMSON_BUTTON;
	}

	@Override
	public SteelTrap.Trap.Displays getBlockDisplayTrap(World world, Location loc) {
		BlockDisplay tnt = world.spawn(loc, BlockDisplay.class, itemDisplay -> {
			itemDisplay.setBlock(Material.RED_CANDLE.createBlockData());
			itemDisplay.setTransformation(new Transformation(
				new Vector3f(-0.5f, -0.2f, -0.5f),
				new Quaternionf(),
				new Vector3f(1),
				new Quaternionf()
			));
			itemDisplay.setInterpolationDuration(4);
		});

		ItemDisplay center = world.spawn(loc, ItemDisplay.class, itemDisplay -> {
			itemDisplay.setItemStack(new ItemStack(Material.CRIMSON_NYLIUM));
			itemDisplay.setTransformation(new Transformation(
				new Vector3f(),
				new Quaternionf(),
				new Vector3f(),
				new Quaternionf()
			));
			itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
			itemDisplay.setInterpolationDuration(4);
		});
		return SteelTrap.Trap.Displays.of(center, tnt);
	}

	@Override
	public SteelTrap.Trap.Displays getUnderwaterBlockDisplayTrap(World world, Location loc) {
		return getBlockDisplayTrap(world, loc);
	}

	@Override
	public void trapThrow(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.PLAYERS, 0.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_SLIME_DEATH, SoundCategory.PLAYERS, 0.9f, 0.5f);
		world.playSound(loc, Sound.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.PLAYERS, 0.7f, 0.8f);

		Location pLoc = player.getEyeLocation();
		pLoc.add(pLoc.getDirection());
		pLoc.setPitch(pLoc.getPitch() + 90);
		ParticleUtils.drawParticleCircleExplosion(player, pLoc, 0, 1, 0, 0, 40, 1.6f, false, 0, -0.25, Particle.CRIT_MAGIC);
	}

	@Override
	public void trapMidairTick(World world, Player player, Location loc) {
		new PartialParticle(Particle.REDSTONE, loc)
			.count(10)
			.data(new Particle.DustOptions(Color.MAROON, 1.6f))
			.delta(0.3)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void trapLand(World world, Player player, Location loc, SteelTrap.Trap.Displays trap, double radius) {
		var center = trap.getCenter();
		Transformation transformationCenter = center.getTransformation();
		center.setInterpolationDelay(-1);
		center.setTransformation(new Transformation(
			new Vector3f(0, -0.2f, 0),
			transformationCenter.getLeftRotation(),
			new Vector3f((float) radius * 1.33f, 0.1f, (float) radius * 1.33f),
			transformationCenter.getRightRotation()
		));
		trap.getTnt().ifPresent(tnt -> {
			tnt.setInterpolationDelay(-1);
			tnt.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(), new Quaternionf()));
		});
		world.playSound(loc, Sound.ENTITY_WARDEN_DIG, SoundCategory.PLAYERS, 0.9f, 1.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.5f, 0.6f);
		world.playSound(loc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 0.5f, 0.8f);
	}

	@Override
	public void trapPrimingTick(World world, Player player, SteelTrap.Trap.Displays trap, Location loc, int ticks, int maxTicks, double triggerRadius, double explosionRadius) {
		double smallerRadius = triggerRadius - 0.5;
		if (ticks % (maxTicks / 8) == 0) {
			new PPCircle(Particle.REDSTONE, loc.clone().subtract(0, 0.1, 0), smallerRadius)
				.countPerMeter(3)
				.delta(triggerRadius / 10, 0.1, triggerRadius / 10)
				.data(new Particle.DustOptions(Color.MAROON, 1.7f))
				.spawnAsPlayerActive(player);
		}
		for (int deg = 0; deg < 360 * ticks / maxTicks; deg += 72) {
			Vector direction = new Vector(FastUtils.cosDeg(deg), 0, FastUtils.sinDeg(deg));
			Location fangCenter = loc.clone().add(direction.clone().multiply(smallerRadius));
			fangCenter.setDirection(new Vector(0, 2, 0).subtract(direction));
			drawSpike(fangCenter, 0.4 * triggerRadius, 0.3 * triggerRadius, 3, 0.3, player);
		}
	}

	@Override
	public void trapPrimed(World world, Player player, Location loc, double triggerRadius, double explosionRadius) {
		world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.2f, 1.0f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(loc, Sound.ENTITY_WOLF_GROWL, SoundCategory.PLAYERS, 0.8f, 0.8f, 180);

		new PPCircle(Particle.SQUID_INK, loc, triggerRadius)
			.count(48)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.1, 0.01, 0)
			.extra(explosionRadius - triggerRadius)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void trapPrimedTick(World world, Player player, Location loc, double triggerRadius, int ticks, boolean isEnhanced) {
		double smallerRadius = triggerRadius - 0.5;
		for (int deg = 0; deg < 360; deg += 72) {
			Vector direction = new Vector(FastUtils.cosDeg(deg), 0, FastUtils.sinDeg(deg));
			Location fangCenter = loc.clone().add(direction.clone().multiply(smallerRadius));
			fangCenter.setDirection(new Vector(0, 2, 0).subtract(direction));
			drawSpike(fangCenter, 0.4 * triggerRadius, 0.3 * triggerRadius, 3, 0.3, player);
		}
		new PPCircle(Particle.REDSTONE, loc.clone().subtract(0, 0.1, 0), smallerRadius)
			.countPerMeter(1)
			.delta(triggerRadius / 10, 0.1, triggerRadius / 10)
			.data(new Particle.DustOptions(Color.fromRGB(0x6b0000), 2.1f))
			.spawnAsPlayerActive(player);
	}

	@Override
	public void trapExplode(World world, Player player, Location loc, double triggerRadius, double explosionRadius) {
		world.playSound(loc, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 1.3f, 0.8f);
		world.playSound(loc, Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 1.3f, 1.0f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.5f, 0.9f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.5f, 0.6f);
		world.playSound(loc, Sound.ENTITY_SLIME_HURT, SoundCategory.PLAYERS, 1.0f, 0.7f);

		new PPCircle(Particle.CRIMSON_SPORE, loc, explosionRadius)
			.ringMode(false)
			.countPerMeter(12)
			.delta(0.3, 0.15, 0.3)
			.extraRange(0, 0.5)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.CLOUD, loc, 0.5)
			.count(48)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.075, 0, 0)
			.extraRange(triggerRadius, explosionRadius)
			.spawnAsPlayerActive(player);

		for (int deg = 0; deg < 360; deg += 72) {
			Vector direction = new Vector(FastUtils.cosDeg(deg), 0, FastUtils.sinDeg(deg));
			Location fangCenter = loc.clone().add(direction.clone().multiply(triggerRadius * triggerRadius / 3));
			fangCenter.setDirection(new Vector(0, 1, 0).subtract(direction));
			drawSpike(fangCenter, 1.5, 1, 5, 5, player);
		}
	}

	@Override
	public void trapDespawn(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_BURP, 1.5f, 0.8f);
		world.playSound(loc, Sound.BLOCK_HONEY_BLOCK_BREAK, 1.0f, 0.6f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc)
			.count(60)
			.extraRange(0.1, 0.3)
			.spawnAsPlayerActive(player);
	}

	private void drawSpike(Location startLoc, double length, double radius, double spikeTicks, double contPerMeter, Player player) {
		new BukkitRunnable() {
			int mTicks = 0;
			final Vector mDirection = startLoc.getDirection().normalize();

			@Override
			public void run() {
				mTicks++;
				double maxProgress = length * mTicks / spikeTicks;
				for (double progress = 0; progress <= maxProgress; progress += 0.3) {
					double normalisedEffectiveProgress = (maxProgress - progress) / length;
					Location spikeLoc = startLoc.clone().add(mDirection.clone().multiply(progress));
					new PPCircle(Particle.REDSTONE, spikeLoc, radius * Math.pow(normalisedEffectiveProgress, 1.5))
						.countPerMeter(contPerMeter)
						.data(new Particle.DustOptions(
							ParticleUtils.getTransition(
								Color.fromRGB(0xffdfd5),
								Color.fromRGB(0x6b0000),
								normalisedEffectiveProgress
							), (float) (0.9f + normalisedEffectiveProgress / 2)))
						.delta(0, -10, 0)
						.directionalMode(true)
						.extra(1)
						.spawnAsPlayerActive(player);
				}

				if (mTicks >= spikeTicks) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
