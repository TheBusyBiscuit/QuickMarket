package me.mrCookieSlime.QuickMarket.shop;

import org.bukkit.ChatColor;

public enum ShopType {
	
	BUY("&lBuy: &r"),
	SELL("&lSell: &r"),
	SELL_ALL("&lSell: &r");
	
	private String sign;
	
	private ShopType(String string) {
		this.sign = string;
	}
	
	public String getSignMessage() {
		return ChatColor.translateAlternateColorCodes('&', sign);
	}
}