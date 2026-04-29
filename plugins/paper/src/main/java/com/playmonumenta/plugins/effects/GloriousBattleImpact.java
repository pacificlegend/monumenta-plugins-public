package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class GloriousBattleImpact extends Effect {
	public static final String effectID = "GloriousBattleImpact";
	private static final List<LivingEntity> mImpacted = new ArrayList<>();
	private static @Nullable BukkitRunnable mRunnable = null;

	private final UUID mAppliedBy;
	@SuppressWarnings("NullAway") // it'll never be null where it matters
	private LivingEntity mEntity;
	private final double mDamage;
	private final Vector mDirection;
	private double mFallDistanceLastTick;

	private GloriousBattleImpact(int duration, UUID player, double damage, Vector direction, int fallDistance) {
		super(duration, effectID, false);
		mAppliedBy = player;
		mDamage = damage;
		mDirection = direction.clone().normalize().multiply(0.2);
		mFallDistanceLastTick = fallDistance;
	}

	public GloriousBattleImpact(int duration, UUID player, double damage, Vector direction) {
		this(duration, player, damage, direction, 0);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (!(entity instanceof LivingEntity le)) {
			return;
		}
		mEntity = le;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mImpacted.add(le)); // fix tick order issue
		if (mRunnable == null || mRunnable.isCancelled()) {
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					runnableTick();
				}
			};
			mRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	private static void runnableTick() {
		List<LivingEntity> toRemove = new ArrayList<>(); // avoid CME
		for (LivingEntity entity : mImpacted) {
			GloriousBattleImpact impactEffect = Plugin.getInstance().mEffectManager.getActiveEffect(entity, GloriousBattleImpact.class);
			if (impactEffect == null || impactEffect.getDuration() <= 0) {
				toRemove.add(entity);
				continue;
			}
			if (impactEffect.check()) {
				impactEffect.damage();
				toRemove.add(entity);
			}
		}
		mImpacted.removeAll(toRemove);
	}

	private boolean check() {
		if (mFallDistanceLastTick > 2.0 && mEntity.isOnGround()) {
			new PartialParticle(
				Particle.EXPLOSION_LARGE,
				LocationUtils.getHeightLocation(mEntity, 0.1),
				1
			).spawnAsEnemy();
			return true;
		}
		mFallDistanceLastTick = EntityUtils.getEntityStackBase(mEntity).getFallDistance();

		BoundingBox offsetBox = mEntity.getBoundingBox().clone();
		offsetBox.shift(mDirection.getX(), 0.15, mDirection.getZ());
		offsetBox.expand(0.1, -0.4, 0.1);

		if (LocationUtils.collidesWithBlocks(offsetBox, mEntity.getWorld())) {
			new PartialParticle(
				Particle.EXPLOSION_LARGE,
				LocationUtils.getHeightLocation(mEntity, 0.65),
				1
			).spawnAsEnemy();
			return true;
		}

		double widthDelta = PartialParticle.getWidthDelta(mEntity);
		double heightDelta = PartialParticle.getHeightDelta(mEntity);

		new PartialParticle(
			Particle.REDSTONE,
			LocationUtils.getHeightLocation(mEntity, 0.6),
			2,
			widthDelta,
			heightDelta / 2,
			widthDelta,
			new Particle.DustOptions(Color.WHITE, 1.2f)
		).spawnAsEnemy();

		return false;
	}

	private void damage() {
		DamageUtils.damage(Bukkit.getPlayer(mAppliedBy), mEntity, DamageEvent.DamageType.TRUE,
			mDamage, ClassAbility.GLORIOUS_BATTLE, true);

		World world = mEntity.getWorld();
		world.playSound(mEntity.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.5f, 1.35f);
		world.playSound(mEntity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(mEntity.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 1.7f, 0.5f);
		world.playSound(mEntity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.7f, 0.5f);
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();

		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("player", mAppliedBy.toString());
		object.addProperty("damage", mDamage);
		object.addProperty("dirX", mDirection.getX());
		object.addProperty("dirY", mDirection.getY());
		object.addProperty("dirZ", mDirection.getZ());
		object.addProperty("fall", mFallDistanceLastTick);

		return object;
	}

	public static GloriousBattleImpact deserialize(JsonObject object) {
		int duration = object.get("duration").getAsInt();
		UUID uuid = UUID.fromString(object.get("player").getAsString());
		double damage = object.get("damage").getAsDouble();
		double x = object.get("dirX").getAsDouble();
		double y = object.get("dirY").getAsDouble();
		double z = object.get("dirZ").getAsDouble();
		int fall = object.get("fall").getAsInt();

		return new GloriousBattleImpact(duration, uuid, damage, new Vector(x, y, z), fall);
	}

	@Override
	public String toString() {
		return String.format("GloriousBattleImpact duration:%d damage:%f direction:%s falldistance:%f", mDuration, mDamage, mDirection, mFallDistanceLastTick);
	}
}
