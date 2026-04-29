package com.playmonumenta.plugins.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;

public class CopyBlightArenaCommand {
	static final String COMMAND = "siriuscopyarena";
	private static final EnumSet<Material> IGNORED_MATS = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK
	);

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.siriuscopyarena");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LocationArgument("middle"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes(CopyBlightArenaCommand::execute)
			.register();
	}

	private static void execute(CommandSender sender, CommandArguments args) {
		//stops build sharders swapping the arena and makes sure it only works on build.
		//could remove shard check but want to be safe
		if (!Plugin.IS_PLAY_SERVER && !ServerProperties.getShardName().equals("build")) {
			MMLog.debug("Copying Arena");
			Location middle = args.getUnchecked("middle");
			Location mCornerOne = middle.clone().add(43, 49, 58);
			Location mCornerTwo = middle.clone().subtract(75, 7, 60);
			Map<String, List<BlockData>> mStates = new HashMap<>();
			for (double x = mCornerTwo.getX(); x < mCornerOne.getX(); x++) {
				for (double z = mCornerTwo.getZ(); z < mCornerOne.getZ(); z++) {
					List<BlockData> blockData = new ArrayList<>();
					for (double y = mCornerTwo.getY(); y < mCornerOne.getY(); y++) {
						Location loc = new Location(middle.getWorld(), x, y, z);
						if (!IGNORED_MATS.contains(loc.getBlock().getType())) {
							blockData.add(loc.getBlock().getBlockData());
						}
					}
					mStates.put("x" + (mCornerOne.getX() - x) + "z" + (mCornerOne.getZ() - z), blockData);
				}
			}
			JsonObject blight = new JsonObject();
			JsonArray blightArr = new JsonArray();
			blight.add("blight", blightArr);
			for (String key : mStates.keySet()) {
				JsonObject object = new JsonObject();
				StringBuilder output = new StringBuilder();
				List<BlockData> list = mStates.get(key);
				if (!list.isEmpty()) {
					for (BlockData data : list) {
						String s = data.toString();
						s = s.substring(15, s.length() - 1);
						output.append(s).append(", ");
					}
					output.deleteCharAt(output.length() - 1);
					output.deleteCharAt(output.length() - 1);
					object.addProperty(key, output.toString());
					blightArr.add(object);
				}
			}
			try {
				MMLog.debug("Wrote arena JSON");
				FileUtils.writeJson(Plugin.getInstance().getDataFolder() + "/SiriusBlightArena.json", blight);
			} catch (Exception e) {
				MMLog.severe("Failed to write Sirius blight arena JSON", e);
			}
		} else {
			sender.sendMessage("You do not have permission to use this bosstag");
		}
	}
}
