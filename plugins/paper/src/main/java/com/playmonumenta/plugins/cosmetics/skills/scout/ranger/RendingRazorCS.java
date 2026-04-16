package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class RendingRazorCS implements CosmeticSkill {
	private float mStartingAngle = 0;
	private final HashMap<Integer, ItemDisplay> mRazorDisplayMap = new HashMap<>();

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.RENDING_RAZOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHEARS;
	}

	public Material getSpinningMaterial() {
		return Material.IRON_HOE;
	}

	public String getSpinningName() {
		return "Rending Razor";
	}

	public double sizeOffset() {
		return 0.25;
	}

	public void razorCast(Player player) {
		final World world = player.getWorld();
		final Location playerLoc = player.getLocation();
		mStartingAngle = player.getLocation().getYaw();
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(playerLoc, Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(playerLoc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1, 2);
		world.playSound(playerLoc, "minecraft:entity.breeze.charge", SoundCategory.PLAYERS, 1, 1.4f);
	}

	public void tick(Player player, World world, Location loc, double bladeRadius, int degrees) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.5f, 0.9f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.3f, 1.7f);
		world.playSound(loc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 0.2f, 1.7f);

		float pitch = FastUtils.randomFloatInRange(8, 35) * (FastUtils.RANDOM.nextBoolean() ? -1.0f : 0.5f);
		loc.setPitch(pitch);
		loc.setYaw(mStartingAngle - degrees + 15);

		if (degrees % 120 == 0) {
			final int rings = (int) Math.round(bladeRadius);
			for (int i = 0; i < 2; i++) {
				if (i == 1) {
					loc.setYaw(loc.getYaw() + 180);
				}

				ParticleUtils.drawHalfArc(loc, bladeRadius / 2, 180, -50, 100, rings, 0.5, false, 90,
					(pLoc, ring, angleProgress) -> {
						new PartialParticle(Particle.DUST_COLOR_TRANSITION, pLoc, 1)
							.count(1)
							.data(new Particle.DustTransition(ParticleUtils.getTransition(
								Color.fromRGB(150, 255, 255),
								Color.WHITE,
								Math.min(angleProgress + 0.5 * ring / rings, 1)
							), Color.fromRGB(0x5796bd), 0.7f + 0.6f * (float) angleProgress * ring / rings))
							.spawnAsPlayerActive(player);
					});
			}
		}
	}

	public void razorHit(final Player player, final Location location) {
		final World world = player.getWorld();
		new PartialParticle(Particle.CRIT, location).count(10).delta(0.2).extra(0.1).spawnAsPlayerActive(player);
		world.playSound(location, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1, 0.8f);
		world.playSound(location, Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.5f, 0.8f);
		world.playSound(location, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1, 2);
		world.playSound(location, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 1, 2);
	}

	public void razorPierce(Player player, Location location) {
		location.getWorld().playSound(location, Sound.ENTITY_BREEZE_DEATH, 0.6f, 1.6f);
		location.getWorld().playSound(location, Sound.ITEM_TRIDENT_RETURN, 0.6f, 0.7f);
	}

	public void razorReturned(final Location playerLoc, int startingTick) {
		final World world = playerLoc.getWorld();
		world.playSound(playerLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.4f, 1.6f);
		world.playSound(playerLoc, "minecraft:entity.breeze.charge", 1, 1.5f);
		world.playSound(playerLoc, Sound.ENTITY_WITCH_THROW, 1, 0.7f);
	}

	public void onDeath() {
		// Just in case the player dies / unloads, hopefully proof against memory leaks
		for (ItemDisplay display : mRazorDisplayMap.values()) {
			if (display != null) {
				display.remove();
			}
		}
		mRazorDisplayMap.clear();
	}

	public void addItemDisplay(Player player, double radius) {
		int currentTick = Bukkit.getCurrentTick();
		Location loc = player.getLocation();

		if (mRazorDisplayMap.get(currentTick) != null) {
			mRazorDisplayMap.get(currentTick).remove();
		}

		float size = (float) (radius + sizeOffset());

		mRazorDisplayMap.put(currentTick,
			loc.getWorld().spawn(loc, ItemDisplay.class));
		EntityUtils.setRemoveEntityOnUnload(mRazorDisplayMap.get(currentTick));
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
			() -> {
				if (mRazorDisplayMap.get(currentTick) != null) {
					mRazorDisplayMap.get(currentTick).remove();
				}
				mRazorDisplayMap.remove(currentTick);
			}, Constants.TICKS_PER_MINUTE);
		mRazorDisplayMap.get(currentTick).setItemStack(DisplayEntityUtils.generateRPItem(getSpinningMaterial(), getSpinningName()));
		mRazorDisplayMap.get(currentTick).setTransformation(
			new Transformation(
				new Vector3f(),
				new AxisAngle4f(),
				new Vector3f(size, size, size / 2),
				new AxisAngle4f()
			));
		mRazorDisplayMap.get(currentTick).setTeleportDuration(2);
		mRazorDisplayMap.get(currentTick).setInterpolationDelay(0);
	}

	// Would be nice if pitch adjusted itself
	public void spinDisplay(Location location, int startingTick) {
		ItemDisplay display = mRazorDisplayMap.get(startingTick);
		if (display != null) {
			Location loc = location.clone();
			loc.setYaw(80 * (startingTick - Bukkit.getCurrentTick()));
			loc.setPitch(90);
			display.teleport(loc);
		}
	}

	public void removeDisplay(int startingTick) {
		ItemDisplay display = mRazorDisplayMap.remove(startingTick);
		if (display != null) {
			display.remove();
		}
	}

}
