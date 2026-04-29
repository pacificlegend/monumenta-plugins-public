package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellAstralCollapse extends Spell {
	public static final int START_SIZE = 8;
	private static final String SPELL_NAME = "Collapsed Star (☠)";
	private static final int GRACE_PERIOD = 30;
	private static final int SPIN_SPEED = 22;
	private static final int REFILL_BLOCK_INTERVAL = 2 * 20;
	private static final String TAG = "AuroraBlackHole";
	private static final Transformation TRANSFORM_ZERO = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(), new Quaternionf());

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Consumer<LivingEntity> mEntityEater;
	private final Location mHoleCenter;
	private final Aurora.BlockDestroyer mBlockDestroyer;

	@Nullable
	private ItemDisplay mBlackHole = null;
	@Nullable
	private ItemDisplay mAccretionDisk = null;
	@Nullable
	private ItemDisplay mAccretionDiskVertical = null;

	private float mBlackHoleSize = START_SIZE;
	private Queue<Block> mBlocks = new ArrayDeque<>();

	public SpellAstralCollapse(Plugin plugin, LivingEntity boss, Location center, Aurora.BlockDestroyer blockDestroyer, Consumer<LivingEntity> entityEater) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mEntityEater = entityEater;
		mHoleCenter = mCenter.clone().add(0, 2.5, 0);
		mBlockDestroyer = blockDestroyer;
	}

	@Override
	public void run() {
		mBlocks.clear();
		fillBlocksToBeDestroyed();

		mBlackHole = mCenter.getWorld().spawn(mHoleCenter, ItemDisplay.class, display -> {
			display.setBrightness(new Display.Brightness(9, 9));
			display.setTransformation(TRANSFORM_ZERO);
			display.setItemStack(new ItemStack(Material.BLACK_CONCRETE));

			display.addScoreboardTag(TAG);
			EntityUtils.setRemoveEntityOnUnload(display);
		});
		mAccretionDisk = mCenter.getWorld().spawn(mHoleCenter, ItemDisplay.class, display -> {
			display.setBrightness(new Display.Brightness(9, 9));
			display.setTransformation(TRANSFORM_ZERO);
			display.setRotation(0, 90);
			display.setItemStack(DisplayEntityUtils.generateRPItem(Material.HEART_OF_THE_SEA, "Stilldreamer's Slumber"));

			display.addScoreboardTag(TAG);
			EntityUtils.setRemoveEntityOnUnload(display);
		});
		mAccretionDiskVertical = mCenter.getWorld().spawn(mHoleCenter, ItemDisplay.class, display -> {
			display.setBrightness(new Display.Brightness(9, 9));
			display.setTransformation(TRANSFORM_ZERO);
			display.setItemStack(DisplayEntityUtils.generateRPItem(Material.HEART_OF_THE_SEA, "Stilldreamer's Slumber"));
			display.setBillboard(Display.Billboard.VERTICAL);

			display.addScoreboardTag(TAG);
			EntityUtils.setRemoveEntityOnUnload(display);
		});
		mBlackHole.setInterpolationDuration(GRACE_PERIOD);
		mAccretionDisk.setInterpolationDuration(5);
		mAccretionDiskVertical.setInterpolationDuration(5);

		new PartialParticle(Particle.PORTAL, mCenter)
			.count(90)
			.extra(START_SIZE)
			.spawnAsBoss();
		new PartialParticle(Particle.END_ROD, mCenter)
			.count(30)
			.extra(START_SIZE * 0.1)
			.spawnAsBoss();
		new PartialParticle(Particle.FLAME, mCenter)
			.count(20)
			.extra(START_SIZE * 0.2)
			.spawnAsBoss();

		new PPPillar(Particle.END_ROD, mCenter, 10)
			.count(25)
			.extra(0.04)
			.spawnAsBoss();

		new PartialParticle(Particle.FLASH, mCenter.clone().add(0, 0.5, 0))
			.minimumCount(1)
			.spawnAsBoss();

		World world = mBoss.getWorld();
		world.playSound(mCenter, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.BLOCKS, 3.0f, 0.5f);
		world.playSound(mCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.BLOCKS, 2.5f, 0.5f);
		world.playSound(mCenter, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.5f, 0.1f);
		world.playSound(mCenter, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 3.0f, 0.6f);
		world.playSound(mCenter, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.BLOCKS, 2.0f, 0.7f);

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBlackHole == null || !mBlackHole.isValid()) {
					this.cancel();
					return;
				}
				if (mTicks % REFILL_BLOCK_INTERVAL == 0) {
					fillBlocksToBeDestroyed();
				}
				if (mTicks >= GRACE_PERIOD) {
					new PPPillar(Particle.END_ROD, mCenter, 10)
						.count(10)
						.delta(0, 1, 0)
						.directionalMode(true)
						.extra(1)
						.spawnAsBoss();

					new PPPillar(Particle.END_ROD, mCenter.clone().subtract(0, 10, 0), 10)
						.count(10)
						.delta(0, -1, 0)
						.directionalMode(true)
						.extra(1)
						.spawnAsBoss();

					new Hitbox.SphereHitbox(
						mHoleCenter,
						mBlackHoleSize - 2
					).getHitEntitiesByClass(LivingEntity.class).forEach(livingEntity -> {
						if (livingEntity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
							return;
						}
						if (livingEntity == mBoss || livingEntity.getLastDamageCause() == null) {
							return;
						}
						livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 5, -1, true, false, false));
						EffectManager.getInstance().addEffect(livingEntity, "EventHorizonRoot", new PercentSpeed(5, -1.0, "EventHorizonSpeed"));
						MovementUtils.pullTowardsNormalized(mHoleCenter, livingEntity, 0.2f, false);
					});

					new Hitbox.SphereHitbox(
						mHoleCenter,
						mBlackHoleSize / 3 - 1
					).getHitEntitiesByClass(LivingEntity.class).forEach(livingEntity -> {
						if (livingEntity instanceof Player player && player.getGameMode() != GameMode.SPECTATOR) {
							PlayerUtils.killPlayer(player, mBoss, SPELL_NAME, true, true, true);
						} else if (livingEntity != mBoss && livingEntity.getLastDamageCause() != null) {
							mEntityEater.accept(livingEntity);
							DamageUtils.damage(mBoss, livingEntity, DamageEvent.DamageType.TRUE, 99999999, null, true, false, SPELL_NAME);
						}
					});

					// gets the first solid block from the queue and pops the non-solid ones before it.
					@Nullable
					Block block = mBlocks.poll();
					if (block != null) {
						Material type = block.getType();
						Location blockLocation = block.getLocation().add(0.5, 0.5, 0.5);
						if (blockLocation.distanceSquared(mCenter) >= mBlackHoleSize * mBlackHoleSize && mBlackHoleSize < Aurora.ARENA_RADIUS) {
							mBlackHoleSize++;
							updateBlackhole();
						}

						mBlockDestroyer.destroy(List.of(block));
						world.spawn(blockLocation, ItemDisplay.class, display -> {
							display.setItemStack(new ItemStack(type));
							display.setTeleportDuration(10);

							EntityUtils.setRemoveEntityOnUnload(display);

							Bukkit.getScheduler().runTaskLater(mPlugin, () -> display.teleport(mHoleCenter), 1);
							Bukkit.getScheduler().runTaskLater(mPlugin, display::remove, 10);
						});

						world.playSound(blockLocation, Sound.BLOCK_STONE_BREAK, 1.2f, 0.8f);
					}
				}
				updateAccretionDisk(mTicks);

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void fillBlocksToBeDestroyed() {
		mBlocks = BlockUtils.getBlocksInCylinder(mCenter.clone().subtract(0, 1, 0), Aurora.ARENA_RADIUS, 10).stream()
			.filter(block -> Aurora.isBreakableMaterial(block.getType()))
			.sorted(Comparator.comparing(block -> block.getLocation().add(0.5, 0.5, 0.5).distanceSquared(mCenter)))
			.collect(ArrayDeque::new, ArrayDeque::add, ArrayDeque::addAll);
	}

	@Override
	public void cancel() {
		super.cancel();
		if (mBlackHole == null) {
			return;
		}
		mBlackHole.remove();

		if (mAccretionDisk == null) {
			return;
		}
		mAccretionDisk.remove();

		if (mAccretionDiskVertical == null) {
			return;
		}
		mAccretionDiskVertical.remove();
	}

	private void updateAccretionDisk(int tick) {
		if (mAccretionDisk == null) {
			return;
		}
		mAccretionDisk.setInterpolationDelay(-1);
		mAccretionDisk.setTransformation(new Transformation(
			new Vector3f(),
			new AxisAngle4f((float) Math.toRadians(tick * SPIN_SPEED), 0, 0, 1),
			new Vector3f(mBlackHoleSize),
			new AxisAngle4f()
		));

		if (mAccretionDiskVertical == null) {
			return;
		}
		mAccretionDiskVertical.setInterpolationDelay(-1);
		mAccretionDiskVertical.setTransformation(new Transformation(
			new Vector3f(),
			new AxisAngle4f((float) Math.toRadians(tick * SPIN_SPEED), 0, 0, 1),
			new Vector3f(mBlackHoleSize * 2 / 3),
			new AxisAngle4f()
		));
	}

	private void updateBlackhole() {
		if (mBlackHole == null) {
			return;
		}
		mBlackHole.setInterpolationDelay(-1);
		mBlackHole.setTransformation(new Transformation(
			new Vector3f(),
			new AxisAngle4f(),
			new Vector3f(mBlackHoleSize / 3),
			new AxisAngle4f()
		));
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return 3 * 20;
	}
}
