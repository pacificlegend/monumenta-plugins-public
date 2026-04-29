package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SpellCelestialPillars extends Spell {
	public static final LoSPool ELITE_POOL = LoSPool.fromString("~AuroraElite");
	public static final NamespacedKey DROPPED_ITEM_KEY = NamespacedKeyUtils.fromString("epic:r3/aurora/stellar_matter");
	public static final NamespacedKey GIVEN_ITEM_KEY = NamespacedKeyUtils.fromString("epic:r3/aurora/loamskattar_catalyst");
	private static final int ELITE_SPAWN_COOLDOWN_BASE = 60 * 20;
	private static final int MORE_PLAYER_DECREASE = 6 * 20;

	private final Plugin mPlugin;
	private final Location mCenter;
	private final double mRage;
	private final double mSpawnCooldown;
	private final List<CelestialPillar> mPillars = new ArrayList<>();
	private final List<Item> mItems = new ArrayList<>();
	private final Consumer<Player> mPlayerPickupAction;
	private final Aurora.BlockRepairer mBlockRepairer;

	private enum Pillar {
		RED(new Vector(13, 1, 13), "AuroraStarPillarRed", NamedTextColor.RED),
		BLUE(new Vector(13, 1, -13), "AuroraStarPillarBlue", NamedTextColor.BLUE),
		GREEN(new Vector(-13, 1, -13), "AuroraStarPillarGreen", NamedTextColor.GREEN),
		YELLOW(new Vector(-13, 1, 13), "AuroraStarPillarYellow", NamedTextColor.YELLOW),
				;

		private final Vector mOffset;
		private final String mGrowable;
		private final int mGlowColor;

		Pillar(Vector offset, String growable, TextColor glowColor) {
			mOffset = offset;
			mGrowable = growable;
			mGlowColor = glowColor.value();
		}

		public Vector getOffset() {
			return mOffset;
		}

		public String getGrowable() {
			return mGrowable;
		}

		public int getGlowColor() {
			return mGlowColor;
		}
	}

	private double mScaledTicks;

	public SpellCelestialPillars(Plugin plugin, Location center, double rage, int playerCount, Consumer<Player> playerPickupAction, Aurora.BlockRepairer blockRepairer) {
		mPlugin = plugin;
		mCenter = center;
		mRage = rage;
		mPlayerPickupAction = playerPickupAction;
		mBlockRepairer = blockRepairer;
		mSpawnCooldown = (ELITE_SPAWN_COOLDOWN_BASE - MORE_PLAYER_DECREASE * playerCount) * 2;
		mScaledTicks = mSpawnCooldown;
	}

	@Override
	public void run() {
		mPillars.clear();
		for (Pillar info : Pillar.values()) {
			Location loc = mCenter.clone().add(info.getOffset());

			mBlockRepairer.repair(BlockUtils.getBlocksInSphere(loc, 3));
			mPillars.add(new CelestialPillar(mPlugin, loc.clone(), info.getGrowable(), info.getGlowColor(), mRage));
		}

		mScaledTicks = 0;

		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				mItems.removeIf(item -> {
					if (!item.isValid()) {
						return true;
					}
					Optional<Player> playerOptional = new Hitbox.SphereHitbox(item.getLocation(), 1.5).getHitPlayers(true).stream().findFirst();
					playerOptional.ifPresent(player -> {
						mPlayerPickupAction.accept(player);
						player.playPickupItemAnimation(item);
						item.remove();
					});

					return playerOptional.isPresent();
				});

				int size = mPillars.size();
				if (size == 0) {
					return;
				}
				mScaledTicks += (size + 2.0) / 3;

				if (mScaledTicks >= mSpawnCooldown) {
					mScaledTicks = 0;
					FastUtils.getRandomElement(mPillars).summonElite();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	public boolean checkAndDestroyPillarsInSphere(Location center, double radius) {
		ListIterator<CelestialPillar> pillarsIterator = mPillars.listIterator();

		boolean destroyedPillar = false;
		while (pillarsIterator.hasNext()) {
			CelestialPillar pillar = pillarsIterator.next();
			if (pillar.isPillarY(center.getY()) && LocationUtils.xzDistance(pillar.getLocation(), center) <= radius + 1.5) {
				pillar.destroy();
				mBlockRepairer.repair(BlockUtils.getBlocksInSphere(pillar.getLocation(), 2));

				summonMatter(pillar.getLocation());
				pillarsIterator.remove();
				destroyedPillar = true;
			}
		}

		return destroyedPillar;
	}

	public void checkAndDestroyPillars(Location blockLocation) {
		ListIterator<CelestialPillar> pillarsIterator = mPillars.listIterator();

		while (pillarsIterator.hasNext()) {
			CelestialPillar pillar = pillarsIterator.next();
			if (pillar.isPillarBlock(blockLocation)) {
				pillar.destroy();
				mBlockRepairer.repair(BlockUtils.getBlocksInSphere(pillar.getLocation(), 2));

				summonMatter(pillar.getLocation());
				pillarsIterator.remove();
			}
		}
	}

	private void summonMatter(Location loc) {
		@Nullable
		LootTable lootTable = Bukkit.getLootTable(DROPPED_ITEM_KEY);
		if (lootTable != null) {
			Collection<ItemStack> items = lootTable.populateLoot(FastUtils.RANDOM, new LootContext.Builder(loc).build());
			items.forEach(itemStack -> {
				Item item = loc.getWorld().dropItem(loc, itemStack);
				item.setCanMobPickup(false);
				item.setCanPlayerPickup(false);

				GlowingManager.startGlowing(item, NamedTextColor.GREEN, -1, 0);
				EntityUtils.setRemoveEntityOnUnload(item);

				mItems.add(item);
			});
		}
	}

	public boolean isAnyPillar(Location blockLocation) {
		for (CelestialPillar pillar : mPillars) {
			if (pillar.isPillarBlock(blockLocation)) {
				return true;
			}
		}
		return false;
	}

	public List<CelestialPillar> getPillars() {
		return mPillars;
	}

	public void glow(int duration) {
		mPillars.forEach(celestialPillar -> celestialPillar.glow(duration));
	}

	@Override
	public void cancel() {
		mPillars.forEach(CelestialPillar::destroy);
		mPillars.clear();
		mItems.forEach(Entity::remove);
		mItems.clear();

		super.cancel();
	}

	@Override
	public int cooldownTicks() {
		return Aurora.SPELL_INTERVAL;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

}
