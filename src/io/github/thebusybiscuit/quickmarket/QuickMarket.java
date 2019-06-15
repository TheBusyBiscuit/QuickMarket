package io.github.thebusybiscuit.quickmarket;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.cscorelib2.config.Config;
import io.github.thebusybiscuit.cscorelib2.config.Localization;
import io.github.thebusybiscuit.cscorelib2.updater.BukkitUpdater;
import io.github.thebusybiscuit.quickmarket.shop.MarketStand;
import io.github.thebusybiscuit.quickmarket.shop.PlayerMarket;
import io.github.thebusybiscuit.quickmarket.shop.PlayerShop;
import net.milkbowl.vault.economy.Economy;

public class QuickMarket extends JavaPlugin {

	private static QuickMarket instance;
	public Config cfg;
	public Localization local;
	private boolean clearlag, backpacks;
	public Economy economy;

	protected List<UUID> worlds = new ArrayList<>();
	
	@Override
	public void onEnable() {
		instance = this;
		
		cfg = new Config(this);
		new BukkitUpdater(this, getFile(), 94051).start();
		
		Metrics metrics = new Metrics(this);
		
		metrics.addCustomChart(new Metrics.SingleLineChart("player_shops_on_servers_using_quickmarket", () -> {
			File file = new File("data-storage/QuickMarket/shops");
			
			if (!file.exists()) return 0;
			else return file.listFiles().length;
		}));
		
		local = new Localization(this);
		local.setPrefix("&6Quickmarket &7> ");
		local.setDefaultMessage("shops.not-a-chest", "&cThis Sign must be facing a Chest!");
		local.setDefaultMessage("shops.not-a-valid-price", "&cThe Price on Line 3 is not valid!");
		local.setDefaultMessage("shops.not-a-valid-amount", "&cThe Amount on Line 2 is not valid!");
		local.setDefaultMessage("shops.not-a-valid-type", "&cUnknown Shop Type on Line 4 Allowed: (sell/buy)");
		local.setDefaultMessage("shops.full-inventory", "&4That Shop is full!");
		local.setDefaultMessage("shops.insufficient-funds", "&cYou do not have enough Money to buy from this Shop");
		local.setDefaultMessage("shops.insufficient-funds-owner", "&cThe Owner of this Shop has insufficient Funds!");
		local.setDefaultMessage("shops.not-enough-items", "&cYou do not have enough Items");
		local.setDefaultMessage("shops.not-enough-items-owner", "&cThis Shop is sold out!");
		local.setDefaultMessage("shops.disabled", "&cThis Shop is currently disabled! Come back later!");
		local.setDefaultMessage("shops.editing", "&cThis Shop is currently being edited! Please come back later!");
		local.setDefaultMessage("shops.delete-via-sign", "&4Delete the Shop using the Shopmenu instead of breaking the Block");
		local.setDefaultMessage("shops.no-access", "&4You are not allowed to edit this Shop!");
		local.setDefaultMessage("shops.no-permission", "&4You are not allowed to create Shops!");
		
		local.setDefaultMessage("shops.sold", "&8+ &6{MONEY} &8[&cSold&o {AMOUNT} &cItems&8]");
		local.setDefaultMessage("shops.sold-owner", "&8- &6{MONEY} &8[&c{PLAYER} sold&o {AMOUNT} &cItems&8]");
		local.setDefaultMessage("shops.bought", "&8- &6{MONEY} &8[&cBought&o {AMOUNT} &cItems&8]");
		local.setDefaultMessage("shops.bought-owner", "&8+ &6{MONEY} &8[&c{PLAYER} bought&o {AMOUNT} &cItems&8]");
		local.setDefaultMessage("shops.reached-max-price", "&cMax Price reached!");
		
		local.setDefaultMessage("market.link", "&eYou are not done yet! RIGHT CLICK an existing MarketStand Sign to link it to the Shop you are creating, click any other Block to cancel the Shop Creation");
		local.setDefaultMessage("market.link-success", "&aSuccessfully created a Market-Shop!");
		local.setDefaultMessage("market.link-abort", "&cShop Creation cancelled.");
		local.setDefaultMessage("market.insufficient-funds", "&cYou do not have enough Money to rent this Marketstand for that long");
		local.setDefaultMessage("market.too-long", "&cYou cannot rent a Marketstand for that long!");
		local.setDefaultMessage("market.rented", "&aYou successfully rented this MarketStand for %days% Day(s)!");
		local.setDefaultMessage("market.not-a-valid-price", "&cThe Price on Line 2 is not valid!");
		local.setDefaultMessage("market.delete-via-sign", "&4Delete the Shop by destroying the Marketstand Sign");
		local.setDefaultMessage("market.no-permission", "&4You are not allowed to create Marketstands!");
		local.save();
		
		if (!new File("data-storage/QuickMarket/shops").exists()) new File("data-storage/QuickMarket/shops").mkdirs();
		if (!new File("data-storage/QuickMarket/markets").exists()) new File("data-storage/QuickMarket/markets").mkdirs();
		
		if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
			getServer().getPluginManager().disablePlugin(this);
			System.err.println("[QuickMarket] Could not find Vault! Make sure to install Vault and an Economy Plugin");
		}
		
		for (File file: new File("data-storage/QuickMarket/shops").listFiles()) {
			if (!file.getName().contains(";")) new PlayerShop(new Config(file));
		}
		
		try {
			for (World world: Bukkit.getWorlds()) {
				if (!worlds.contains(world.getUID())) {
					for (File file: new File("data-storage/QuickMarket/shops").listFiles()) {
						if (file.getName().split(";")[0].equals(world.getUID().toString())) {
							Config cfg = new Config(file);
							if (cfg.getBoolean("market")) new PlayerMarket(cfg);
							else new PlayerShop(cfg);
						}
					}
					for (File file: new File("data-storage/QuickMarket/markets").listFiles()) {
						if (file.getName().split(";")[0].equals(world.getUID().toString())) {
							new MarketStand(new Config(file));
						}
					}
					System.out.println("[QuickMarket] Loaded " + PlayerShop.shops.size() + " Shop(s) for World \"" + world.getName() + "\"");
				}
			}
			
			getServer().getPluginManager().registerEvents(new Listener() {
				
				@EventHandler
				public void onWorldLoad(WorldLoadEvent e) {
					World world = e.getWorld();
					
					if (!worlds.contains(world.getUID())) {
						for (File file: new File("data-storage/QuickMarket/shops").listFiles()) {
							if (file.getName().split(";")[0].equals(world.getUID().toString())) {
								Config cfg = new Config(file);
								if (cfg.getBoolean("market")) new PlayerMarket(cfg);
								else new PlayerShop(cfg);
							}
						}
						for (File file: new File("data-storage/QuickMarket/markets").listFiles()) {
							if (file.getName().split(";")[0].equals(world.getUID().toString())) {
								new MarketStand(new Config(file));
							}
						}
						System.out.println("[QuickMarket] Loaded " + PlayerShop.shops.size() + " Shop(s) for World \"" + world.getName() + "\"");
					}
				}
				
			}, this);
			
			if (!setupEconomy()) {
				getServer().getPluginManager().disablePlugin(this);
				throw new IllegalStateException("Disabling QuickMarket - No Economy Plugin found!");
			}
			
			new MarketListener(this);
			
			clearlag = getServer().getPluginManager().isPluginEnabled("ClearLag");
			backpacks = getServer().getPluginManager().isPluginEnabled("PrisonUtils");
			
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				
				@Override
				public void run() {
					for (PlayerShop shop: PlayerShop.shops) {
						shop.update(true);
					}
					for (MarketStand market: MarketStand.map.values()) {
						market.update();
					}
				}
			}, 0L, cfg.getInt("options.item-refresh-delay") * 20L);
		}
		catch (Exception x) {
			x.printStackTrace();
			PlayerShop.shops = null;
		}
	}
	
	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
	    if (economyProvider != null) {
	      economy = (Economy)economyProvider.getProvider();
	    }

	    return economy != null;
	}
	
	public boolean isClearLagInstalled() {
		return clearlag;
	}
	
	public boolean isPrisonUtilsInstalled() {
		return backpacks;
	}
	
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(getInstance());
		
		if (PlayerShop.shops != null) {
			for (PlayerShop shop: PlayerShop.shops) {
				shop.store();
			}
			for (MarketStand market: MarketStand.map.values()) {
				market.save();
			}
			System.out.println("[QuickMarket] Saved " + PlayerShop.shops.size() + " Shop(s)!");
		}
		
		instance = null;
		PlayerShop.chests = null;
		PlayerShop.shops = null;
		PlayerShop.signs = null;
		PlayerShop.summaries = null;
	}

	public static QuickMarket getInstance() {
		return instance;
	}

}
