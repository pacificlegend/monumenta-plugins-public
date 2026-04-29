package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class SpellMoonlightSlash extends Spell {
	private static final String SPELL_NAME = "Moonlight Slash";
	private static final Color COLOR_START = Color.fromRGB(0xa355ff);
	private static final Color COLOR_END = Color.fromRGB(0x4320af);
	private static final double DAMAGE = 32;
	private static final float KNOCKBACK_P4 = 0.3f;
	private static final int TELEGRAPH_DURATION = 30;
	private static final int RINGS_NORMAL = 14;
	private static final int RINGS_P4 = 20;
	private static final double ANGLE_NORMAL = 70;
	private static final double ANGLE_P4 = 100;
	public static final int COOLDOWN_REDUCTION = 3 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final SpellCooldownManager mSpellCooldownManager;
	private final Consumer<Player> mCooldownReducer;

	private final boolean mPhase4;
	private final int mRings;
	private final double mAngle;

	public SpellMoonlightSlash(Plugin plugin, LivingEntity boss, double passiveCooldownMult, boolean p4, Consumer<Player> cooldownReducer) {
		mPlugin = plugin;
		mBoss = boss;
		mPhase4 = p4;
		mRings = p4 ? RINGS_P4 : RINGS_NORMAL;
		mAngle = p4 ? ANGLE_P4 : ANGLE_NORMAL;
		// boss cannot cast moonblade while aura farming!
		int cooldown = (int) (TELEGRAPH_DURATION + Aurora.SPELL_INTERVAL * passiveCooldownMult * (p4 ? 0.66 : 1));
		mSpellCooldownManager = new SpellCooldownManager(cooldown, 50, boss::isValid, () -> boss.hasAI() && boss.hasGravity());
		mCooldownReducer = cooldownReducer;
	}

	@Override
	public void run() {
		if (mSpellCooldownManager.onCooldown()) {
			return;
		}
		mSpellCooldownManager.setOnCooldown();

		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();

		// Sounds
		world.playSound(bossLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 1.1f, 1.6f);
		world.playSound(bossLoc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 1.1f, 1.0f);

		Location secondLoc = LocationUtils.getHalfHeightLocation(mBoss);
		ParticleUtils.drawCleaveArc(
			secondLoc,
			1.6,
			0,
			100 - mAngle,
			80 + mAngle,
			mRings,
			0,
			0,
			0.2,
			15,
			(loc, rings, angleProgress) ->
				new PartialParticle(Particle.CRIT_MAGIC, loc)
					.delta(0.1, 2 * Math.abs(0.5 - angleProgress), 0.1)
					.spawnAsBoss());

		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Set<Player> hit = new HashSet<>();

			Location bossLocSlash = mBoss.getLocation();
			world.playSound(bossLocSlash, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.9f, 1.2f);
			world.playSound(bossLocSlash, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.2f, 2.0f);
			world.playSound(bossLocSlash, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.0f, 0.7f);
			world.playSound(bossLocSlash, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 0.8f, 0.8f);

			mBoss.swingMainHand();

			Location secondAttackLoc = LocationUtils.getHalfHeightLocation(mBoss).setDirection(secondLoc.getDirection());
			ParticleUtils.drawHalfArc(
				secondAttackLoc,
				1.6,
				-30.0, // randomly flip slash on phase 4
				90 - mAngle,
				90 + mAngle,
				mRings,
				0.2,
				(loc, rings, angleProgress) -> particle(loc, rings, world, hit, bossLoc)
			);

			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				world.playSound(bossLocSlash, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.8f, 1.45f);
				world.playSound(bossLocSlash, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.7f, 2.0f);
				world.playSound(bossLocSlash, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 0.8f, 0.8f);

				ParticleUtils.drawHalfArc(
					secondAttackLoc,
					1.6,
					210.0,
					90 - mAngle,
					90 + mAngle,
					mRings,
					0.2,
					(loc, rings, angleProgress) -> particle(loc, rings, world, hit, bossLoc)
				);
			}, 4);
		}, TELEGRAPH_DURATION));

	}

	private void particle(Location loc, double rings, World world, Set<Player> hit, Location bossLoc) {
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc)
			.data(new Particle.DustTransition(
				ParticleUtils.getTransition(COLOR_START, COLOR_END, Math.pow(rings / mRings, 2)),
				Color.BLACK,
				1.14f))
			.spawnAsBoss();
		new Hitbox.AABBHitbox(world, BoundingBox.of(loc, 0.3, 0.3, 0.3)).getHitPlayers(true).stream()
			.filter(player -> !hit.contains(player))
			.forEach(player -> {
				hit.add(player);
				BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, SPELL_NAME, bossLoc);
				if (mPhase4) {
					MovementUtils.knockAway(bossLoc, player, KNOCKBACK_P4, 0.2f, false);
				}
				mCooldownReducer.accept(player);
			});
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
