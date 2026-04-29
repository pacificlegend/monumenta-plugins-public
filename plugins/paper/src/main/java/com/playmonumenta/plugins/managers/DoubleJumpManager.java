package com.playmonumenta.plugins.managers;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DoubleJumpEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.jetbrains.annotations.Nullable;


public class DoubleJumpManager {

	/*
	The double jump manager works on a system of flightSources which can be given and taken away from the player.
	Whenever a player has at least 1 flightSource, flight is enabled and whenever a PlayerToggleFlightEvent occurs it is cancelled
	and a DoubleJumpEvent is called with the highest priority flightSource.
	This system is needed since having flight enabled at all times and cancelling the event causes players with
	high ping to stall in the air for a fraction of a second when attempting to double jump, thus we use this system to make sure
	flight is only enabled when required for triggers.
	 */

	private static final float DEFAULT_FLY_SPEED = 0.1f;
	private static final HashMap<Player, Set<FlightSource>> FLIGHT_SOURCES_MAP = new HashMap<>();

	public static void addFlightSource(Player player, FlightSource source) {
		if (FLIGHT_SOURCES_MAP.containsKey(player)) {
			FLIGHT_SOURCES_MAP.get(player).add(source);
		} else {
			FLIGHT_SOURCES_MAP.put(player, new HashSet<>(Collections.singleton(source)));
		}
		updateFlight(player);
	}

	public static void removeFlightSource(Player player, FlightSource source) {
		if (FLIGHT_SOURCES_MAP.containsKey(player)) {
			FLIGHT_SOURCES_MAP.get(player).remove(source);
		}
		updateFlight(player);
	}

	public static Set<FlightSource> getFlightSource(Player player) {
		if (!FLIGHT_SOURCES_MAP.containsKey(player)) {
			return new HashSet<>();
		}
		return FLIGHT_SOURCES_MAP.get(player);
	}

	public static boolean hasFlightSource(Player player, FlightSource source) {
		if (!FLIGHT_SOURCES_MAP.containsKey(player)) {
			return false;
		}
		return FLIGHT_SOURCES_MAP.get(player).contains(source);
	}

	private static void updateFlight(Player player) {
		setFlying(player, FLIGHT_SOURCES_MAP.containsKey(player) && !FLIGHT_SOURCES_MAP.get(player).isEmpty());
	}

	private static void setFlying(Player player, boolean flight) {
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		if (!flight) {
			player.setFlySpeed(DEFAULT_FLY_SPEED);
		}
		player.setAllowFlight(flight);
		player.setFlyingFallDamage(flight ? TriState.TRUE : TriState.FALSE);
	}

	public void playerToggleFlightEvent(Player player, PlayerToggleFlightEvent event) {
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		if (!FLIGHT_SOURCES_MAP.containsKey(player) || FLIGHT_SOURCES_MAP.get(player).isEmpty()) {
			return;
		}
		event.setCancelled(true);
		player.setFlying(false);

		//Event is called last so that things like .setFlying can be overwritten in the event
		@Nullable FlightSource source = getHighestPrioritySource(player);
		if (source != null) {
			Bukkit.getPluginManager().callEvent(new DoubleJumpEvent(player, source));
		}
	}

	private @Nullable DoubleJumpManager.FlightSource getHighestPrioritySource(Player player) {
		if (!FLIGHT_SOURCES_MAP.containsKey(player)) {
			return null;
		}
		FlightSource source = FLIGHT_SOURCES_MAP.get(player).stream().findAny().orElse(null);
		for (FlightSource s : FLIGHT_SOURCES_MAP.get(player)) {
			if (source == null || s.mPriority > source.mPriority) {
				source = s;
			}
		}
		return source;
	}

	public void onHurt(Player player, DamageEvent event) {
		if (event.getType() != DamageEvent.DamageType.FALL) {
			return;
		}
		if (FLIGHT_SOURCES_MAP.containsKey(player) && player.hasFlyingFallDamage() == TriState.TRUE) {
			if (player.getFallDistance() > 7) {
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 1f, 1f);
			} else {
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SMALL_FALL, 1f, 1f);
			}
		}
	}

	public static class FlightSource {

		public final String mName;
		public final int mPriority;

		public FlightSource(String name, int priority) {
			mName = name;
			mPriority = priority;
		}
	}
}
