package com.playmonumenta.plugins.seasonalevents.community;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.seasonalevents.MonumentaContent;
import com.playmonumenta.plugins.seasonalevents.community.CommunityEvent.TieredRewardSchema;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.redissync.RedisAPI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.Nullable;

public class CommunityMissionManager {
	private static @Nullable CommunityMissionManager instance;

	// redis keys
	private static final String KEY_PREFIX = "community_missions";
	private static final String KEY_CLAIMED_REWARDS = KEY_PREFIX + ":claimed_rewards";

	private static final String HERO_SCOREBOARD = "CommunityHeroLevel";
	private final List<CommunityEvent> mSchedule = new ArrayList<>();

	private CommunityMissionManager() {
		loadSchedule();
	}

	public static CommunityMissionManager getInstance() {
		if (instance == null) {
			instance = new CommunityMissionManager();
		}
		return instance;
	}

	// maybe should be switched to use json later
	private void loadSchedule() {
		// test run
		mSchedule.add(new CommunityEvent(
			LocalDateTime.of(2025, 12, 17, 0, 0),
			LocalDateTime.of(2025, 12, 24, 0, 0),
			new CommunityMissionDefinition(CommunityMissionType.ELDRASK, 50, 100, 200, 3, 6,
				new TieredRewardSchema(
					"epic:r2/eldrask/condensed_experience", 2,
					"epic:r2/eldrask/materials/epic_material", 2,
					"epic:r2/eldrask/materials/epic_material", 4
				)
			),
			new CommunityMissionDefinition(CommunityMissionType.SNOW_SPIRIT, 50, 150, 250, 3, 6,
				new TieredRewardSchema(
					"epic:event/winter2018/ice_diamond", 10,
					"epic:event/winter2018/ice_diamond", 20,
					"epic:event/winter2018/cryosphere", 4
				)
			),
			new CommunityMissionDefinition(CommunityMissionType.RUIN, 50, 100, 200, 3, 6,
				new TieredRewardSchema(
					"epic:r3/items/currency/shattered_mask", 2,
					"epic:r3/items/currency/shattered_mask", 2,
					"epic:r3/fragments/masquerader_fragment", 2
				)
			),
			"scoreboard players set $KaulIceSpleefUnlocked const 1", "unlock a new Ice Spleef map!"
		));
		mSchedule.add(new CommunityEvent(
			LocalDateTime.of(2026, 1, 21, 0, 0),
			LocalDateTime.of(2026, 2, 5, 0, 0),
			new CommunityMissionDefinition(CommunityMissionType.SANCTUM, 200, 400, 750, 2, 5,
				new TieredRewardSchema(
					"epic:r1/dungeons/sanctumstrike/consuming_blight", 4,
					"epic:r1/dungeons/sanctumstrike/consuming_blight", 8,
					"epic:r1/dungeons/sanctumstrike/taferna_fragment", 2
				)
			),
			new CommunityMissionDefinition(CommunityMissionType.DEPTHS, 4000, 10000, 18000, 25, 60,
				new TieredRewardSchema(
					"epic:r2/depths/loot/voidstained_geode", 4,
					"epic:r2/items/currency/hyper_crystalline_shard", 2,
					"epic:r2/depths/loot/voidstained_geode", 8
				)
			),
			new CommunityMissionDefinition(CommunityMissionType.FISHING_COMBAT, 150, 300, 500, 2, 5,
				new TieredRewardSchema(
					"epic:r3/items/currency/hyperchromatic_archos_ring", 2,
					"epic:r3/items/fishing/sand_dollar", 6,
					"epic:r3/items/fishing/sand_dollar", 8
				)
			),
			"scoreboard players set $KaulIceSpleefUnlocked const 1", "unlock a new Ice Spleef map!"
		));
	}

	public @Nullable CommunityEvent getCurrentEvent() {
		LocalDateTime now = DateUtils.localDateTime();
		for (CommunityEvent event : mSchedule) {
			if (event.isActive(now)) {
				return event;
			}
		}
		return null;
	}

	public @Nullable CommunityEvent getDisplayEvent() {
		CommunityEvent active = getCurrentEvent();
		if (active != null) {
			return active;
		}

		LocalDateTime now = DateUtils.localDateTime();
		return mSchedule.stream()
			.filter(e -> e.mEnd.isBefore(now))
			.max(Comparator.comparing(e -> e.mEnd))
			.orElse(null);
	}

	public void addProgress(Player player, MonumentaContent content, int amount) {
		CommunityEvent currentEvent = getCurrentEvent();
		if (currentEvent == null) {
			return;
		}

		CommunityMissionDefinition targetDef = null;
		for (CommunityMissionDefinition def : currentEvent.mMissions) {
			if (def.mType.mContent == content) {
				targetDef = def;
				break;
			}
		}
		if (targetDef == null) {
			return;
		}

		final CommunityMissionDefinition def = targetDef;
		final UUID uuid = player.getUniqueId();

		String keyTotal = getRedisKey(currentEvent, def.mType.name(), "total");
		String keyRanking = getRedisKey(currentEvent, def.mType.name(), "ranking");

		// incrby and zincrby are done atomically so total and ranking stay in sync
		RedisAPI.multi(conn -> {
			conn.incrby(keyTotal, amount);
			conn.zincrby(keyRanking, amount, uuid.toString());
		}).whenComplete((result, ex) -> {
			if (ex != null) {
				MMLog.warning("Community Missions: Failed to add progress for " + uuid, ex);
				return;
			}
			long newTotal = (Long) result.get(0);
			long newPersonal = ((Double) result.get(1)).longValue();
			long oldPersonal = newPersonal - amount;
			long oldTotal = newTotal - amount;

			// alert for personal contrib level incr
			if (oldPersonal < def.mContribTier1 && newPersonal >= def.mContribTier1) {
				player.sendMessage(Component.text("You reached Contribution Tier 1 for " + def.mType.mName + "!", NamedTextColor.GREEN));
			}
			if (oldPersonal < def.mContribTier2 && newPersonal >= def.mContribTier2) {
				player.sendMessage(Component.text("You reached Contribution Tier 2 for " + def.mType.mName + "!", NamedTextColor.GREEN));
			}

			// global alert for goal tier level incr
			if (checkGoalCrossed(oldTotal, newTotal, def.mGoalTier1)) {
				sendNetworkAlert("goal", def.mType.name(), "1");
			}
			if (checkGoalCrossed(oldTotal, newTotal, def.mGoalTier2)) {
				sendNetworkAlert("goal", def.mType.name(), "2");
			}
			if (checkGoalCrossed(oldTotal, newTotal, def.mGoalTier3)) {
				sendNetworkAlert("goal", def.mType.name(), "3");
				checkGlobalCompletion(currentEvent);
			}
		});
	}

	private void checkGlobalCompletion(CommunityEvent event) {
		String completionKey = KEY_PREFIX + ":" + event.mEventId + ":completed";

		// batch completionKey + all mission totals in one round-trip
		CompletableFuture<String> completionFuture;
		List<CompletableFuture<String>> missionFutures = new ArrayList<>();
		try (RedisAPI.BorrowedCommands<String, String> conn = RedisAPI.borrow()) {
			completionFuture = conn.get(completionKey).toCompletableFuture();
			for (CommunityMissionDefinition def : event.mMissions) {
				missionFutures.add(conn.get(getRedisKey(event, def.mType.name(), "total")).toCompletableFuture());
			}
		}

		List<CompletableFuture<?>> allFutures = new ArrayList<>(missionFutures);
		allFutures.add(completionFuture);
		CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).whenComplete((unused, ex) -> {
			if (ex != null) {
				MMLog.warning("Community Missions: Failed to check global completion for " + event.mEventId, ex);
				return;
			}
			if (completionFuture.join() != null) {
				// Abort if already completed (the mission completion key is already set)
				return;
			}
			boolean allComplete = true;
			for (int i = 0; i < event.mMissions.size(); i++) {
				String totalStr = missionFutures.get(i).join();
				long total = (totalStr == null) ? 0 : Long.parseLong(totalStr);
				if (total < event.mMissions.get(i).mGoalTier3) {
					allComplete = false;
					break;
				}
			}
			if (!allComplete) {
				return;
			}
			try (RedisAPI.BorrowedCommands<String, String> conn = RedisAPI.borrow()) {
				conn.setnx(completionKey, "true").toCompletableFuture().whenComplete((success, setnxEx) -> {
					if (setnxEx != null) {
						MMLog.warning("Community Missions: Failed to set completion flag for " + event.mEventId, setnxEx);
						return;
					}
					if (success) {
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
							sendNetworkAlert("finale", event.mEventId);
						}, 200);
					}
				});
			}
		});
	}

	// broadcasts the command to everyone on every shard, similar to hunts
	private void sendNetworkAlert(String type, String... args) {
		String command = "communitymissions internalbroadcast " + type + " " + String.join(" ", args);
		try {
			NetworkRelayAPI.sendBroadcastCommand(command);
		} catch (Exception e) {
			MMLog.warning("Community Missions Error: Exception sending network broadcast: " + command, e);
		}
	}

	// command broadcasted above is picked up here on each shard individually, and then depending on type,
	// sends the message to the right people on that shard
	public void handleIncomingAlert(String type, String[] args) {
		CommunityEvent event = getDisplayEvent();
		if (event == null) {
			return;
		}

		List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

		if (type.equalsIgnoreCase("goal") && args.length >= 2) {
			String missionName = args[0];
			int tier = Integer.parseInt(args[1]);

			// getting what the alert is from
			CommunityMissionDefinition def = null;
			for (CommunityMissionDefinition d : event.mMissions) {
				if (d.mType.name().equals(missionName)) {
					def = d;
					break;
				}
			}
			if (def != null) {
				announceGoal(onlinePlayers, event, def, tier);
			}

		} else if (type.equalsIgnoreCase("finale")) {
			announceFinale(onlinePlayers, event);
		}
	}

	// goal tier announcement to players that reached contrib tier 1
	private void announceGoal(List<Player> players, CommunityEvent event, CommunityMissionDefinition def, int tier) {
		String keyRanking = getRedisKey(event, def.mType.name(), "ranking");
		// check each players score
		for (Player p : players) {
			try (RedisAPI.BorrowedCommands<String, String> conn = RedisAPI.borrow()) {
				conn.zscore(keyRanking, p.getUniqueId().toString()).toCompletableFuture().whenComplete((scoreDouble, ex) -> {
					if (ex != null) {
						MMLog.warning("Community Missions: Failed to get score for " + p.getUniqueId() + " in " + keyRanking, ex);
						return;
					}
					long score = (scoreDouble != null) ? scoreDouble.longValue() : 0;
					if (score >= def.mContribTier1) {
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
							if (p.isOnline()) {
								Component msg = Component.text("Community Mission Goal Reached!", NamedTextColor.GOLD, TextDecoration.BOLD)
									.append(Component.newline())
									.append(Component.text(def.mType.mName + " has hit Goal Tier " + tier + "!", NamedTextColor.YELLOW));
								p.sendMessage(msg);
							}
						});
					}
				});
			}
		}
	}

	// final announcement to everyone that they've been reached
	private void announceFinale(List<Player> players, CommunityEvent event) {
		Component message1 = Component.text("All 3 Community Missions have reached Tier 3! Amazing Job!", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
		for (Player p : players) {
			p.sendMessage(message1);
			p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 0.5f);
		}
		// since this alr gets broadcasted to every shard using dispatchcommand is fine here
		if (event.mCompletionFunctionCommand != null) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), event.mCompletionFunctionCommand);
			});
		}
	}

	// rewards
	public void tryClaimRewards(Player player) {
		LocalDateTime now = DateUtils.localDateTime();
		String uuidStr = player.getUniqueId().toString();
		// check each event and if rewards aren't claimed, calc and claim them
		for (CommunityEvent event : mSchedule) {
			if (event.isFinished(now)) {
				String claimKey = uuidStr + ":" + event.mEventId;

				CompletableFuture<Boolean> isClaimedFuture;
				try (RedisAPI.BorrowedCommands<String, String> conn = RedisAPI.borrow()) {
					isClaimedFuture = conn.sismember(KEY_CLAIMED_REWARDS, claimKey).toCompletableFuture();
				}
				isClaimedFuture.whenComplete((isClaimed, ex) -> {
					if (ex != null) {
						MMLog.warning("Community Missions: Failed to check claimed status for " + claimKey, ex);
						return;
					}
					if (isClaimed) {
						return;
					}

					getMissionDataInternal(event, player.getUniqueId()).whenComplete((missionDataList, dataEx) -> {
						if (dataEx != null) {
							MMLog.warning("Community Missions: Failed to get mission data for " + player.getUniqueId(), dataEx);
							return;
						}
						if (missionDataList.isEmpty()) {
							return;
						}

						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
							if (!player.isOnline()) {
								return;
							}

							if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_VIRTUAL_INVENTORIES)) {
								player.sendMessage(Component.text("You have unclaimed rewards, but may not claim them here.", NamedTextColor.GRAY));
								return;
							}

							int totalHeroPoints = 0;
							boolean earnedAny = false;

							for (CommunityMissionData data : missionDataList) {
								CommunityMissionDefinition def = data.mDef;
								double multiplier = data.getRewardMultiplier();
								if (multiplier <= 0) {
									continue;
								}
								// for each mission, get the goal tier reached and multiply by contrib tier multi
								// reward amount should alr be dividable by 2, but if it isn't, down
								if (data.mTotalContribution >= def.mGoalTier1) {
									int amount = (int) Math.floor(def.mAmountT1 * multiplier);
									if (amount > 0 && def.mLootT1 != null) {
										earnedAny = true;
										giveRewards(player, def.mLootT1, amount);
										player.sendMessage(Component.text(def.mType.mName + " Tier 1 Reward: " + amount + "x Items", NamedTextColor.YELLOW));
									}
								}
								if (data.mTotalContribution >= def.mGoalTier2) {
									int amount = (int) Math.floor(def.mAmountT2 * multiplier);
									if (amount > 0 && def.mLootT2 != null) {
										earnedAny = true;
										giveRewards(player, def.mLootT2, amount);
										player.sendMessage(Component.text(def.mType.mName + " Tier 2 Reward: " + amount + "x Items", NamedTextColor.YELLOW));
									}
								}
								if (data.mTotalContribution >= def.mGoalTier3) {
									int amount = (int) Math.floor(def.mAmountT3 * multiplier);
									if (amount > 0 && def.mLootT3 != null) {
										earnedAny = true;
										giveRewards(player, def.mLootT3, amount);
										player.sendMessage(Component.text(def.mType.mName + " Tier 3 Reward: " + amount + "x Items", NamedTextColor.YELLOW));
									}
								}
								// also calc and add hero point leaderboard for each mission to total
								totalHeroPoints += data.getLeaderboardPoints();
							}
							// give rewards and points and mark as given
							if (earnedAny || totalHeroPoints > 0) {
								if (totalHeroPoints > 0) {
									ScoreboardUtils.addScore(player, HERO_SCOREBOARD, totalHeroPoints);
									player.sendMessage(Component.text("You earned " + totalHeroPoints + " Community Hero Levels!", NamedTextColor.LIGHT_PURPLE));
								}
							}
							try (RedisAPI.BorrowedCommands<String, String> conn = RedisAPI.borrow()) {
								conn.sadd(KEY_CLAIMED_REWARDS, claimKey).exceptionally(saddEx -> {
									MMLog.warning("Community Missions: Failed to mark rewards as claimed for " + claimKey, saddEx);
									return null;
								});
							}
						});
					});
				});
			}
		}
	}

	// should be exact same as how pass does it
	private void giveRewards(Player p, String lootTablePath, int count) {
		for (int i = 0; i < count; i++) {
			givePlayerLootTable(p, lootTablePath);
		}
	}

	private void givePlayerLootTable(Player p, String lootTablePath) {
		if (lootTablePath == null || lootTablePath.isEmpty()) {
			return;
		}
		LootContext context = new LootContext.Builder(p.getLocation()).build();
		LootTable rewardTable = Bukkit.getLootTable(NamespacedKeyUtils.fromString(lootTablePath));
		if (rewardTable != null) {
			Collection<ItemStack> items = rewardTable.populateLoot(FastUtils.RANDOM, context);
			for (ItemStack item : items) {
				InventoryUtils.giveItem(p, item);
			}
		}
	}

	// getters for gui
	public CompletableFuture<List<CommunityMissionData>> getCurrentMissionData(UUID playerUuid) {
		CommunityEvent current = getDisplayEvent();
		if (current == null) {
			return CompletableFuture.completedFuture(new ArrayList<>());
		}
		return getMissionDataInternal(current, playerUuid);
	}

	private CompletableFuture<List<CommunityMissionData>> getMissionDataInternal(CommunityEvent event, UUID playerUuid) {
		List<CommunityMissionDefinition> missions = event.mMissions;
		String uuidStr = playerUuid.toString();

		// all reads for every mission in one atomic round-trip: 4 commands per mission
		// order: get(total), zscore, zrevrank, zcard
		return RedisAPI.multi(conn -> {
			for (CommunityMissionDefinition def : missions) {
				String keyTotal = getRedisKey(event, def.mType.name(), "total");
				String keyRanking = getRedisKey(event, def.mType.name(), "ranking");
				conn.get(keyTotal);
				conn.zscore(keyRanking, uuidStr);
				conn.zrevrank(keyRanking, uuidStr);
				conn.zcard(keyRanking);
			}
		}).handle((result, ex) -> {
			if (ex != null) {
				MMLog.warning("Community Missions: Failed to get mission data for " + playerUuid, ex);
				return new ArrayList<>();
			}
			List<CommunityMissionData> out = new ArrayList<>();
			for (int i = 0; i < missions.size(); i++) {
				CommunityMissionDefinition def = missions.get(i);
				int base = i * 4;
				try {
					String totalStr = (String) result.get(base);
					Double score = (Double) result.get(base + 1);
					Long rankIdx = (Long) result.get(base + 2);
					Long count = (Long) result.get(base + 3);

					long total = totalStr == null ? 0 : Long.parseLong(totalStr);
					long personal = score == null ? 0 : score.longValue();

					out.add(new CommunityMissionData(def, total, personal, rankIdx, count));
				} catch (Exception e) {
					MMLog.warning("Community Missions Error: Couldn't get mission data", e);
					out.add(new CommunityMissionData(def, 0, 0, null, 0L));
				}
			}
			return out;
		});
	}

	// debuggers / for mods
	public boolean setTotalContribution(int missionIndex, long amount) {
		CommunityEvent current = getCurrentEvent();
		if (current == null || missionIndex < 0 || missionIndex >= current.mMissions.size()) {
			return false;
		}

		CommunityMissionDefinition def = current.mMissions.get(missionIndex);
		try (RedisAPI.BorrowedCommands<String, String> conn = RedisAPI.borrow()) {
			conn.set(getRedisKey(current, def.mType.name(), "total"), String.valueOf(amount)).exceptionally(ex -> {
				MMLog.warning("Community Missions: Failed to set total contribution for mission " + missionIndex, ex);
				return null;
			});
		}
		return true;
	}

	public boolean setPlayerContribution(int missionIndex, UUID uuid, long newAmount) {
		CommunityEvent current = getCurrentEvent();
		if (current == null || missionIndex < 0 || missionIndex >= current.mMissions.size()) {
			return false;
		}

		CommunityMissionDefinition def = current.mMissions.get(missionIndex);
		String keyRanking = getRedisKey(current, def.mType.name(), "ranking");
		String keyTotal = getRedisKey(current, def.mType.name(), "total");

		try (RedisAPI.BorrowedCommands<String, String> conn = RedisAPI.borrow()) {
			conn.zscore(keyRanking, uuid.toString()).toCompletableFuture().whenComplete((oldScoreDouble, ex) -> {
				if (ex != null) {
					MMLog.warning("Community Missions: Failed to get current score for " + uuid, ex);
					return;
				}
				long oldScore = (oldScoreDouble != null) ? oldScoreDouble.longValue() : 0;
				long diff = newAmount - oldScore;
				if (diff != 0) {
					try (RedisAPI.BorrowedCommands<String, String> conn2 = RedisAPI.borrow()) {
						conn2.zadd(keyRanking, (double) newAmount, uuid.toString()).exceptionally(zaddEx -> {
							MMLog.warning("Community Missions: Failed to set player contribution score for " + uuid, zaddEx);
							return null;
						});
						conn2.incrby(keyTotal, diff).exceptionally(incrEx -> {
							MMLog.warning("Community Missions: Failed to update total contribution for mission " + missionIndex, incrEx);
							return null;
						});
					}
				}
			});
		}

		return true;
	}

	// helpers
	private String getRedisKey(CommunityEvent event, String missionName, String subKey) {
		return KEY_PREFIX + ":" + event.mEventId + ":" + missionName + ":" + subKey;
	}

	private boolean checkGoalCrossed(long oldVal, long newVal, long goal) {
		return oldVal < goal && newVal >= goal;
	}
}
