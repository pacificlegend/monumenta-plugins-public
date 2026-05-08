package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentDamageReceived extends Effect {
	public static final String effectID = "PercentDamageReceived";
	public static final String GENERIC_NAME = "PercentDamageReceived";

	protected final double mAmount;
	protected final EnumSet<DamageType> mAffectedDamageTypes;

	protected PercentDamageReceived(final int duration, final double amount, final @Nullable EnumSet<DamageType> affectedDamageTypes,
	                             final String effectID) {
		super(duration, effectID);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes == null ? DamageType.getNonTrueTypes() : affectedDamageTypes;
	}

	public PercentDamageReceived(final int duration, final double amount, final @Nullable EnumSet<DamageType> affectedDamageTypes) {
		this(duration, amount, affectedDamageTypes, effectID);
	}

	public PercentDamageReceived(final int duration, final double amount) {
		this(duration, amount, null);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isDebuff() {
		return mAmount > 0;
	}

	@Override
	public boolean isBuff() {
		return mAmount < 0;
	}

	public EnumSet<DamageType> getAffectedDamageTypes() {
		return mAffectedDamageTypes;
	}

	@Override
	public void onHurt(final LivingEntity entity, final DamageEvent event) {
		if (mAffectedDamageTypes.contains(event.getType())) {
			double amount = mAmount;
			if (EntityUtils.isBoss(entity) && isDebuff()) {
				amount /= 2;
			}
			event.updateDamageWithMultiplier(1 + amount, mAffectedDamageTypes);
		}
	}

	@Override
	public JsonObject serialize() {
		final JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		final JsonArray jsonArray = new JsonArray();
		for (DamageType damageType : mAffectedDamageTypes) {
			jsonArray.add(damageType.name());
		}
		object.add("type", jsonArray);

		return object;
	}

	public static PercentDamageReceived deserialize(JsonObject object, Plugin plugin) {
		final int duration = object.get("duration").getAsInt();
		final double amount = object.get("amount").getAsDouble();

		if (object.has("type")) {
			final JsonArray damageTypes = object.getAsJsonArray("type");
			final List<DamageType> damageTypeList = new ArrayList<>();
			for (final JsonElement element : damageTypes) {
				damageTypeList.add(DamageEvent.DamageType.valueOf(element.getAsString()));
			}

			final EnumSet<DamageEvent.DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new PercentDamageReceived(duration, amount, damageTypeSet);
		} else {
			return new PercentDamageReceived(duration, amount);
		}
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(-mAmount).append(
			getDisplayedName() != null ? Component.text(" " + getDisplayedName()) : Component.empty()
		);
	}

	@Override
	public @Nullable String getDisplayedName() {
		return StringUtils.getDamageTypeString(mAffectedDamageTypes, true, null) + "Resistance";
	}

	@Override
	public String toString() {
		StringBuilder types = new StringBuilder();
		for (final DamageType type : mAffectedDamageTypes) {
			if (!types.isEmpty()) {
				types.append(",");
			}
			types.append(type.name());
		}
		return String.format("PercentDamageReceived duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
