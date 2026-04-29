package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellZone;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class ZoneBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_zone";

	public enum ParticleShape {
		NONE, FLOOR, WALL
	}

	@BossParam(help = "Entities outside the safe zone and within targeters are damaged")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that players must be in before this passive spell will run")
		public int DETECTION = 0;

		@BossParam(help = "Delay before starting the safe zone")
		public int DELAY = 0;

		@BossParam(help = "Radius of the safe zone")
		public int SAFE_RADIUS = 12;

		@BossParam(help = "Height of the safe zone")
		public int SAFE_HEIGHT = 4;

		@BossParam(help = "Entities to target, when they are not within the safe zone")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;

		@BossParam(help = "Damage when not within the safe zone")
		public double DAMAGE = 5;

		@BossParam(help = "Damage Type")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;

		@BossParam(help = "Time period in ticks that governs how often effects are applied")
		public int PASSIVE_RATE = PASSIVE_RUN_INTERVAL_DEFAULT;

		@BossParam(help = "Should being outside the safe zone respect player's immunity frames?")
		public boolean RESPECT_IFRAMES = true;

		@BossParam(help = "Effects applied to the player when outside the safe zone")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Percentage of health dealt as true damage, does not respect iframes")
		public double TRUE_DAMAGE_PERCENTAGE = 0;

		@BossParam(help = "Can the passive be disabled by stuns and silences")
		public boolean CANCELABLE = false;

		@BossParam(help = "Particle outline of the safe zone")
		public ParticleShape PARTICLE_SHAPE = ParticleShape.FLOOR;

		@BossParam(help = "Particles summoned for the safe zone")
		public ParticlesList PARTICLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 3, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(Color.WHITE, 1.0f)))
			.build();

		@BossParam(help = "Sound for when the player is outside the safe zone")
		public SoundsList SOUND = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_CAT_HISS, 1.0f, 1.5f))
			.build();

		@BossParam(help = "Density of the ring. Density = Particle Count / (2pi * Radius) (in ring mode)")
		public double PARTICLE_DENSITY = 1;

		@BossParam(help = "Name of the spell")
		public String NAME = "";

	}

	ZoneBoss.Parameters mParameters = new ZoneBoss.Parameters();

	public ZoneBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(List.of(
			new SpellZone(plugin, boss, mParameters)
		));
		ZoneBoss.Parameters.getParameters(boss, identityTag, mParameters);
		super.constructBoss(activeSpells, Collections.emptyList(), mParameters.DETECTION, null, mParameters.DELAY);
	}
}
