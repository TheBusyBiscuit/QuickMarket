package me.mrCookieSlime.QuickMarket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Variable;
import me.mrCookieSlime.CSCoreLibPlugin.general.Math.DoubleHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ShopSummary {
	
	public ShopSummary(final UUID owner) {
		Bukkit.getScheduler().runTaskTimer(QuickMarket.getInstance(), () -> {
			Player pl = Bukkit.getPlayer(owner);
			if (pl != null) {
				String symbol = QuickMarket.getInstance().cfg.getString("options.money-symbol");
				
				for (String p: temp_moneyS.keySet()) {
					int amount = temp_itemsS.get(p);
					double money = temp_moneyS.get(p);
					QuickMarket.getInstance().local.sendTranslation(pl, "shops.sold-owner", false, new Variable("{MONEY}", symbol + DoubleHandler.getFancyDouble(money)), new Variable("{AMOUNT}", String.valueOf(amount)), new Variable("{PLAYER}", p));
				}
				
				for (String p: temp_moneyB.keySet()) {
					int amount = temp_itemsB.get(p);
					double money = temp_moneyB.get(p);
					QuickMarket.getInstance().local.sendTranslation(pl, "shops.bought-owner", false, new Variable("{MONEY}", symbol + DoubleHandler.getFancyDouble(money)), new Variable("{AMOUNT}", String.valueOf(amount)), new Variable("{PLAYER}", p));
				}
				
				temp_moneyS.clear();
				temp_moneyB.clear();
				temp_itemsS.clear();
				temp_itemsB.clear();
			}
		}, 0L, 60 * 20L);
	}
	
	public Map<String, Double> temp_moneyS = new HashMap<>(), temp_moneyB = new HashMap<>();
	public Map<String, Integer> temp_itemsS = new HashMap<>(), temp_itemsB = new HashMap<>();

}
