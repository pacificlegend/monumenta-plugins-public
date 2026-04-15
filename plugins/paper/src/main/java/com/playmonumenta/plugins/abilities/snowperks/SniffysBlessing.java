package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SniffysBlessing extends Ability {
	private static final String SCOREBOARD = "SniffysBlessing";
	private static final int POINT_COST = 10;

	public static final AbilityInfo<SniffysBlessing> INFO =
		new SnowPerkGui.SnowPerkInfo<>(SniffysBlessing.class, "Sniffy's Blessing", SniffysBlessing::new)
			.unlockReq(player -> false)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.SNIFFER_EGG)
			.description(getDescription());

	public SniffysBlessing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Not implemented yet! Just a dummy perk just to hint at future Sniffy content.
	}

	public static Description<SniffysBlessing> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(Style.style(TextColor.color(0x551000)))
			.addLine("*Unlock:* *Open the gates.*").styles(Style.style(TextColor.color(0x963219)), Style.style(TextColor.color(0xB8531E)))
			.addDashedLine()
			.addLine("*All of your class abilities are set to Level 2.*").styles(DescriptionUtils.OBFUSCATED)
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
