package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellGalacticThunderstorm extends Spell {
	private static final int TELEGRAPH_DURATION = 2 * 20;
	private static final int MARGIN = 3;
	private static final double HITBOX_SIZE = 0.6;
	private static final int DAMAGE = 36;
	private static final int BLIND_DURATION = 2 * 20;
	private static final String SPELL_NAME = "Galactic Thunderstorm";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;

	public SpellGalacticThunderstorm(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
	}

	@Override
	public void run() {
		summonBatch();
		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, this::summonBatch, TELEGRAPH_DURATION + 10));
	}

	private void summonBatch() {
		List<Player> players = Aurora.playersInRange(mCenter);
		if (players.isEmpty()) {
			return;
		}
		Collections.shuffle(players);

		List<Location> points = new ArrayList<>();
		for (int i = 0; i < 2; i++) { // sets 2 locations per iteration
			Location loc1 = Aurora.getRandomArenaLocation(mCenter, MARGIN).add(0, 1, 0);
			Location loc2;
			if (i < players.size()) {
				loc2 = Aurora.withSurfaceY(players.get(i).getLocation(), mCenter);
			} else {
				loc2 = Aurora.getRandomArenaLocation(mCenter, MARGIN);
			}

			Vector dir = LocationUtils.getHorizontalDirectionTo(loc2, loc1);
			double extraLength = LocationUtils.rayLengthToSphereSurface(mCenter.toVector(), loc2.toVector(), dir, Aurora.ARENA_RADIUS);
			loc2 = Aurora.withSurfaceY(loc2.add(dir.multiply(extraLength)), mCenter).add(0, 1, 0);

			points.add(loc1);
			points.add(loc2);
		}
		World world = mBoss.getWorld();

		points.forEach(location -> {
			world.playSound(location, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1.4f, 1.8f);
			world.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1.5f, 0.6f);
			world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.HOSTILE, 0.9f, 0.5f);
			world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1.6f, 0.5f);
		});

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= TELEGRAPH_DURATION) {
					points.forEach(pointLoc -> {
						world.playSound(pointLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 2.6f, 0.5f);
						world.playSound(pointLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 2.6f, 1.5f);
						world.playSound(pointLoc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 1.6f, 1.5f);
						world.playSound(pointLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3.0f, 0.4f);
						world.playSound(pointLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 2.0f, 0.4f);
						world.playSound(pointLoc, Sound.ITEM_HOE_TILL, SoundCategory.HOSTILE, 2.0f, 0.8f);

						new PartialParticle(Particle.CRIT_MAGIC, pointLoc)
							.count(26)
							.extra(0.5)
							.spawnAsBoss();

						new PartialParticle(Particle.EXPLOSION_LARGE, pointLoc)
							.count(4)
							.extra(0.25)
							.spawnAsBoss();

						new PartialParticle(Particle.FLASH, pointLoc).minimumCount(1).spawnAsBoss();

						Hitbox.unionOf(points.stream()
							.filter(other -> !other.equals(pointLoc))
							.map(otherLoc -> {
								new PPLine(Particle.END_ROD, pointLoc, otherLoc)
									.countPerMeter(3)
									.distanceFalloff(44)
									.delta(0.01)
									.spawnAsBoss();

								new PPLine(Particle.FIREWORKS_SPARK, pointLoc, otherLoc)
									.countPerMeter(1)
									.distanceFalloff(44)
									.extra(0.02)
									.spawnAsBoss();

								return Hitbox.approximateCylinder(pointLoc, otherLoc, HITBOX_SIZE, false);
							}).toList()
						).getHitPlayers(true).forEach(player -> {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, false, SPELL_NAME);
							player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, BLIND_DURATION, 9));
						});
					});
					this.cancel();
					return;
				}
				points.forEach(pointLoc -> {
					if (mTicks % 5 == 0) {
						world.playSound(pointLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 5.0f, 0.5f + 1.25f * mTicks / TELEGRAPH_DURATION);
					}
					new PartialParticle(Particle.SMOKE_LARGE, pointLoc)
						.count(5)
						.delta(0.2, 0.5, 0.2)
						.extra(0.03)
						.spawnAsBoss();

					new PartialParticle(Particle.END_ROD, pointLoc).spawnAsBoss();

					if (mTicks % 5 == 0) {
						points.stream()
							.filter(other -> !other.equals(pointLoc))
							.forEach(otherLoc -> {
								new PPLine(Particle.REDSTONE, pointLoc, otherLoc)
									.data(new Particle.DustOptions(Color.fromRGB(0xfffa5a), 1.5f))
									.distanceFalloff(32)
									.countPerMeter(2)
									.delta(0.0, 0.2, 0)
									.spawnAsBoss();
							});
					}
				});
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public int cooldownTicks() {
		return 2 * TELEGRAPH_DURATION + Aurora.SPELL_INTERVAL / 4;
	}
}
