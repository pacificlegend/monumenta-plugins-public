package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPFlower;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TwistedRitualCS extends BladeDanceCS {
	public static final String NAME = "Twisted Ritual";
	private static final float PI_2 = (float) (2 * Math.PI);

	@Override
	public Material getDisplayItem() {
		return Material.FIRE_CORAL_FAN;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Blood leaks through the floor.",
			"The onlookers notice a strange symbol forming,",
			"but it is already too late."
		);
	}

	@Override
	public void danceStart(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 1.7f, 0.1f);
		world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 1.7f, 1.1f);
		world.playSound(loc, Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 1.6f, 15);
	}

	@Override
	public void danceTick(Player player, World world, Location centre, int tick, int duration, double radius) {
		float progress = (float) tick / duration;
		if (tick % 2 == 0) {
			world.playSound(LocationUtils.randomLocationInCircle(centre, radius / 2), Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 0.5f, progress);
		}

		if (tick % 5 == 1) {
			new PPSpiral(Particle.DUST_COLOR_TRANSITION, centre.clone().add(0, 0.2, 0), radius * (1 - progress))
				.countPerBlockPerCurve(6)
				.data(new Particle.DustTransition(Color.RED, Color.BLACK, 1.2f + progress / 2))
				.curves(8)
				.curveAngle(180)
				.ticks(4)
				.reversed(true)
				.spawnAsPlayerActive(player);
		}
		for (double j = 0; j < 360; j += 72) {
			double angle = Math.toRadians(j);
			new PartialParticle(Particle.SMOKE_LARGE, centre.clone().add(FastUtils.cos(tick / 8.0 + angle) * radius, 0.2, FastUtils.sin(tick / 8.0 + angle) * radius))
				.delta(0, 1, 0)
				.directionalMode(true)
				.extra(0.1)
				.spawnAsPlayerActive(player);
		}
	}

	@Override
	public void danceEnd(Player player, World world, Location centre, double radius) {
		world.playSound(centre, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.3f, 0.1f);
		world.playSound(centre, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5f, 0.1f);
		world.playSound(centre, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 1.7f, 0.4f);
		world.playSound(centre, Sound.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 0.9f, 0.5f);
		world.playSound(centre, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.5f, 0.5f);

		new PPFlower(Particle.REDSTONE, centre.clone().add(0, 0.2, 0), radius)
			.count(450)
			.petals(5)
			.sharp(true)
			.transitionColors(Color.RED, Color.MAROON, 1.9f)
			.spawnAsPlayerActive(player);
	}

	@Override
	public void danceHit(Player player, World world, LivingEntity mob, Location mobLoc) {
		for (int i = 0; i < 3; i++) {
			slash(world, mobLoc, player);
		}
		ParticleUtils.launchOrb(VectorUtils.randomUnitVector().setY(1.0), mobLoc, player, player, 300, player.getLocation(), new Particle.DustOptions(Color.MAROON, 1.7f), livingEntity ->
			world.playSound(livingEntity, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 0.6f, 0.1f)
		);
		world.playSound(mobLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(mobLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(mobLoc, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.7f, 0.1f);
	}

	private void slash(World world, Location location, Player player) {
		new PartialParticle(Particle.CRIT, location).count(15).extra(0.8).spawnAsPlayerActive(player);

		Quaternionf leftRotation = new Quaternionf().rotateYXZ(FastUtils.randomFloatInRange(0, PI_2), FastUtils.randomFloatInRange(0, PI_2), FastUtils.randomFloatInRange(0, PI_2));
		ItemDisplay display = world.spawn(location, ItemDisplay.class, itemDisplay -> {
			itemDisplay.setItemStack(new ItemStack(Material.REDSTONE_BLOCK));
			itemDisplay.setTransformation(new Transformation(new Vector3f(), leftRotation, new Vector3f(), new Quaternionf()));
			itemDisplay.setInterpolationDuration(2);
			EntityUtils.setRemoveEntityOnUnload(itemDisplay);
		});
		Plugin plugin = Plugin.getInstance();
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			display.setInterpolationDelay(-1);
			display.setTransformation(new Transformation(
				new Vector3f(),
				leftRotation,
				new Vector3f(3.0f, 0.1f, 0.1f),
				new Quaternionf()
			));
		}, 1);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			display.setInterpolationDelay(-1);
			display.setInterpolationDuration(5);
			display.setTransformation(new Transformation(
				new Vector3f(),
				leftRotation,
				new Vector3f(5.5f, 0.0f, 0.0f),
				new Quaternionf()
			));
		}, 3);
		Bukkit.getScheduler().runTaskLater(plugin, display::remove, 10);
	}

}
