package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.managers.DoubleJumpManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DoubleJumpEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private final Player mPlayer;
	private final DoubleJumpManager.FlightSource mCause;

	public DoubleJumpEvent(Player player, DoubleJumpManager.FlightSource source) {
		mPlayer = player;
		mCause = source;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public DoubleJumpManager.FlightSource getSource() {
		return mCause;
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
