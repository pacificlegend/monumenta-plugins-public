package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class WindUpCar extends Ability {
	private static final String SCOREBOARD = "WindUpCar";
	private static final int POINT_COST = 3;
	private static final String EFFECT_SOURCE = "WindUpCar";
	private static final double SPEED_BOOST = 0.10;
	private static final double SPEED_CAP = 0.50;
	private static final int TIME_INTERVAL = 60 * 20;

	public static final AbilityInfo<WindUpCar> INFO =
		new SnowPerkGui.SnowPerkInfo<>(WindUpCar.class, "Wind-Up Car", WindUpCar::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.POWERED_RAIL)
			.description(getDescription())
			.remove(player -> Plugin.getInstance().mEffectManager.clearEffects(player, EFFECT_SOURCE));

	private final BukkitTask mSpeedTimer;

	public WindUpCar(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mSpeedTimer = new BukkitRunnable() {
			int mMinutes = 0;

			@Override
			public void run() {
				mMinutes++;

				double speedBoost = Math.min(mMinutes * SPEED_BOOST, SPEED_CAP);

				mPlugin.mEffectManager.clearEffects(mPlayer, EFFECT_SOURCE);
				mPlugin.mEffectManager.addEffect(mPlayer, EFFECT_SOURCE, new PercentSpeed(TIME_INTERVAL, speedBoost, "WindUpCarSpeed").displaysTime(false));

				if (mMinutes <= 5) {
					speedupFX(mMinutes);
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mPlugin.mEffectManager.clearEffects(mPlayer, EFFECT_SOURCE);
			}
		}.runTaskTimer(plugin, TIME_INTERVAL, TIME_INTERVAL);

		cancelOnDeath(mSpeedTimer);
	}

	// Does more effects based on how long has passed/how fast you are
	private void speedupFX(int minutes) {
		double speedBoost = Math.min(minutes * SPEED_BOOST, SPEED_CAP);
		mPlayer.sendMessage(MessagingUtils.fromMiniMessage("<#74D2D2>[Wind-Up Car]<gray> Speed increased to <white>%s<gray>!".formatted(StringUtils.multiplierToPercentageWithSign(speedBoost))));

		Location loc = mPlayer.getLocation().add(0, 0.25, 0);
		loc.setPitch(0);

		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 6, 0.14f, true, 0, 0, Particle.END_ROD);
		if (minutes >= 2) {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 6, 0.18f, true, 0, 0, Particle.END_ROD);
		}
		if (minutes >= 3) {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 6, 0.22f, true, 30, 0, Particle.END_ROD);
		}
		if (minutes >= 4) {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 6, 0.27f, true, 0, 0, Particle.END_ROD);
		}
		if (minutes >= 5) {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 6, 0.34f, true, 0, 0, Particle.END_ROD);
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, 1, 0, 0, 6, 0.29f, true, 30, 0, Particle.END_ROD);
		}

		int maxTime = minutes * 3 + 4;
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0 -> mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.7f, 1.0f);
					case 3 -> mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.7f, 1.167f);
					case 6 -> mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.7f, 1.333f);
					case 9 -> mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.7f, 1.5f);
					case 12 -> mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.7f, 1.667f);
					case 15 -> mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.7f, 1.833f);
					case 18 -> mPlayer.playSound(mPlayer, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.7f, 2.0f);
					default -> { }
				}
				if (minutes == 5 && mTicks == 18) {
					mPlayer.playSound(mPlayer, Sound.BLOCK_PISTON_EXTEND, SoundCategory.PLAYERS, 1f, 1.5f);
				}

				double radius = 2 - (mTicks * 0.025);
				double theta = mTicks * 0.75;
				Location particleLoc = mPlayer.getLocation().add(radius * FastUtils.cos(theta), mTicks * 0.12, radius * FastUtils.sin(theta));
				new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION, particleLoc, 4).delta(0.15, 0.05, 0.15).spawnAsPlayerPassive(mPlayer);

				mTicks++;
				if (mTicks >= maxTime) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public void invalidate() {
		if (mSpeedTimer != null) {
			mSpeedTimer.cancel();
		}
	}

	public static Description<WindUpCar> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Gain +%p *Speed* for every %t").statValues(stat(SPEED_BOOST), stat(TIME_INTERVAL)).styles(DescriptionUtils.WHITE)
			.addLine("that have elapsed from the start.")
			.addLine("(Max +%p Speed)").statValues(stat(SPEED_CAP))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
