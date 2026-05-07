package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellMeteorRain extends Spell {
	private static final String SPELL_NAME = "Meteor Rain";
	private static final double RADIUS = 3;
	private static final double HEIGHT = 8;
	private static final int DAMAGE = 40;
	private static final DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.BLAST;
	private static final int STAR_COUNT = 3;
	private static final int STAR_DURATION = 30;
	private static final int DURATION = STAR_DURATION * STAR_COUNT;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private boolean mIsRunning = false;

	public SpellMeteorRain(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
	}

	@Override
	public boolean isRunning() {
		return mIsRunning;
	}

	@Override
	public void run() {
		mIsRunning = true;

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				Aurora.playersInRange(mCenter).forEach(player -> summonStar(Aurora.withSurfaceY(player.getLocation(), mCenter)));
				mTicks++;
				if (mTicks >= STAR_COUNT) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, STAR_DURATION);
	}

	public void summonStar(Location location) {
		Location loc = Aurora.withSurfaceY(location, mCenter);
		mBoss.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RETURN, 1.2f, 0.8f);

		new BukkitRunnable() {
			private int mTicks = 0;
			private final World mBossWorld = mBoss.getWorld();

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					new PPCircle(Particle.FLAME, loc.clone().add(0, 0.15, 0), RADIUS)
						.count(20)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(1, 0, mTicks == 20 ? 0.5 : -0.5)
						.extra(3)
						.spawnAsBoss();
					new PPCircle(Particle.SPELL_WITCH, loc.clone().add(0, 0.15, 0), RADIUS)
						.count(10)
						.delta(0.3, 0, 0.3)
						.spawnAsBoss();
				}
				if (mTicks % 10 == 0) {
					new PPCircle(Particle.SMOKE_NORMAL, loc.clone().add(0, 0.15, 0), RADIUS)
						.count(12)
						.spawnAsBoss();
				}
				if (mTicks >= STAR_DURATION - 10) {
					mBossWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.2f);
				}
				if (mTicks == STAR_DURATION - 10) {
					new PPLine(Particle.SMOKE_LARGE, loc.clone().add(0, 20, 0), loc)
						.countPerMeter(3)
						.delay(10)
						.extra(0.05)
						.spawnAsBoss();
				}
				if (mTicks >= STAR_DURATION) {
					// starfall
					new PPCircle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 0.15, 0), 0.5)
						.count(20)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(0.15, 0, 0)
						.extra(RADIUS)
						.spawnAsBoss();
					new PartialParticle(Particle.EXPLOSION_LARGE, loc).minimumCount(1).spawnAsBoss();

					mBossWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 0.8f);
					mBossWorld.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 0.9f, 1.0f);
					mBossWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.0f);

					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(loc, HEIGHT, RADIUS);
					hitbox.getHitPlayers(true).forEach(player -> {
						BossUtils.blockableDamage(mBoss, player, DAMAGE_TYPE, DAMAGE, SPELL_NAME, loc);
						MovementUtils.knockAway(loc, player, 0.5f, 0.65f);
					});

					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return DURATION + Aurora.SPELL_INTERVAL / 2;
	}
}
