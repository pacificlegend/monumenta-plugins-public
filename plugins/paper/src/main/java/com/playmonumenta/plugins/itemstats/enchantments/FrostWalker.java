package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.chunk.ChunkPartialUnloadEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrostWalker implements Enchantment, Listener {
	public static final int PER_LEVEL = 1;
	public static final int TICKS_PER_MELT_STAGE_MIN = 10;
	public static final int TICKS_PER_MELT_STAGE_MAX = 20;
	public static final BlockData[] FROSTED_ICE_STATES = new BlockData[4];

	static {
		for (int i = 0; i < 4; i++) {
			int finalI = i;
			FROSTED_ICE_STATES[i] = Bukkit.createBlockData(Material.FROSTED_ICE, state -> ((Ageable) state).setAge(finalI));
		}
	}

	private @Nullable Location mPreviousPlayerLoc;
	private static final PriorityQueue<FrostedIceState> frostedIce = new PriorityQueue<>();
	private static @Nullable BukkitRunnable mFrostedIceRunnable;
	private static final Map<Chunk, Set<FrostedIceState>> frostedIceChunkCache = new HashMap<>();

	private record FrostedIceState(int tickToChangeOn, Block block, int age) implements Comparable<FrostedIceState> {
		@Override
		public int compareTo(@NotNull FrostWalker.FrostedIceState o) {
			return tickToChangeOn - o.tickToChangeOn;
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof FrostedIceState other) && other.block.equals(block);
		}
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FROST_WALKER;
	}

	@Override
	public String getName() {
		return "Frost Walker";
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (mFrostedIceRunnable == null) {
			mFrostedIceRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					while (!frostedIce.isEmpty() && frostedIce.peek().tickToChangeOn <= Bukkit.getCurrentTick()) {
						FrostedIceState ice = frostedIce.remove();
						if (ice.age == 3) {
							ice.block.setType(Material.WATER);
						} else if (ice.block.getType() == Material.FROSTED_ICE) { // if its not frosted ice anymore someone broke it, dont put it back
							ice.block.setBlockData(FROSTED_ICE_STATES[ice.age + 1]);
							FrostedIceState newIce = new FrostedIceState(getNextMeltTick(), ice.block, ice.age + 1);
							frostedIce.add(newIce);
						}
					}
				}
			};
			mFrostedIceRunnable.runTaskTimer(plugin, 0, 1);
		}
	}

	/*
	fix conditions

	should activate if one of the following is true:
	- the player is standing on solid ground
	- the block below the player's feet is water and the player is more than 0.1 blocks above the water
	- the block below the player's feet is air, the player is NOT standing on solid ground, the block two below the player's feet is water, and the player is in the bottom 0.2522 meters of the block they are in
	 */

	@Override
	public void onMovement(Plugin plugin, Player player, double value, PlayerMoveEvent event) {
		if (value == 0) {
			return;
		}
		if (mPreviousPlayerLoc != null && mPreviousPlayerLoc.getX() == player.getLocation().getX() && mPreviousPlayerLoc.getZ() == player.getLocation().getZ()) {
			// only run if the player's horizontal position changed
			return;
		}
		mPreviousPlayerLoc = player.getLocation();

		/*
		fix conditions

		should activate if one of the following is true:
		- the player is standing on solid ground (activate 1 below player)
		- the block below the player's feet is water and the player is more than 0.1 blocks above the water (activate 1 below player)
		- the block below the player's feet is air, the player is NOT standing on solid ground, the block two below the player's feet is water, and the player is in the bottom 0.3 meters of the block they are in (activate 2 below player)
		 */
		Location target;
		boolean grounded = PlayerUtils.isOnGround(player);
		Location belowPlayer = player.getLocation().add(0, -1, 0);
		Location twoBelowPlayer = player.getLocation().add(0, -2, 0);
		double yPosWithinBlock = player.getLocation().getY() - player.getLocation().getBlockY();

		if (grounded) {
			target = belowPlayer.toCenterLocation();
		} else if (belowPlayer.getBlock().getType() == Material.WATER) {
			if (yPosWithinBlock > 0.1) {
				target = belowPlayer.toCenterLocation();
			} else {
				return;
			}
		} else if (belowPlayer.getBlock().getType() == Material.AIR && twoBelowPlayer.getBlock().getType() == Material.WATER && yPosWithinBlock < 0.3) {
			target = twoBelowPlayer.toCenterLocation();
		} else {
			return;
		}

		int radius = Math.min(PER_LEVEL * (int) value, 6);
		List<Block> blocks = new ArrayList<>();
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				if (x * x + z * z < (radius + 0.5) * (radius + 0.5)) { // +0.5 is a small adjustment for nicer looking circles
					blocks.add(target.clone().add(x, 0, z).getBlock());
				}
			}
		}
		blocks.forEach(FrostWalker::makeIce);
	}

	@EventHandler(ignoreCancelled = false)
	public void onChunkUnload(ChunkPartialUnloadEvent event) {
		Set<FrostedIceState> set = frostedIceChunkCache.get(event.getChunk());
		if (set == null) {
			return;
		}
		for (FrostedIceState ice : set) {
			ice.block.setType(Material.WATER);
			frostedIce.remove(ice);
		}
		frostedIceChunkCache.remove(event.getChunk());
	}

	private static boolean blockCanBeIced(Block block) {
		if (block.getRelative(0, 1, 0).getType() != Material.AIR) {
			return false;
		}
		if (block.getType() == Material.FROSTED_ICE) {
			return true;
		}
		return block.getType() == Material.WATER
			&& ((Levelled) block.getBlockData()).getLevel() == 0 // source block
			&& !ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED) // self explanatory
			&& block.getWorld().getNearbyEntities(block.getBoundingBox(),
				entity -> entity instanceof LivingEntity).isEmpty(); // no entities are in this block, check this last for performance
	}

	private static void makeIce(Block block) {
		if (!blockCanBeIced(block)) {
			return;
		}
		FrostedIceState ice;
		if (block.getType() == Material.WATER) {
			block.setType(Material.FROSTED_ICE);
			block.setBlockData(FROSTED_ICE_STATES[0]);
			ice = new FrostedIceState(getNextMeltTick(), block, 0);
		} else {
			// frosted ice, probably already in queue
			Optional<FrostedIceState> maybeIce = frostedIce.stream().filter(i -> i.block.equals(block)).findFirst();
			if (maybeIce.isPresent()) {
				FrostedIceState oldIce = maybeIce.get();
				frostedIce.remove(oldIce);
				int newAge = Math.max(oldIce.age - FastUtils.randomIntInRange(0, 1), 0);
				ice = new FrostedIceState(getNextMeltTick(), block, newAge);
				block.setBlockData(FROSTED_ICE_STATES[newAge]);
			} else {
				ice = new FrostedIceState(getNextMeltTick(), block, 0);
				block.setBlockData(FROSTED_ICE_STATES[0]);
			}
		}
		frostedIce.add(ice);
		Set<FrostedIceState> set = frostedIceChunkCache.computeIfAbsent(block.getChunk(), c -> new HashSet<>());
		set.add(ice);
	}

	private static int getNextMeltTick() {
		return Bukkit.getCurrentTick() + FastUtils.randomIntInRange(TICKS_PER_MELT_STAGE_MIN, TICKS_PER_MELT_STAGE_MAX);
	}
}
