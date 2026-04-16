package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.cosmetics.skills.HexfallCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class VerdantRazorCS extends RendingRazorCS implements HexfallCS {
	public static final String NAME = "Verdant Razor";

	private float mStartingAngle = 0;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Vine slinging's got a different meaning once",
			"you attach a sharp object to the end of it.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_INGOT;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Material getSpinningMaterial() {
		return Material.STONE_HOE;
	}

	@Override
	public String getSpinningName() {
		return "Forest's Reaper";
	}

	@Override
	public double sizeOffset() {
		return -0.5;
	}

	private static final Particle.DustTransition GREEN = new Particle.DustTransition(Color.fromRGB(140, 210, 45), Color.fromRGB(70, 105, 27), 1.2f);

	@Override
	public void razorCast(Player player) {
		Location loc = player.getLocation();
		World world = loc.getWorld();

		mStartingAngle = player.getLocation().getYaw();
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1f, 0.6f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 0.8f);
	}

	@Override
	public void tick(Player player, World world, Location loc, double bladeRadius, int degrees) {
		world.playSound(loc, "minecraft:entity.breeze.charge", SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.BLOCK_AZALEA_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);

		new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc)
			.count(10)
			.delta(0.25)
			.extra(0.1)
			.data(GREEN)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.ELECTRIC_SPARK, loc)
			.count(3)
			.delta(0.25)
			.extra(0.1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.FALLING_DUST, loc, 1)
			.delta(0.25)
			.extra(0)
			.data(Bukkit.createBlockData(Material.GREEN_TERRACOTTA))
			.spawnAsPlayerActive(player);

		float pitch = FastUtils.randomFloatInRange(8, 35) * (FastUtils.RANDOM.nextBoolean() ? -1.0f : 0.5f);
		loc.setPitch(pitch);
		loc.setYaw(mStartingAngle - degrees + 15);

		if (degrees % 60 == 0) {
			final int rings = (int) Math.round(bladeRadius / 0.8);

			ParticleUtils.drawHalfArc(loc, bladeRadius / 2, 180, -30, 120, rings, 0.4, false, 90,
				(pLoc, ring, angleProgress) -> {
					new PartialParticle(Particle.REDSTONE, pLoc, 1)
						.count(1)
						.data(new Particle.DustOptions(ParticleUtils.getTransition(
							Color.fromRGB(140, 210, 45),
							Color.fromRGB(70, 105, 27),
							Math.min(angleProgress + 0.5 * ring / rings, 1)),
							0.7f + 0.8f * (float) angleProgress * ring / rings))
						.spawnAsPlayerActive(player);
				});
		}
	}

	@Override
	public void razorHit(final Player player, final Location location) {
		final World world = player.getWorld();

		new PartialParticle(Particle.BLOCK_CRACK, location, 150)
			.delta(0.4)
			.extra(0)
			.data(Bukkit.createBlockData(Material.AZALEA_LEAVES))
			.spawnAsPlayerActive(player);

		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(location, "minecraft:entity.breeze.deflect", SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(location, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(location, "minecraft:entity.armadillo.hurt_reduced", SoundCategory.PLAYERS, 2f, 0.4f);
	}

	@Override
	public void razorPierce(Player player, Location location) {
		final World world = location.getWorld();

		new PartialParticle(Particle.DAMAGE_INDICATOR, location.clone().add(0, 0.5, 0))
			.count(5)
			.delta(0.2)
			.extra(0.1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.BLOCK_CRACK, location, 30)
			.delta(0.3)
			.extra(0)
			.data(Bukkit.createBlockData(Material.AZALEA_LEAVES))
			.spawnAsPlayerActive(player);

		world.playSound(location, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.8f, 1.5f);
		world.playSound(location, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(location, Sound.ENTITY_BEE_STING, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 2f);
	}

	@Override
	public void razorReturned(final Location loc, int startingTick) {
		World world = loc.getWorld();

		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1f, 0.8f);
		world.playSound(loc, Sound.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1f, 0.4f);
		world.playSound(loc, "block.vault.insert_item", SoundCategory.PLAYERS, 1.8f, 1.1f);
	}
}
