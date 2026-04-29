package com.playmonumenta.plugins.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageShieldedEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private final Player mPlayer;
	private final @Nullable LivingEntity mSource;
	private final EntityDamageEvent.DamageCause mCause;
	private final int mStunTicks;
	// mStunTicks can be -1; in this case we can't process the
	// stun ticks yet because the damage needs to go through first

	public DamageShieldedEvent(Player player, @Nullable LivingEntity source, EntityDamageEvent.DamageCause cause, int stunTicks) {
		mPlayer = player;
		mSource = source;
		mCause = cause;
		mStunTicks = stunTicks;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public @Nullable LivingEntity getSource() {
		return mSource;
	}

	public EntityDamageEvent.DamageCause getCause() {
		return mCause;
	}

	public int getStunTicks() {
		return mStunTicks;
	}

	@Override
	public boolean isCancelled() {
		return mIsCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.mIsCancelled = cancelled;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
