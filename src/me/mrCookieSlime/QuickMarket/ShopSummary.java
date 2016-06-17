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
		Bukkit.getScheduler().scheduleSyncRepeatingTask(QuickMarket.getInstance(), new Runnable() {
			
			@Override
			public void run() {
				Player pl = Bukkit.getPlayer(owner);
				if (pl != null) {
					for (String p: temp_moneyS.keySet()) {
						int amount = temp_itemsS.get(p);
						double money = temp_moneyS.get(p);
						QuickMarket.getInstance().local.sendTranslation(pl, "shops.sold-owner", false, new Variable("{MONEY}", "$" + DoubleHandler.getFancyDouble(money)), new Variable("{AMOUNT}", String.valueOf(amount)), new Variable("{PLAYER}", p));
					}
					for (String p: temp_moneyB.keySet()) {
						int amount = temp_itemsB.get(p);
						double money = temp_moneyB.get(p);
						QuickMarket.getInstance().local.sendTranslation(pl, "shops.bought-owner", false, new Variable("{MONEY}", "$" + DoubleHandler.getFancyDouble(money)), new Variable("{AMOUNT}", String.valueOf(amount)), new Variable("{PLAYER}", p));
					}
					temp_moneyS.clear();
					temp_moneyB.clear();
					temp_itemsS.clear();
					temp_itemsB.clear();
				}
			}
		}, 0L, 60 * 20L);
	}
	
	public Map<String, Double> temp_moneyS = new HashMap<String, Double>(), temp_moneyB = new HashMap<String, Double>();
	public Map<String, Integer> temp_itemsS = new HashMap<String, Integer>(), temp_itemsB = new HashMap<String, Integer>();

}
