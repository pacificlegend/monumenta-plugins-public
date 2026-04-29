package com.playmonumenta.plugins.bosses.bosses.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.aurora.AuroraConditionalTp;
import com.playmonumenta.plugins.bosses.spells.aurora.SpellAuroraSmokeGrenade;
import com.playmonumenta.plugins.bosses.spells.aurora.SpellBladeThrow;
import com.playmonumenta.plugins.bosses.spells.aurora.SpellGalacticCloak;
import com.playmonumenta.plugins.bosses.spells.aurora.SpellShatteredMeteorite;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class AuroraBladeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_aurora_blade";

	public AuroraBladeBoss(Plugin plugin, LivingEntity boss, Location center, Aurora.BlockDestroyer ignored) {
		super(plugin, identityTag, boss);

		List<Spell> actives = List.of(
			new SpellGalacticCloak(plugin, boss, center),
			new SpellShatteredMeteorite(plugin, boss, center),
			new SpellBladeThrow(plugin, boss),
			new SpellAuroraSmokeGrenade(plugin, boss)
		);

		List<Spell> passiveSpells = List.of(
			new SpellShieldStun(8 * 20),
			new AuroraConditionalTp(boss, center, boss::teleport)
		);

		BossBarManager bossBar = new BossBarManager(boss, Aurora.DETECTION_RANGE, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, null, true);
		constructBoss(new SpellManager(actives), passiveSpells, Aurora.DETECTION_RANGE, bossBar, 100, PASSIVE_RUN_INTERVAL_DEFAULT, true);
	}
}
