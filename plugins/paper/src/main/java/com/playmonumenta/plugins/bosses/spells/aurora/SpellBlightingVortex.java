package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellBlightingVortex extends Spell {
	private static final String SPELL_NAME = "Blighting Vortex";
	private static final int INTERVAL = 5;
	private static final double RADIUS = 7;
	private static final int TELEGRAPH_DURATION = 2 * 20;
	private static final int DURATION = TELEGRAPH_DURATION + 6 * INTERVAL;
	private static final double DEBUFF = 0.05;
	private static final double MAX_DEBUFF = 0.30;
	private static final float PULL_VELOCITY = 0.3f;
	private static final String SOURCE = "BlightingVortexBlight";
	private static final int THROW_DURATION = 20;

	private final Plugin mPlugin;
	private final Location mCenter;
	private final LivingEntity mBoss;

	public SpellBlightingVortex(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mCenter = center;
		mBoss = boss;
	}

	@Override
	public void run() {
		List<Player> players = Aurora.playersInRange(mBoss.getLocation());
		if (players.isEmpty()) {
			return;
		}
		Player player = FastUtils.getRandomElement(players);
		Location loc = Aurora.withSurfaceY(player.getLocation(), mCenter).add(0, 1.25, 0);
		Location bossLoc = LocationUtils.getHalfHeightLocation(mBoss);
		Location displacement = loc.clone().subtract(bossLoc);
		World world = mBoss.getWorld();

		world.playSound(bossLoc, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 1.5f, 2.0f);
		world.playSound(bossLoc, Sound.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.HOSTILE, 1.7f, 1.2f);
		world.playSound(bossLoc, Sound.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.HOSTILE, 1.7f, 0.5f);

		new PPBezier(Particle.SCULK_SOUL, bossLoc, bossLoc.clone().add(displacement.clone().multiply(0.5).add(VectorUtils.randomUnitVector().multiply(3))), loc)
			.count((int) (displacement.length() * 8))
			.distanceFalloff(Aurora.ARENA_RADIUS * 2)
			.delta(0.2)
			.extra(0.1)
			.delay(THROW_DURATION)
			.spawnAsBoss();

		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> summonBomb(loc), THROW_DURATION));
	}

	private void summonBomb(Location bombLocation) {
		World world = mBoss.getWorld();

		world.playSound(bombLocation, Sound.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.HOSTILE, 1.7f, 0.1f);
		world.playSound(bombLocation, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 1.0f, 0.1f);
		world.playSound(bombLocation, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.8f, 1.5f);

		Location center = bombLocation.clone();
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks <= TELEGRAPH_DURATION) {
					world.playSound(bombLocation, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.6f, 0.6f);

					new PPCircle(Particle.SQUID_INK, center.clone().subtract(0, 0.5, 0), RADIUS)
						.countPerMeter(2)
						.spawnAsBoss();

					new PPCircle(Particle.CRIT, center, RADIUS)
						.count(120)
						.ringMode(false)
						.spawnAsBoss();

					new PartialParticle(Particle.SCULK_SOUL, center)
						.count(2)
						.delta(0.5)
						.spawnAsBoss();
				} else {
					world.playSound(bombLocation, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.HOSTILE, 1.5f, 0.1f);
					world.playSound(bombLocation, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 1.5f, 0.5f);

					new PPSpiral(Particle.DUST_COLOR_TRANSITION, center, RADIUS)
						.data(new Particle.DustTransition(Color.fromRGB(0x018E92), Color.fromRGB(0x0C1014), 1.4f))
						.countPerBlockPerCurve(10)
						.curves(2)
						.angleOffset(mTicks * 20)
						.ticks(2)
						.delta(0, 0.6, 0)
						.spawnAsBoss();

					new PartialParticle(Particle.SCULK_SOUL, center)
						.count(10)
						.delta(0.5)
						.spawnAsBoss();

					new PPCircle(Particle.SQUID_INK, center.clone().subtract(0, 0.5, 0), RADIUS)
						.countPerMeter(2)
						.rotateDelta(true)
						.directionalMode(true)
						.delta(-1, 0.0, 0.2)
						.extra(RADIUS * 0.05)
						.spawnAsBoss();

					new Hitbox.SphereHitbox(center, RADIUS).getHitPlayers(true).forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, 1, null, true, false, SPELL_NAME);
						MovementUtils.pullTowardsNormalized(center, player, PULL_VELOCITY, false);
						@Nullable
						Effect currentEffect = mPlugin.mEffectManager.getActiveEffect(player, SOURCE);
						double prevAmp = currentEffect == null ? 0 : -currentEffect.getMagnitude();
						mPlugin.mEffectManager.addEffect(player, SOURCE, new PercentHealthBoost(60 * 60 * 60, Math.max(-MAX_DEBUFF, prevAmp - DEBUFF), SOURCE)
							.deleteOnDeath(true)
							.deleteOnLogout(true)
						);
					});
				}

				mTicks += INTERVAL;
				if (mTicks >= DURATION) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, INTERVAL));
	}

	@Override
	public void cancel() {
		Aurora.playersInRange(mBoss.getLocation(), true).forEach(player ->
			EffectManager.getInstance().clearEffects(player, SOURCE));
		super.cancel();
	}

	@Override
	public int cooldownTicks() {
		return DURATION + 2 * 20;
	}
}
