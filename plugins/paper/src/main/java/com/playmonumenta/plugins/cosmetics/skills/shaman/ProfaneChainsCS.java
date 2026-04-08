package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Bukkit;
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

public class ProfaneChainsCS extends ChainLightningCS {
	public static final String NAME = "Profane Chains";

	@Override
	public Material getDisplayItem() {
		return Material.CHAIN;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A bloody chain wraps around your enemies.",
			"Bound together, their fate is sealed."
		);
	}

	@Override
	public void chainLightningCast(Player player, LivingEntity prevTarget, LivingEntity target, int i) {
		// list is cleared after this tick
		if (i == 0) {
			lightning(player, prevTarget, target);
		} else {
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> lightning(player, prevTarget, target), i);
		}
	}

	private static void lightning(Player player, LivingEntity prevTarget, LivingEntity target) {
		Location targetLoc = LocationUtils.getHalfHeightLocation(target);

		World world = targetLoc.getWorld();

		world.playSound(targetLoc, Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.1f, 0.7f);
		world.playSound(targetLoc, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 1.5f, 0.66f);
		world.playSound(targetLoc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1.0f, 0.66f);
		world.playSound(targetLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.4f, 1.4f);
		world.playSound(targetLoc, Sound.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 2.0f, 0.5f);


		Location start = LocationUtils.getHalfHeightLocation(prevTarget);
		Vector dir = LocationUtils.getDirectionTo(targetLoc, start);
		start.setDirection(dir);
		start.setPitch(start.getPitch() - 90);

		new PPLine(Particle.DUST_COLOR_TRANSITION, start, targetLoc)
			.countPerMeter(8)
			.delay(4)
			.data(new Particle.DustTransition(Color.RED, Color.MAROON, 1.4f))
			.spawnAsPlayerActive(player);

		Vector up = start.getDirection();
		Vector right = up.clone().crossProduct(dir);
		double distance = start.distance(targetLoc);
		double random = FastUtils.randomDoubleInRange(0, 1000);

		new PPParametric(Particle.REDSTONE, start, (progress, packagedValues) -> {
			double m1 = Math.pow(FastUtils.sin(random + progress * distance), 3) + Math.pow(FastUtils.cos(random + progress * distance * Math.E), 3);
			double m2 = Math.pow(FastUtils.sin(random + progress * distance * Math.E), 3) - Math.pow(FastUtils.cos(random + progress * distance), 3);
			Vector upComponent = up.clone().multiply(m1/ 3);
			Vector rightComponent = right.clone().multiply(m2 / 3);
			packagedValues.location(start.clone().add(dir.clone().multiply(distance * progress)).add(upComponent).add(rightComponent));
		}).count((int) (8 * distance))
			.delay(4)
			.data(new Particle.DustOptions(Color.fromRGB(0x6b0000), 1.8f))
			.spawnAsEntityActive(player);

		new PartialParticle(Particle.CRIT_MAGIC, targetLoc)
			.count(40)
			.delta(0.1, target.getHeight() / 4, 0.1)
			.extra(target.getWidth())
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.DUST_COLOR_TRANSITION, targetLoc, target.getWidth())
			.countPerMeter(5)
			.delta(0.15)
			.data(new Particle.DustTransition(Color.RED, Color.fromRGB(0x6b0000), 2.2f))
			.spawnAsPlayerActive(player);
	}

	@Override
	public void chainLightningSound(Player player) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_BITE, SoundCategory.PLAYERS, 0.5f, 0.9f);
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_WITCH_HURT, SoundCategory.PLAYERS, 0.8f, 0.66f);
	}
}
