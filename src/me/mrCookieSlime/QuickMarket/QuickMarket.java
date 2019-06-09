package me.mrCookieSlime.QuickMarket;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.mrCookieSlime.CSCoreLibPlugin.PluginUtils;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Localization;
import me.mrCookieSlime.CSCoreLibSetup.CSCoreLibLoader;
import me.mrCookieSlime.QuickMarket.shop.MarketStand;
import me.mrCookieSlime.QuickMarket.shop.PlayerMarket;
import me.mrCookieSlime.QuickMarket.shop.PlayerShop;
import me.mrCookieSlime.bstats.bukkit.Metrics;
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
		CSCoreLibLoader loader = new CSCoreLibLoader(this);
		
		if (loader.load()) {
			instance = this;
			
			PluginUtils utils = new PluginUtils(this);
			utils.setupConfig();
			cfg = utils.getConfig();
			
			
			utils.setupUpdater(94051, getFile());
			utils.setupMetrics();
			
			Metrics metrics = utils.getMetrics();
			
			metrics.addCustomChart(new Metrics.SingleLineChart("player_shops_on_servers_using_quickmarket", () -> {
				File file = new File("data-storage/QuickMarket/shops");
				
				if (!file.exists()) return 0;
				else return file.listFiles().length;
			}));
			
			utils.setupLocalization();
			local = utils.getLocalization();
			local.setPrefix("&6Quickmarket &7> ");
			local.setDefault("shops.not-a-chest", "&cThis Sign must be facing a Chest!");
			local.setDefault("shops.not-a-valid-price", "&cThe Price on Line 3 is not valid!");
			local.setDefault("shops.not-a-valid-amount", "&cThe Amount on Line 2 is not valid!");
			local.setDefault("shops.not-a-valid-type", "&cUnknown Shop Type on Line 4 Allowed: (sell/buy)");
			local.setDefault("shops.full-inventory", "&4That Shop is full!");
			local.setDefault("shops.insufficient-funds", "&cYou do not have enough Money to buy from this Shop");
			local.setDefault("shops.insufficient-funds-owner", "&cThe Owner of this Shop has insufficient Funds!");
			local.setDefault("shops.not-enough-items", "&cYou do not have enough Items");
			local.setDefault("shops.not-enough-items-owner", "&cThis Shop is sold out!");
			local.setDefault("shops.disabled", "&cThis Shop is currently disabled! Come back later!");
			local.setDefault("shops.editing", "&cThis Shop is currently being edited! Please come back later!");
			local.setDefault("shops.delete-via-sign", "&4Delete the Shop using the Shopmenu instead of breaking the Block");
			local.setDefault("shops.no-access", "&4You are not allowed to edit this Shop!");
			local.setDefault("shops.no-permission", "&4You are not allowed to create Shops!");
			
			local.setDefault("shops.sold", "&8+ &6{MONEY} &8[&cSold&o {AMOUNT} &cItems&8]");
			local.setDefault("shops.sold-owner", "&8- &6{MONEY} &8[&c{PLAYER} sold&o {AMOUNT} &cItems&8]");
			local.setDefault("shops.bought", "&8- &6{MONEY} &8[&cBought&o {AMOUNT} &cItems&8]");
			local.setDefault("shops.bought-owner", "&8+ &6{MONEY} &8[&c{PLAYER} bought&o {AMOUNT} &cItems&8]");
			local.setDefault("shops.reached-max-price", "&cMax Price reached!");
			
			local.setDefault("market.link", "&eYou are not done yet! RIGHT CLICK an existing MarketStand Sign to link it to the Shop you are creating, click any other Block to cancel the Shop Creation");
			local.setDefault("market.link-success", "&aSuccessfully created a Market-Shop!");
			local.setDefault("market.link-abort", "&cShop Creation cancelled.");
			local.setDefault("market.insufficient-funds", "&cYou do not have enough Money to rent this Marketstand for that long");
			local.setDefault("market.too-long", "&cYou cannot rent a Marketstand for that long!");
			local.setDefault("market.rented", "&aYou successfully rented this MarketStand for %days% Day(s)!");
			local.setDefault("market.not-a-valid-price", "&cThe Price on Line 2 is not valid!");
			local.setDefault("market.delete-via-sign", "&4Delete the Shop by destroying the Marketstand Sign");
			local.setDefault("market.no-permission", "&4You are not allowed to create Marketstands!");
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
