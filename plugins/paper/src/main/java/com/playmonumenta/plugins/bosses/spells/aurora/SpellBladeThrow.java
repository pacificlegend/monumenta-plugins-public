package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellBladeThrow extends Spell {
	private static final int DELAY = 2 * 20;
	private static final double SPEED = 0.96;
	private static final double HITBOX_SIZE = 1.1;
	private static final String SPELL_NAME = "Blade Throw";
	private static final int DAMAGE = 50;
	private static final int VULNERABILITY_DURATION = 4 * 20;
	private static final double VULNERABILITY_AMOUNT = 0.4;
	private static final int COUNT = 24;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;

	public SpellBladeThrow(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();
		world.playSound(bossLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 2.0f, 1.8f);
		world.playSound(bossLoc, Sound.ENTITY_VINDICATOR_CELEBRATE, SoundCategory.HOSTILE, 2.0f, 0.8f);
		world.playSound(bossLoc, Sound.ENTITY_PILLAGER_HURT, SoundCategory.HOSTILE, 2.0f, 0.8f);
		world.playSound(bossLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 2.5f, 0.6f);

		EntityUtils.selfRoot(mBoss, DELAY + COUNT);
		mPlugin.mEffectManager.addEffect(mBoss, PercentKnockbackResist.GENERIC_NAME, new PercentKnockbackResist(DELAY + COUNT, 1.0, PercentKnockbackResist.GENERIC_NAME));

		mActiveTasks.add(new BukkitRunnable() {
			private Vector mDirection = VectorUtils.randomHorizontalUnitVector();
			int mTicks = 0;

			@Override
			public void run() {
				throwDagger(mDirection.clone());
				mDirection = mDirection.rotateAroundY(2 * Math.PI / COUNT);
				mTicks++;
				if (mTicks >= COUNT) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void throwDagger(Vector dir) {
		World world = mBoss.getWorld();

		Location startLoc = LocationUtils.getHalfHeightLocation(mBoss);
		Location endLoc = LocationUtils.rayTraceToBlock(startLoc, dir.clone(), Aurora.ARENA_RADIUS * 2, null);
		startLoc.setDirection(dir);

		int ticks = 2 * (int) (endLoc.clone().subtract(startLoc).length() / SPEED);

		new PPLine(Particle.REDSTONE, startLoc, endLoc)
			.countPerMeter(2)
			.data(new Particle.DustOptions(Color.RED, 1.4f))
			.delta(0.25)
			.delay(ticks)
			.spawnAsBoss();

		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.playSound(startLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2.5f, 1.2f);
			world.playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 2.0f, 0.7f);
			EntityUtils.setDirection(dir, mBoss);

			BukkitRunnable runnable = new BukkitRunnable() {
				private final Vector mVelocity = dir.clone().multiply(SPEED);
				private final ItemDisplay mDisplay = world.spawn(startLoc, ItemDisplay.class, display -> {
					ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
					ItemUtils.setPlainName(itemStack, "Blightblood Falchion");
					display.setItemStack(itemStack);
					// Look forwards
					display.setTransformation(new Transformation(
						new Vector3f(),
						new Quaternionf(0.27097052f, 0.6532104f, 0.27056867f, 0.6532104f),
						new Vector3f(2.5f),
						new Quaternionf()
					));

					EntityUtils.setRemoveEntityOnUnload(display);
				});
				private final PPPeriodic mSweepParticle = new PPPeriodic(Particle.SWEEP_ATTACK, startLoc).delta(0.25);
				private Location mCurrentLocation = startLoc.clone();
				private int mTicks = 0;

				@Override
				public void run() {
					mSweepParticle.location(mCurrentLocation).spawnAsBoss();

					Location prevLoc = mCurrentLocation.clone();
					mCurrentLocation = mCurrentLocation.add(mVelocity);

					if (mTicks > 50 || prevLoc.getBlock().isSolid() || mCurrentLocation.getBlock().isSolid()) {
						this.cancel();
						return;
					}

					Hitbox.approximateCylinder(prevLoc, mCurrentLocation, HITBOX_SIZE, false).getHitPlayers(true).forEach(player -> {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, SPELL_NAME, mCurrentLocation.clone().subtract(mVelocity));
						EffectManager.getInstance().addEffect(player, "DaggerThrowVulnerability", new PercentDamageReceived(VULNERABILITY_DURATION, VULNERABILITY_AMOUNT));
						Location bossLoc = mBoss.getLocation();

						world.playSound(bossLoc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 2.0f, 0.1f);
						world.playSound(bossLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
						world.playSound(bossLoc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 2.0f, 0.1f);

						new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION, bossLoc)
							.count(75)
							.delta(0.3, 0.6, 0.3)
							.spawnAsBoss();
					});


					// prevent blade from looking like its behind the boss
					mDisplay.setTeleportDuration(2);
					mDisplay.teleport(mCurrentLocation);
					mTicks++;
				}

				@Override
				public synchronized void cancel() throws IllegalStateException {
					mDisplay.remove();
					super.cancel();
				}
			};
			mActiveRunnables.add(runnable);
			runnable.runTaskTimer(mPlugin, 0, 1);
		}, DELAY));
	}

	@Override
	public int cooldownTicks() {
		return 6 * 20;
	}
}
