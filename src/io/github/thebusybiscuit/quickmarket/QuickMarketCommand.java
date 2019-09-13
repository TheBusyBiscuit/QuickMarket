package io.github.thebusybiscuit.quickmarket;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuickMarketCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.GOLD + "QuickMarket v" + ChatColor.YELLOW + QuickMarket.getInstance().getDescription().getVersion());
		}
		else {
			sender.sendMessage("QuickMarket v" + QuickMarket.getInstance().getDescription().getVersion());
		}
		return true;
	}

}
