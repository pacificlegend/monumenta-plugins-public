package com.playmonumenta.plugins.commands;

import com.goncalomb.bukkit.mylib.reflect.NBTTagCompound;
import com.goncalomb.bukkit.mylib.reflect.NBTTagList;
import com.goncalomb.bukkit.nbteditor.bos.BookOfSouls;
import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulsDatabase;
import com.playmonumenta.plugins.bosses.bosses.GenericTargetBoss;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class BossTagFixes {

	// Vanilla entity type IDs (without "minecraft:" prefix) that do NOT implement the Mob
	// interface — i.e. entities that have no AI and therefore cannot validly use boss tags
	// that require Mob. Includes non-living entities (projectiles, vehicles, objects) and
	// living-but-not-Mob entities (armor_stand, player).
	private static final Set<String> NON_MOB_ENTITY_IDS = Set.of(
		// living but not Mob
		"armor_stand",
		"player",
		// projectiles
		"arrow",
		"breeze_wind_charge",
		"dragon_fireball",
		"egg",
		"ender_pearl",
		"eye_of_ender",
		"fireball",
		"fishing_bobber",
		"llama_spit",
		"potion",
		"shulker_bullet",
		"small_fireball",
		"snowball",
		"spectral_arrow",
		"thrown_exp_bottle",
		"trident",
		"wind_charge",
		"wither_skull",
		// vehicles
		"boat",
		"chest_boat",
		"chest_minecart",
		"command_block_minecart",
		"furnace_minecart",
		"hopper_minecart",
		"minecart",
		"spawner_minecart",
		"tnt_minecart",
		// other non-living
		"area_effect_cloud",
		"block_display",
		"end_crystal",
		"evoker_fangs",
		"experience_orb",
		"falling_block",
		"firework_rocket",
		"glow_item_frame",
		"interaction",
		"item",
		"item_display",
		"item_frame",
		"leash_knot",
		"lightning_bolt",
		"marker",
		"ominous_item_spawner",
		"painting",
		"text_display",
		"tnt"
	);

	// Removes all tags starting with tagPrefix from non-Mob entities in this entity's tree
	// (this entity and all passengers, recursively). Mutates entityNbt in place.
	// Returns true if any tags were removed.
	private static boolean fixEntityNBT(NBTTagCompound entityNbt, String tagPrefix) {
		boolean modified = false;

		String rawId = entityNbt.getString("id");
		if (rawId != null) {
			String entityId = (rawId.startsWith("minecraft:") ? rawId.substring(10) : rawId).toLowerCase(Locale.ROOT);
			if (NON_MOB_ENTITY_IDS.contains(entityId)) {
				NBTTagList tagsList = entityNbt.getList("Tags");
				if (tagsList != null) {
					for (int i = tagsList.size() - 1; i >= 0; i--) {
						if (((String) tagsList.get(i)).startsWith(tagPrefix)) {
							tagsList.remove(i);
							modified = true;
						}
					}
				}
			}
		}

		NBTTagList passengers = entityNbt.getList("Passengers");
		if (passengers != null) {
			for (Object passenger : passengers.getAsArray()) {
				if (passenger instanceof NBTTagCompound passengerNbt) {
					modified |= fixEntityNBT(passengerNbt, tagPrefix);
				}
			}
		}

		return modified;
	}

	public static void fixGenericTargetNonMobEntities(Player player) {
		player.sendMessage(Component.empty()
			.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			.append(Component.text("Fixing " + GenericTargetBoss.identityTag + ": removing from non-Mob entities...", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

		List<String> fixed = new ArrayList<>();

		for (String soulName : LibraryOfSoulsAPI.getSoulNames()) {
			Soul soul = SoulsDatabase.getInstance().getSoul(soulName);
			if (soul == null) {
				continue;
			}

			EntityNBT entityNBT = BookOfSouls.bookToEntityNBT(soul.getBoS());
			if (entityNBT == null) {
				continue;
			}

			if (fixEntityNBT(entityNBT.getData(), GenericTargetBoss.identityTag)) {
				SoulsDatabase.getInstance().update(player, new BookOfSouls(entityNBT));
				fixed.add(soul.getLabel());
			}
		}

		player.sendMessage(Component.empty()
			.append(Component.text("[BossTag] ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
			.append(Component.text("Fixed " + fixed.size() + " soul(s):", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
		for (String label : fixed) {
			player.sendMessage(Component.text("  - " + label, NamedTextColor.WHITE));
		}
	}
}
