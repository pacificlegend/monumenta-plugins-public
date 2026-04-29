package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.SpellBaseGrenadeLauncher;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.ArrayList;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellAuroraSmokeGrenade extends SpellBaseGrenadeLauncher {
	public static final Material GRENADE_MATERIAL = Material.SCULK_SHRIEKER;
	public static final int LOBS = 1;
	public static final int LOBS_DELAY = 5;
	public static final int DURATION = 15 * 20;
	public static final int DEBUFF_DURATION = 6 * 20;
	public static final int COOLDOWN = 7 * 20;
	public static final int TELEGRAPH_DURATION = 2 * 20;
	private static final double RADIUS = 4;
	private static final String DISPLAY_TAG = "AuroraSmokeGrenadeDisplay";

	private final LivingEntity mBoss;

	public SpellAuroraSmokeGrenade(Plugin plugin, LivingEntity boss) {
		super(plugin, boss, GRENADE_MATERIAL, false, 0, LOBS, LOBS_DELAY, DURATION, COOLDOWN, TELEGRAPH_DURATION, RADIUS,
			() -> {
				// Grenade Targets
				return Aurora.playersInRange(boss.getLocation());
			},
			(Location loc) -> {
				// Explosion Targets
				return new ArrayList<>();
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
				world.playSound(loc, Sound.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.8f, 1.5f);
				world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.HOSTILE, 1.8f, 1.8f);
				world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 2.5f, 0.8f);

				Display display = world.spawn(loc, ItemDisplay.class, core -> {
					core.setItemStack(new ItemStack(Material.SCULK_SHRIEKER));
					core.setTransformation(new Transformation(
						new Vector3f(0, 0.3f, 0),
						new Quaternionf(),
						new Vector3f(1),
						new Quaternionf()
					));

					core.addScoreboardTag(DISPLAY_TAG);
					EntityUtils.setRemoveEntityOnUnload(core);
				});

				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;
						if (mTicks >= TELEGRAPH_DURATION) {
							if (display.isValid()) {
								display.remove();
							}
							world.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 3.5f, 0.5f);
							world.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 2.5f, 0.8f);
							world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 3.5f, 0.7f);

							new PartialParticle(Particle.SMOKE_LARGE, loc)
								.count(60)
								.extra(RADIUS / 10)
								.spawnAsBoss();

							new Hitbox.SphereHitbox(loc, RADIUS).getHitPlayers(true).forEach(player -> {
								player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DEBUFF_DURATION, 9, true, false, false));
								player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, DEBUFF_DURATION, 9, true, false, false));
								player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, DEBUFF_DURATION, 9, true, false, false));
							});
							this.cancel();
						} else if (mTicks % 4 == 0) {
							world.playSound(loc, Sound.BLOCK_LEVER_CLICK, SoundCategory.HOSTILE, 2.5f, 0.6f + (float) mTicks / TELEGRAPH_DURATION);
						}
					}
				}.runTaskTimer(plugin, 0, 1);
			},
			(LivingEntity b, LivingEntity target, Location loc) -> {
				// Hit Action on Explosion Targets
			},
			(Location loc) -> {
				// Ring Aesthetics
			},
			(Location loc, int ticks) -> {
				// Center Aesthetics
				if (ticks % 10 == 0) {
					ParticleUtils.drawCircleTelegraph(loc.clone().add(0, 0.1, 0), RADIUS, 40, 1, 0, 0.1, true, Particle.SMOKE_LARGE, plugin, boss);
				}
				new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 0.1, 0), 1).extra(0)
					.delta(RADIUS / 2.6, 0, RADIUS / 3)
					.data(new Particle.DustOptions(Color.RED, 1.5f))
					.spawnAsBoss();
			},
			(LivingEntity b, LivingEntity target, Location loc) -> {
				// Lingering Effect Actions
			},
			LoSPool.LibraryPool.EMPTY, 0.3f, 0.1, 0.1, null,
			(Location loc) -> {
				// Landing Location telegraph
				ParticleUtils.drawCircleTelegraph(loc, RADIUS, 32, 1, 1, 0.1, false, Particle.SPELL_WITCH, plugin, boss);
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
