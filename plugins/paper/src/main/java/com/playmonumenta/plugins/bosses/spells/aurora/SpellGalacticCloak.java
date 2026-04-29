package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayDeque;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellGalacticCloak extends Spell {
	private static final int TELEGRAPH_DURATION = 20;
	private static final double DAMAGE = 65;
	private static final double HITBOX_SIZE = 0.9;
	private static final float KNOCKBACK = 0.36f;
	private static final String SPELL_NAME = "Galactic Cloak";
	public static final double DISTANCE_BEHIND_TARGET = 3.5;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private List<Player> mPlayers = List.of();

	public SpellGalacticCloak(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();
		world.playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 1.6f, 0.7f);
		world.playSound(bossLoc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 1.1f, 1.2f);
		world.playSound(bossLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 1.5f, 0.5f);

		new PartialParticle(Particle.SMOKE_LARGE, bossLoc)
			.count(30)
			.extra(0.2)
			.spawnAsBoss();

		mBoss.setInvulnerable(true);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.setInvulnerable(false), TELEGRAPH_DURATION);
		EntityUtils.selfRoot(mBoss, TELEGRAPH_DURATION);

		mPlayers = Aurora.playersInRange(bossLoc);
		mPlayers.forEach(p -> p.hideEntity(mPlugin, mBoss));

		ArrayDeque<Player> players = mPlayers.stream().limit(2).collect(ArrayDeque::new, ArrayDeque::add, ArrayDeque::addAll);

		new BukkitRunnable() {
			@Override
			public void run() {
				@Nullable
				Player player = players.poll();
				if (player == null) {
					this.cancel();
					return;
				}

				target(player);
			}
		}.runTaskTimer(mPlugin, TELEGRAPH_DURATION, TELEGRAPH_DURATION + 10);
	}

	private void target(Player player) {
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();

		world.playSound(bossLoc, Sound.ENTITY_PILLAGER_AMBIENT, SoundCategory.HOSTILE, 1.4f, 0.4f);
		world.playSound(bossLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.4f, 1.2f);
		world.playSound(bossLoc, Sound.BLOCK_TRIAL_SPAWNER_BREAK, SoundCategory.HOSTILE, 1.6f, 0.5f);
		world.playSound(bossLoc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.HOSTILE, 1.0f, 0.5f);

		mPlayers.forEach(p -> p.hideEntity(mPlugin, mBoss));

		EntityUtils.selfRoot(mBoss, TELEGRAPH_DURATION);

		Location pLoc = player.getLocation().add(0, 1, 0);
		pLoc.setPitch(0);
		Vector playerDir = pLoc.getDirection();
		double distanceToWall = LocationUtils.rayLengthToSphereSurface(mCenter.toVector(), pLoc.toVector(), playerDir.clone().multiply(-1), Aurora.ARENA_RADIUS - 0.6);
		if (distanceToWall <= DISTANCE_BEHIND_TARGET) {
			pLoc = pLoc.add(playerDir.clone().multiply(DISTANCE_BEHIND_TARGET - distanceToWall));
		}

		world.playSound(pLoc, Sound.ENTITY_PILLAGER_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.4f);
		world.playSound(pLoc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 0.8f, 0.5f);
		world.playSound(pLoc, Sound.ENTITY_BREEZE_INHALE, SoundCategory.HOSTILE, 1.5f, 1.6f);

		Vector halfDisplacement = playerDir.clone().multiply(DISTANCE_BEHIND_TARGET);
		Location behind = pLoc.clone().subtract(halfDisplacement);
		Location front = pLoc.clone().add(halfDisplacement);

		new PPLine(Particle.CRIT, behind, front)
			.countPerMeter(8)
			.extra(1.2)
			.delay(5)
			.spawnAsBoss();

		new PPLine(Particle.REDSTONE, behind, front)
			.data(new Particle.DustOptions(Color.RED, 1.4f))
			.countPerMeter(5)
			.delta(0.25)
			.delay(5)
			.spawnAsBoss();

		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mBoss.teleport(behind);
			mPlayers.forEach(p -> p.showEntity(mPlugin, mBoss));
		}, TELEGRAPH_DURATION - 5));

		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (mBoss instanceof Mob mobBoss) {
				mobBoss.getPathfinder().findPath(front);
			}
			mBoss.setVelocity(playerDir.clone().setY(0.35));

			world.playSound(behind, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.8f, 1.6f);
			world.playSound(behind, Sound.ENTITY_BREEZE_JUMP, SoundCategory.HOSTILE, 0.9f, 0.6f);
			world.playSound(behind, Sound.ENTITY_ARROW_HIT, SoundCategory.HOSTILE, 1.6f, 0.6f);
			world.playSound(behind, Sound.BLOCK_TRIAL_SPAWNER_BREAK, SoundCategory.HOSTILE, 1.5f, 1.2f);
			world.playSound(behind, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.HOSTILE, 1.0f, 1.8f);

			for (int n = 0; n < 3; n++) {
				Vector displacement = halfDisplacement.clone().multiply(2);
				Vector horizontal = new Vector(0, 1, 0)
					.rotateAroundAxis(playerDir, FastUtils.randomDoubleInRange(0, Math.PI * 2))
					.crossProduct(playerDir)
					.multiply(HITBOX_SIZE);

				new PPParametric(Particle.CRIT_MAGIC, behind, (i, packagedValues) -> {
					packagedValues.location(behind.clone().add(displacement.clone().multiply(i)));
					Vector d = horizontal.clone().multiply((1 - i * i * i) * FastUtils.randomSign());
					packagedValues.offset(d.getX(), d.getY(), d.getZ());
				})
					.count(50)
					.directionalMode(true)
					.extra(1)
					.spawnAsBoss();
			}

			new PPLine(Particle.SPELL_WITCH, behind, front)
				.countPerMeter(5)
				.delta(0.02)
				.extra(HITBOX_SIZE * 0.1)
				.delay(2)
				.spawnAsBoss();

			new PPLine(Particle.SWEEP_ATTACK, behind, front)
				.countPerMeter(5)
				.delta(0.1)
				.delay(2)
				.spawnAsBoss();

			Hitbox.approximateCylinder(behind, front, HITBOX_SIZE, true).getHitPlayers(true).forEach(hitPlayer -> {
				hitPlayer.playSound(behind, Sound.ENTITY_ITEM_BREAK, SoundCategory.HOSTILE, 1.5f, 0.5f);

				DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MELEE, DAMAGE, null, true, false, SPELL_NAME);
				MovementUtils.knockAway(behind, hitPlayer, KNOCKBACK, KNOCKBACK, false);
			});
		}, TELEGRAPH_DURATION));
	}

	@Override
	public int cooldownTicks() {
		return 8 * 20;
	}
}
