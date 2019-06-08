package me.mrCookieSlime.QuickMarket.shop;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Variable;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Math.DoubleHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.World.CustomSkull;
import me.mrCookieSlime.QuickMarket.QuickMarket;

public class MarketStand {
	
	public static Map<String, MarketStand> map = new HashMap<String, MarketStand>();
	
	double cost;
	Sign sign;
	UUID owner = null;
	String player = null;
	long timestamp;

	public MarketStand(Config cfg) {
		try {
			sign = (Sign) cfg.getLocation("sign").getWorld().getBlockAt(cfg.getLocation("sign")).getState();
			timestamp = cfg.getLong("timestamp");
			cost = cfg.getDouble("cost");
			
			if (timestamp > System.currentTimeMillis()) {
				if (cfg.contains("owner")) owner = UUID.fromString(cfg.getString("owner"));
				player = Bukkit.getOfflinePlayer(owner).getName();
			}
			
			map.put(location(sign.getLocation()), this);
		} catch(Exception x) {
			System.err.println("[QuickMarket] Found remainings of deleted Market @ " + cfg.getFile().getName());
			cfg.getFile().delete();
		}
	}
	
	public MarketStand(Block sign, double cost) {
		this.sign = (Sign) sign.getState();
		this.timestamp = 0;
		this.cost = cost;
		update();
		
		map.put(location(sign.getLocation()), this);
	}

	public void update() {
		if (timestamp > 0 && timestamp < System.currentTimeMillis()) {
			abandon();
		}
		
		sign = (Sign) sign.getBlock().getState();
		if (owner != null) {
			player = Bukkit.getOfflinePlayer(owner).getName();
			sign.setLine(0, ChatColor.translateAlternateColorCodes('&', QuickMarket.getInstance().cfg.getString("marketstand.prefix-unavailable")));
			sign.setLine(1, ChatColor.translateAlternateColorCodes('&', "&4&lRENTED"));
			sign.setLine(2, timeleft());
			sign.setLine(3, player);
		}
		else {
			sign.setLine(0, ChatColor.translateAlternateColorCodes('&', QuickMarket.getInstance().cfg.getString("marketstand.prefix-available")));
			sign.setLine(1, ChatColor.translateAlternateColorCodes('&', "&6&lFOR RENT"));
			sign.setLine(2, "");
			sign.setLine(3, ChatColor.translateAlternateColorCodes('&', "&6" + QuickMarket.getInstance().cfg.getString("options.money-symbol")) + DoubleHandler.getFancyDouble(cost) + "/day");
		}
		sign.update();
	}
	
	private String timeleft() {
		int hours = (int) ((timestamp - System.currentTimeMillis()) / (1000 * 60 * 60));
		return (hours / 24) + "d " + (hours % 24) + "h left";
	}

	public void save() {
		if (new File("data-storage/QuickMarket/markets/" + sign.getWorld().getUID().toString() + "_" + sign.getBlock().getX() + "_" + sign.getBlock().getY() + "_" + sign.getBlock().getZ() + ".market").exists()) {
			new File("data-storage/QuickMarket/markets/" + sign.getWorld().getUID().toString() + "_" + sign.getBlock().getX() + "_" + sign.getBlock().getY() + "_" + sign.getBlock().getZ() + ".market").delete();
		}
		Config cfg = new Config("data-storage/QuickMarket/markets/" + sign.getWorld().getUID().toString() + ";" + sign.getBlock().getX() + ";" + sign.getBlock().getY() + ";" + sign.getBlock().getZ() + ".market");
		
		try {
			cfg.setValue("sign", sign.getLocation());
			
			if (owner == null) cfg.setValue("owner", null);
			else cfg.setValue("owner", owner.toString());
			
			cfg.setValue("timestamp", timestamp);
			cfg.setValue("cost", cost);
			
			cfg.save();
		}
		catch(Exception x) {
			System.err.println("[QuickMarket] ERROR: Could not save a Shop");
		}
	}
	
	public static String location(Location l) {
		return l.getWorld().getName() + ";" + l.getBlock().getX() + ";" + l.getBlock().getY() + ";" + l.getBlock().getZ();
	}

	public void openGUI(final Player p) throws Exception {
		String symbol = QuickMarket.getInstance().cfg.getString("options.money-symbol");
		ChestMenu menu = new ChestMenu("&4Market Manager");
		
		menu.addItem(0, new CustomItem(CustomSkull.getItem("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzFiYzJiY2ZiMmJkMzc1OWU2YjFlODZmYzdhNzk1ODVlMTEyN2RkMzU3ZmMyMDI4OTNmOWRlMjQxYmM5ZTUzMCJ9fX0="), "&7Rent for &b+1 &7Day", "&7Cost: &6" + symbol + DoubleHandler.getFancyDouble(cost), "", "&eCLICK &7to rent"));
		menu.addMenuClickHandler(0, (player, slot, item, action) -> {
			rent(p, 1);
			return false;
		});
		
		menu.addItem(1, new CustomItem(CustomSkull.getItem("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNkOWVlZWU4ODM0Njg4ODFkODM4NDhhNDZiZjMwMTI0ODVjMjNmNzU3NTNiOGZiZTg0ODczNDE0MTk4NDcifX19"), "&7Rent for &b+2 &7Days", "&7Cost: &6" + symbol + DoubleHandler.getFancyDouble(cost * 2), "", "&eCLICK &7to rent"));
		menu.addMenuClickHandler(1, (player, slot, item, action) -> {
			rent(p, 2);
			return false;
		});
		
		menu.addItem(2, new CustomItem(CustomSkull.getItem("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWQ0ZWFlMTM5MzM4NjBhNmRmNWU4ZTk1NTY5M2I5NWE4YzNiMTVjMzZiOGI1ODc1MzJhYzA5OTZiYzM3ZTUifX19"), "&7Rent for &b+3 &7Days", "&7Cost: &6" + symbol + DoubleHandler.getFancyDouble(cost * 3), "", "&eCLICK &7to rent"));
		menu.addMenuClickHandler(2, (player, slot, item, action) -> {
			rent(p, 3);
			return false;
		});
		
		if (timestamp > System.currentTimeMillis() && owner != null) {
			menu.addItem(8, new CustomItem(new ItemStack(Material.BARRIER), "&4Abandon Marketstand", "", "&7Refund: &c" + symbol + DoubleHandler.getFancyDouble(getRefund()) + " &4(50%)", "", "&eCLICK &7to abandon this Marketstand"));
			menu.addMenuClickHandler(8, (player, slot, item, action) -> {
				if (owner != null) {
					QuickMarket.getInstance().economy.depositPlayer(p, getRefund());
					abandon();
					update();
					p.playSound(p.getLocation(), Sound.ENTITY_BAT_DEATH, 1F, 1F);
					p.closeInventory();
				}
				return false;
			});
		}
		
		menu.open(p);
	}
	
	private double getRefund() {
		return DoubleHandler.fixDouble(cost / 2 * (timestamp - System.currentTimeMillis()) / (1000 * 60 * 60 * 24));
	}

	public void rent(Player p, int days) {
		double cost = DoubleHandler.fixDouble(this.cost * days);
		if (QuickMarket.getInstance().economy.getBalance(p) >= cost) {
			long timestamp = this.timestamp < System.currentTimeMillis() ? System.currentTimeMillis(): this.timestamp;
			long max = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * QuickMarket.getInstance().cfg.getInt("marketstand.max-days");
			
			if (timestamp + 1000 * 60 * 60 * 24 * days > max) {
				QuickMarket.getInstance().local.sendTranslation(p, "market.too-long", true);
			}
			else {
				this.timestamp = timestamp + 1000 * 60 * 60 * 24 * days;
				this.owner = p.getUniqueId();
				QuickMarket.getInstance().economy.withdrawPlayer(p, cost);
				QuickMarket.getInstance().local.sendTranslation(p, "market.rented", true, new Variable("%days%", String.valueOf(days)));
				
				p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
				update();
				updateShops();
			}
		}
		else {
			QuickMarket.getInstance().local.sendTranslation(p, "market.insufficient-funds", true);
		}
		p.closeInventory();
	}
	
	private void abandon() {
		this.owner = null;
		this.player = null;
		this.timestamp = 0;
		updateShops();
	}

	private void updateShops() {
		for (PlayerShop shop: PlayerShop.shops) {
			if (shop instanceof PlayerMarket) {
				if (((PlayerMarket) shop).getMarket().equals(location(sign.getLocation()))) shop.update(true);
			}
		}
	}

	public boolean isOwner(Player p) {
		return owner == null || p.getUniqueId().equals(owner);
	}

	public void delete() {
		map.remove(location(sign.getLocation()));
		File file = new File("data-storage/QuickMarket/markets/" + sign.getWorld().getUID().toString() + ";" + sign.getBlock().getX() + ";" + sign.getBlock().getY() + ";" + sign.getBlock().getZ() + ".market");
		if (file.exists()) file.delete();
		Iterator<PlayerShop> shops = PlayerShop.shops.iterator();
		
		while (shops.hasNext()) {
			PlayerShop shop = shops.next();
			if (shop instanceof PlayerMarket) {
				if (((PlayerMarket) shop).getMarket().equals(location(sign.getLocation()))) {
					shop.delete(true);
					shops.remove();
				}
			}
		}
	}

}
