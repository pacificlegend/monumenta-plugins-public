package com.playmonumenta.plugins.cosmetics.skills;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public interface ScoreBuyableCS extends BuyableCS {
	String getScoreboard();

	int getScoreCost();

	String getCostNameSingular();

	@Override
	default boolean attemptPurchase(Player player) {
		String scoreboard = getScoreboard();
		int score = ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0);
		int priceNum = getScoreCost();
		if (score < priceNum) {
			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
			player.sendMessage(Component.text("You don't have enough " + getCostNameSingular() + "s to buy this cosmetic skill!", NamedTextColor.RED));
			return false;
		}
		ScoreboardUtils.setScoreboardValue(player, scoreboard, score - priceNum);
		return true;
	}

	@Override
	default List<String> getCostDescription() {
		int priceNum = getScoreCost();
		return List.of(priceNum + " " + getCostNameSingular() + (priceNum == 1 ? "" : "s"));
	}
}
