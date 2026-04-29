package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.ChargedSpell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class SwingHandBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_swinghand";

	@BossParam(help = "Boss will swing their hand")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Should the boss swing their mainhand? Otherwise swing their offhand.")
		public boolean MAINHAND = true;

		@BossParam(help = "Delay before swinging.")
		public int DELAY = Constants.TICKS_PER_SECOND * 5;

		@BossParam(help = "Time in ticks before the boss swing their hand.")
		public int COOLDOWN = Constants.TICKS_PER_SECOND * 14;

		@BossParam(help = "Range in blocks that players must be in before this passive spell will run")
		public int DETECTION = 0;

		@BossParam(help = "Amount of casts of the swing per cooldown")
		public int CHARGE = 1;

		@BossParam(help = "Interval in ticks between charge casts")
		public int CHARGE_INTERVAL = 40;
	}

	public SwingHandBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SwingHandBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new SwingHandBoss.Parameters());

		ChargedSpell spell = new ChargedSpell(p.CHARGE, p.CHARGE_INTERVAL, p.COOLDOWN) {
			@Override
			public void launch() {
				if (mBoss instanceof Mob mob) {
					if (p.MAINHAND) {
						mob.swingMainHand();
					} else {
						mob.swingOffHand();
					}
				}
			}
		};

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
