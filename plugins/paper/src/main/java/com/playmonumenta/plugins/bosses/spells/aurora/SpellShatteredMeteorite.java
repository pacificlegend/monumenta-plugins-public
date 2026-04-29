package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellShatteredMeteorite extends Spell {
	private static final int TELEGRAPH_DURATION = 30;
	private static final int COUNT = 3;
	private static final int SPAWN_INTERVAL = 10;
	private static final double INITIAL_SPEED = 1.3;
	private static final double BASE_SPEED = 0.3;
	private static final double DECELERATION = 0.05;
	private static final double DAMAGE = 60;
	private static final double HITBOX_SIZE = 0.4;
	private static final float KNOCKBACK = 0.36f;
	private static final String SPELL_NAME = "Meteorite Shrapnel";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;

	public SpellShatteredMeteorite(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
	}

	@Override
	public void run() {
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				List<Player> players = Aurora.playersInRange(mCenter);
				if (players.isEmpty()) {
					return;
				}

				Location pLoc = LocationUtils.randomSafeLocationInDonut(FastUtils.getRandomElement(players).getLocation(), 3, 9, location ->
					LocationUtils.xzDistance(location, mCenter) <= Aurora.ARENA_RADIUS);
				pLoc.setY(pLoc.getBlockY() + 1);
				pLoc.setYaw(0);
				pLoc.setPitch(0);

				shootStarProjectiles(Aurora.withSurfaceY(pLoc, mCenter).add(0, 1, 0));
				mTicks++;
				if (mTicks >= COUNT) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, SPAWN_INTERVAL));
	}

	private void shootStarProjectiles(Location loc) {
		World world = mBoss.getWorld();
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 2.5f, 1.5f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 2.5f, 0.1f);
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 2.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.HOSTILE, 1.8f, 1.5f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1.2f, 1.5f);

		new PPPillar(Particle.END_ROD, loc, 3)
			.count(20)
			.spawnAsBoss();

		new PPCircle(Particle.CRIT, loc, 0.2)
			.count(30)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(1, 0, 0)
			.extra(0.5)
			.spawnAsBoss();

		ItemDisplay swordDisplay = world.spawn(loc.clone().add(0, 8, 0), ItemDisplay.class, display -> {
			display.setBillboard(Display.Billboard.VERTICAL);

			ItemStack itemStack = new ItemStack(Material.IRON_SWORD);
			ItemUtils.setPlainName(itemStack, "Energized Maelstrom");
			display.setItemStack(itemStack);

			display.setTransformation(new Transformation(
				new Vector3f(),
				new AxisAngle4f((float) Math.toRadians(135), 0, 0, 1),
				new Vector3f(3),
				new AxisAngle4f()
			));
			display.setTeleportDuration(8);
		});

		ItemDisplay amethystDisplay = world.spawn(loc, ItemDisplay.class, display -> {
			display.setItemStack(new ItemStack(Material.AMETHYST_BLOCK));

			display.setTransformation(new Transformation(
				new Vector3f(),
				new Quaternionf(),
				new Vector3f(1.4f, 1.4f, 1.4f),
				new Quaternionf()
			));
		});

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.6f);
			world.playSound(loc, Sound.ITEM_TRIDENT_THROW, 1.6f, 0.5f);
			world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, 2.0f, 1.5f);
			swordDisplay.teleport(loc);
		}, TELEGRAPH_DURATION - 8);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.5f);
			world.playSound(loc, Sound.ENTITY_ITEM_BREAK, 2.0f, 0.5f);
			world.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_BREAK, 2.0f, 1.5f);
			world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 2.0f, 0.8f);
			world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.8f);

			amethystDisplay.remove();
			swordDisplay.remove();

			new PartialParticle(Particle.EXPLOSION_LARGE, loc)
				.count(10)
				.delta(0.4)
				.spawnAsBoss();
		}, TELEGRAPH_DURATION);

		for (double theta = 0; theta < 360; theta += 72) {
			Location origin = loc.clone().add(0, 1.25, 0);
			double x = FastUtils.cosDeg(theta);
			double z = FastUtils.sinDeg(theta);
			Vector dir = new Vector(x, 0, z);
			loc.setDirection(dir);

			new PPLine(Particle.REDSTONE, loc, dir, LocationUtils.rayLengthToSphereSurface(mCenter, loc, Aurora.ARENA_RADIUS))
				.countPerMeter(4)
				.delta(0, 0.3, 0)
				.data(new Particle.DustOptions(Color.WHITE, 2.2f))
				.delay(TELEGRAPH_DURATION / 2)
				.spawnAsBoss();

			mActiveTasks.add(new BukkitRunnable() {
				final Location mProjLoc = origin.clone();
				int mProjTicks = 0;

				double mProjSpeed = INITIAL_SPEED;

				@Override
				public void run() {
					if (mProjTicks > 200 || mProjLoc.distance(mCenter) > Aurora.ARENA_RADIUS) {
						this.cancel();
						return;
					}

					mProjTicks += 1;

					Location prevLoc = mProjLoc.clone();
					mProjLoc.add(dir.multiply(mProjSpeed + BASE_SPEED));
					mProjSpeed *= (1 - DECELERATION);

					new PPLine(Particle.ELECTRIC_SPARK, prevLoc, mProjLoc)
						.countPerMeter(2)
						.extra(0.1)
						.spawnAsBoss();

					new PPLine(Particle.REDSTONE, prevLoc, mProjLoc)
						.countPerMeter(3)
						.data(new Particle.DustOptions(Color.fromRGB(220, 110, 255), 1.9f))
						.delta(0, 0.5, 0)
						.spawnAsBoss();

					Hitbox.unionOf(List.of(
						Hitbox.approximateCylinder(prevLoc, mProjLoc.clone().subtract(0, 0.3, 0), HITBOX_SIZE, false),
						Hitbox.approximateCylinder(prevLoc, mProjLoc.clone().add(0, 0.3, 0), HITBOX_SIZE, false))
					).getHitPlayers(true).forEach(player -> {
						player.playSound(mProjLoc, Sound.ENTITY_BREEZE_JUMP, SoundCategory.HOSTILE, 1.0f, 0.6f);
						player.playSound(mProjLoc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.HOSTILE, 1.4f, 0.5f);
						player.playSound(mProjLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.6f, 1.9f);

						new PartialParticle(Particle.CRIT_MAGIC, player.getLocation(), 25, 0.5, 1, 0.5, 0).spawnAsBoss();

						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, SPELL_NAME, origin);
						MovementUtils.knockAway(prevLoc, player, KNOCKBACK);
						this.cancel();
					});

					if (mProjLoc.getBlock().isSolid()) {
						world.playSound(mProjLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, TELEGRAPH_DURATION, 1));
		}
	}

	@Override
	public int cooldownTicks() {
		return COUNT * TELEGRAPH_DURATION + 4 * 20;
	}
}
