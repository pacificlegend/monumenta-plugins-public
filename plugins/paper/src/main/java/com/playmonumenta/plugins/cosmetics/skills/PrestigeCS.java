package com.playmonumenta.plugins.cosmetics.skills;

public interface PrestigeCS extends ScoreBuyableCS {
	String CHALLENGE_POINTS_SCOREBOARD = "ChallengePoints";

	@Override
	default String getScoreboard() {
		return CHALLENGE_POINTS_SCOREBOARD;
	}

	@Override
	default int getScoreCost() {
		return 1;
	}

	@Override
	default String getCostNameSingular() {
		return "Challenge Point";
	}

	@Override
	default CosmeticSkillShopGUI.CSSet getSet() {
		return CosmeticSkillShopGUI.CSSet.PRESTIGE;
	}
}
