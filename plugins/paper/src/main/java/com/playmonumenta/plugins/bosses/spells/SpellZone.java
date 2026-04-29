package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.ZoneBoss;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class SpellZone extends Spell {
	protected final ZoneBoss.Parameters mP;
	protected final LivingEntity mBoss;
	protected final Plugin mPlugin;

	public SpellZone(Plugin plugin, LivingEntity boss, ZoneBoss.Parameters p) {
		mP = p;
		mBoss = boss;
		mPlugin = plugin;
	}

	@Override
	public void run() {
		if (mP.CANCELABLE && EntityUtils.shouldCancelSpells(mBoss)) {
			return;
		}

		List<? extends LivingEntity> targetlist = mP.TARGETS.getTargetsList(mBoss);

		List<Entity> withinHitbox = new Hitbox
			.UprightCylinderHitbox(mBoss.getLocation(), mP.SAFE_HEIGHT, mP.SAFE_RADIUS)
			.getHitEntities(e -> e instanceof LivingEntity);

		for (LivingEntity e : targetlist) {
			if (withinHitbox.contains(e)) {
				continue;
			}

			if (mP.TRUE_DAMAGE_PERCENTAGE > 0) {
				DamageUtils.damagePercentHealth(mBoss, e, mP.TRUE_DAMAGE_PERCENTAGE, false, false, mP.NAME);
			}

			if (mP.DAMAGE > 0) {
				DamageUtils.damage(mBoss, e, mP.DAMAGE_TYPE, mP.DAMAGE, null, mP.RESPECT_IFRAMES, false, mP.NAME);
			}

			mP.EFFECTS.apply(e, mBoss);
			mP.SOUND.play(e.getLocation());
		}

		drawShape();
	}

	private void drawShape() {
		if (mP.PARTICLE_SHAPE == ZoneBoss.ParticleShape.NONE) {
			return;
		}

		Location loc = mBoss.getLocation();
		loc.setY(loc.getY() + 0.1);

		int rings = mP.PARTICLE_SHAPE == ZoneBoss.ParticleShape.FLOOR ? 1 : mP.SAFE_HEIGHT;

		for (double y = 0; y < rings; y++) {
			for (ParticlesList.CParticle c : mP.PARTICLE.getParticleList()) {
				new PPCircle(c.mParticle, loc, mP.SAFE_RADIUS)
					.countPerMeter(mP.PARTICLE_DENSITY * c.mCount)
					.delta(c.mDx, c.mDy, c.mDz)
					.ringMode(true)
					.extra(c.mVelocity)
					.data(c.mExtra2)
					.spawnAsEnemy();
			}

			loc.add(0, 1, 0);
		}
	}

	@Override
	public int cooldownTicks() {
		return mP.PASSIVE_RATE;
	}
}
