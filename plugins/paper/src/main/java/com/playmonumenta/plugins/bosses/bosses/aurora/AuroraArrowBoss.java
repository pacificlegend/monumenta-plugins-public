package com.playmonumenta.plugins.bosses.bosses.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.aurora.AuroraConditionalTp;
import com.playmonumenta.plugins.bosses.spells.aurora.SpellAstralHuntingTrap;
import com.playmonumenta.plugins.bosses.spells.aurora.SpellAuroraVolley;
import com.playmonumenta.plugins.bosses.spells.aurora.SpellBlightingVortex;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class AuroraArrowBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_aurora_arrow";

	public AuroraArrowBoss(Plugin plugin, LivingEntity boss, Location center, Aurora.BlockDestroyer blockDestroyer) {
		super(plugin, identityTag, boss);

		List<Spell> actives = List.of(
			new SpellBlightingVortex(plugin, boss, center),
			new SpellAstralHuntingTrap(plugin, boss, blockDestroyer),
			new SpellAuroraVolley(plugin, boss)
		);

		BossBarManager bossBar = new BossBarManager(boss, Aurora.DETECTION_RANGE, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, null, true);
		constructBoss(new SpellManager(actives), List.of(new AuroraConditionalTp(boss, center, boss::teleport)), Aurora.DETECTION_RANGE, bossBar, 100, PASSIVE_RUN_INTERVAL_DEFAULT, true);
	}
}
