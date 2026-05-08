package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SpellStardustBlaster extends Spell {
	private static final String SPELL_NAME = "Stardust Blaster";
	private static final double RADIUS = 0.7;
	private static final int DAMAGE = 25;
	private static final double TRUE_DAMAGE = 0.1;
	private static final int CHARGE_TIME = 50;
	private static final int AIM_TIME = 10;
	private static final int INTERVAL = 10;
	private static final int MAX_CASTS = 3;
	private final double mRange;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final boolean mTrueDamage;
	private final int mMaxPlayers;

	public SpellStardustBlaster(Plugin plugin, LivingEntity boss, Location center, double range, boolean trueDamage, int maxPlayers) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRange = range;
		mTrueDamage = trueDamage;
		mMaxPlayers = maxPlayers;
	}

	@Override
	public void run() {
		new BukkitRunnable() {
			int mCasts = 0;

			@Override
			public void run() {
				summonBlasters(CHARGE_TIME - mCasts * INTERVAL);

				mCasts++;
				if (mCasts >= MAX_CASTS) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, INTERVAL);
	}

	private void summonBlasters(final int chargeTime) {
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();

		List<Player> players = Aurora.playersInRange(bossLoc);
		Collections.shuffle(players);

		players.stream().limit(mMaxPlayers).forEach(player -> {
			player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 0.8f, 2.0f);
			player.playSound(player, "minecraft:entity.breeze.charge", SoundCategory.HOSTILE, 1.6f, 0.65f);
			player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 0.8f, 0.5f);

			BukkitRunnable playerRunnable = new BukkitRunnable() {
				private final Location mReferenceLocation = LocationUtils.randomSafeLocationInCircle(mBoss.getLocation(), mRange, location ->
					location.distanceSquared(mCenter) < Aurora.ARENA_RADIUS * Aurora.ARENA_RADIUS
				);
				private final Location mDisplayLocation = mReferenceLocation.clone().add(0, 1.25, 0);
				private final Entity mDisplay = summonAuroraAsterBlaster(mDisplayLocation);

				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					if (mTicks >= chargeTime) {
						summonLaser(mDisplayLocation);

						this.cancel();
						return;
					}

					boolean isChangingDirection = mTicks <= AIM_TIME;
					if (isChangingDirection) {
						Vector dir = LocationUtils.getDirectionTo(player.getLocation(), mReferenceLocation);
						mDisplayLocation.setDirection(dir);
						EntityUtils.setDirection(dir, mDisplay);
					}
					if (mTicks % 2 == 0) {
						laserTelegraph(mDisplayLocation);
					}
					if (mTicks == AIM_TIME) {
						world.playSound(mReferenceLocation, Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.HOSTILE, 0.8f, 0.1f);
						world.playSound(mReferenceLocation, Sound.BLOCK_LEVER_CLICK, SoundCategory.HOSTILE, 1.2f, 0.25f);
					}
				}

				@Override
				public synchronized void cancel() throws IllegalStateException {
					if (mDisplay.getChunk().isLoaded()) {
						mDisplay.remove();
					}
					new PartialParticle(Particle.EXPLOSION_NORMAL, mDisplay.getLocation().clone().add(0, 1.5, 0))
						.count(5)
						.extra(.1);
					super.cancel();
				}
			};
			mActiveRunnables.add(playerRunnable);
			playerRunnable.runTaskTimer(mPlugin, 0, 1);
		});
	}

	private static Entity summonAuroraAsterBlaster(Location location) {
		Entity entity = location.getWorld().spawn(location, ItemDisplay.class, display -> {
			display.setItemStack(new ItemStack(Material.BEACON));
			display.setTransformation(new Transformation(
				new Vector3f(),
				new AxisAngle4f((float) (Math.PI / 2), 1, 0, 0),
				new Vector3f(1),
				new AxisAngle4f()
			));
		});
		EntityUtils.setRemoveEntityOnUnload(entity);

		new PartialParticle(Particle.END_ROD, location)
			.count(8)
			.extra(0.01);
		return entity;
	}

	private void laserTelegraph(Location bossLocation) {
		new PPLine(Particle.ELECTRIC_SPARK, bossLocation.clone(), bossLocation.getDirection(), LocationUtils.rayLengthToSphereSurface(mCenter, bossLocation, Aurora.ARENA_RADIUS))
			.countPerMeter(4)
			.spawnAsBoss();
	}

	private void summonLaser(Location laserCenter) {
		laserCenter.clone().add(laserCenter.getDirection());
		double length = LocationUtils.rayLengthToSphereSurface(mCenter, laserCenter, Aurora.ARENA_RADIUS + 4);
		Vector dir = laserCenter.getDirection();
		Vector displacement = dir.clone().multiply(length);
		Location target = laserCenter.clone().add(displacement);

		new PPLine(Particle.SPELL_WITCH, laserCenter, dir, length)
			.countPerMeter(3)
			.delta(0.1)
			.spawnAsBoss();

		World world = laserCenter.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundCategory.HOSTILE, 1.5f, 1.7f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.2f, 2.0f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BREEZE_SHOOT, SoundCategory.HOSTILE, 1.8f, 0.7f);
		world.playSound(mBoss.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1.2f, 1.8f);

		new PPLine(Particle.END_ROD, laserCenter, target)
			.count((int) length)
			.distanceFalloff(length)
			.delay(4)
			.extra(0.025)
			.spawnAsBoss();

		Hitbox hitbox = Hitbox.approximateCylinder(laserCenter, target, RADIUS, true);
		hitbox.getHitPlayers(true).forEach(player -> {
			BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, true, false, SPELL_NAME, laserCenter, 4 * 20, 1);
			if (mTrueDamage) {
				DamageUtils.damagePercentHealth(mBoss, player, TRUE_DAMAGE, false, false, SPELL_NAME);
			}
			MovementUtils.knockAway(laserCenter, player, 0.5f, true);
		});

	}


	@Override
	public int cooldownTicks() {
		return INTERVAL + CHARGE_TIME + Aurora.SPELL_INTERVAL;
	}
}
