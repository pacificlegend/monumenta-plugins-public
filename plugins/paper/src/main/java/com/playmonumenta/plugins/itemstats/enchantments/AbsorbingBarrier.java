package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AbsorbingBarrier implements Enchantment {
	private static final int HIT_DURATION_REQUIREMENT = Constants.TICKS_PER_SECOND * 4;
	private static final int BARRIER_FULL_RECHARGE_TIME = 3;
	private static final double HEALTH_REQ = 0.75;
	private static final double ABSORPTION_PERCENTAGE_PER_LEVEL = 0.1;

	private static final HashMap<UUID, Integer> BARRIER_MAP = new HashMap<>();

	@Override
	public String getName() {
		return "Absorbing Barrier";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ABSORBING_BARRIER;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		double level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ABSORBING_BARRIER);
		if (level <= 0) {
			BARRIER_MAP.remove(player.getUniqueId());
		}
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// Debuffs/Environmental hazards count as a hit
		if (event.isBlocked()) {
			return;
		}

		// Ensure hit only occurs once a tick
		if (MetadataUtils.checkOnceThisTick(plugin, player, "AbsorbingBarrierHit")) {
			UUID uuid = player.getUniqueId();
			int tick = BARRIER_MAP.getOrDefault(uuid, 0);
			boolean isAboveReq = player.getHealth() >= EntityUtils.getMaxHealth(player) * HEALTH_REQ;

			if (tick >= HIT_DURATION_REQUIREMENT && isAboveReq) { // Audio when taking damage with barrier up
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1, 0.5f);
			}

			BARRIER_MAP.put(uuid, 0);
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && player.getGameMode() != GameMode.SPECTATOR) {
			UUID uuid = player.getUniqueId();

			int tick = BARRIER_MAP.merge(uuid, Constants.TICKS_PER_SECOND, Integer::sum);
			double maxHealth = EntityUtils.getMaxHealth(player);

			// Restart timer if lower than the health requirement
			if (player.getHealth() <= maxHealth * HEALTH_REQ) {
				BARRIER_MAP.put(uuid, 0);
				return;
			}

			if (tick >= HIT_DURATION_REQUIREMENT) {
				double maxAbsorption = maxHealth * level * ABSORPTION_PERCENTAGE_PER_LEVEL;

				if (AbsorptionUtils.getAbsorption(player) < maxAbsorption) {
					player.playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.65f, 1f);
					player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.65f, 1f);
				}

				AbsorptionUtils.addAbsorption(player, maxAbsorption / BARRIER_FULL_RECHARGE_TIME, maxAbsorption, Constants.TICKS_PER_SECOND * 3);
			}
		}
	}

}
