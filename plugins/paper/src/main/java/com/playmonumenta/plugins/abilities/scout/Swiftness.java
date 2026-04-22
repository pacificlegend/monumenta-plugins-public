package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.SwiftnessCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static net.kyori.adventure.util.TriState.FALSE;
import static net.kyori.adventure.util.TriState.TRUE;

public class Swiftness extends Ability {
	private static final double SPEED_BONUS = 0.1;
	private static final int JUMP_BOOST_POTENCY = 2; // Jump Boost 3, effect potency is 0 indexed
	private static final String NO_JUMP_BOOST_TAG = "SwiftnessJumpBoostDisable";
	private static final String NO_DEFAULT_TRIGGER_TAG = "SwiftnessDefaultTriggerDisable";
	private static final int COOLDOWN = 60;
	private static final int MAX_JUMP = 3;
	private static final double DOUBLE_JUMP_STRENGTH = 0.4;
	private static final double DOUBLE_JUMP_STRENGTH_MULTIPLIER = 0.35;
	private static final double DASH_VULNERABILITY_MULTIPLIER = 0.15;
	private static final int DASH_VULNERABILITY_DURATION = 5 * Constants.TICKS_PER_SECOND;
	private static final int DASH_IMMUNITY_DURATION = 8; // 0.4s

	public static final String CHARM_JUMP_BOOST = "Swiftness Jump Boost Amplifier";
	public static final String CHARM_COOLDOWN = "Swiftness Double Jump Cooldown";
	public static final String CHARM_DOUBLE_JUMP_STRENGTH = "Swiftness Double Jump Strength";

	public static final String CHARM_DASH_VULNERABILITY_AMPLIFIER = "Swiftness Enhancement Vulnerability Amplifier";
	public static final String CHARM_DASH_VULNERABILITY_DURATION = "Swiftness Dash Vulnerability Duration";
	public static final String CHARM_DASH_RESISTANCE_DURATION = "Swiftness Dash Resistance Duration";


	public static final AbilityInfo<Swiftness> INFO =
		new AbilityInfo<>(Swiftness.class, "Swiftness", Swiftness::new)
			.linkedSpell(ClassAbility.SWIFTNESS)
			.scoreboardId("Swiftness")
			.shorthandName("Swf")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Gain jump height, double jump, and increased movement.")
			.addTrigger(new AbilityTriggerInfo<>("toggle", "toggle jump boost", null,
				Swiftness::toggleJumpBoost, new AbilityTrigger(AbilityTrigger.Key.SWAP).enabled(false).sneaking(false)
				.lookDirections(AbilityTrigger.LookDirection.UP)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PROJECTILE_WEAPON), null))
			.addTrigger(new AbilityTriggerInfo<>("toggle_default_trigger", "toggle default trigger",
				"Toggle the default behavior of double-tapping space to dash",
				Swiftness::toggleDefaultTrigger, new AbilityTrigger(AbilityTrigger.Key.SWAP).enabled(false).sneaking(false)
				.lookDirections(AbilityTrigger.LookDirection.DOWN)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PROJECTILE_WEAPON), null))
			.addTrigger(new AbilityTriggerInfo<>("jump", "alternative jump trigger", null,
				Swiftness::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false).sneaking(true)
				.lookDirections(AbilityTrigger.LookDirection.LEVEL), null))
			.remove(Swiftness::removeFlying)
			.displayItem(Material.RABBIT_FOOT)
			.ignoresSilence(true);

	private final int mJumpBoostLevel;
	private final double mSpeed;
	private boolean mWasInNoMobilityZone = false;
	private boolean mJumpBoost;
	private boolean mDefaultTriggerDisabled;
	private final double mDashStrength;
	private final double mVulnerabilityMultiplier;
	private final int mVulnerabilityDuration;
	private final int mResistanceDuration;
	private final SwiftnessCS mCosmetic;
	private int mTotalJumps = 0;
	private @Nullable BukkitTask mDashRunnable;
	private boolean mIsActive = false;

	public Swiftness(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mJumpBoost = !mPlayer.getScoreboardTags().contains(NO_JUMP_BOOST_TAG);
		mJumpBoostLevel = JUMP_BOOST_POTENCY + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);
		mDefaultTriggerDisabled = mPlayer.getScoreboardTags().contains(NO_DEFAULT_TRIGGER_TAG);
		mSpeed = isLevelTwo() ? SPEED_BONUS : 0;
		mDashStrength = (isLevelOne() ? 0 : DOUBLE_JUMP_STRENGTH_MULTIPLIER) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DOUBLE_JUMP_STRENGTH);
		mVulnerabilityMultiplier = DASH_VULNERABILITY_MULTIPLIER + CharmManager.getLevelPercentDecimal(player, CHARM_DASH_VULNERABILITY_AMPLIFIER);
		mVulnerabilityDuration = CharmManager.getDuration(mPlayer, CHARM_DASH_VULNERABILITY_DURATION, DASH_VULNERABILITY_DURATION);
		mResistanceDuration = CharmManager.getDuration(mPlayer, CHARM_DASH_RESISTANCE_DURATION, DASH_IMMUNITY_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new SwiftnessCS());

		if (!mDefaultTriggerDisabled) {
			mPlayer.setAllowFlight(true);
			if (!canToggleFlight(mPlayer)) {
				mPlayer.setFlyingFallDamage(TRUE);
				mPlayer.setFlySpeed(0f);
			}
		}
	}

	@Override
	public void invalidate() {
		if (mDashRunnable != null) {
			mDashRunnable.cancel();
		}

		mPlayer.setFlySpeed(0.1f);
		mPlayer.setFlyingFallDamage(FALSE);
	}

	@Override
	public void playerToggleFlightEvent(PlayerToggleFlightEvent event) {
		if (canToggleFlight(mPlayer)) {
			mPlayer.setFlySpeed(0.1f);
			return;
		}

		event.setCancelled(true);
		mPlayer.setFlying(false);
		cast();
	}

	public boolean cast() {
		if (!canCast() || ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}
		putOnCooldown();
		mTotalJumps++;

		double jumpStrength = DOUBLE_JUMP_STRENGTH * (1 + mDashStrength);
		boolean backwards = false;
		if (jumpStrength < 0) {
			backwards = true;
			jumpStrength *= -1;
		}

		Vector dir = mPlayer.getLocation().getDirection().normalize();
		dir.multiply(1 + mDashStrength);
		if (backwards && !isEnhanced()) {
			dir.setY(dir.getY() * -1);
		}

		if (!isEnhanced()) {
			mCosmetic.swiftnessDoubleJump(mPlayer, mPlayer.getLocation());

			mPlayer.setVelocity(dir.setY(dir.getY() * 0.2 + jumpStrength));
		} else {
			// Swiftness Enhancement
			mCosmetic.swiftnessDash(mPlayer, mPlayer.getLocation());

			if (mDashRunnable != null) {
				mDashRunnable.cancel();
			}

			mIsActive = true;
			int duration = (int) (jumpStrength * 5);
			mDashRunnable = new BukkitRunnable() {
				int mT = 0;
				boolean mDash = true;

				@Override
				public void run() {
					if (mT > duration * 2 || mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.isValid()) {
						mDashRunnable = null;
						mIsActive = false;
						this.cancel();
						return;
					}

					if (mIsActive && mT > mResistanceDuration) {
						mIsActive = false;
					}

					mCosmetic.swiftnessDashTick(mPlayer, dir);

					Hitbox hitbox = new Hitbox.AABBHitbox(mPlayer.getWorld(), mPlayer.getBoundingBox());
					hitbox.getHitMobs().forEach(e -> EntityUtils.applyVulnerability(mPlugin, mVulnerabilityDuration, mVulnerabilityMultiplier, e));

					if (mDash) {
						if (mT > duration) {
							mPlayer.setVelocity(mPlayer.getVelocity().multiply(0.5));
							mDash = false;
						} else {
							mPlayer.setVelocity(dir);
						}
					}

					mT++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		return true;
	}

	@Override
	public void periodicTrigger(final boolean twoHertz, final boolean oneSecond, final int ticks) {
		if (mTotalJumps != 0 && PlayerUtils.isOnGround(mPlayer)) {
			mTotalJumps = 0;
		}

		final boolean isInNoMobilityZone = ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES);

		if (canCast() && !isInNoMobilityZone && !mWasInNoMobilityZone && !mDefaultTriggerDisabled) {
			setFlying(mPlayer);
		} else {
			removeFlying(mPlayer);
		}

		mWasInNoMobilityZone = isInNoMobilityZone;

		if (oneSecond && !mWasInNoMobilityZone && mJumpBoost) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 21,
				mJumpBoostLevel, true, false));
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mIsActive && event.getType().isDefendable()) {
			event.setFlatDamage(0);
			event.setCancelled(true);

			mPlayer.playSound(mPlayer, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1f, 1.3f);
			return;
		}

		// setFlyingFallDamage does not play audio when taking damage via fall
		if (event.getType() == DamageEvent.DamageType.FALL) {
			if (mPlayer.getFallDistance() > 7) {
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 1f, 1f);
			} else {
				mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_SMALL_FALL, 1f, 1f);
			}
		}
	}

	public boolean toggleJumpBoost() {
		if (EntityUtils.isSilenced(mPlayer)) {
			return false;
		}

		if (mJumpBoost) {
			mJumpBoost = false;
			mPlayer.addScoreboardTag(NO_JUMP_BOOST_TAG);
			mPlugin.mPotionManager.removePotion(mPlayer, PotionID.ABILITY_SELF, PotionEffectType.JUMP);
			mCosmetic.toggleJumpBoostOff(mPlayer);
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned off");
		} else {
			mJumpBoost = true;
			mPlayer.removeScoreboardTag(NO_JUMP_BOOST_TAG);
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, 21,
				mJumpBoostLevel, true, false));
			mCosmetic.toggleJumpBoostOn(mPlayer);
			MessagingUtils.sendActionBarMessage(mPlayer, "Jump Boost has been turned on");
		}
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	public boolean toggleDefaultTrigger() {
		if (EntityUtils.isSilenced(mPlayer)) {
			return false;
		}

		if (mDefaultTriggerDisabled) {
			mDefaultTriggerDisabled = false;
			mPlayer.removeScoreboardTag(NO_DEFAULT_TRIGGER_TAG);
			MessagingUtils.sendActionBarMessage(mPlayer, "Swiftness default trigger enabled");
			setFlying(mPlayer);
		} else {
			mDefaultTriggerDisabled = true;
			mPlayer.addScoreboardTag(NO_DEFAULT_TRIGGER_TAG);
			MessagingUtils.sendActionBarMessage(mPlayer, "Swiftness default trigger disabled");
			removeFlying(mPlayer);
		}
		return true;
	}

	private static void setFlying(final Player player) {
		player.setAllowFlight(true);
	}

	private static void removeFlying(final Player player) {
		if (!canToggleFlight(player)) {
			player.setAllowFlight(false);
		}
	}

	private static boolean canToggleFlight(Player player) {
		return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
	}

	public double getFleetfootedBonus() {
		return mSpeed;
	}

	private boolean canCast() {
		return !isOnCooldown() && mTotalJumps < MAX_JUMP && !AbilityUtils.isSilenced(mPlayer);
	}

	@Override
	public @Nullable String getMode() {
		return mJumpBoost ? null : "disabled";
	}

	private static Description<Swiftness> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Passively gain jump boost.")
			.addLine("Gain the ability to double jump.")
			.addLine("(Max three jumps while midair)")
			.addLine()
			.addStat("Effect: Jump Boost %d")
			.statValues(stat(a -> a.mJumpBoostLevel + 1, JUMP_BOOST_POTENCY + 1))
			.addStat("Cooldown: %t")
			.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<Swiftness> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Swiftness*'s double jump strength").styles(UNDERLINED)
			.addLine("by %p and increase *Fleetfooted*'s speed.").styles(UNDERLINED)
			.statValues(stat(a -> a.mDashStrength, DOUBLE_JUMP_STRENGTH_MULTIPLIER))
			.addLine()
			.addStatComparison("Effect: +%p1 -> +%p2 Speed")
			.statValues(stat(0.1), stat(a -> a.mSpeed + 0.1, SPEED_BONUS + 0.1))
			.addDashedLine();

	}

	private static Description<Swiftness> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Swiftness*' double jump is transformed into").styles(UNDERLINED)
			.addLine("a dash. Become invulnerable for %t and inflict")
			.statValues(stat(a -> a.mResistanceDuration, DASH_IMMUNITY_DURATION))
			.addLine("vulnerability when passing through mobs.")
			.addLine()
			.addStat("Effect: %p Vulnerability for %t")
			.statValues(stat(a -> a.mVulnerabilityMultiplier, DASH_VULNERABILITY_MULTIPLIER), stat(a -> a.mVulnerabilityDuration, DASH_VULNERABILITY_DURATION))
			.addDashedLine();
	}
}
