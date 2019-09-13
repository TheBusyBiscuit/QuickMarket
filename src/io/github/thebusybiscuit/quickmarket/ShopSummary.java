package io.github.thebusybiscuit.quickmarket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.cscorelib2.math.DoubleHandler;

public class ShopSummary {
	
	public Map<String, Double> temp_moneyS = new HashMap<>();
	public Map<String, Double> temp_moneyB = new HashMap<>();
	public Map<String, Integer> temp_itemsS = new HashMap<>();
	public Map<String, Integer> temp_itemsB = new HashMap<>();
	
	public ShopSummary(final UUID owner) {
		Bukkit.getScheduler().runTaskTimer(QuickMarket.getInstance(), () -> {
			Player pl = Bukkit.getPlayer(owner);
			if (pl != null) {
				String symbol = QuickMarket.getInstance().cfg.getString("options.money-symbol");
				
				for (String p: temp_moneyS.keySet()) {
					int amount = temp_itemsS.get(p);
					double money = temp_moneyS.get(p);
					QuickMarket.getInstance().local.sendMessage(pl, "shops.sold-owner", false, (msg) -> {
						return msg.replace("{MONEY}", symbol + DoubleHandler.getFancyDouble(money))
								.replace("{AMOUNT}", String.valueOf(amount))
								.replace("{PLAYER}", p);
					});
				}
				
				for (String p: temp_moneyB.keySet()) {
					int amount = temp_itemsB.get(p);
					double money = temp_moneyB.get(p);
					QuickMarket.getInstance().local.sendMessage(pl, "shops.bought-owner", false, (msg) -> {
						return msg.replace("{MONEY}", symbol + DoubleHandler.getFancyDouble(money))
								.replace("{AMOUNT}", String.valueOf(amount))
								.replace("{PLAYER}", p);
					});
				}
				
				temp_moneyS.clear();
				temp_moneyB.clear();
				temp_itemsS.clear();
				temp_itemsB.clear();
			}
		}, 0L, 60 * 20L);
	}

}
