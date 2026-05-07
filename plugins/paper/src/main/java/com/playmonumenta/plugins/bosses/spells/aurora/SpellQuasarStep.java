package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellQuasarStep extends Spell {
	private static final String SPELL_NAME = "Quasar Step";
	private static final int DAMAGE = 30;
	private static final int DAMAGE_ADD = 10;
	private static final int CHARGE_TIME = 30;
	private static final int RECAST_DELAY = 20;
	private static final double LINE_RADIUS = 1.25;
	private static final double BOSS_RADIUS = 2;
	private static final double PLAYER_RADIUS = 3;
	private static final int MAX_CASTS = 100;
	private static final int MAX_OVERSHOOT = 6;
	private static final Color DUST_COLOR = Color.fromRGB(0xff9c40);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;

	public SpellQuasarStep(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
	}

	@Override
	public void run() {
		List<Player> players = Aurora.playersInRange(mCenter);
		if (players.isEmpty()) {
			return;
		}
		Player firstPlayer = FastUtils.getRandomElement(players);

		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();

		world.playSound(bossLoc, Sound.BLOCK_BELL_RESONATE, 0.7f, 1.5f);
		world.playSound(bossLoc, Sound.BLOCK_BELL_RESONATE, 0.8f, 0.75f);
		world.playSound(bossLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.6f);
		world.playSound(bossLoc, Sound.ENTITY_BREEZE_INHALE, 1.2f, 0.8f);

		EntityUtils.selfRoot(mBoss, CHARGE_TIME);
		mBoss.setAI(false);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			int mTickThreshold = CHARGE_TIME;
			int mCastNum = 0;
			Location mBossLocation = bossLoc;
			Location mTpLocation = getTpLoc(mBossLocation, firstPlayer);

			@Override
			public void run() {
				mTicks++;
				if (mTicks >= mTickThreshold) {
					if (damageAction(mBossLocation, mTpLocation, mCastNum) && mCastNum <= MAX_CASTS) {
						EntityUtils.selfRoot(mBoss, RECAST_DELAY);
						mTicks = 0;
						mTickThreshold = RECAST_DELAY;

						mBossLocation = mBoss.getLocation();
						Player randomPlayer = FastUtils.getRandomElement(players);
						mTpLocation = getTpLoc(mBossLocation, randomPlayer);
					} else {
						this.cancel();
					}
					mCastNum++;
				} else if (mTicks % 5 == 0) {
					EntityUtils.setDirection(LocationUtils.getDirectionTo(mTpLocation, mBossLocation), mBoss);
					telegraph(mBossLocation, mTpLocation, (float) mTicks / mTickThreshold);
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				mBoss.setAI(true);
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private Location getTpLoc(Location bossLoc, Player player) {
		Location pLoc = Aurora.withSurfaceY(player.getLocation(), mCenter);
		Vector dir = LocationUtils.getDirectionTo(pLoc, bossLoc);
		double overshoot = LocationUtils.rayLengthToSphereSurface(mCenter.toVector(), pLoc.toVector(), dir, Aurora.ARENA_RADIUS - 1);
		pLoc.add(dir.multiply(Math.min(MAX_OVERSHOOT, overshoot)));
		return Aurora.withSurfaceY(pLoc, mCenter);
	}

	@Override
	public void cancel() {
		EntityUtils.cancelSelfRoot(mBoss);
		super.cancel();
	}

	private void telegraph(Location bossLocation, Location playerLocation, float percentComplete) {
		World world = mBoss.getWorld();

		Location bossCenter = bossLocation.clone().add(0, 1, 0);
		Location playerCenter = playerLocation.clone().add(0, 1, 0);
		new PPLine(Particle.FLAME, bossCenter, playerCenter)
			.countPerMeter(3)
			.delta(LINE_RADIUS / 2)
			.spawnAsBoss();

		new PPLine(Particle.TRIAL_SPAWNER_DETECTION, bossCenter, playerCenter)
			.countPerMeter(3)
			.delta(LINE_RADIUS / 2)
			.spawnAsBoss();

		new PPLine(Particle.FIREWORKS_SPARK, bossCenter, playerCenter)
			.countPerMeter(3)
			.delta(LINE_RADIUS / 4)
			.extra(0.1)
			.spawnAsBoss();

		new PPCircle(Particle.TRIAL_SPAWNER_DETECTION, bossLocation.clone().add(0, 0.15, 0), BOSS_RADIUS)
			.count(30)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1, 0, 0.5)
			.extra(-0.1)
			.spawnAsBoss();
		new PPCircle(Particle.TRIAL_SPAWNER_DETECTION, playerLocation.clone().add(0, 0.15, 0), PLAYER_RADIUS - 1)
			.count(30)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1, 0, 0.5)
			.extra(0.1)
			.spawnAsBoss();

		world.playSound(bossLocation, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 1.2f, 0.5f + percentComplete);

	}

	// return true if any player hit
	private boolean damageAction(Location bossLocation, Location playerLocation, int extraCasts) {
		new PartialParticle(Particle.FLASH, playerLocation).minimumCount(1).spawnAsBoss();

		for (int i = 0; i < 5; i++) {
			playerLocation.setPitch(FastUtils.randomFloatInRange(-90, 90));
			ParticleUtils.drawCleaveArc(
				playerLocation,
				PLAYER_RADIUS,
				FastUtils.randomDoubleInRange(0, 360),
				-90,
				270,
				1,
				0,
				0,
				1,
				40,
				(loc, rings, angleProgress) ->
					new PartialParticle(Particle.REDSTONE, loc)
						.data(new Particle.DustOptions(DUST_COLOR, 1.3f))
						.spawnAsBoss());
		}

		Location lineStart = bossLocation.clone().add(0, 1, 0);
		Location lineEnd = playerLocation.clone().add(0, 1, 0);

		new PPLine(Particle.DUST_COLOR_TRANSITION, lineStart, lineEnd)
			.countPerMeter(10)
			.delta(LINE_RADIUS / 2)
			.data(new Particle.DustTransition(Color.WHITE, DUST_COLOR, 1.6f))
			.spawnAsBoss();

		new PPLine(Particle.CRIT, lineStart, lineEnd)
			.countPerMeter(6)
			.delta(LINE_RADIUS / 2)
			.deltaVariance(false, true, false)
			.extraRange(0.05, 0.15)
			.spawnAsBoss();

		new PPCircle(Particle.FLAME, bossLocation.clone().add(0, 0.15, 0), BOSS_RADIUS - 1)
			.count(40)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1, 0, 0.5)
			.extra(-0.2 * BOSS_RADIUS)
			.spawnAsBoss();

		new PartialParticle(Particle.FLASH, lineStart).minimumCount(1).spawnAsBoss();

		new PPCircle(Particle.FLAME, playerLocation.clone().add(0, 0.15, 0), PLAYER_RADIUS - 1)
			.count(30)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1, 0, 0.5)
			.extra(0.2)
			.spawnAsBoss();

		new PartialParticle(Particle.FLASH, lineEnd).minimumCount(1).spawnAsBoss();

		World world = mBoss.getWorld();
		world.playSound(playerLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.2f);
		world.playSound(playerLocation, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.6f, 0.5f);
		world.playSound(playerLocation, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 0.5f);

		Hitbox bossHitbox = new Hitbox.UprightCylinderHitbox(bossLocation, BOSS_RADIUS, BOSS_RADIUS);
		Hitbox playerHitbox = new Hitbox.UprightCylinderHitbox(playerLocation, PLAYER_RADIUS, PLAYER_RADIUS);
		Hitbox lineHitbox = Hitbox.approximateCylinder(lineStart, lineEnd, LINE_RADIUS, true);
		Set<Player> hitPlayers = new HashSet<>();
		hitPlayers.addAll(bossHitbox.getHitPlayers(true));
		hitPlayers.addAll(playerHitbox.getHitPlayers(true));
		hitPlayers.addAll(lineHitbox.getHitPlayers(true));

		hitPlayers.forEach(player ->
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE + extraCasts * DAMAGE_ADD, null, true, false, SPELL_NAME)
		);

		mBoss.teleport(playerLocation);

		return !hitPlayers.isEmpty();
	}

	@Override
	public int cooldownTicks() {
		return CHARGE_TIME + RECAST_DELAY + Aurora.SPELL_INTERVAL;
	}
}
