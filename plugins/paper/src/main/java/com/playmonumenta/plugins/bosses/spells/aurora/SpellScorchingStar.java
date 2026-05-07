package com.playmonumenta.plugins.bosses.spells.aurora;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellScorchingStar extends Spell {
	private static final double DAMAGE = 24;
	private static final int FIRE_TICKS = 4 * 20;
	private static final int TELEGRAPH_DURATION = 30;
	private static final int DURATION = 4 * 20;
	private static final double TURN_FACTOR_1 = Math.PI / 30;
	private static final double TURN_FACTOR_2 = Math.PI / 50;
	private static final int TURN_FACTOR_CHANGE_TIME = 2 * 20;
	private static final double SPEED = 0.36;
	private static final double HITBOX_SIZE = 0.6;
	private static final String SPELL_NAME = "Scorching Star";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mAcceleration;

	public SpellScorchingStar(Plugin plugin, LivingEntity boss, double acceleration) {
		mPlugin = plugin;
		mBoss = boss;
		mAcceleration = acceleration;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();

		Location bossLoc = mBoss.getLocation();
		world.playSound(bossLoc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 1.6f, 0.5f);
		world.playSound(bossLoc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 2.5f, 1.6f);
		world.playSound(bossLoc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 2.0f, 0.1f);
		world.playSound(bossLoc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.HOSTILE, 1.6f, 1.0f);

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				for (int i = 0; i < 5; i++) {
					double time = mTicks + i / 5.0;
					double x = FastUtils.sin(time);
					double z = FastUtils.cos(time);

					double partialY = FastUtils.sin(time / 1.34);
					double ySign = Math.signum(partialY);
					double y = Math.sqrt(Math.abs(partialY)) * ySign;
					double mult = FastUtils.sin(y * Math.PI / 2) * 4 * mTicks / TELEGRAPH_DURATION;
					Location pLoc = LocationUtils.getHalfHeightLocation(mBoss);

					new PartialParticle(Particle.FLAME, pLoc.clone().add(x * mult, y, z * mult)).spawnAsBoss();

					new PartialParticle(Particle.FLAME, pLoc.clone().add(-x * mult, y, -z * mult)).spawnAsBoss();
				}
				world.playSound(bossLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 3.0f, 0.5f + 1.5f * mTicks / TELEGRAPH_DURATION);

				if (mTicks >= TELEGRAPH_DURATION) {
					List<Player> players = Aurora.playersInRange(bossLoc);
					Collections.shuffle(players);
					players.stream()
						.limit(3)
						.forEach(player -> mActiveTasks.add(new BukkitRunnable() {
							int mSpawnedTimes = 0;

							@Override
							public void run() {
								spawnFireball(player, FastUtils.randomDoubleInRange(90, 105) * FastUtils.randomSign());

								mSpawnedTimes++;
								if (mSpawnedTimes >= 3) {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 2)));

					this.cancel();
					return;
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void spawnFireball(Player target, double initialAngle) {
		World world = mBoss.getWorld();
		Location bossLoc = LocationUtils.getHalfHeightLocation(mBoss);
		world.playSound(bossLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.7f, 0.7f);
		world.playSound(bossLoc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 1.5f, 1.5f);
		world.playSound(bossLoc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.HOSTILE, 1.5f, 1.2f);

		ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
		if (skull.getItemMeta() instanceof SkullMeta skullMeta) {
			PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
			profile.setProperty(new ProfileProperty("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzJlYmVhMTdjMzIzNTYzN2E3NDQ4ODczODA2MDllMzhmYWU3NDhhMjY5YzY3NThkZDA5Njk4NmYyYWI5ZjgxNCJ9fX0="));
			skullMeta.setPlayerProfile(profile);
			skull.setItemMeta(skullMeta);
		}

		BukkitRunnable fireballRunnable = new BukkitRunnable() {
			private final Display mDisplay = world.spawn(bossLoc, ItemDisplay.class, display -> {
				display.setItemStack(skull);
				display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);

				display.setGlowing(true);
				ScoreboardUtils.getExistingTeamOrCreate("red").addEntity(display);

				EntityUtils.setRemoveEntityOnUnload(display);
			});
			private Location mCurrentLoc = bossLoc;
			private Vector mCurrentDir = LocationUtils.getDirectionTo(target.getLocation().add(0, 4, 0), bossLoc)
				.rotateAroundY(Math.toRadians(initialAngle));
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks >= DURATION) {
					this.cancel();
					return;
				}

				Location targetLocation = target.getLocation();

				Vector projectedDir = targetLocation.clone().subtract(mCurrentLoc).toVector().normalize();

				double turnAngle = mTicks >= TURN_FACTOR_CHANGE_TIME ? TURN_FACTOR_2 : TURN_FACTOR_1;
				double newAngle = Math.acos(Math.clamp(mCurrentDir.dot(projectedDir), -1, 1));
				double halfEndpointDistance = FastUtils.sin(newAngle / 2);

				// Only do calculations if there's actually a direction change
				if (newAngle <= turnAngle) {
					mCurrentDir = projectedDir;
				} else if (halfEndpointDistance != 0) {
					double length = (halfEndpointDistance + FastUtils.sin(turnAngle - newAngle / 2)) / (2 * halfEndpointDistance);
					Vector newerDirection = mCurrentDir.clone().add(projectedDir.subtract(mCurrentDir).multiply(length)).normalize();
					if (Double.isFinite(newerDirection.getX())) {
						mCurrentDir = newerDirection;
					}
				}

				Location prevLoc = mCurrentLoc.clone();
				mCurrentLoc = mCurrentLoc.add(mCurrentDir.multiply(SPEED + mTicks * mAcceleration));
				if (mCurrentLoc.getBlock().isSolid()) {
					this.cancel();
					return;
				}

				mDisplay.setTeleportDuration(2);
				mDisplay.teleport(mCurrentLoc);

				new PartialParticle(Particle.FLAME, mCurrentLoc)
					.count(3)
					.delta(HITBOX_SIZE)
					.spawnAsBoss();

				new PartialParticle(Particle.SMOKE_LARGE, mCurrentLoc).spawnAsBoss();

				List<Player> players = new Hitbox.AABBHitbox(mCurrentLoc.getWorld(), BoundingBox.of(mCurrentLoc, HITBOX_SIZE, HITBOX_SIZE, HITBOX_SIZE)).getHitPlayers(true);
				players.forEach(player -> {
					BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, true, false, SPELL_NAME, prevLoc);
					MovementUtils.knockAway(mCurrentLoc, player, 0.2f, 0.3f, false);
					EntityUtils.applyFire(mPlugin, FIRE_TICKS, player, mBoss);
				});

				if (!players.isEmpty()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				world.playSound(mCurrentLoc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1.0f, 1.4f);
				world.playSound(mCurrentLoc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1.0f, 0.8f);
				mDisplay.remove();
				super.cancel();
			}
		};
		mActiveRunnables.add(fireballRunnable);
		fireballRunnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return TELEGRAPH_DURATION + DURATION;
	}
}
