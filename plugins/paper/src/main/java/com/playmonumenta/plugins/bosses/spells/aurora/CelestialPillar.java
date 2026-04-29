package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CelestialPillar extends Spell {
	private static final int PILLAR_HEIGHT = 6;
	private static final int ELITE_SPAWN_TIME = 2 * 20;

	private final Plugin mPlugin;
	private final Location mLocation;
	private final Location mTop;
	private final Location mBlockLocation;
	private final double mRage;
	private final List<BlockDisplay> mBlocks = new ArrayList<>();

	private boolean mDead = false;

	public CelestialPillar(Plugin plugin, Location location, String growableName, int glowColor, double rage) {
		mPlugin = plugin;
		mLocation = location;
		mTop = location.clone().add(0, PILLAR_HEIGHT, 0);
		mBlockLocation = location.toBlockLocation();
		mRage = rage;
		GrowableAPI.grow(growableName, mLocation, 1, 2, false);
		World world = mLocation.getWorld();
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (Block block : BlockUtils.getBlocksInPillar(location, 1, 5)) {
				if (block.isSolid()) {
					BlockDisplay spawn = world.spawn(block.getLocation(), BlockDisplay.class, display -> {
						display.setBlock(block.getBlockData());
						display.setTransformation(new Transformation(
							new Vector3f(-0.0025f),
							new Quaternionf(),
							new Vector3f(1.005f),
							new Quaternionf()
						));
						display.setGlowColorOverride(Color.fromRGB(glowColor));
					});
					mBlocks.add(spawn);
				}
			}
		}, 20);
		Aurora.playersInRange(location).stream()
			.filter(p -> LocationUtils.xzDistance(p.getLocation(), location) <= 1.5)
			.forEach(p -> {
				Location pLoc = p.getLocation();
				Vector dir = LocationUtils.getDirectionTo(pLoc, location);
				p.teleport(pLoc.clone().add(dir.multiply(2)));
			});
	}

	public void glow(int duration) {
		mBlocks.forEach(display -> display.setGlowing(true));
		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBlocks.forEach(display -> display.setGlowing(false));
		}, duration));
	}


	public void summonElite() {
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				World world = mLocation.getWorld();
				double angle = mTicks * 5.0 + mTicks / 20.0;
				Vector offset = new Vector(FastUtils.cosDeg(angle), FastUtils.sinDeg(angle * 2) / 2, FastUtils.sinDeg(angle)).multiply(1.5 - (double) mTicks / ELITE_SPAWN_TIME);

				new PartialParticle(Particle.SOUL_FIRE_FLAME, mTop.clone().add(offset))
					.delta(0.2)
					.spawnAsBoss();

				new PartialParticle(Particle.SOUL_FIRE_FLAME, mTop.clone().subtract(offset))
					.delta(0.2)
					.spawnAsBoss();

				if (mTicks % 10 == 0) {
					world.playSound(mTop, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 4.0f, 0.25f + 0.5f * mTicks / ELITE_SPAWN_TIME);
				}

				mTicks++;
				if (mTicks >= ELITE_SPAWN_TIME) {
					world.playSound(mTop, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 3.5f, 1.8f);
					world.playSound(mTop, Sound.BLOCK_ANVIL_USE, SoundCategory.HOSTILE, 3.5f, 0.5f);

					Entity elite = SpellCelestialPillars.ELITE_POOL.spawn(mTop);
					if (elite instanceof LivingEntity livingElite) {
						Aurora.rageBuff(livingElite, mRage, true);
					}
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	public void destroy() {
		cancel();
		mDead = true;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				for (int y = 0; y <= PILLAR_HEIGHT; y++) {
					mLocation.clone().add(x, y, z).getBlock().setType(Material.AIR);
				}
			}
		}
		mBlocks.forEach(Entity::remove);
		mBlocks.clear();

		new PartialParticle(Particle.EXPLOSION_LARGE, mLocation.clone().add(0, 1, 0))
			.count(9)
			.delta(0.5, 1, 0.5)
			.spawnAsBoss();

		new PartialParticle(Particle.EXPLOSION_NORMAL, mLocation.clone().add(0, 1, 0))
			.count(50)
			.extra(0.25)
			.spawnAsBoss();

		World world = mLocation.getWorld();
		world.playSound(mLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.8f, 0.8f);
		world.playSound(mLocation, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 0.9f, 1.2f);
		world.playSound(mLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.BLOCKS, 2.0f, 1.6f);
	}

	public boolean isPillarBlock(Location blockLocation) {
		return Math.abs(blockLocation.getBlockX() - mBlockLocation.getBlockX()) <= 1
			&& blockLocation.getBlockY() - mBlockLocation.getBlockY() <= 5
			&& Math.abs(blockLocation.getBlockZ() - mBlockLocation.getBlockZ()) <= 1;
	}

	public boolean isPillarY(double y) {
		return Math.abs(y - mBlockLocation.getBlockY()) <= 3;
	}

	public Location getLocation() {
		return mLocation;
	}

	public Location getTopLocation() {
		return mTop;
	}

	@Override
	public void run() {
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public boolean isDead() {
		return mDead;
	}
}
