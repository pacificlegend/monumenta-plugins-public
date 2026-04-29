package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellDuelist extends Spell {

	private static final Particle.DustOptions SWORD_COLOR = new Particle.DustOptions(Color.fromRGB(225, 225, 225), 1.5f);
	private static final String SLOW_SOURCE = "DuelistSweepChargeup";
	private static final int RANGE = 5;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mCooldown;
	private final int mDamage;

	public SpellDuelist(Plugin plugin, LivingEntity boss, int cooldown, int damage) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldown = cooldown;
		mDamage = damage;
	}

	@Override
	public void run() {
		if (!(mBoss instanceof Mob)) {
			return;
		}

		Location loc = mBoss.getLocation();
		LivingEntity target = ((Mob) mBoss).getTarget();
		if (!(target instanceof Player && LocationUtils.hasLineOfSight(mBoss, target))) {
			List<Player> players = PlayerUtils.playersInRange(loc, RANGE, false);
			for (Player player : players) {
				if (LocationUtils.hasLineOfSight(mBoss, player) && loc.distance(player.getLocation()) < RANGE) {
					target = player;
					break;
				}
			}
		}

		if (target == null) {
			return;
		}
		Location center = LocationUtils.getHalfHeightLocation(mBoss);
		Vector dir = center.getY() <= target.getLocation().getY() ?
			LocationUtils.getDirectionTo(LocationUtils.getHalfHeightLocation(target), center) :
			center.getDirection();
		Vector[] axisVectors = VectorUtils.getAxesFromNormal(dir);
		Vector[] axes;
		Vector normal;
		int telegraphDuration;
		if (FastUtils.randomBoolean()) {
			//high sweep
			axes = new Vector[]{axisVectors[1], dir};
			normal = axisVectors[0];
			telegraphDuration = 15;
		} else {
			axes = new Vector[]{axisVectors[0], dir};
			normal = axisVectors[1];
			telegraphDuration = 25;
		}

		mPlugin.mEffectManager.addEffect(mBoss, SLOW_SOURCE, new PercentSpeed(telegraphDuration, -0.6, SLOW_SOURCE));
		((Mob) mBoss).setTarget(target);

		mWorld.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 1f, 22.5f / telegraphDuration);
		mWorld.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1f, 20f / telegraphDuration);
		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.HOSTILE, 1f, 1.3f);
		mWorld.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.HOSTILE, 1f, 1.4f);


		new PPCircle(Particle.CRIT, center, RANGE)
			.countPerMeter(10)
			.ringMode(false)
			.axes(axes[0], axes[1])
			.arcDegree(0, 180)
			.ticks(5)
			.spawnAsBoss();

		new PPLine(Particle.REDSTONE, center, axes[0], RANGE)
			.countPerMeter(5)
			.data(SWORD_COLOR)
			.spawnAsBoss();

		new PPLine(Particle.WAX_OFF, center, axes[0], RANGE)
			.countPerMeter(4)
			.delta(0.25)
			.spawnAsBoss();

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Location newCenter = LocationUtils.getHalfHeightLocation(mBoss);
			Vector newCenterVec = newCenter.toVector();

			mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8f, 1f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.0f, 0.75f);
			mWorld.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 0.8f, 0.8f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE, 1.0f, 0.6f);

			new PPCircle(Particle.SWEEP_ATTACK, newCenter, RANGE)
				.countPerMeter(5)
				.ringMode(false)
				.axes(axes[0], axes[1])
				.arcDegree(0, 180)
				.ticks(5)
				.spawnAsBoss();

			new PPCircle(Particle.CRIT_MAGIC, newCenter, RANGE)
				.countPerMeter(5)
				.ringMode(false)
				.axes(axes[0], axes[1])
				.arcDegree(0, 180)
				.extra(0.4)
				.ticks(5)
				.spawnAsBoss();

			Hitbox.ApproximateFreeformHitbox hitbox = new Hitbox.ApproximateFreeformHitbox(mWorld, BoundingBox.of(newCenter, RANGE, RANGE, RANGE), vec ->
				Math.abs(vec.clone().subtract(newCenterVec).dot(normal)) <= 0.5 && vec.distanceSquared(newCenterVec) <= RANGE * RANGE
			);
			mBoss.swingMainHand();
			for (Player player : hitbox.getHitPlayers(true)) {
				DamageUtils.damage(mBoss, player, DamageType.MELEE, mDamage);
			}
		}, telegraphDuration);
	}

	@Override
	public boolean canRun() {
		Location loc = mBoss.getLocation();
		List<Player> players = PlayerUtils.playersInRange(loc, RANGE, false);
		if (!players.isEmpty()) {
			for (Player player : players) {
				if (LocationUtils.hasLineOfSight(mBoss, player)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

}
