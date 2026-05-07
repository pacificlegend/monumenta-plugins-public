package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.WingedBoss;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellStarShower extends Spell {
	private static final String SPELL_NAME = "Star Shower";
	private static final String SPELL_NAME_BLOCK_BREAK = "Star Shower (✶)";
	private static final double RADIUS = 2.5;
	private static final double BIG_RADIUS = 3;
	private static final double HEIGHT = 8;
	private static final int DAMAGE = 40;
	private static final DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.BLAST;
	private static final int BIG_DAMAGE = 56;
	private static final float KNOCKBACK = 0.8f;
	private static final int STAR_INTERVAL = 30;
	private static final int STAR_DURATION = 30;
	private static final int BIG_STAR_DURATION = 3 * 20;
	private static final int CHARGE_TIME = 2 * 20;
	private static final int DURATION = 6 * 20;
	public static final int COMPLETE_DURATION = CHARGE_TIME + DURATION + BIG_STAR_DURATION;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Consumer<Integer> mOnBreak;
	private final Aurora.BlockDestroyer mBlockDestroyer;
	private final SpellCelestialPillars mPillar;

	private final ChargeUpManager mChargeUpManager;

	private int mPillarsBroken = 0;
	private boolean mIsRunning = false;

	public SpellStarShower(Plugin plugin, LivingEntity boss, Location center, SpellCelestialPillars pillar, Consumer<Integer> onBreak, Aurora.BlockDestroyer blockDestroyer) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mOnBreak = onBreak;
		mBlockDestroyer = blockDestroyer;
		mChargeUpManager = new ChargeUpManager(boss,
			CHARGE_TIME,
			Component.text(SPELL_NAME, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
			BossBar.Color.PINK,
			BossBar.Overlay.PROGRESS,
			Aurora.DETECTION_RANGE
		);
		mPillar = pillar;
	}

	@Override
	public boolean isRunning() {
		return mIsRunning;
	}

	@Override
	public void run() {
		mPillarsBroken = 0;
		mIsRunning = true;

		mPillar.glow(COMPLETE_DURATION);
		mPlugin.mBossManager.createBossInternal(mBoss, new WingedBoss(mPlugin, mBoss));

		mChargeUpManager.setTime(0);
		mChargeUpManager.setChargeTime(CHARGE_TIME);
		mChargeUpManager.setTitle(Component.text("Preparing ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME, NamedTextColor.LIGHT_PURPLE)));
		World world = mBoss.getWorld();

		new PPSpiral(Particle.WAX_OFF, mBoss.getLocation(), 8)
			.count(250)
			.delta(0, 1, 0)
			.directionalMode(true)
			.extra(5)
			.curveAngle(300)
			.reversed(true)
			.ticks(CHARGE_TIME)
			.spawnAsBoss();

		new PPSpiral(Particle.SPELL_WITCH, mBoss.getLocation(), 8)
			.count(125)
			.delta(0, 1, 0)
			.directionalMode(true)
			.extra(1.5)
			.curveAngle(-300)
			.reversed(true)
			.ticks(CHARGE_TIME)
			.spawnAsBoss();

		BukkitRunnable runnable = new BukkitRunnable() {
			boolean mStarted = false;

			@Override
			public void run() {
				Location bossLoc = mBoss.getLocation();
				if (!mStarted) {
					world.playSound(bossLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 5.0f, 0.5f + (float) mChargeUpManager.getTime() / DURATION);

					if (mChargeUpManager.nextTick()) {
						mChargeUpManager.setTime(DURATION);
						mChargeUpManager.setChargeTime(DURATION);
						mChargeUpManager.setTitle(Component.text("Preparing ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME_BLOCK_BREAK, NamedTextColor.LIGHT_PURPLE)));
						mStarted = true;
					}
				} else {
					if (mChargeUpManager.previousTick()) {
						Aurora.playersInRange(bossLoc).forEach(player -> summonBigStar(player.getLocation()));

						mChargeUpManager.setTime(BIG_STAR_DURATION);
						mChargeUpManager.setChargeTime(BIG_STAR_DURATION);
						mChargeUpManager.setTitle(Component.text("Unleashing ", NamedTextColor.WHITE).append(Component.text(SPELL_NAME_BLOCK_BREAK, NamedTextColor.RED)));
						this.cancel();

						new BukkitRunnable() {
							@Override
							public void run() {
								if (mChargeUpManager.previousTick()) {
									mChargeUpManager.remove();
									mOnBreak.accept(mPillarsBroken);
									mPillarsBroken = 0;

									this.cancel();
								}

							}
						}.runTaskTimer(mPlugin, 1, 1);
						// delay to make sure pillars are broken first
					} else if ((mChargeUpManager.getTime() + 1) % STAR_INTERVAL == 0) {
						Aurora.playersInRange(bossLoc).forEach(player -> summonStar(player.getLocation()));
					}
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				mPlugin.mBossManager.removeAbility(mBoss, WingedBoss.identityTag);
				mBoss.setGravity(true);
				mIsRunning = false;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	public void summonBigStar(Location location) {
		Location loc = Aurora.withSurfaceY(location, mCenter);

		mBoss.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.9f, 0.9f);
		new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.15, 0), BIG_RADIUS)
			.countPerMeter(2)
			.data(new Particle.DustOptions(Color.RED, 2.8f))
			.ringMode(false)
			.spawnAsBoss();

		new BukkitRunnable() {
			private int mTicks = 0;
			private final World mBossWorld = mBoss.getWorld();

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					new PPCircle(Particle.WAX_OFF, loc.clone().add(0, 0.15, 0), BIG_RADIUS)
						.count(30)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(1, 0, 0)
						.extra(4)
						.spawnAsBoss();
					new PPCircle(Particle.SPELL_INSTANT, loc.clone().add(0, 0.15, 0), BIG_RADIUS)
						.count(6)
						.rotateDelta(true)
						.directionalMode(true)
						.extra(0.1)
						.spawnAsBoss();
				}

				if (mTicks % 10 == 0) {
					new PPCircle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.15, 0), BIG_RADIUS)
						.count(12)
						.spawnAsBoss();
				}

				if (mTicks == BIG_STAR_DURATION - 20) {
					mBossWorld.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.7f, 0.1f);
					new PPLine(Particle.SMOKE_NORMAL, loc.clone().add(0, 20, 0), loc)
						.count(60)
						.delay(20)
						.delta(0.1)
						.extra(0.01)
						.spawnAsBoss();

					new PPLine(Particle.GUST, loc.clone().add(0, 20, 0), loc)
						.count(20)
						.delay(20)
						.spawnAsBoss();

					new PPLine(Particle.FLAME, loc.clone().add(0, 20, 0), loc)
						.count(40)
						.delay(20)
						.extra(0.1)
						.spawnAsBoss();
				}

				if (mTicks >= BIG_STAR_DURATION - 20) {
					mBossWorld.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.1f, 1.5f - (mTicks - BIG_STAR_DURATION + 20) / 20.0f);
					mBossWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.2f - (mTicks - BIG_STAR_DURATION + 20) / 20.0f);
				}

				if (mTicks >= BIG_STAR_DURATION) { // Explode!
					new PPCircle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 0.15, 0), 0.5)
						.count(25)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(0.15, 0, 0)
						.extra(BIG_RADIUS)
						.spawnAsBoss();
					new PPCircle(Particle.FLAME, loc.clone().add(0, 0.15, 0), 0.5)
						.count(30)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(0.15, 0, 0)
						.extra(BIG_RADIUS)
						.spawnAsBoss();
					new PPCircle(Particle.SMALL_FLAME, loc.clone().add(0, 0.15, 0), BIG_RADIUS)
						.count(20)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(1, 0, -0.5)
						.extra(-0.15)
						.spawnAsBoss();
					new PartialParticle(Particle.EXPLOSION_LARGE, loc).minimumCount(1).spawnAsBoss();
					new PartialParticle(Particle.FLASH, loc).minimumCount(1).spawnAsBoss();

					mBossWorld.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.8f, 0.3f);
					mBossWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.2f, 0.2f);
					mBossWorld.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 1.9f, 0.9f);
					mBossWorld.playSound(loc, Sound.ENTITY_BREEZE_DEATH, 1.5f, 0.4f);
					mBossWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.7f, 1.8f);

					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(loc, HEIGHT, RADIUS);
					hitbox.getHitPlayers(true).forEach(player -> {
						BossUtils.blockableDamage(mBoss, player, DAMAGE_TYPE, BIG_DAMAGE, SPELL_NAME, loc);
						MovementUtils.knockAway(loc, player, KNOCKBACK, KNOCKBACK, false);
					});

					Location sphereLoc = loc.clone().subtract(0, 1, 0);
					if (mPillar.checkAndDestroyPillarsInSphere(sphereLoc, BIG_RADIUS)) {
						mPillarsBroken++;
					} else {
						mBlockDestroyer.destroy(BlockUtils.getBlocksInSphere(sphereLoc, BIG_RADIUS));
					}
					this.cancel();
				}

				mTicks++;

			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void summonStar(Location location) {
		Location loc = Aurora.withSurfaceY(location, mCenter);
		mBoss.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.7f);

		new BukkitRunnable() {
			private int mTicks = 0;
			private final World mBossWorld = mBoss.getWorld();

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					new PPCircle(Particle.WAX_OFF, loc.clone().add(0, 0.15, 0), RADIUS)
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
					mBossWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.8f);
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
					mBossWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 2.0f);

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
		return COMPLETE_DURATION + Aurora.SPELL_INTERVAL;
	}
}
