package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellAstralHuntingTrap extends SpellBaseGrenadeLauncher {
	public static final String SPELL_NAME = "Explosive Hunting Trap";
	public static final Material GRENADE_MATERIAL = Material.MANGROVE_TRAPDOOR;
	public static final int LOBS = 1;
	public static final int LOBS_DELAY = 5;
	public static final int DURATION = 15 * 20;
	public static final int COOLDOWN = 9 * 20;
	public static final int LINGERING_DURATION = 15 * 20;
	private static final float KNOCKBACK_SPEED = 2.0f;
	private static final double TRAP_RADIUS = 2;
	private static final int DAMAGE = 55;
	private static final String DISPLAY_TAG = "AstralHuntTrapDisplay";
	private final LivingEntity mBoss;

	public SpellAstralHuntingTrap(Plugin plugin, LivingEntity boss, Aurora.BlockDestroyer blockDestroyer) {
		super(plugin, boss, GRENADE_MATERIAL, false, 10, LOBS, LOBS_DELAY, DURATION, COOLDOWN, LINGERING_DURATION, TRAP_RADIUS,
			() -> {
				// Grenade Targets
				return Aurora.playersInRange(boss.getLocation());
			},
			(Location loc) -> {
				// Explosion Targets
				return PlayerUtils.playersInRange(loc, TRAP_RADIUS, true);
			},
			(LivingEntity b, Location loc) -> {
				// Boss Aesthetics
				b.getWorld().playSound(loc, Sound.BLOCK_LEVER_CLICK, SoundCategory.HOSTILE, 3f, 0.5f);
				b.getWorld().playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 3f, 0.6f);
				b.getWorld().playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.HOSTILE, 2f, 1.5f);
			},
			(LivingEntity b, Location loc) -> {
				// Grenade Aesthetics
				new PartialParticle(Particle.SPELL_INSTANT, loc)
					.count(3)
					.extra(0.05)
					.spawnAsBoss();
			},
			(LivingEntity b, Location locRaw) -> {
				// Explosion Aesthetics
				Location loc = locRaw.clone();
				loc.setPitch(0);
				loc.setYaw(0);

				World world = b.getWorld();
				world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.HOSTILE, 1.8f, 1.5f);
				world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.HOSTILE, 1.8f, 0.5f);
				world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.HOSTILE, 2.2f, 1.2f);
				world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1.8f, 2.0f);

				new PPCircle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 0.5, 0), 0.5)
					.count(25)
					.delta(0.1, 0, 0)
					.extra(TRAP_RADIUS)
					.rotateDelta(true)
					.directionalMode(true)
					.spawnAsBoss();

				Display display1 = world.spawn(loc, ItemDisplay.class, door -> {
					door.setItemStack(new ItemStack(Material.MANGROVE_TRAPDOOR));
					door.setTransformation(new Transformation(
						new Vector3f(0, 0.25f, 0),
						new Quaternionf(),
						new Vector3f((float) TRAP_RADIUS),
						new Quaternionf()
					));

					door.addScoreboardTag(DISPLAY_TAG);
					ScoreboardUtils.addEntityToTeam(door, "red");
					door.setGlowing(true);
					EntityUtils.setRemoveEntityOnUnload(door);
				});

				Display display2 = world.spawn(loc, ItemDisplay.class, core -> {
					core.setItemStack(new ItemStack(Material.TNT));
					core.setTransformation(new Transformation(
						new Vector3f(0, -0.15f, 0),
						new Quaternionf(),
						new Vector3f(0.66f),
						new Quaternionf()
					));

					core.addScoreboardTag(DISPLAY_TAG);
					EntityUtils.setRemoveEntityOnUnload(core);
				});

				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					if (display1.isValid()) {
						display1.remove();
					}
					if (display2.isValid()) {
						display2.remove();
					}
				}, LINGERING_DURATION);
			},
			(LivingEntity b, LivingEntity target, Location loc) -> {
				// Hit Action on Explosion Targets
			},
			(Location loc) -> {
				// Ring Aesthetics
			},
			(Location loc, int ticks) -> {
				// Center Aesthetics
				if (ticks % 20 == 0) {
					ParticleUtils.drawCircleTelegraph(loc.clone().add(0, 0.1, 0), TRAP_RADIUS, 40, 1, 0, 0, true, Particle.CRIT, plugin, boss);
				}
				new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 0.1, 0), 1).extra(0)
					.delta(TRAP_RADIUS / 2.6, 0, TRAP_RADIUS / 3)
					.data(new Particle.DustOptions(Color.RED, 1.5f))
					.spawnAsBoss();
			},
			(LivingEntity b, LivingEntity target, Location loc) -> {
				// Lingering Effect Actions
				new PartialParticle(Particle.EXPLOSION_LARGE, loc)
					.count(16)
					.delta(TRAP_RADIUS, 0.5, TRAP_RADIUS)
					.spawnAsBoss();

				World world = b.getWorld();
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.5f, 0.9f);
				world.playSound(loc, Sound.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 2.0f, 0.6f);

				MovementUtils.knockAway(loc, target, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2, false);
				DamageUtils.damage(b, target, DamageEvent.DamageType.BLAST, DAMAGE, null, true, false, SPELL_NAME);
				blockDestroyer.destroy(BlockUtils.getBlocksInSphere(loc, TRAP_RADIUS + 1));

				world.getNearbyEntities(loc, 6, 6, 6, e ->
					e.getScoreboardTags().contains(DISPLAY_TAG)
				).forEach(Entity::remove);
				// PLEASE REFACTOR this spell later!
			},
			() -> {
				// Additional parameters
				return new AdditionalGrenadeParameters(new Location(boss.getWorld(), 0, 0, 0), 0, 0, -1,
					new ChargeUpManager(boss,
						0,
						Component.text("Charging ", NamedTextColor.YELLOW).append(Component.text(SPELL_NAME, NamedTextColor.GOLD)),
						BossBar.Color.YELLOW,
						BossBar.Overlay.PROGRESS,
						Aurora.DETECTION_RANGE
					), true, 1, 0, false);
			},
			(Location loc) -> {
				// Landing Location telegraph
				new PPPillar(Particle.REDSTONE, loc, 2.4)
					.count(32)
					.data(new Particle.DustOptions(Color.RED, 1.56f))
					.spawnAsBoss();

				ParticleUtils.drawCircleTelegraph(loc, TRAP_RADIUS, 32, 1, 20, 0.1, false, Particle.END_ROD, plugin, boss);
			}
		);
		mBoss = boss;
	}

	@Override
	public void cancel() {
		mBoss.getWorld().getNearbyEntities(mBoss.getLocation(), Aurora.DETECTION_RANGE, Aurora.DETECTION_RANGE, Aurora.DETECTION_RANGE, e ->
			e.getScoreboardTags().contains(DISPLAY_TAG)
		).forEach(Entity::remove);

		super.cancel();
	}
}
