package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.bosses.aurora.AuroraArrowBoss;
import com.playmonumenta.plugins.bosses.bosses.aurora.AuroraBladeBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.VectorUtils;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellAuroraMinis extends Spell {
	@FunctionalInterface
	private interface BossSummoner {
		BossAbilityGroup summon(Plugin plugin, LivingEntity boss, Location center, Aurora.BlockDestroyer blockDestroyer);
	}

	private static final List<Pair<String, BossSummoner>> SOUL_IDS = List.of(Pair.of("BladeoftheStars", AuroraBladeBoss::new), Pair.of("ArrowoftheStars", AuroraArrowBoss::new));
	private static final int SPAWN_TIME = 20;
	private static final int CHECK_INTERVAL = 20;
	private static final double BASE_MAX_HEALTH = 2500;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final double mRage;
	private final boolean mDoubleBoss;
	private final Aurora.BlockDestroyer mBlockDestroyer;

	private final List<Entity> mCurrentBosses = new ArrayList<>();
	private Runnable mOnFinish = () -> {
	};

	public SpellAuroraMinis(Plugin plugin, LivingEntity boss, Location center, double rage, boolean doubleBoss, Aurora.BlockDestroyer blockDestroyer) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRage = rage;
		mDoubleBoss = doubleBoss;
		mBlockDestroyer = blockDestroyer;
	}

	@Override
	public void run() {
		MMLog.severe("SpellAuroraMinis is not supposed to be cast!");
		Thread.dumpStack();
	}

	public void summonMinibosses(Location tentacleOrigin, Runnable onFinish) {
		mOnFinish = onFinish;
		Location raisedCenter = mCenter.clone().add(0, 3, 0);

		if (mDoubleBoss) {
			spawnBoss(tentacleOrigin, raisedCenter.clone().subtract(6, 0, 0), SOUL_IDS.getFirst());
			spawnBoss(tentacleOrigin, raisedCenter.clone().add(6, 0, 0), SOUL_IDS.getLast());
		} else {
			spawnBoss(tentacleOrigin, raisedCenter, FastUtils.getRandomElement(SOUL_IDS));
		}

		mActiveTasks.add(new BukkitRunnable() {
			private final World mWorld = mBoss.getWorld();
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= SPAWN_TIME) {
					mWorld.playSound(raisedCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.0f, 0.6f);
					mWorld.playSound(raisedCenter, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 1.8f, 1.0f);
					mWorld.playSound(raisedCenter, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.4f, 0.5f);
					this.cancel();
				}

				mWorld.playSound(raisedCenter, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 2.5f, 1.5f * mTicks / SPAWN_TIME);
				mWorld.playSound(raisedCenter, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1.3f, 1.5f * mTicks / SPAWN_TIME);
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));

		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				mCurrentBosses.removeIf(entity -> !entity.isValid());

				if (mCurrentBosses.isEmpty()) {
					this.cancel();

					World world = mBoss.getWorld();
					Location skyCenter = mCenter.clone().add(0, 32, 0);
					world.playSound(skyCenter, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 9.0f, 0.8f);
					world.playSound(skyCenter, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.HOSTILE, 9.0f, 1.0f, 500);
					world.playSound(skyCenter, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.HOSTILE, 9.0f, 1.0f, 500);
					world.playSound(skyCenter, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.HOSTILE, 9.0f, 1.0f, 500);
					world.playSound(skyCenter, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.HOSTILE, 9.0f, 1.5f, 500);
					world.playSound(skyCenter, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.HOSTILE, 9.0f, 1.5f, 500);

					Aurora.playersInRange(mCenter, true).forEach(player -> {
						mPlugin.mPotionManager.addPotion(player, PotionManager.PotionID.BOSS, new PotionEffect(PotionEffectType.BLINDNESS, 20, 9, true, false));
						player.sendMessage(Component.text("You hear a noise that your mind fails to properly comprehend. Pain flashes through your head.", NamedTextColor.GRAY));
						player.sendMessage(Component.text("You reach up and find blood dripping from your ears, your nose, your eyes... Is that Aurora?", NamedTextColor.GRAY));
					});

					Bukkit.getScheduler().runTaskLater(mPlugin, mOnFinish, 30);
				}
			}
		}.runTaskTimer(mPlugin, SPAWN_TIME + CHECK_INTERVAL, CHECK_INTERVAL));
	}

	private void spawnBoss(Location tentacleOrigin, Location spawnLoc, Pair<String, BossSummoner> firstBoss) {
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= SPAWN_TIME) {
					@Nullable Entity summon = LibraryOfSoulsIntegration.summon(spawnLoc, firstBoss.first());
					if (summon == null) {
						MMLog.severe(String.format("Aurora: Soul '%s' failed to spawn", firstBoss.first()));
						return;
					}
					if (!(summon instanceof LivingEntity livingSummon)) {
						MMLog.severe(String.format("Aurora: Soul '%s' not a LivingEntity!", firstBoss.first()));
						return;
					}
					BossAbilityGroup ability = firstBoss.second().summon(mPlugin, livingSummon, mCenter, mBlockDestroyer);
					mCurrentBosses.add(livingSummon);
					GlowingManager.startGlowing(livingSummon, NamedTextColor.GOLD, -1, GlowingManager.BOSS_SPELL_PRIORITY);
					Aurora.bossRageBuff(livingSummon, BASE_MAX_HEALTH, mRage, Aurora.playersInRange(mCenter).size());
					BossManager.getInstance().manuallyRegisterBoss(livingSummon, ability);

					new PartialParticle(Particle.CLOUD, spawnLoc).count(22).extra(0.5).spawnAsBoss();

					new PPSpiral(Particle.SCULK_SOUL, spawnLoc, 3).count(80).delta(0.2).ticks(6).spawnAsBoss();

					this.cancel();
					return;
				}

				Location middleLoc = spawnLoc.clone().add(VectorUtils.randomUnitVector().multiply(5));
				new PPBezier(Particle.SCULK_CHARGE_POP, tentacleOrigin, middleLoc, spawnLoc).count(100).delta(0.02).delay(10).spawnAsBoss();

				new PartialParticle(Particle.SONIC_BOOM, spawnLoc).minimumCount(1).spawnAsBoss();

				mTicks += 2;
			}

		}.runTaskTimer(mPlugin, 0, 2));
	}

	@Override
	public void cancel() {
		super.cancel();

		mCurrentBosses.clear();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
