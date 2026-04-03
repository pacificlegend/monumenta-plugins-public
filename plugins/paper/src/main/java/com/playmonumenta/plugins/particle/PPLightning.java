package com.playmonumenta.plugins.particle;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PPLightning extends AbstractPartialParticle<PPLightning> {
	/*
	 * Length of the lightning bolt.
	 */
	protected double mLength;

	/*
	 * Random variance applied after each downward hop of the lightning's points
	 * (initially evenly spread along the Y axis), drawing jagged lines.
	 */
	protected double mHopXZ;
	protected double mHopY;
	protected int mDuration = Constants.TICKS_PER_SECOND;

	protected @Nullable BukkitRunnable mRunnable;

	protected double mHopsPerBlock = 2;
	protected int mMinimumHops = 3;

	/*
	 * Allow people to change the direction of the lightning.
	 */
	protected Vector mDirection;

	// To prevent each line from looking too sparse (ugly),
	// if player's particle multiplier setting is not completely off,
	// use at least this many particles.
	private static final int HOP_MINIMUM_PARTICLES = 4;

	private final ArrayList<Location> mGeneratedHops = new ArrayList<>();
	private final HashMap<Integer, List<Location>> mParticleLocations = new HashMap<>();

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	// Specify both ends, like PPLine
	public PPLightning(Particle particle, Location strikeLocation, Location startLocation, double hopLength) {
		this(particle, strikeLocation, startLocation, hopLength, hopLength);
	}

	public PPLightning(Particle particle, Location strikeLocation, Location startLocation, double hopXZ, double hopY) {
		this(particle, strikeLocation, LocationUtils.getDirectionTo(strikeLocation, startLocation), strikeLocation.distance(startLocation), hopXZ, hopY);
	}

	// Specify strike location and height, traditional
	public PPLightning(Particle particle, Location strikeLocation, double length, double hopLength) {
		this(particle, strikeLocation, length, hopLength, hopLength);
	}

	public PPLightning(Particle particle, Location strikeLocation, double length, double hopXZ, double hopY) {
		this(particle, strikeLocation, new Vector(0, -1, 0), length, hopXZ, hopY);
	}

	// Specify strike location and direction. Note that the direction points TOWARDS the strike location!
	public PPLightning(Particle particle, Location strikeLocation, Vector direction, double length, double hopLength) {
		this(particle, strikeLocation, direction, length, hopLength, hopLength);
	}

	public PPLightning(Particle particle, Location strikeLocation, Vector direction, double length, double hopXZ, double hopY) {
		super(particle, strikeLocation);
		mDirection = direction.clone().normalize();
		mLength = length;
		mHopXZ = hopXZ;
		mHopY = hopY;
	}

	@Override
	public PPLightning copy() {
		return copy(new PPLightning(mParticle, mLocation.clone(), mDirection.clone(), mLength, mHopXZ, mHopY));
	}

	@Override
	public PPLightning copy(PPLightning copy) {
		super.copy(copy);
		copy.mDuration = mDuration;
		copy.mHopsPerBlock = mHopsPerBlock;
		copy.mMinimumHops = mMinimumHops;
		copy.mRunnable = mRunnable;
		copy.mGeneratedHops.addAll(mGeneratedHops);
		copy.mParticleLocations.putAll(mParticleLocations);
		return copy;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	public PPLightning length(double length) {
		mLength = length;
		return this;
	}

	public double length() {
		return mLength;
	}

	public PPLightning hopXZ(double hopXZ) {
		mHopXZ = hopXZ;
		return this;
	}

	public double hopXZ() {
		return mHopXZ;
	}

	public PPLightning hopY(double hopY) {
		mHopY = hopY;
		return this;
	}

	public double hopY() {
		return mHopY;
	}

	public PPLightning duration(int duration) {
		mDuration = Math.max(1, duration);
		return this;
	}

	public PPLightning hopsPerBlock(double hops) {
		mHopsPerBlock = hops;
		return this;
	}

	public double hopsPerBlock() {
		return mHopsPerBlock;
	}

	public PPLightning minimumHops(int minimumHops) {
		mMinimumHops = minimumHops;
		return this;
	}

	public int minimumHops() {
		return mMinimumHops;
	}

	public @Nullable BukkitRunnable runnable() {
		return mRunnable;
	}

	/**
	 * Sets the direction of the lightning. Will forcibly normalise the input vector.
	 * @param direction Direction. Defaults to (0, -1, 0).
	 * @return The same PPLightning
	 */
	public PPLightning direction(Vector direction) {
		// Forcibly normalise to avoid shenanigans
		mDirection = direction.clone().normalize();
		return this;
	}

	public PPLightning endpoints(Location to, Location from) {
		Preconditions.checkArgument(to.getWorld().equals(from.getWorld()), "Locations must be in the same world!");
		mDirection = LocationUtils.getDirectionTo(to, from);
		mLength = to.distance(from);
		mLocation = to;
		return this;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		generateHopsOnce(packagedValues.location());

		int hopParticleCount = Math.max(HOP_MINIMUM_PARTICLES, packagedValues.count());
		List<Location> hopParticleLocations = generateParticleLocationsOnce(hopParticleCount);
		double particlesPerTick = hopParticleLocations.size() / (double) mDuration;

		packagedValues.count(1);

		mRunnable = new BukkitRunnable() {
			int mAnimationProgress = 0;
			int mIndexPointer = 0;

			@Override
			public void run() {
				try {
					// Frame of animation this run, starting at 1
					mAnimationProgress++;
					int targetIndex = (int) Math.round(mAnimationProgress * particlesPerTick) - 1;
					Preconditions.checkArgument(mAnimationProgress > 0, "Animation progress must be greater than 0");
					Preconditions.checkArgument(mIndexPointer >= 0, "Index pointer must be greater than or equal to 0");

					for (int index = mIndexPointer; index <= targetIndex; index++) {
						packagedValues.location(
							hopParticleLocations.get(index)
						);
						spawnUsingSettings(packagedValues);
					}

					// If this was the last frame of animation
					if (mAnimationProgress >= mDuration) {
						cancel();
						mRunnable = null;
					} else {
						mIndexPointer = targetIndex;
					}
				} catch (Exception e) {
					// Ensure the task is cancelled if an exception is thrown
					cancel();
					mRunnable = null;
					throw e;
				}
			}
		};
		mRunnable.runTaskTimerAsynchronously(Plugin.getInstance(), 0, 1);
	}

	// Each lightning bolt will have the same hops for all players,
	// and so look "the same" in the way the same bolt jags from any screen,
	// just drawing differently based on their particle multiplier settings
	private void generateHopsOnce(Location strikeLocation) {
		if (!mGeneratedHops.isEmpty()) {
			return;
		}

		Vector xPrime = new Vector(0, 0, 1).crossProduct(mDirection).normalize(); // (1, 0, 0) by default
		Vector yPrime = mDirection.clone().multiply(-1);
		Vector zPrime = mDirection.getCrossProduct(xPrime); // (0, 0, 1) by default
		if (xPrime.lengthSquared() == 0) {
			xPrime = new Vector(1, 0, 0);
			zPrime = new Vector(0, 1, 0);
		}


		int hopCount = Math.max((int) Math.ceil(mLength * mHopsPerBlock), mMinimumHops);

		for (int hopIndex = 0; hopIndex <= hopCount; hopIndex++) {
			double currentLength = mLength - (mLength / hopCount * hopIndex);

			// Displacement of the hop point, taking strikeLocation as the origin, in cylindrical coordinates, along the axis of mDirection
			// First and last points don't get varied
			double radius = (hopIndex == 0 || hopIndex == hopCount) ? 0 : mHopXZ * Math.sqrt(FastUtils.randomDoubleInRange(0, 1)); // Normalise chance per unit area
			double angle = (hopIndex == 0 || hopIndex == hopCount) ? 0 : FastUtils.randomDoubleInRange(0, 2 * Math.PI);
			double length = Math.clamp(currentLength + FastUtils.randomDoubleInRange(-mHopY, mHopY), 0, mLength);

			// Convert to Cartesian displacements in the cylinder
			Vector currentHopDisplacementPrime = new Vector(
				radius * FastUtils.cos(angle),
				length,
				radius * FastUtils.sin(angle));
			// Transform according to mDirection
			Vector currentHopDisplacement = new Vector(
				new Vector(xPrime.getX(), yPrime.getX(), zPrime.getX()).dot(currentHopDisplacementPrime),
				new Vector(xPrime.getY(), yPrime.getY(), zPrime.getY()).dot(currentHopDisplacementPrime),
				new Vector(xPrime.getZ(), yPrime.getZ(), zPrime.getZ()).dot(currentHopDisplacementPrime)
			);
			mGeneratedHops.add(strikeLocation.clone().add(currentHopDisplacement));
		}
	}

	// Store locations for the same hopParticleCount for performance,
	// generating the same results only once,
	// then referring to them again as the timer loops
	private List<Location> generateParticleLocationsOnce(int hopParticleCount) {
		@Nullable List<Location> hopParticleLocations = mParticleLocations.get(hopParticleCount);
		if (hopParticleLocations == null) {
			hopParticleLocations = new ArrayList<>();

			for (int hopIndex = 0; hopIndex < mGeneratedHops.size(); hopIndex++) {
				Location currentHop = mGeneratedHops.get(hopIndex);
				if (hopIndex == 0) {
					hopParticleLocations.add(currentHop);

					continue;
				}

				Location previousHop = mGeneratedHops.get(hopIndex - 1);

				// Move from previous to current hop
				Location movingParticleLocation = previousHop.clone();
				Location hopInterval = currentHop.clone().subtract(previousHop).multiply(1d / hopParticleCount);
				for (int i = 0; i < hopParticleCount; i++) {
					movingParticleLocation.add(hopInterval);
					hopParticleLocations.add(movingParticleLocation.clone());
				}
			}
			mParticleLocations.put(hopParticleCount, hopParticleLocations);
		}
		return hopParticleLocations;
	}
}
