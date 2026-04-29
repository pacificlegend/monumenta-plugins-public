package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SpellAuroraBlockPlacer extends Spell {
	public static final int DURATION = 30;

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final SpellCelestialPillars mPillars;

	private final double mBottomY;

	public SpellAuroraBlockPlacer(LivingEntity boss, Location center, SpellCelestialPillars pillars) {
		mBoss = boss;
		mBottomY = center.getY() - 1;

		mCenter = center;
		mPillars = pillars;
	}

	// This code sucks just know it works to give slabs between edges
	private Material getArenaMaterial(Location loc) {
		int blockY = loc.getBlockY();
		double distance = LocationUtils.xzDistance(loc, mCenter);
		double topBlockY = mCenter.getY() - 1;

		if (distance >= Aurora.ARENA_SEMI_OUTER_RADIUS + 0.5 && blockY <= topBlockY + 2) {
			return Material.WHITE_STAINED_GLASS;
		}
		if (distance >= Aurora.ARENA_INNER_RADIUS + 0.5) {
			if (blockY <= topBlockY + 1) {
				return Material.WHITE_STAINED_GLASS;
			}
			if (blockY == topBlockY + 2 && distance >= Aurora.ARENA_SEMI_OUTER_RADIUS) {
				return Material.DIORITE_SLAB;
			}
		}
		if (blockY == topBlockY + 1 && distance >= Aurora.ARENA_INNER_RADIUS) {
			return Material.DIORITE_SLAB;
		}
		if (blockY <= topBlockY) {
			return Material.WHITE_STAINED_GLASS;
		}

		return Material.AIR;
	}


	@Override
	public void run() {
		for (LivingEntity entity : EntityUtils.getNearbyMobs(mCenter, Aurora.DETECTION_RANGE)) {
			if (EntityUtils.isBoss(entity) || EntityUtils.isElite(entity)) {
				ascendEntity(entity);
			}
		}

		tryBlockBreak();
	}

	private void tryBlockBreak() {
		final Location loc = mBoss.getLocation();

		final int xRad = 1;
		final int yRad = 3;
		final int zRad = 1;
		final double testLocX = loc.getX();
		final double testLocY = loc.getY();
		final double testLocZ = loc.getZ();
		final Location testLoc = new Location(loc.getWorld(), 0, 0, 0);
		Block testBlock;
		Material testMat;

		int badScore = 0;
		int requiredScore = 7;
		final List<Block> breakBlockList = new ArrayList<>();

		// If the launcher is a mob with a valid player target with a GEQ 2 block height difference, reduce threshold
		if (mBoss instanceof final Mob mob && mob.getTarget() instanceof final Player target
			&& target.getLocation().getY() >= loc.getY() + 2 && PlayerUtils.isOnGround(target)) {
			requiredScore /= 2;
		}

		/* Special case for Slime Blocks, which are full blocks that aren't caught by subsequent checks */
		for (double x = testLocX - xRad; x <= testLocX + xRad; x++) {
			for (double z = testLocZ - zRad; z <= testLocZ + zRad; z++) {
				testLoc.set(x, testLocY, z);
				testBlock = testLoc.getBlock();
				testMat = testBlock.getType();

				if (testMat == Material.SLIME_BLOCK) {
					requiredScore = 0;
					breakBlockList.add(testBlock);
				}
			}
		}

		for (double x = testLocX - xRad; x <= testLocX + xRad; x++) {
			for (double y = testLocY; y <= testLocY + yRad; y++) {
				for (double z = testLocZ - zRad; z <= testLocZ + zRad; z++) {
					testLoc.set(x, y, z);
					testBlock = testLoc.getBlock();
					testMat = testBlock.getType();
					/* Mob pathfinding frequently doesn't navigate over or around open trapdoors */
					final boolean evilTrapdoor = testBlock.getBlockData() instanceof final TrapDoor trapdoor
						&& trapdoor.isOpen();

					if (BlockUtils.isEnvHazardForMobs(testMat) || BlockUtils.mobCannotPathfindOver(testBlock.getBlockData()) || evilTrapdoor) {
						requiredScore = 0;
						breakBlockList.add(testBlock);
					} else if (!BlockUtils.isMechanicalBlock(testMat) && LocationUtils.xzDistance(testLoc, mCenter) <= Aurora.ARENA_RADIUS + 1
						&& Location.locToBlock(y) > Aurora.getSurfaceY(testLoc, mCenter)
						&& !mPillars.isAnyPillar(testLoc)
						&& blockMaterialHasCollision(testMat)
						&& !testBlock.getLocation().subtract(0, 1, 0).getBlock().getType().equals(Material.BEDROCK)
					) {
						if (y > testLocY || shouldBreakSlab(testBlock)) {
							breakBlockList.add(testBlock);
							badScore += 2;
						} else {
							badScore += 1;
						}
					}
				}
			}
		}

		/* If the threshold has been met, attempt to break blocks */
		if (badScore >= requiredScore) {
			final int finalBadScore = badScore;
			MMLog.trace(() -> "[SpellBlockBreak] Launcher " + mBoss.getName() + " has achieved " + finalBadScore +
				" badScore and is attempting to break blocks");
			breakBlocks(loc, breakBlockList);
		}
	}

	private void ascendEntity(LivingEntity livingEntity) {
		Location loc = livingEntity.getLocation();
		if (loc.y() < mBottomY) {
			Location newLoc = loc.clone();
			newLoc.setY(loc.y() + 1);
			livingEntity.teleport(loc);
			livingEntity.setVelocity(new Vector(0, 0.6, 0));

			if (livingEntity instanceof Mob mob) {
				LivingEntity target = mob.getTarget();
				if (target != null) {
					mob.getPathfinder().findPath(target.getLocation());
				}
			}
		}
		Location belowLoc = loc.clone().subtract(0, 1, 0).toBlockLocation();

		loc.setY(mCenter.getY());
		if (loc.distanceSquared(mCenter) > Aurora.ARENA_RADIUS * Aurora.ARENA_RADIUS) {
			return;
		}

		boolean placedBlock = false;
		List<Player> players = Aurora.playersInRange(mCenter);
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				Location blockLoc = belowLoc.clone().add(x + 0.5, 0, z + 0.5);
				Block block = blockLoc.getBlock();
				Material expectedType = getArenaMaterial(blockLoc);

				if (block.getType() == Material.AIR &&
					players.stream().noneMatch(player -> player.getBoundingBox().expand(0.5).contains(block.getLocation().add(0.5, 0.5, 0.5).toVector()))
				) {
					placedBlock |= TemporaryBlockChangeManager.INSTANCE.changeBlock(block, expectedType, DURATION);
				}
			}
		}
		if (placedBlock) {
			belowLoc.getWorld().playSound(belowLoc, Sound.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 1.0f, 1.2f);
		}
	}

	/**
	 * Helper method to handle destruction of blocks in world
	 *
	 * @param blockList List of blocks to break
	 */
	public void breakBlocks(final Location loc, final List<Block> blockList) {
		/* Call an event with these exploding blocks to give plugins a chance to modify it */
		final EntityExplodeEvent event = new EntityExplodeEvent(mBoss, loc, blockList, 0f);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() || blockList.isEmpty()) {
			MMLog.trace(() -> "[SpellBlockBreak] Launcher " + mBoss.getName() +
				"'s EntityExplodeEvent was cancelled or the blockList is empty. Returning false");
			return;
		}

		/* Remove any remaining blocks which might have been modified by the event */
		final Material particleMat = blockList.get(0).getType();
		for (final Block block : blockList) {
			if (BlockUtils.isValuableBlock(block.getType()) || BlockUtils.isNonEmptyContainer(block)) {
				block.breakNaturally(new ItemStack(Material.IRON_PICKAXE));
			} else {
				block.setType(Material.AIR);
			}
		}

		if (!blockList.isEmpty()) {
			loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 0.25f, FastUtils.randomFloatInRange(0.7f, 1.0f));

			new PartialParticle(Particle.BLOCK_CRACK, loc)
				.count(15)
				.delta(1.0, 2.0, 1.0)
				.extra(0.05)
				.data(particleMat.createBlockData())
				.spawnAsEntityActive(mBoss);
		}

	}

	/**
	 * Helper method to check for block material collision with entities
	 *
	 * @param material Material of the block to be tested
	 * @return True if an entity's hitbox can collide with the block material
	 */
	private boolean blockMaterialHasCollision(final Material material) {
		return (material.isSolid()
			|| ItemUtils.HEADS.contains(material)
			|| ItemUtils.CARPETS.contains(material)
			|| ItemUtils.CANDLES.contains(material)
			|| ItemUtils.FLOWER_POTS.contains(material));
	}

	private boolean shouldBreakSlab(final Block block) {
		if (block instanceof final Slab s) {
			// if block is slab, don't break if it's a bottom slab
			return s.getType() != Slab.Type.BOTTOM;
		}
		// if block is not a slab, don't break
		return true;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
