package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.ScorchedEarthCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class ScorchedEarthDamage extends Effect {
	public static final String effectID = "ScorchedEarthDamage";

	private final double mDamage;
	private final Player mAlchemist;
	private final PlayerItemStats mStats;
	private final int mFireTickDuration;
	private final ScorchedEarthCS mCosmetic;

	private int mLastDamageTick = 0;

	public ScorchedEarthDamage(final int duration, final double damage, final Player player, final PlayerItemStats stats,
	                           final int fireDuration, final ScorchedEarthCS cosmetic) {
		super(duration, effectID);
		mDamage = damage;
		mAlchemist = player;
		mStats = stats;
		mFireTickDuration = fireDuration;
		mCosmetic = cosmetic;
	}

	@Override
	public double getMagnitude() {
		return mDamage;
	}

	@Override
	public void onHurt(final LivingEntity entity, final DamageEvent event) {
		/* Prevent effect from activating on events it shouldn't */
		if (event.getSource() == null || event.isCancelled() || event.getAbility() == ClassAbility.SCORCHED_EARTH) {
			return;
		}

		final DamageType type = event.getType();
		/* Only allow 1 application every 2 ticks, disregard certain damage types, and prevent effect from procing on low damage attacks */
		if (Bukkit.getCurrentTick() - mLastDamageTick < 2 || !type.isScalable() || event.getBaseDamage() <= 1) {
			return;
		}

		if ((type == DamageType.MELEE && event.getDamager() instanceof final Player player && player.getCooledAttackStrength(0.5f) > 0.9)
			|| (type == DamageType.PROJECTILE && event.getDamager() instanceof final Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, false))
			|| (type != DamageType.MELEE && type != DamageType.PROJECTILE && event.getDamager() instanceof Player)) {
			mLastDamageTick = Bukkit.getCurrentTick();
			DamageUtils.damage(mAlchemist, entity, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.SCORCHED_EARTH, mStats),
				mDamage, true, false, false);
			EntityUtils.applyFire(Plugin.getInstance(), mFireTickDuration, entity, mAlchemist);
			mCosmetic.damageEffect(entity, mAlchemist);
		}
	}

	@Override
	public String toString() {
		return String.format("ScorchedEarthDamage duration=%d", this.getDuration());
	}
}
