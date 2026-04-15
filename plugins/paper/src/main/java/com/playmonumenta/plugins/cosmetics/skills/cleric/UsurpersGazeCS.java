package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.cosmetics.skills.IntruderCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class UsurpersGazeCS extends TouchofRadianceCS implements IntruderCS {
	public static final String NAME = "Usurper's Gaze";

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_EYE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A sharp, piercing gaze.",
			"The Shadow makes its move.",
			"A new ruler has ascended to the throne."
		);
	}

	@Override
	public NamedTextColor glowColor() {
		return NamedTextColor.RED;
	}

	@Override
	public void tickEffect(LivingEntity entity) {
		Location crownLoc = entity.getLocation().add(0, entity.getHeight() + 1, 0);

		new PPCircle(Particle.DUST_COLOR_TRANSITION, crownLoc, 0.75)
			.count(50)
			.data(new Particle.DustTransition(Color.RED, Color.fromRGB(0x6b0000), 1.3f))
			.innerRadiusFactor(0.7)
			.delta(0.01)
			.spawnAsEntityBuff(entity);


		for (int deg = 0; deg < 360; deg += 72) {
			Vector vec = new Vector(FastUtils.sinDeg(deg) * 0.75, -0.3, FastUtils.cosDeg(deg) * 0.75);
			new PPPillar(Particle.DUST_COLOR_TRANSITION, crownLoc.clone().add(vec), 0.8)
				.count(10)
				.data(new Particle.DustTransition(Color.RED, Color.fromRGB(0x6b0000), 1.16f))
				.spawnAsEntityBuff(entity);
		}
	}

	@Override
	public void loseEffect(LivingEntity entity) {
		World world = entity.getWorld();
		Location crownLoc = entity.getLocation().add(0, entity.getHeight() + 0.4, 0);
		world.playSound(crownLoc, Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.PLAYERS, 0.6f, 1.0f);
		world.playSound(crownLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.1f, 0.9f);
	}

	@Override
	public void castOnAlly(Player player, LivingEntity target) {
		World world = target.getWorld();
		Location targetLoc = target.getEyeLocation();
		Location pLoc = player.getEyeLocation();
		pLoc.setPitch(pLoc.getPitch() - 90);

		world.playSound(targetLoc, Sound.ENTITY_ALLAY_ITEM_THROWN, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(pLoc, Sound.ENTITY_ALLAY_ITEM_THROWN, SoundCategory.PLAYERS, 0.6f, 0.1f);
		world.playSound(pLoc, Sound.ENTITY_ALLAY_ITEM_THROWN, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(pLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.5f, 1.5f);
		world.playSound(pLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 1.0f);
		world.playSound(pLoc, Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1.25f, 0.7f);

		doParticles(player, target, targetLoc, pLoc);
	}

	@Override
	public void castOnHeretic(Player player, LivingEntity target) {
		World world = target.getWorld();
		Location targetLoc = target.getEyeLocation();
		Location pLoc = player.getEyeLocation();
		pLoc.setPitch(pLoc.getPitch() - 90);

		world.playSound(pLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(pLoc, Sound.ENTITY_ALLAY_ITEM_THROWN, SoundCategory.PLAYERS, 1.5f, 1.0f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.5f, 0.6f);
		world.playSound(pLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 0.7f);
		world.playSound(pLoc, Sound.AMBIENT_CAVE, SoundCategory.PLAYERS, 1.25f, 2f, 34);
		world.playSound(pLoc, Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1.25f, 0.5f);
		world.playSound(pLoc, Sound.ENTITY_SKELETON_HORSE_HURT, SoundCategory.PLAYERS, 0.5f, 0.6f);

		doParticles(player, target, targetLoc, pLoc);
	}

	@Override
	public void applyWeakness(Player player, LivingEntity target, double radius) {
		new PPCircle(Particle.CRIT_MAGIC, target.getLocation(), target.getWidth())
			.count(90)
			.delta(0, 1, 0)
			.directionalMode(true)
			.extraRange(0.1, target.getHeight())
			.spawnAsPlayerActive(player);
	}

	private void doParticles(Player player, LivingEntity target, Location targetLoc, Location pLoc) {
		Vector direction = LocationUtils.getDirectionTo(targetLoc, pLoc);
		Vector up = pLoc.getDirection();
		Vector horizontal = up.clone().crossProduct(direction).normalize();
		for (int i = -2; i <= 2; i++) {
			Vector horizontalAdd = horizontal.clone().multiply(i * 1.5);
			double cosI = FastUtils.cos(i);
			Vector upAdd = up.clone().multiply(cosI * 1.5 + 0.8);
			Location eyeLoc = pLoc.clone().add(horizontalAdd).add(upAdd).add(direction);
			spawnEye(player, eyeLoc, LocationUtils.getDirectionTo(targetLoc, eyeLoc));

			new PPCircle(Particle.TRIAL_SPAWNER_DETECTION, player.getLocation(), player.getWidth())
				.count(90)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.TRIAL_SPAWNER_DETECTION, target.getLocation(), target.getWidth())
				.count(90)
				.spawnAsPlayerActive(player);
			ParticleUtils.launchOrb(direction, eyeLoc, player, target, 300, null, new Particle.DustOptions(Color.RED, 0.7f), livingEntity -> {
			});
		}
	}

	private void spawnEye(Player player, Location loc, Vector dir) {
		loc.setDirection(dir);
		loc.setPitch(loc.getPitch() - 90);
		Vector up = loc.getDirection();
		Vector horizontal = up.clone().crossProduct(dir);
		new PPParametric(Particle.REDSTONE, loc, (parameter, packagedValues) -> {
			int signDeterminer = (int) (parameter * 4);
			double t = (parameter * 4) % 1;
			double x = t * (signDeterminer % 2 == 0 ? 1 : -1);
			double sqrtY = 1 - Math.sqrt(t);
			double y = sqrtY * sqrtY * (signDeterminer >= 2 ? 1 : -1);

			packagedValues.location(loc.clone().add(up.clone().multiply(x)).add(horizontal.clone().multiply(y)));
		}).count(100).data(new Particle.DustOptions(Color.MAROON, 1.2f)).spawnAsPlayerActive(player);

		new PartialParticle(Particle.END_ROD, loc)
			.minimumCount(1)
			.delta(0, 1, 0)
			.extra(0.01)
			.directionalMode(true)
			.spawnAsPlayerActive(player);
	}
}
