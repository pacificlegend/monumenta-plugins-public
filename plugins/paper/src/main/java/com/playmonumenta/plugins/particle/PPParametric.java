package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A {@link PartialParticle} that takes a function with a single parameter (ranging from 0 to 1) to spawn multiple particles in some parametric way.
 * The function can for example parameterize the position of the particles to form arbitrary curves, while also altering the delta or other values along the curve.
 *
 * @see PPCircle PPCircle to draw circles and rings
 * @see PPLine PPLine to draw simple lines
 */
public class PPParametric extends AbstractPartialParticle<PPParametric> {

	public interface ParametricFunction {
		void transform(double parameter, PartialParticleBuilder packagedValues);
	}

	private final ParametricFunction mFunction;

	private boolean mIncludeStart = true;
	private boolean mIncludeEnd = false;
	private int mAnimationTicks = 1;

	public PPParametric(Particle particle, Location location, ParametricFunction function) {
		super(particle, location);
		mFunction = function;
	}

	@Override
	public PPParametric copy() {
		return copy(new PPParametric(mParticle, mLocation.clone(), mFunction));
	}

	@Override
	public PPParametric copy(PPParametric copy) {
		super.copy(copy);
		copy.mIncludeStart = mIncludeStart;
		copy.mIncludeEnd = mIncludeEnd;
		copy.mAnimationTicks = mAnimationTicks;
		return copy;
	}

	public PPParametric includeStart(boolean includeStart) {
		mIncludeStart = includeStart;
		return this;
	}

	public PPParametric includeEnd(boolean includeEnd) {
		mIncludeEnd = includeEnd;
		return this;
	}

	public PPParametric delay(int animationTicks) {
		if (animationTicks < 1) {
			animationTicks = 1;
		}
		mAnimationTicks = animationTicks;
		return this;
	}

	public double delay() {
		return mAnimationTicks;
	}

	@Override
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		int count = super.getPartialCount(multiplier, player, source);
		if (mIncludeStart && mIncludeEnd) {
			count++;
		} else if (!mIncludeStart && !mIncludeEnd && count > 1) {
			count--;
		}
		return count;
	}

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		int count = packagedValues.count();
		packagedValues.count(1);
		if (mAnimationTicks > 1) {
			new BukkitRunnable() {
				int mT = 0;
				final int mCountPerTick = (int) Math.ceil((float) count / mAnimationTicks);
				int mI = 0;

				@Override
				public void run() {
					for (int i = 0; i <= mCountPerTick; i++) {
						if ((i == 0 && !mIncludeStart) || (i == count && !mIncludeEnd)) {
							continue;
						}
						if (mI > count) {
							this.cancel();
							return;
						}
						mFunction.transform(1.0 * mI / count, packagedValues);
						spawnUsingSettings(packagedValues);

						mI++;
					}

					mT++;
					if (mT >= mAnimationTicks || mI > count) {
						this.cancel();
					}
				}
			}.runTaskTimerAsynchronously(Plugin.getInstance(), 0, 1);
		} else {
			for (int i = 0; i <= count; i++) {
				if ((i == 0 && !mIncludeStart) || (i == count && !mIncludeEnd)) {
					continue;
				}
				mFunction.transform(1.0 * i / count, packagedValues);
				spawnUsingSettings(packagedValues);
			}
		}
	}

}
