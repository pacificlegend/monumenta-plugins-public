package com.playmonumenta.plugins.effects;

import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class MultishotEffect extends SingleArgumentEffect {
	public static final String effectID = "MultishotEffect";

	public static final Set<EntityType> PERMISSIBLE_PROJECTILE_TYPES = Set.of(
		EntityType.ARROW,
		EntityType.SPECTRAL_ARROW,
		EntityType.TRIDENT,
		EntityType.SNOWBALL
	);

	public MultishotEffect(int duration, int stacks) {
		this(duration, stacks, false);
	}

	public MultishotEffect(int duration, int stacks, boolean hidden) {
		super(duration, stacks, effectID);
		this.displays(!hidden);
	}

	@Override
	public boolean isDebuff() {
		return !doesDisplay() || mAmount < 0;
	}

	@Override
	public boolean isBuff() {
		return doesDisplay() && mAmount > 0;
	}


	@Override
	public double getMagnitude() {
		return mAmount;
	}

	@Override
	public boolean shouldDeleteOnDeath() {
		return true;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text(((mAmount > 0) ? "+" : "") + (int) mAmount + " " + getDisplayedName(), NamedTextColor.GREEN);
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Multishot Level" + ((int) mAmount == 1 ? "" : "s");
	}

	@Override
	public String toString() {
		return String.format(
			"%s | duration:%s stacks:%s",
			getClass().getName(),
			getDuration(),
			getMagnitude()
		);
	}
}
