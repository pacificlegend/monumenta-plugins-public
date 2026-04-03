package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class VolcanicDemiseFinisher implements EliteFinisher {
	public static final String NAME = "Volcanic Demise";
	private static final int DURATION = Constants.TICKS_PER_SECOND;
	private static final int GROUND_CHANGE_LINGER_DURATION = 3 * Constants.TICKS_PER_SECOND;

	private static final List<Material> MATERIALS = List.of(Material.SMOOTH_RED_SANDSTONE, Material.NETHERRACK, Material.MAGMA_BLOCK);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		new BukkitRunnable() {
			double mTimeLeft = DURATION;
			static final int TICKS_PER_EMANATING_CIRCLE = 5;
			final Location mLoc = loc.clone();
			final World mWorld = loc.getWorld();

			final List<BlockDisplay> mDemiseDisplays = new ArrayList<>();

			@Override
			public void run() {
				mTimeLeft -= 1;

				if (mTimeLeft >= 0) {
					// Particles: Emanating Circles
					if (mTimeLeft % TICKS_PER_EMANATING_CIRCLE == 0) {
						new PPCircle(Particle.FLAME, mLoc.clone().add(0, 0.2, 0), 2)
							// RotateDelta originates from positive X
							.delta(1, 0, 0).rotateDelta(true)
							// 1 particle per 2 degrees; 90 particles per pi radians.
							// 1 radian per radius meters circumference
							// 90/(pi * radius) particles per meter
							.countPerMeter(90.0 / (Math.PI * 2)).extra(0.2).directionalMode(true).distanceFalloff(15)
							.spawnAsPlayerActive(p);

						new PPCircle(Particle.REDSTONE, mLoc.clone().add(0, 0.2, 0), 2)
							// 1 particle per 6 degrees; 30 particles per pi radians.
							// 1 radian per radius meters circumference
							// 30/(pi * radius) particles per meter
							.countPerMeter(30.0 / (Math.PI * 2)).extra(0)
							.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1)).distanceFalloff(15)
							.spawnAsPlayerActive(p);
					}
					new PartialParticle(Particle.LAVA, mLoc, 3, 2.5, 0, 2.5, 0.05).distanceFalloff(20)
						.spawnAsPlayerActive(p);

					// Particles: Meteor Trail
					Location meteorTrailLoc = mLoc.clone().add(0, mTimeLeft, 0);
					new PartialParticle(Particle.FLAME, meteorTrailLoc, 10, 0.2f, 0.2f, 0.2f, 0.1)
						.distanceFalloff(40).spawnAsPlayerActive(p);
					new PartialParticle(Particle.SMOKE_LARGE, meteorTrailLoc, 5, 0, 0, 0, 0.05)
						.distanceFalloff(40).spawnAsPlayerActive(p);
					mWorld.playSound(meteorTrailLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 1);
				}

				// Impact
				if (mTimeLeft == 0) {
					// Particles: Impact
					new PartialParticle(Particle.FLAME, mLoc, 50, 0, 0, 0, 0.175)
						.distanceFalloff(20).spawnAsPlayerActive(p);
					new PartialParticle(Particle.SMOKE_LARGE, mLoc, 10, 0, 0, 0, 0.25)
						.distanceFalloff(20).spawnAsPlayerActive(p);
					mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0.9f);


					// Displays: Convert some blocks visually (emulates real demise)
					for (Display display : mDemiseDisplays) {
						display.remove();
					}
					mDemiseDisplays.clear();
					for (Block block : LocationUtils.getNearbyBlocks(mLoc.getBlock(), 4)) {
						if (DepthsUtils.canConvertToSnow(block) &&
							block.getRelative(BlockFace.UP).isEmpty() &&
							!BlockUtils.isNonEmptyContainer(block) &&
							FastUtils.RANDOM.nextInt(8) < 1) {
							BlockDisplay blockDisplay = mWorld.spawn(block.getLocation().add(-0.01, -1.01, -0.01), BlockDisplay.class);
							blockDisplay.setBlock(MATERIALS.get(FastUtils.randomIntInRange(0, MATERIALS.size() - 1)).createBlockData());
							blockDisplay.setBrightness(new Display.Brightness(mLoc.getBlock().getLightFromBlocks(), mLoc.getBlock().getLightFromSky()));
							blockDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.02f, 1.02f, 1.02f), new Quaternionf()));
							EntityUtils.setRemoveEntityOnUnload(blockDisplay);
							mDemiseDisplays.add(blockDisplay);
						}
					}
				}

				// Clean up block displays
				if (mTimeLeft <= -GROUND_CHANGE_LINGER_DURATION) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				for (Display display : mDemiseDisplays) {
					display.remove();
				}
				mDemiseDisplays.clear();

				super.cancel();
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.MAGMA_BLOCK;
	}
}
