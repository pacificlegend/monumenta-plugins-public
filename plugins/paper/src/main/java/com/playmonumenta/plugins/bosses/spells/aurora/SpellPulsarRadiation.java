package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Bukkit;
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
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellPulsarRadiation extends Spell {
	private static final String SPELL_NAME = "Pulsar Radiation";
	private static final double RADIUS = 1.1;
	private static final double SOUND_THRESHOLD = 9;
	private static final int DAMAGE = 60;
	private static final double DAMAGE_PERCENT = 0.2;
	private static final int CHARGE_TIME = 4 * 20;
	private static final int DURATION = 9999 * 20;
	private static final double DEGREE_INC = 0.45;
	private static final String TAG = "AuroraPulsar";
	private static final Transformation TRANSFORM_ZERO = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(), new Quaternionf());

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Location mRaisedCenter;

	private double mDegreeInc = DEGREE_INC;
	@Nullable
	private BeamRunnable mMasterRunnable = null;

	public SpellPulsarRadiation(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRaisedCenter = mCenter.clone().add(0, 2.5, 0);
	}

	public void onPhase4() {
		mDegreeInc *= 1.4;
		if (mMasterRunnable != null) {
			mMasterRunnable.onPhase4();
		}
	}

	@Override
	public void run() {
		World world = mRaisedCenter.getWorld();

		world.playSound(mRaisedCenter, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 2.0f, 1.5f);
		world.playSound(mRaisedCenter, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 3.0f, 2.0f);
		world.playSound(mRaisedCenter, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 2.0f, 1.5f);
		world.playSound(mRaisedCenter, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 2.0f, 0.5f);
		world.playSound(mRaisedCenter, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.5f, 1.0f);

		mMasterRunnable = new BeamRunnable();
		mActiveRunnables.add(mMasterRunnable);
		mMasterRunnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void laser(double degrees, int tick, boolean phase4) {
		double rad = Math.toRadians(degrees);
		Vector vec = new Vector(Math.cos(rad), 0, Math.sin(rad)).multiply(Aurora.ARENA_RADIUS);
		Vector vecParticles = new Vector(Math.cos(rad + 0.05), 0, Math.sin(rad + 0.05)).multiply(Aurora.ARENA_RADIUS);
		if (tick % 2 == 0) {
			Aurora.playersInRange(mCenter, true).forEach(player -> {
				Vector playerFromCenter = player.getLocation().subtract(mCenter).toVector().setY(0);
				float angle = vec.angle(playerFromCenter);
				double distanceToCenter = playerFromCenter.length();
				double distanceToLaser = distanceToCenter * FastUtils.sin(angle);
				if (distanceToLaser <= SOUND_THRESHOLD) {
					Location soundLoc = mCenter.clone().add(vec.clone().multiply(distanceToCenter * FastUtils.cos(angle) / Aurora.ARENA_RADIUS));
					player.playSound(soundLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 0.9f, 1.5f);
				}
			});
		}
		for (int i = 0; i < 6; i++) {
			Particle soulFireFlame = phase4 ? Particle.END_ROD : Particle.SOUL_FIRE_FLAME;
			double vel0 = phase4 ? 0.2 : 0.08;
			double vel1 = phase4 ? 0.3 : 0.12;
			new PartialParticle(soulFireFlame, mRaisedCenter.clone().add(0, FastUtils.randomDoubleInRange(-0.5, 0.5), 0))
				.directionalMode(true)
				.delta(vecParticles.getX(), vecParticles.getY(), vecParticles.getZ())
				.extraRange(vel0, vel1)
				.spawnAsBoss();

			new PartialParticle(soulFireFlame, mRaisedCenter.clone().add(0, FastUtils.randomDoubleInRange(-0.5, 0.5), 0))
				.directionalMode(true)
				.delta(-vecParticles.getX(), -vecParticles.getY(), -vecParticles.getZ())
				.extraRange(vel0, vel1)
				.spawnAsBoss();
		}
		if (tick % 10 == 0) {
			Hitbox.approximateCylinder(
				mRaisedCenter.clone().subtract(vec),
				mRaisedCenter.clone().add(vec),
				RADIUS, false
			).getHitPlayers(true).forEach(this::doDamage);
		}
	}

	private void doDamage(Player player) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, SPELL_NAME);
		DamageUtils.damagePercentHealth(mBoss, player, DAMAGE_PERCENT, false, false, SPELL_NAME);
		MovementUtils.knockAway(mRaisedCenter, player, 0.8f, -0.1f, false);
	}

	private void crystalParticles(int tick) {
		new PPPillar(Particle.CRIT_MAGIC, mCenter, 4)
			.count(2)
			.spawnAsBoss();

		new PPPillar(Particle.END_ROD, mCenter, 4)
			.count(2)
			.spawnAsBoss();

		for (int i = 0; i < 5; i++) {
			double time = tick + i / 5.0;
			double x = FastUtils.sin(time);
			double z = FastUtils.cos(time);

			double partialY = FastUtils.sin(time / 1.34);
			double ySign = Math.signum(partialY);
			double y = Math.sqrt(Math.abs(partialY)) * ySign;
			double mult = FastUtils.sin(y * Math.PI / 2) * 3;
			Location pLoc = mCenter.clone().add(new Vector(0, 2, 0));

			pLoc.setX(pLoc.getX() + x * mult);
			pLoc.setZ(pLoc.getZ() + z * mult);
			pLoc.setY(pLoc.getY() + y);
			new PartialParticle(Particle.CRIT_MAGIC, pLoc)
				.delta(0.075)
				.spawnAsBoss();
		}
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return CHARGE_TIME + Aurora.SPELL_INTERVAL;
	}

	private class BeamRunnable extends BukkitRunnable {
		private static final float SIN_45 = (float) Math.sqrt(2) / 2;
		private final List<Player> mPlayers;
		private final ItemDisplay mCrystalCore;
		private final ItemDisplay mCrystalGlassInner;
		private final ItemDisplay mCrystalGlassOuter;
		private final ItemDisplay mBeamCore;
		private final ItemDisplay mBeamGlow;
		private final World mWorld;

		boolean mStarted;
		int mTicks;
		double mDegrees;
		float mLength;

		private boolean mPhase4 = false;

		public BeamRunnable() {
			mWorld = mBoss.getWorld();
			mPlayers = Aurora.playersInRange(mCenter);
			mCrystalCore = mWorld.spawn(mRaisedCenter, ItemDisplay.class, display -> {
				display.setItemStack(new ItemStack(Material.SEA_LANTERN));
				display.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf(),
					new Vector3f(1.5f),
					new Quaternionf()
				));
				display.setBrightness(new Display.Brightness(15, 15));
				display.setInterpolationDuration(2);

				EntityUtils.setRemoveEntityOnUnload(display);
				display.addScoreboardTag(TAG);
			});
			mCrystalGlassInner = mWorld.spawn(mRaisedCenter, ItemDisplay.class, display -> {
				display.setItemStack(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS));
				display.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf(),
					new Vector3f(2.0f),
					new Quaternionf()
				));
				display.setBrightness(new Display.Brightness(15, 15));
				display.setInterpolationDuration(2);

				EntityUtils.setRemoveEntityOnUnload(display);
				display.addScoreboardTag(TAG);
			});
			mCrystalGlassOuter = mWorld.spawn(mRaisedCenter, ItemDisplay.class, display -> {
				display.setItemStack(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS));
				display.setBrightness(new Display.Brightness(15, 15));
				display.setInterpolationDuration(2);

				EntityUtils.setRemoveEntityOnUnload(display);
				display.addScoreboardTag(TAG);
			});
			mBeamCore = mWorld.spawn(mRaisedCenter, ItemDisplay.class, display -> {
				display.setItemStack(new ItemStack(Material.SEA_LANTERN));
				display.setBrightness(new Display.Brightness(15, 15));
				display.setInterpolationDuration(3);

				EntityUtils.setRemoveEntityOnUnload(display);
				display.addScoreboardTag(TAG);
			});
			mBeamGlow = mWorld.spawn(mRaisedCenter, ItemDisplay.class, display -> {
				display.setItemStack(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS));
				display.setBrightness(new Display.Brightness(15, 15));
				display.setInterpolationDuration(3);

				EntityUtils.setRemoveEntityOnUnload(display);
				display.addScoreboardTag(TAG);
			});
			mStarted = false;
			mTicks = 0;
			mDegrees = 0;
			mLength = 0f;
		}

		@Override
		public void run() {
			if (!mCrystalCore.isValid()) {
				this.cancel();
				return;
			}
			crystalParticles(mTicks);
			mCrystalGlassOuter.setInterpolationDelay(-1);
			mCrystalGlassInner.setInterpolationDelay(-1);
			mCrystalCore.setInterpolationDelay(-1);
			if (!mPhase4) {
				Transformation outerTransform = mCrystalGlassOuter.getTransformation();
				mCrystalGlassOuter.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf().rotationY(mTicks * (float) Math.PI / 60).rotateAxis((float) Math.PI / 3, 1, 0, 1),
					outerTransform.getScale(),
					outerTransform.getRightRotation()
				));
				Transformation innerTransform = mCrystalGlassInner.getTransformation();
				mCrystalGlassInner.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf().setAngleAxis((float) Math.PI / 3, SIN_45, 0, SIN_45).rotateY(mTicks * (float) Math.PI / 60),
					innerTransform.getScale(),
					innerTransform.getRightRotation()
				));
				Transformation coreTransform = mCrystalCore.getTransformation();
				mCrystalCore.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf().setAngleAxis((float) Math.PI / 3, SIN_45, 0, SIN_45).rotateY(mTicks * (float) Math.PI / 60),
					coreTransform.getScale(),
					coreTransform.getRightRotation()
				));
			}

			if (mStarted) {
				if (mTicks >= CHARGE_TIME + DURATION) {
					this.cancel();
					return;
				}
				laser(mDegrees, mTicks, mPhase4);
				mBeamCore.setInterpolationDelay(-1);
				mBeamCore.setInterpolationDuration(1);
				mBeamCore.setTransformation(new Transformation(
					new Vector3f(),
					new AxisAngle4f((float) Math.toRadians(-mDegrees - mDegreeInc / 2), 0, 1, 0),
					new Vector3f(Aurora.ARENA_RADIUS * 2 + 4, 0.3f + oscillatedOffset(), 0.3f + oscillatedOffset()),
					new AxisAngle4f()
				));
				mBeamGlow.setInterpolationDelay(-1);
				mBeamGlow.setInterpolationDuration(1);
				mBeamGlow.setTransformation(new Transformation(
					new Vector3f(),
					new AxisAngle4f((float) Math.toRadians(-mDegrees - mDegreeInc / 2), 0, 1, 0),
					new Vector3f(Aurora.ARENA_RADIUS * 2 + 4, 0.4f + oscillatedOffset(), 0.4f + oscillatedOffset()),
					new AxisAngle4f()
				));

				mDegrees += mDegreeInc;
			} else {
				if (mTicks >= CHARGE_TIME) {
					mStarted = true;

					mWorld.playSound(mRaisedCenter, Sound.ENTITY_BREEZE_JUMP, SoundCategory.HOSTILE, 5.0f, 0.5f);
					mWorld.playSound(mRaisedCenter, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 5.0f, 1.5f);
					mWorld.playSound(mRaisedCenter, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 5.0f, 0.5f);
					mWorld.playSound(mRaisedCenter, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5.0f, 0.7f);
					mWorld.playSound(mRaisedCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 3.0f, 0.8f);
					mWorld.playSound(mRaisedCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.5f, 1.1f);

					for (int i = 0; i < 5; i++) {
						ParticleUtils.drawParticleCircleExplosion(
							mBoss,
							mRaisedCenter,
							0,
							0.5,
							FastUtils.randomDoubleInRange(0, 360),
							FastUtils.randomDoubleInRange(0, 360),
							24,
							0.8f - i * 0.1f,
							true,
							0,
							Particle.SOUL_FIRE_FLAME
						);
					}
					return;
				}
				mPlayers.forEach(player -> {
					Location pLoc = player.getLocation();
					player.playSound(pLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.1f, 1.0f + (float) mTicks / CHARGE_TIME);
					player.playSound(pLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.1f, 0.25f + (float) mTicks / CHARGE_TIME);
				});

				mBeamGlow.setInterpolationDelay(-1);
				mBeamGlow.setTransformation(new Transformation(
					new Vector3f(),
					new Quaternionf(),
					new Vector3f(mLength, 0.4f + oscillatedOffset(), 0.4f + oscillatedOffset()),
					new Quaternionf()
				));

				mLength += 2 * (float) Aurora.ARENA_RADIUS / CHARGE_TIME;
			}
			mTicks++;
		}

		public void onPhase4() {
			mCrystalGlassOuter.setInterpolationDelay(-1);
			mCrystalGlassInner.setInterpolationDelay(-1);
			mCrystalCore.setInterpolationDelay(-1);
			mCrystalCore.setTransformation(TRANSFORM_ZERO);
			mCrystalGlassInner.setTransformation(TRANSFORM_ZERO);
			mCrystalGlassOuter.setTransformation(TRANSFORM_ZERO);
			mBeamCore.setItemStack(new ItemStack(Material.PEARLESCENT_FROGLIGHT));
			mBeamGlow.setItemStack(new ItemStack(Material.WHITE_STAINED_GLASS));
			mPhase4 = true;
		}

		private float oscillatedOffset() {
			return (float) (FastUtils.sin(mTicks * 0.72) * 0.06);
		}

		@Override
		public synchronized void cancel() throws IllegalStateException {
			Transformation newTransform = new Transformation(
				new Vector3f(),
				new Quaternionf(),
				new Vector3f(),
				new Quaternionf()
			);
			mBeamGlow.setInterpolationDelay(-1);
			mCrystalCore.setInterpolationDelay(-1);
			mCrystalGlassInner.setInterpolationDelay(-1);
			mCrystalGlassOuter.setInterpolationDelay(-1);
			mBeamCore.setInterpolationDelay(-1);

			mBeamGlow.setTransformation(newTransform);
			mCrystalCore.setTransformation(newTransform);
			mCrystalGlassInner.setTransformation(newTransform);
			mCrystalGlassOuter.setTransformation(newTransform);
			mBeamCore.setTransformation(newTransform);
			mBeamGlow.setTransformation(newTransform);

			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mCrystalCore.remove();
				mCrystalGlassInner.remove();
				mCrystalGlassOuter.remove();
				mBeamCore.remove();
				mBeamGlow.remove();
			}, 3);
			super.cancel();
		}
	}
}
