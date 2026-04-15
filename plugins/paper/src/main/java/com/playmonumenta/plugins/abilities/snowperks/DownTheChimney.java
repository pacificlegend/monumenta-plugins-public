package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class DownTheChimney extends Ability {
	private static final String SCOREBOARD = "DownTheChimney";
	private static final int POINT_COST = 0;

	public static final AbilityInfo<DownTheChimney> INFO =
		new SnowPerkGui.SnowPerkInfo<>(DownTheChimney.class, "Down The Chimney", DownTheChimney::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.TINTED_GLASS)
			.description(getDescription());

	public DownTheChimney(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Perk is handled in Coalrupted start mechs!
	}

	public static Description<DownTheChimney> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Start the game inside the Sierhaven")
			.addLine("Academy's reset chute.")
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
