package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellStarlightSurge extends Spell {
	private static final String SPELL_NAME = "Starlight Surge";
	private static final int ENERGY_PER_PILLAR = 4;
	private static final int ENERGY_RANGE = 10;
	private static final int ENERGY_DURATION = 8 * 20;
	private static final int CHARGE_DURATION = ENERGY_DURATION + 5;
	private static final double GRAVITY = 30.0 / 20 / 20;
	private static final double PROJECTILE_RADIUS = 6;
	private static final double ATTACK_RANGE = 18.5;
	private static final int DAMAGE = 40;
	private static final int EXPLODE_DELAY = 30;
	private static final double VULNERABILITY = 0.25;
	private static final int VULNERABILITY_DURATION = 2 * 60 * 20;
	public static final String VULNERABILITY_SOURCE = "StarlightSurgeVulnerability";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final double mVulnerability;
	private final SpellCelestialPillars mCelestialPillar;
	private final BiConsumer<Player, Double> mOnDebuff;
	private final List<Entity> mEnergies = new ArrayList<>();
	private final int mMaxProjectiles;

	public SpellStarlightSurge(Plugin plugin, LivingEntity boss, Location center, double vulnerabilityMult, SpellCelestialPillars celestialPillar, BiConsumer<Player, Double> onDebuff, int maxProjectiles) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mCelestialPillar = celestialPillar;
		mVulnerability = VULNERABILITY * vulnerabilityMult;
		mOnDebuff = onDebuff;
		mMaxProjectiles = maxProjectiles;
	}

	@Override
	public void run() {
		if (mCelestialPillar.getPillars().isEmpty()) {
			return;
		}
		mPlugin.mEffectManager.addEffect(mBoss, "StarlightSurgeSlowness", new PercentSpeed(CHARGE_DURATION, -0.5, "StarlightSurgeSpeed"));
		mBoss.setPose(Pose.SNEAKING, true);

		Location bossLoc = mBoss.getLocation();
		World world = bossLoc.getWorld();

		world.playSound(bossLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1.0f, 1.7f);
		world.playSound(bossLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.2f, 1.0f);
		world.playSound(bossLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 3.0f, 0.4f);
		world.playSound(bossLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 3.0f, 0.1f);

		mEnergies.clear();

		Set<Player> debuffPlayers = new HashSet<>();

		BukkitRunnable masterRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= CHARGE_DURATION) {
					debuffPlayers.forEach(player -> {
						@Nullable
						Effect currentEffect = mPlugin.mEffectManager.getActiveEffect(player, VULNERABILITY_SOURCE);
						double previousPower = currentEffect == null ? 0 : currentEffect.getMagnitude();
						double newPower = previousPower + mVulnerability;
						mPlugin.mEffectManager.addEffect(player, VULNERABILITY_SOURCE, new PercentDamageReceived(VULNERABILITY_DURATION, newPower).deleteOnLogout(true).deleteOnDeath(true));
						mOnDebuff.accept(player, newPower);

						player.playSound(player, Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 3.0f, 2.0f, 3);
						player.sendMessage(Aurora.formatMessage("ɪɴᴄᴏᴍᴘᴇᴛᴇɴᴄᴇ...", "[Aurora]", Aurora.AURORA_COLOR));
					});

					this.cancel();
					return;
				}

				if (mTicks % 5 == 0) {
					world.playSound(bossLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 3.0f, mTicks * 1.5f / CHARGE_DURATION);
				}

				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				mBoss.setPose(Pose.STANDING);
			}
		};
		masterRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(masterRunnable);

		mCelestialPillar.getPillars().forEach(pillar -> {
			List<Entity> selfEnergies = spawnEnergy(pillar.getLocation());
			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					selfEnergies.removeIf(entity -> !entity.isValid() || entity.isDead());
					if (selfEnergies.isEmpty() || pillar.isDead()) {
						this.cancel();
						return;
					}

					Location particleLoc = pillar.getTopLocation().clone().add(0, mTicks / 10.0, 0);
					Location raisedLoc = pillar.getLocation().clone().add(0, 1.1, 0);
					double currentRadius = ATTACK_RANGE * selfEnergies.size() / ENERGY_PER_PILLAR;
					if (mTicks >= ENERGY_DURATION) {
						this.cancel();
						if (pillar.isDead() || selfEnergies.isEmpty()) {
							return;
						}
						Location topLoc = pillar.getTopLocation();
						selfEnergies.forEach(entity ->
							new PPLine(Particle.ELECTRIC_SPARK, entity.getLocation(), topLoc)
								.countPerMeter(3)
								.spawnAsBoss()
						);

						new PartialParticle(Particle.FIREWORKS_SPARK, particleLoc)
							.count(200)
							.extra(currentRadius / 16)
							.spawnAsBoss();

						new PPSpiral(Particle.SPELL_WITCH, raisedLoc, currentRadius)
							.countPerBlockPerCurve(10)
							.curves(10)
							.curveAngle(90)
							.distanceFalloff(52)
							.spawnAsBoss();

						world.playSound(topLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 3.0f, 0.5f);
						world.playSound(topLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 3.0f, 0.5f);

						spawnSurge(topLoc, selfEnergies.size(), currentRadius);
						debuffPlayers.addAll(new Hitbox.UprightCylinderHitbox(pillar.getLocation(), 10, currentRadius).getHitPlayers(true));

						return;
					}

					for (int i = 0; i < 5; i++) {
						double time = mTicks + i / 5.0;
						double x = FastUtils.sin(time);
						double z = FastUtils.cos(time);

						double partialY = FastUtils.sin(time / 1.34);
						double ySign = Math.signum(partialY);
						double y = Math.sqrt(Math.abs(partialY)) * ySign;
						double mult = FastUtils.sin(y * Math.PI / 2);
						Location pLoc = pillar.getTopLocation().clone();

						pLoc.setX(pLoc.getX() + x * mult);
						pLoc.setZ(pLoc.getZ() + z * mult);
						pLoc.setY(pLoc.getY() + y);
						new PartialParticle(Particle.CRIT_MAGIC, pLoc)
							.delta(0.075)
							.spawnAsBoss();
					}

					if (mTicks % 10 == 0) {
						selfEnergies.forEach(entity -> {
							Location location = entity.getLocation();
							new PPLine(Particle.DUST_COLOR_TRANSITION, location, pillar.getTopLocation())
								.countPerMeter(10)
								.data(new Particle.DustTransition(Color.FUCHSIA, Color.PURPLE, 1.36f))
								.delta(0.05)
								.delay(5)
								.spawnAsBoss();
						});

						new PPCircle(Particle.CRIT, raisedLoc, currentRadius)
							.countPerMeter(8)
							.distanceFalloff(52)
							.spawnAsBoss();
					}

					// rising energy ball
					double cos = FastUtils.cos(mTicks / 20.0);
					double sin = FastUtils.sin(mTicks / 20.0);
					PartialParticle particle = new PartialParticle(Particle.END_ROD, particleLoc)
						.directionalMode(true)
						.extra(0.05 + 0.05 * mTicks / CHARGE_DURATION);
					particle.delta(cos, 1, sin).spawnAsBoss();
					particle.delta(-cos, 1, -sin).spawnAsBoss();

					new PartialParticle(Particle.SOUL_FIRE_FLAME, particleLoc).spawnAsBoss();
					mTicks++;
				}

				@Override
				public synchronized void cancel() throws IllegalStateException {
					super.cancel();
					selfEnergies.forEach(entity -> {
						new PartialParticle(Particle.EXPLOSION_LARGE, entity.getLocation()).spawnAsBoss();
						entity.remove();
					});
				}
			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runnable);
		});
	}

	private List<Entity> spawnEnergy(Location pillarLoc) {
		List<Entity> selfEnergies = new ArrayList<>();
		for (int i = 0; i < ENERGY_PER_PILLAR; i++) {
			Location spawnLoc = LocationUtils.randomLocationInDonut(pillarLoc, 2, ENERGY_RANGE);
			spawnLoc.setY(spawnLoc.getY() + FastUtils.randomIntInRange(2, 9));
			@Nullable
			Entity summon = LibraryOfSoulsAPI.summon(spawnLoc, "EvilStarEnergy");
			if (summon == null) {
				MMLog.severe("Aurora: Soul 'EvilStarEnergy' doesn't exist!");
				return List.of();
			}
			EntityUtils.setRemoveEntityOnUnload(summon);
			mEnergies.add(summon);
			selfEnergies.add(summon);
		}
		return selfEnergies;
	}

	private void spawnSurge(Location pillarLoc, int aliveEnergyCount, double currentRadius) {
		mActiveTasks.add(new BukkitRunnable() {
			private final World mWorld = pillarLoc.getWorld();
			private final int mChargeTime = mMaxProjectiles * aliveEnergyCount / ENERGY_PER_PILLAR;
			int mTicks = 0;

			@Override
			public void run() {
				mWorld.playSound(pillarLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.2f, 1.0f);
				mWorld.playSound(pillarLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.5f, 0.6f);

				Location fallLoc = LocationUtils.randomSafeLocationInCircle(pillarLoc, currentRadius - PROJECTILE_RADIUS, location ->
					Aurora.withSurfaceY(location, mCenter).getBlock().isSolid() && location.distanceSquared(mCenter) < Aurora.ARENA_RADIUS * Aurora.ARENA_RADIUS
				).add(0, 4, 0);

				new PPCircle(Particle.CRIT_MAGIC, fallLoc.clone().add(0, 0.15, 0), PROJECTILE_RADIUS)
					.count(40)
					.rotateDelta(true)
					.directionalMode(true)
					.delta(1, 0, 0)
					.extra(0.5)
					.spawnAsBoss();

				summonProjectile(pillarLoc, fallLoc);

				mTicks++;
				if (mTicks >= mChargeTime) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	public void summonProjectile(Location startLoc, Location fallLoc) {
		Location loc = Aurora.withSurfaceY(fallLoc, mCenter);

		new BukkitRunnable() {
			private final Location mCurrentLocation = startLoc.clone();
			private final Vector mVelocity = new Vector(
				(loc.x() - startLoc.x()) / EXPLODE_DELAY,
				(loc.y() - startLoc.y()) / EXPLODE_DELAY + GRAVITY / 2 * EXPLODE_DELAY,
				(loc.z() - startLoc.z()) / EXPLODE_DELAY
			);
			private int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= EXPLODE_DELAY) {
					new PPCircle(Particle.END_ROD, loc.clone().add(0, 0.15, 0), 0.5)
						.count(40)
						.rotateDelta(true)
						.directionalMode(true)
						.distanceFalloff(52)
						.delta(1, 0, -0.5)
						.extra(0.075 * PROJECTILE_RADIUS)
						.spawnAsBoss();

					World world = loc.getWorld();

					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 1.8f);
					world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.3f, 1.0f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 2f);
					world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1f, 2f);
					world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.PLAYERS, 0.8f, 1.2f);

					new Hitbox.SphereHitbox(loc, PROJECTILE_RADIUS).getHitPlayers(true).forEach(player ->
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.BLAST, DAMAGE, false, false, SPELL_NAME, loc)
					);

					this.cancel();
					return;
				}

				new PPLine(Particle.ELECTRIC_SPARK, mCurrentLocation.clone(), mCurrentLocation.add(mVelocity))
					.countPerMeter(2)
					.spawnAsBoss();
				mVelocity.setY(mVelocity.getY() - GRAVITY);
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void cancel() {
		super.cancel();
		mEnergies.forEach(Entity::remove);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
