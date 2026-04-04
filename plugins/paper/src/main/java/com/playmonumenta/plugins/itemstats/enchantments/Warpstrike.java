package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

public class Warpstrike implements Enchantment {

	// There is a potential bug vector with Warpstrike. Killing a mob through a bedrock wall could get you to otherwise inaccessible areas.
	// This is possible via Tesseract of Death + any AoE ability that goes through walls.
	// Currently (4th April 2026) this presents no issue. Warpstrike is only available within Coalrupted Sierhaven and that place blocks Death Tess.
	// However, any future users of the enchantment should beware these dangers.

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.WARPSTRIKE;
	}

	@Override
	public String getName() {
		return "Warpstrike";
	}

	@Override
	public void onKill(Plugin plugin, Player player, double level, EntityDeathEvent event, LivingEntity enemy) {
		Vector vector = player.getLocation().getDirection();
		Location playerEndLocation = enemy.getLocation().setDirection(vector);
		PlayerUtils.playerTeleport(player, playerEndLocation);
		enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1.25f);
	}
}
