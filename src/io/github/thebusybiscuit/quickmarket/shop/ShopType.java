package io.github.thebusybiscuit.quickmarket.shop;

import org.bukkit.ChatColor;

public enum ShopType {
	
	BUY("Buy", "&lBuy: &r"),
	SELL("Sell", "&lSell: &r"),
	SELL_ALL("Sell All", "&lSell: &r");
	
	private String name, sign;
	
	private ShopType(String name, String msg) {
		this.name = name;
		this.sign = msg;
	}
	
	public String getSignMessage() {
		return ChatColor.translateAlternateColorCodes('&', sign);
	}
	
	public String getName() {
		return name;
	}
}