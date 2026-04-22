package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SniffysBlessing extends Ability {
	private static final String SCOREBOARD = "SniffysBlessing";
	private static final String ADVANCEMENT_REQ = "monumenta:challenges/r1/coalrupted/sniffy";
	private static final int POINT_COST = 8;

	public static final AbilityInfo<SniffysBlessing> INFO =
		new SnowPerkGui.SnowPerkInfo<>(SniffysBlessing.class, "Sniffy's Blessing", SniffysBlessing::new)
			.unlockReq(player -> AdvancementUtils.checkAdvancement(player, ADVANCEMENT_REQ))
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.SNIFFER_EGG)
			.description(getDescription());

	public SniffysBlessing(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		// will make all base class abilities count as Level 2 until their abilities are refreshed.
		// Coalrupted runs a /refreshclass on anyone exiting the instance, so this should not be able to smuggled out... hopefully
		Bukkit.getScheduler().runTask(plugin, () -> {
			List<AbilityInfo<?>> baseClassAbilities = AbilityManager.getBaseOverworldAbilities();
			Collection<Ability> playerAbilities = plugin.mAbilityManager.getPlayerAbilities(player).getAbilities();
			for (Ability ability : playerAbilities) {
				if (baseClassAbilities.contains(ability.getInfo())) {
					ability.setAbilityScore(2);
				}
			}
		});
	}

	public static Description<SniffysBlessing> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(Style.style(TextColor.color(0x551000)))
			.addLine("*Unlock:* *Defeat Corrupted Sniffy.*").styles(Style.style(TextColor.color(0x963219)), Style.style(TextColor.color(0xB8531E)))
			.addDashedLine()
			.addIfElse((a, p) -> AdvancementUtils.checkAdvancement(p, ADVANCEMENT_REQ),
				desc -> desc.addLine("All of your *Level 1* class abilities").styles(DescriptionUtils.WHITE)
					.addLine("are raised to *Level 2*.").styles(DescriptionUtils.WHITE),
				desc -> desc.addLine("*All of your Level 1 class abilities*").styles(DescriptionUtils.OBFUSCATED)
					.addLine("*are raised to Level 2.*").styles(DescriptionUtils.OBFUSCATED))
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
