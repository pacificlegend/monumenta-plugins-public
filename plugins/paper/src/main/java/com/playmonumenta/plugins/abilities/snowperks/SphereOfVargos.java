package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SphereOfVargos extends Ability {
	private static final String SCOREBOARD = "SphereOfVargos";
	private static final int POINT_COST = 1;

	public static final AbilityInfo<SphereOfVargos> INFO =
		new SnowPerkGui.SnowPerkInfo<>(SphereOfVargos.class, "Sphere of Vargos", SphereOfVargos::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.ENDER_EYE)
			.description(getDescription());

	public SphereOfVargos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Perk is handled in Coalrupted start mechs!
	}

	public static Description<SphereOfVargos> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Start the game with an *Ender Pearl*.").styles(Style.style(TextColor.color(0x84D41)))
			.addLine()
			.addStat("Cost: %d Snow Point").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
