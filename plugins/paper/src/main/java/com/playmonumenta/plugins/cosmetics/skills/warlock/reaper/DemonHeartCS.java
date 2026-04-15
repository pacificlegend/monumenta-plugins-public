package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.IntruderCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DemonHeartCS extends DarkPactCS implements IntruderCS {
	public static final String NAME = "Demon Heart";
	private static final int TWIST_ITERATIONS = 4;
	private static final Color TWIST_COLOR_BASE = Color.fromRGB(0x6b0000);
	private static final Color TWIST_COLOR_TIP = Color.RED;

	@Override
	public Material getDisplayItem() {
		return Material.TINTED_GLASS;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"They did not destroy the nations.",
			"But were mingled among the heathen, and learned their works.",
			"And they served their idols."
		);
	}

	@Override
	public void onCast(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 1.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_4, SoundCategory.PLAYERS, 0.55f, 0.75f);
		world.playSound(loc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.PLAYERS, 3.0f, 1.8f, -10000);

		double yaw = Math.toRadians(player.getYaw() + 45);
		for (int i = 0; i < 4; i++) {
			Vector offset = new Vector(FastUtils.cos(yaw + i * Math.PI / 2) * 3, 0, FastUtils.sin(yaw + i * Math.PI / 2) * 3);
			spawnTendril(loc.clone().add(offset), 1.2f, player);
		}

		new BukkitRunnable() {
			double mRadius = 0.5;

			@Override
			public void run() {
				new PPCircle(Particle.SQUID_INK, loc.clone().add(0, 0.2, 0), mRadius)
					.countPerMeter(4)
					.delta(1, 0, 0)
					.rotateDelta(true)
					.directionalMode(true)
					.extra(1 - mRadius / 8)
					.spawnAsPlayerActive(player);

				mRadius += 1;
				if (mRadius >= 6) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);
	}

	@Override
	public void tick(Player player, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location pLoc = player.getLocation();
		if (twoHertz) {
			new PartialParticle(Particle.DUST_COLOR_TRANSITION, pLoc.clone().add(0, 0.2, 0))
				.count(16)
				.delta(0.8, 0.1, 0.8)
				.data(new Particle.DustTransition(Color.RED, Color.BLACK, 2.0f))
				.spawnAsPlayerPassive(player);
		} else {
			new PartialParticle(Particle.DUST_COLOR_TRANSITION, pLoc.clone().add(0, 0.2, 0))
				.count(8)
				.delta(0.5, 0.2, 0.5)
				.data(new Particle.DustTransition(Color.RED, Color.BLACK, 1.4f))
				.spawnAsPlayerPassive(player);
		}

		if (oneHertz) {
			spawnTendril(LocationUtils.randomLocationInDonut(pLoc, 3, 5), 0.6f, player);
		}
	}

	@Override
	public void onKill(Player player, LivingEntity mob) {
		spawnTendril(mob.getLocation(), 1.0f, player);
	}

	@Override
	public void loseEffect(Player player) {
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.6f, 0.8f);
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.6f);
	}

	private void spawnTendril(Location loc, float heightMult, Player player) {
		Location to = loc.clone().add(0, 8, 0);

		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;

			private final int mDuration = FastUtils.RANDOM.nextInt(7, 11);

			final double mXMult = FastUtils.randomDoubleInRange(-1, 1);
			final double mZMult = FastUtils.randomDoubleInRange(-1, 1);
			double mJ = 0;

			@Override
			public void run() {
				mT++;

				for (int i = 0; i < TWIST_ITERATIONS; i++) {
					mJ++;
					float size = 0.7f + (1.7f * (1f - (float) (mJ / (TWIST_ITERATIONS * mDuration))));
					double offset = 0.1 * (1f - (mJ / (TWIST_ITERATIONS * mDuration)));
					double transition = mJ / (TWIST_ITERATIONS * mDuration);
					double pi = (Math.PI * 2) * (1f - (mJ / (TWIST_ITERATIONS * mDuration)));

					Vector vec = new Vector(mXMult * FastUtils.cos(pi), 0,
						mZMult * FastUtils.sin(pi));
					Location tendrilLoc = mL.clone().add(vec);

					new PartialParticle(Particle.REDSTONE, tendrilLoc, 3, offset, offset, offset, 0, new Particle.DustOptions(
						ParticleUtils.getTransition(TWIST_COLOR_TIP, TWIST_COLOR_BASE, transition), size * heightMult))
						.spawnAsPlayerActive(player);

					mL.add(0, 0.2 * heightMult, 0);
					if (mL.distance(to) < 0.4) {
						this.cancel();
						return;
					}
				}

				if (mT >= mDuration) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
