package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SelfReflection extends Ability {
	private static final String SCOREBOARD = "SelfReflection";
	private static final int POINT_COST = 6;
	private static final int MIMIC_DELAY = 30;
	private static final double DAMAGE_PENALTY = -0.25;
	private static final List<ClassAbility> IGNORED_SPELLS = List.of(
		ClassAbility.ALCHEMIST_POTION,
		ClassAbility.LUMINITE_DRILL
	);

	public static final AbilityInfo<SelfReflection> INFO =
		new SnowPerkGui.SnowPerkInfo<>(SelfReflection.class, "Self-Reflection", SelfReflection::new)
			.snowPointCost(POINT_COST)
			.unlockReq(player -> ScoreboardUtils.getScoreboardValue(player, SnowPerkGui.COMPLETIONS).orElse(0) >= 15)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.PAINTING)
			.description(getDescription());

	private boolean mCurrentlyCasting;
	private final List<Location> mPendingCasts;
	private final BukkitRunnable mParticleRunnable;

	public SelfReflection(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mCurrentlyCasting = false;
		mPendingCasts = new ArrayList<>();

		mParticleRunnable = new BukkitRunnable() {
			final Deque<Location> mEchoTrail = new ConcurrentLinkedDeque<>();

			@Override
			public void run() {
				// If there is a pending cast, show the Echo going to the pending cast point
				if (mEchoTrail.size() >= MIMIC_DELAY) {
					Location popLoc = mEchoTrail.removeLast();
					if (!mPendingCasts.isEmpty()) {
						new PartialParticle(Particle.END_ROD, popLoc, 1).spawnAsPlayerPassive(mPlayer);
						new PartialParticle(Particle.CRIT_MAGIC, popLoc, 3).delta(0.1).extra(0.15).spawnAsPlayerPassive(mPlayer);
					}
				}

				// Display all pending cast points
				for (Location pendingLoc : mPendingCasts) {
					Location centerLoc = pendingLoc.clone().add(0, 1, 0);
					Vector up = new Vector(0, 1, 0).multiply(0.6);
					Vector right = centerLoc.getDirection().getCrossProduct(up).multiply(0.5);

					Location loc1 = centerLoc.clone().add(up);
					Location loc2 = centerLoc.clone().add(right);
					Location loc3 = centerLoc.clone().subtract(up);
					Location loc4 = centerLoc.clone().subtract(right);

					List<Location> particleLocs = List.of(loc1, loc2, loc3, loc4);

					new PPLine(Particle.ELECTRIC_SPARK, particleLocs.get(0), particleLocs.get(1)).countPerMeter(3).spawnAsPlayerPassive(mPlayer);
					new PPLine(Particle.ELECTRIC_SPARK, particleLocs.get(1), particleLocs.get(2)).countPerMeter(3).spawnAsPlayerPassive(mPlayer);
					new PPLine(Particle.ELECTRIC_SPARK, particleLocs.get(2), particleLocs.get(3)).countPerMeter(3).spawnAsPlayerPassive(mPlayer);
					new PPLine(Particle.ELECTRIC_SPARK, particleLocs.get(3), particleLocs.get(0)).countPerMeter(3).spawnAsPlayerPassive(mPlayer);
				}

				Location loc = LocationUtils.getEntityCenter(mPlayer);
				mEchoTrail.addFirst(loc);
			}

		};
		cancelOnDeath(mParticleRunnable.runTaskTimer(plugin, 0, 1));
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (type == DamageEvent.DamageType.TRUE) {
			return false;
		}
		if (event.getAbility() != null && !event.getAbility().isFake()) {
			event.setFlatDamage(event.getDamage() * (1 + DAMAGE_PENALTY));
		}
		return false;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		ClassAbility spell = event.getSpell();
		if (mCurrentlyCasting || spell == null || IGNORED_SPELLS.contains(spell)) {
			return true;
		}

		Location castLocation = mPlayer.getLocation();

		recastAtLocation(event.getAbility(), event.getAbility().getInfo(), spell, castLocation);

		return true;
	}

	@SuppressWarnings("unchecked")
	private <T extends Ability> void recastAtLocation(Ability ability, AbilityInfo<T> info, ClassAbility spell, Location originalLoc) {
		// need to cast the generic Ability to the real ability's class
		T realAbility = (T) ability;

		// ignore abilities that we can't retrigger
		List<AbilityTriggerInfo<T>> triggers = info.getTriggers();
		if (triggers.isEmpty()) {
			return;
		}
		AbilityTriggerInfo.TriggerAction<T> method = triggers.getFirst().getAction();
		Runnable action = () -> method.run(realAbility);

		mPendingCasts.add(originalLoc);

		Location fxLoc = LocationUtils.getEntityCenter(mPlayer);
		fxLoc.setPitch(0);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			float pitch = FastUtils.randomFloatInRange(1.35f, 1.75f);
			mPlayer.getWorld().playSound(fxLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.9f, pitch, 0);
			mPlayer.getWorld().playSound(fxLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.9f, pitch, 0);
		}, 3);

		cancelOnDeath(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			// we want to cast the ability from the original spot it was cast from, so fake the player's location when the ability runs
			Location currentLoc = mPlayer.getLocation();
			List<CooldownTimers.Cooldown> originalCooldowns = mPlugin.mTimers.getCooldownList(mPlayer.getUniqueId(), spell);
			try {
				// the ability won't run if it's on cooldown, so temporarily clear its cooldown
				mPlugin.mTimers.removeCooldown(mPlayer, spell, false);
				if (ability instanceof MultipleChargeAbility chargeAbility) {
					chargeAbility.incrementCharges(); // charge abilities need to have at least 1 charge
				}

				NmsUtils.getVersionAdapter().setEntityLocation(mPlayer, originalLoc.toVector(), originalLoc.getYaw(), originalLoc.getPitch());
				mPendingCasts.remove(originalLoc);
				mCurrentlyCasting = true;
				action.run();

				mirrorFX(originalLoc);
			} finally {
				mCurrentlyCasting = false;
				NmsUtils.getVersionAdapter().setEntityLocation(mPlayer, currentLoc.toVector(), currentLoc.getYaw(), currentLoc.getPitch());
				NmsUtils.getVersionAdapter().forceSyncEntityPositionData(mPlayer);
				mPlugin.mTimers.replaceCooldownList(mPlayer, spell, originalCooldowns);
			}
		}, MIMIC_DELAY));
	}

	@Override
	public void invalidate() {
		mParticleRunnable.cancel();
	}

	private void mirrorFX(Location loc) {
		World world = loc.getWorld();
		Location fxLoc = loc.clone().add(0, 1, 0);
		new PartialParticle(Particle.END_ROD, fxLoc, 15).delta(0, 0.5, 0).extra(0.15).extraVariance(0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, fxLoc, 30).delta(0.25, 0.75, 0.25).extra(0.6).extraVariance(0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, fxLoc, 70).delta(0.75, 1.25, 0.75)
			.data(new Particle.DustTransition(Color.fromRGB(0xFFFFFF), Color.fromRGB(0x66F3CC), 1f)).spawnAsPlayerActive(mPlayer);
		float pitch = FastUtils.randomFloatInRange(1.25f, 2f);
		world.playSound(fxLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 2f, pitch, 0);
		world.playSound(fxLoc, Sound.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.PLAYERS, 2f, pitch, 0);
		Location mirrorLoc = fxLoc.clone();
		mirrorLoc.setPitch(0);
		BlockDisplay mirror = world.spawn(mirrorLoc, BlockDisplay.class, display -> {
			GlassPane blockData = (GlassPane) Material.WHITE_STAINED_GLASS_PANE.createBlockData();
			blockData.setFace(BlockFace.EAST, true);
			blockData.setFace(BlockFace.WEST, true);

			display.setBlock(blockData);
			display.setTransformation(new Transformation(new Vector3f(-0.5f, -1f, -0.5f), new Quaternionf(), new Vector3f(1f, 2f, 1f), new Quaternionf()));
			display.setPersistent(false);
			EntityUtils.setRemoveEntityOnUnload(display);
		});
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mirror.remove();
			new PartialParticle(Particle.END_ROD, fxLoc, 8).delta(0, 0.5, 0).extra(0.07).extraVariance(0.03).spawnAsPlayerActive(mPlayer);
		}, 20);
	}

	public static Description<SelfReflection> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.ACHIEVEMENT_ARROW_COLOR)
			.addLine("*Unlock:* *Win 15 runs of Coalrupted Sierhaven.*").styles(DescriptionUtils.REQUIREMENT_LABEL, DescriptionUtils.REQUIREMENT_TEXT)
			.addDashedLine()
			.addIfElse((a, p) -> ScoreboardUtils.getScoreboardValue(p, SnowPerkGui.COMPLETIONS).orElse(0) >= 15,
				desc -> desc.addLine("An *Echo* follows your steps and mimics all").styles(Style.style(TextColor.color(0xB3FFD7)))
					.addLine("abilities you cast, after a %t delay.").statValues(stat(MIMIC_DELAY))
					.addLine()
					.addLine("Passively deal *0.75x* less ability damage.").statValues(stat(DAMAGE_PENALTY)).styles(DescriptionUtils.RED),
				desc -> desc.addLine("*An Echo follows your steps and mimics all*").styles(DescriptionUtils.OBFUSCATED)
					.addLine("*abilities you cast, after a Xs delay.*").styles(DescriptionUtils.OBFUSCATED)
					.addLine()
					.addLine("*Passively deal 0.75x less ability damage.*").styles(DescriptionUtils.OBFUSCATED))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
