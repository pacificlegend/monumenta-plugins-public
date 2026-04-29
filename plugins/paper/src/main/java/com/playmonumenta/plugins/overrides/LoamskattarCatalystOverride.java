package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LoamskattarCatalystOverride extends BaseOverride {
	private static final String COOLDOWN_SOURCE = "LoamskattarCatalystCooldown";
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND;

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (!Aurora.isAuroraLoom(item)) {
			return true;
		}

		if (!Aurora.hasStoredCharges(player) || plugin.mEffectManager.hasEffect(player, COOLDOWN_SOURCE)) {
			return true;
		}

		Location location = player.getLocation();
		for (LivingEntity livingEntity : EntityUtils.getNearbyMobs(location, Aurora.ARENA_RADIUS * 2)) {
			@Nullable
			Aurora aurora = BossManager.getInstance().getBoss(livingEntity, Aurora.class);
			if (aurora == null) {
				continue;
			}
			World world = location.getWorld();
			world.playSound(location, Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 2.0f, 1.4f);
			world.playSound(location, Sound.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 2.0f, 1.2f);
			world.playSound(location, Sound.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 2.0f, 1.5f);

			Aurora.useStoredCharges(player);
			aurora.usedLoamskattarCatalyst();
			aurora.repairBlocks(BlockUtils.getBlocksInCube(location.clone().subtract(0, 5, 0), 5));
			player.updateInventory();

			plugin.mEffectManager.addEffect(player, COOLDOWN_SOURCE, new ItemCooldown(COOLDOWN, item, plugin));
			break;
		}

		return true;
		// Apparently returning true here stops the item from disappearing when you place it, blame paper I guess!
	}

}
