package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.cleric.CelestialBlessingCS;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.cleric.CelestialBlessing.CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED;

public class CelestialBlessingDamageBuff extends PercentDamageDealt {
	public static final String effectID = "CelestialBlessingDamageBuff";

	private final CelestialBlessingCS mCosmetic;
	private final boolean mEnhanced;
	private final Player mPlayer;
	private boolean mMelee;
	private boolean mRanged;
	private boolean mCast;
	private boolean mExtended;
	private final Effect mSpeed;
	private final Effect mAesthetics;

	public CelestialBlessingDamageBuff(int duration, double amount, boolean enhanced, CelestialBlessingCS cosmetic,
	                                   Player player, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes, Effect speed, Effect aesthetics) {
		super(duration, amount, affectedDamageTypes, effectID);
		mEnhanced = enhanced;
		mCosmetic = cosmetic;
		mPlayer = player;
		mSpeed = speed;
		mAesthetics = aesthetics;
		resetExtension();
	}

	public void resetExtension() {
		mMelee = false;
		mRanged = false;
		mCast = false;
		mExtended = false;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		super.onDamage(entity, event, enemy);
		if (event.getFlatDamage() >= 1) {
			if (event.getType() == DamageEvent.DamageType.MELEE && event.getDamager() instanceof final Player player && player.getCooledAttackStrength(0.5f) > 0.9) {
				melee();
			}
			if ((event.getType() == DamageEvent.DamageType.PROJECTILE && event.getDamager() instanceof final Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true))
				|| (event.getAbility() != null && event.getAbility() == ClassAbility.ETHEREAL_ASCENSION)) {
				ranged();
			}
		}
	}

	@Override
	public void onAbilityCast(AbilityCastEvent event, Player player) {
		// Depending on balance might need to remove the fact that cleric counts cbless as a cast
		cast();
	}

	private void extendBuff() {
		if (!mExtended && mEnhanced && mMelee && mRanged && mCast) {
			mExtended = true;
			setDuration(getDuration() + CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED);
			mSpeed.setDuration(mSpeed.getDuration() + CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED);
			mAesthetics.setDuration(mAesthetics.getDuration() + CELESTIAL_BUFF_EXTENSION_DURATION_ENHANCED);
			mCosmetic.enhanceExtension(mPlayer);
		}
	}

	/*
	 * The below methods set these variables for all effects of this type.
	 * This is so that we can catch weird cases like multiple players using
	 * this ability at the same time, particularly if there is a non-enhanced
	 * instance which is stronger than an enhanced instance.
	 */

	private void melee() {
		for (CelestialBlessingDamageBuff e : getAllEffects()) {
			e.mMelee = true;
			e.extendBuff();
		}
	}

	private void ranged() {
		for (CelestialBlessingDamageBuff e : getAllEffects()) {
			e.mRanged = true;
			e.extendBuff();
		}
	}

	private void cast() {
		for (CelestialBlessingDamageBuff e : getAllEffects()) {
			e.mCast = true;
			e.extendBuff();
		}
	}

	private Set<CelestialBlessingDamageBuff> getAllEffects() {
		return Plugin.getInstance().mEffectManager.getEffects(mPlayer, CelestialBlessingDamageBuff.class);
	}
}
