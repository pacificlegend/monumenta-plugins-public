package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.listeners.RepairExplosionsListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.scoreboard;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class LuminiteDrill extends Ability {
	private static final String SCOREBOARD = "LuminiteDrill";
	private static final String ADVANCEMENT_REQ = "monumenta:challenges/r1/coalrupted/spawners";
	private static final int POINT_COST = 4;
	private static final int MAX_USES = 3;
	private static final int DURATION = 4 * 20;

	private static final EnumSet<Material> UNBREAKABLE_BLOCKS = EnumSet.of(
		Material.AIR,
		Material.CAVE_AIR,
		Material.VOID_AIR,
		Material.STRUCTURE_VOID,
		Material.LIGHT,
		Material.MOVING_PISTON,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.STRUCTURE_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.WATER,
		Material.LAVA
	);

	public static final AbilityInfo<LuminiteDrill> INFO =
		new SnowPerkGui.SnowPerkInfo<>(LuminiteDrill.class, "Luminite Drill", LuminiteDrill::new)
			.snowPointCost(POINT_COST)
			.unlockReq(player -> AdvancementUtils.checkAdvancement(player, ADVANCEMENT_REQ))
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.STONECUTTER)
			.linkedSpell(ClassAbility.LUMINITE_DRILL)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LuminiteDrill::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP),
				new AbilityTriggerInfo.TriggerRestriction("Holding a Pickaxe", p -> ItemUtils.isPickaxe(p.getInventory().getItemInMainHand()))))
			.description(getDescription());

	private int mRemainingUses;
	private boolean mActive;

	public LuminiteDrill(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRemainingUses = MAX_USES;
		mActive = false;
	}

	public boolean cast() {
		if (mActive || mRemainingUses <= 0) {
			return false;
		}

		mRemainingUses--;
		mActive = true;
		sendActionBarMessage("Luminite Drill activated! %d uses remaining!".formatted(mRemainingUses));

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.5f, 1f);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mActive = false;
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.5f, 1f);
		}, DURATION);

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mActive) {
			mPlayer.playSound(mPlayer, Sound.ITEM_SPYGLASS_USE, SoundCategory.PLAYERS, 0.57f, 0.8f);
			mPlayer.playSound(mPlayer, Sound.ITEM_SPYGLASS_USE, SoundCategory.PLAYERS, 0.63f, 0.8f);
			new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION, PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0), 3).delta(0.02).extra(0.04).spawnAsPlayerPassive(mPlayer);
			new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION, PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0), 3).delta(0.02).extra(0.04).spawnAsPlayerPassive(mPlayer);
		}
	}

	@Override
	public boolean blockDamageEvent(BlockDamageEvent event) {
		if (!mActive) {
			return true;
		}

		Block block = event.getBlock();
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		if (canMineBlock(block) && ItemUtils.isPickaxe(mainhand)) {
			BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, mPlayer);
			Bukkit.getPluginManager().callEvent(blockBreakEvent);

			if (!event.isCancelled()) {
				CoreProtectIntegration.logRemoval(mPlayer, block);
				RepairExplosionsListener.getInstance().playerReplacedBlockViaPlugin(mPlayer, block);
				block.breakNaturally(mainhand, true);
				ItemUtils.damageItem(mainhand, 1, true);

				World world = mPlayer.getWorld();
				Location loc = mPlayer.getLocation();
				world.playSound(loc, Sound.BLOCK_DEEPSLATE_BREAK, SoundCategory.PLAYERS, 1.1f, 1f);
				world.playSound(loc, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, SoundCategory.PLAYERS, 1.7f, 1f);
				world.playSound(loc, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, SoundCategory.PLAYERS, 1.7f, 1f);
				world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 2f, 1f);
				new PartialParticle(Particle.DUST_PLUME, BlockUtils.getCenterBlockLocation(block), 15).delta(0.5).extra(0.12).spawnAsPlayerActive(mPlayer);
			}
		}
		return true;
	}

	private boolean canMineBlock(Block block) {
		if (block.isLiquid() || block.isEmpty()) {
			return false;
		}

		if (ServerProperties.getUnbreakableBlocks().contains(block.getType())) {
			return false;
		}

		if (UNBREAKABLE_BLOCKS.contains(block.getType())) {
			return false;
		}

		return ZoneUtils.playerCanMineBlock(mPlayer, block);
	}

	public static Description<LuminiteDrill> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.ACHIEVEMENT_ARROW_COLOR)
			.addLine("*Unlock:* *Break 100 spawners in the furthest*").styles(DescriptionUtils.REQUIREMENT_LABEL, DescriptionUtils.REQUIREMENT_TEXT)
			.tab().addLine("*parts of the map.* (%d broken so far!)").styles(DescriptionUtils.REQUIREMENT_TEXT).statValues(scoreboard("CoalruptedSpawnersBroken"))
			.addDashedLine()
			.addIfElse((a, p) -> AdvancementUtils.checkAdvancement(p, ADVANCEMENT_REQ),
				desc -> desc.addTrigger()
					.addLine()
					.addLine("Activate to make your pickaxe instantly mine")
					.addLine("any type of block for the next %t.").statValues(stat(DURATION))
					.addLine("(Unbreakable blocks are excluded)"),
				desc -> desc.addLine("*Activate to make your pickaxe instantly mine*").styles(DescriptionUtils.OBFUSCATED)
					.addLine("*any type of block for the next %t.*").styles(DescriptionUtils.OBFUSCATED)
					.addLine("*(Unbreakable blocks are excluded)*").styles(DescriptionUtils.OBFUSCATED))
			.addLine()
			.addStat("Max Uses: %d *per game*").styles(DescriptionUtils.GREY).statValues(stat(MAX_USES))
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
