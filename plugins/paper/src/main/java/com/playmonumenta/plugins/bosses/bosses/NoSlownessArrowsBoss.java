package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.LivingEntity;

public class NoSlownessArrowsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_no_slowness_arrows";

	public NoSlownessArrowsBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		// This bosstag is implemented in the Mixins repo and only exists here for the sake of tab-completing /bosstag.
	}
}
