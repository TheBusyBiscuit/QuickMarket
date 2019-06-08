package me.mrCookieSlime.QuickMarket.shop;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import me.mrCookieSlime.CSCoreLibPlugin.CSCoreLib;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Variable;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.InvUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Math.DoubleHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Player.PlayerInventory;
import me.mrCookieSlime.CSCoreLibPlugin.general.String.StringUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.World.CustomSkull;
import me.mrCookieSlime.CSCoreLibPlugin.general.audio.Soundboard;
import me.mrCookieSlime.PrisonUtils.Backpacks;
import me.mrCookieSlime.QuickMarket.QuickMarket;
import me.mrCookieSlime.QuickMarket.ShopSummary;

public class PlayerShop {
	
	public static List<PlayerShop> shops = new ArrayList<>();
	public static Map<Block, PlayerShop> signs = new HashMap<>();
	public static Map<Block, PlayerShop> chests = new HashMap<>();
	public static Map<String, List<PlayerShop>> chunks = new HashMap<>();
	public static Map<UUID, ShopSummary> summaries = new HashMap<>();
	
	ShopType type;
	UUID owner = null;
	String player = null;
	double price;
	ItemStack item;
	Item display;
	Chest chest;
	Sign sign;
	int amount;
	long used;
	boolean infinite, disabled;
	boolean[] schedule = new boolean[24];
	
	boolean loaded = true;
	
	private boolean editing = false;
	
	public ShopSummary getSummary() {
		if (!summaries.containsKey(owner)) summaries.put(owner, new ShopSummary(owner));
		return summaries.get(owner);
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}
	
	public PlayerShop(Block sign, Block chest, Player p, int amount, double price, ShopType type) {
		try {
			this.chest = (Chest) chest.getState();
			this.sign = (Sign) sign.getState();
			this.type = type;
			this.owner = p.getUniqueId();
			this.player = p.getName();
			this.price = price;
			this.amount = amount;
			this.item = new CustomItem(Material.APPLE, "Click the Sign");
			this.used = 0;
			this.infinite = false;
			this.disabled = false;
			
			load();
		} catch(Exception x) {
			delete();
		}
	}
	
	private void load() {
		if (sign == null || chest == null) return;
		shops.add(this);
		signs.put(sign.getBlock(), this);
		chests.put(chest.getBlock(), this);
		
		List<PlayerShop> list = chunks.containsKey(chest.getWorld().getUID().toString() + "_" + chest.getChunk().getX() + "_" + chest.getChunk().getZ()) ? chunks.get(chest.getWorld().getUID().toString() + "_" + chest.getChunk().getX() + "_" + chest.getChunk().getZ()): new ArrayList<>();
		list.add(this);
		chunks.put(chest.getWorld().getUID().toString() + "_" + chest.getChunk().getX() + "_" + chest.getChunk().getZ(), list);
	}

	public PlayerShop(Config cfg) {
		try {
			chest = (Chest) cfg.getLocation("chest").getWorld().getBlockAt(cfg.getLocation("chest")).getState();
			sign = (Sign) cfg.getLocation("sign").getWorld().getBlockAt(cfg.getLocation("sign")).getState();
			type = ShopType.valueOf(cfg.getString("type"));
			
			if (cfg.contains("owner")) {
				owner = UUID.fromString(cfg.getString("owner"));
				player = cfg.getString("name");
			}
			
			price = cfg.getDouble("price");
			item = cfg.getItem("item");
			amount = cfg.getInt("amount");
			used = cfg.contains("used") ? cfg.getLong("used"): 0;
			infinite = cfg.getBoolean("infinite");
			disabled = cfg.getBoolean("disabled");
			for (int i = 1; i < 25; i++) {
				schedule[(i - 1)] = cfg.getBoolean("schedule." + (i - 1));
			}
		} catch(Exception x) {
			System.err.println("[QuickMarket] Found remainings of deleted Shop @ " + cfg.getFile().getName());
			cfg.getFile().delete();
		}
		
		load();
	}
	
	public void update(boolean refreshItem) {
		if (!loaded) return;
		
		if (!(chest.getBlock().getState() instanceof Chest) || !(sign.getBlock().getState() instanceof Sign)) {
			System.err.println("[QuickMarket] A Shop's Chest/Sign is no longer existing. Emergency Deletion initiated");
			delete();
			return;
		}
		
		sign = (Sign) sign.getBlock().getState();
		chest = (Chest) chest.getBlock().getState();
		
		if (isMarket()) {
			sign.setLine(0, ChatColor.translateAlternateColorCodes('&', infinite ? QuickMarket.getInstance().cfg.getString("markets.prefix-infinite"): QuickMarket.getInstance().cfg.getString("markets.prefix")));
			sign.setLine(3, "");
		}
		else {
			player = Bukkit.getOfflinePlayer(owner).getName();
			sign.setLine(0, ChatColor.translateAlternateColorCodes('&', infinite ? QuickMarket.getInstance().cfg.getString("shops.prefix-infinite"): QuickMarket.getInstance().cfg.getString("shops.prefix")));
			sign.setLine(3, infinite ? "": player);
		}
		
		sign.setLine(1, type.getSignMessage() + (type == ShopType.SELL_ALL ? "All": String.valueOf(amount)));
		sign.setLine(2, ChatColor.translateAlternateColorCodes('&', "&2" + QuickMarket.getInstance().cfg.getString("options.money-symbol") + (type == ShopType.SELL_ALL ? (DoubleHandler.getFancyDouble(price) + "/ea"): DoubleHandler.getFancyDouble(price))));
		sign.update();
		
		if (refreshItem) respawnItem();
		
		this.update();
	}
	
	protected void respawnItem() {
		if (display != null) {
			display.removeMetadata("no_pickup", QuickMarket.getInstance());
			display.removeMetadata("quickmarket_item", QuickMarket.getInstance());
			display.remove();
		}
		display = chest.getWorld().dropItem(chest.getLocation().add(0.5, 1.2, 0.5), new CustomItem(new CustomItem(item, "&6&lQuickMarket Display Item &e" + CSCoreLib.randomizer().nextInt(10000)), 1));
		display.setVelocity(new Vector(0, 0.1, 0));
		if (QuickMarket.getInstance().cfg.getBoolean("options.item-nametags")) {
			display.setCustomName(StringUtils.formatItemName(item, false));
			display.setCustomNameVisible(true);
		}
		display.setMetadata("no_pickup", new FixedMetadataValue(QuickMarket.getInstance(), true));
		display.setMetadata("quickmarket_item", new FixedMetadataValue(QuickMarket.getInstance(), true));
	}

	public void handleTransaction(Player p, int amount2) {
		if (this.owner == null) {
			QuickMarket.getInstance().local.sendTranslation(p, "shops.disabled", true);
			update(false);
			return;
		}
		if (!(chest.getBlock().getState() instanceof Chest) || !(sign.getBlock().getState() instanceof Sign)) {
			System.err.println("[QuickMarket] A Shop's Chest/Sign is no longer existing. Emergency Deletion initiated");
			delete();
			return;
		}
		if (!isOpen()) {
			QuickMarket.getInstance().local.sendTranslation(p, "shops.disabled", true);
			return;
		}
		if (editing) {
			QuickMarket.getInstance().local.sendTranslation(p, "shops.editing", true);
			return;
		}
		
		double money = 0.0;
		int amount = 0;
		
		switch (type) {
		case BUY: {
			if (amount2 == 0 && isBuyMenuEnabled()) openBuyMenu(p, this.amount);
			else {
				amount = amount2 == 0 ? this.amount: amount2;
				money = DoubleHandler.fixDouble((this.price / this.getAmount()) * amount);
				
				// Check if the Player has enough Money
				if (QuickMarket.getInstance().economy.getBalance(p) >= money) {
					int quantity = 0;
					
					// Get how many items are available in this Shop
					if (infinite) quantity = amount;
					else {
						for (ItemStack item: chest.getInventory().getContents()) {
							if (canStack(item, this.item)) quantity = quantity + item.getAmount();
							else if (QuickMarket.getInstance().isPrisonUtilsInstalled() && Backpacks.isBackPack(item)) {
								for (ItemStack stack: chest.getInventory().getContents()) {
									if (canStack(stack, this.item)) quantity = quantity + stack.getAmount();
								}
							}
						}
					}

					// Check whether the Shop is not out of stock
					if (quantity >= amount) {
						if (!infinite) {
							QuickMarket.getInstance().economy.depositPlayer(Bukkit.getOfflinePlayer(owner), money);
							
							int rest = amount;
							inventory:
							for (int i = 0; i < chest.getInventory().getSize(); i++) {
								ItemStack item = chest.getInventory().getItem(i);
								if (canStack(item, this.item)) {
									int amt = item.getAmount();
									if (amt > rest) {
										amt = amt - rest;
										rest = 0;
									}
									else if (amt == rest) {
										rest = 0;
										amt = 0;
									}
									else rest = rest - amt;
									chest.getInventory().setItem(i, amt == 0 ? null: new CustomItem(item, amt));
									
									if (rest == 0) break inventory;
								}
								else if (QuickMarket.getInstance().isPrisonUtilsInstalled() && Backpacks.isBackPack(item)) {
									Inventory inv = Backpacks.getInventory(item);
									for (int j = 0; j < inv.getSize(); j++) {
										ItemStack stack = inv.getItem(j);
										
										if (canStack(stack, this.item)) {
											int amt = stack.getAmount();
											if (amt > rest) {
												amt = amt - rest;
												rest = 0;
											}
											else if (amt == rest) {
												rest = 0;
												amt = 0;
											}
											else rest = rest - amt;
											inv.setItem(j, amt == 0 ? null: new CustomItem(stack, amt));
										}
										
										if (rest == 0) {
											Backpacks.saveBackpack(inv, item);
											break inventory;
										}
									}
									Backpacks.saveBackpack(inv, item);
								}
							}
						}
						
						QuickMarket.getInstance().economy.withdrawPlayer(p, money);
						
						QuickMarket.getInstance().local.sendTranslation(p, "shops.bought", false, new Variable("{MONEY}", "$" + DoubleHandler.getFancyDouble(money)), new Variable("{AMOUNT}", String.valueOf(amount)));
						refreshTmp(p.getName(), money, amount, ShopType.BUY);
						
						int a = amount % 64 == 0 ? amount / 64: amount / 64 + 1;
						for (int i = 0; i < a; i++) {
							int amt = amount % 64;
							ItemStack is = new CustomItem(this.item, amt == 0 ? 64: amt);
							if (InvUtils.fits(p.getInventory(), is)) p.getInventory().addItem(is);
							else p.getWorld().dropItemNaturally(p.getLocation(), is);
							this.used = used + amount;
						}
						PlayerInventory.update(p);
					}
					else QuickMarket.getInstance().local.sendTranslation(p, "shops.not-enough-items-owner", true);
				}
				else QuickMarket.getInstance().local.sendTranslation(p, "shops.insufficient-funds", true);
			}
			break;
		}
		case SELL: 
		case SELL_ALL:
		{
			boolean full = false;
			if (type.equals(ShopType.SELL)) {
				int n = 0;
				inventory:
				for (ItemStack item: p.getInventory().getContents()) {
					if (canStack(item, this.item)) {
						n = n + item.getAmount();
						if (n >= getAmount()) break inventory;
					}
					else if (QuickMarket.getInstance().isPrisonUtilsInstalled() && Backpacks.isBackPack(item)) {
						for (ItemStack stack: Backpacks.getInventory(item)) {
							if (canStack(stack, this.item)) {
								n = n + stack.getAmount();
								if (n >= getAmount()) break inventory;
							}
						}
					}
				}
				if (n < getAmount()) {
					QuickMarket.getInstance().local.sendTranslation(p, "shops.not-enough-items", true);
					return;
				}
			}
			
			inventory: 
			for (int i = 0; i < p.getInventory().getContents().length; i++) {
				ItemStack item = p.getInventory().getContents()[i];
				
				// Check if the current Item matches the Shop's Item
				if (canStack(item, this.item)) {
					// Get how many Items the Shop Owner can afford
					int quantity = infinite ? item.getAmount() : (int) ((QuickMarket.getInstance().economy.getBalance(Bukkit.getOfflinePlayer(owner)) / this.price));
					if (quantity > item.getAmount()) quantity = item.getAmount();
					
					// Check if the Amount of Items a Player can sell exceeds the Sell Limit
					if (type.equals(ShopType.SELL) && amount + quantity > this.amount) quantity = this.amount - amount;
					
					// Check if the Player can actually sell anything at all
					if (quantity > 0) {
						// Get all Items which did not fit in the Chest
						Map<Integer, ItemStack> rest = infinite ? new HashMap<Integer, ItemStack>(): chest.getInventory().addItem(new CustomItem(item, quantity));
						if (rest.isEmpty()) {
							if (item.getAmount() - quantity > 0) p.getInventory().setItem(i, new CustomItem(item, item.getAmount() - quantity));
							else p.getInventory().setItem(i, null);
						}
						else {
							// Get the spare Items which did not fit into the Inventory
							ItemStack spare = null;
							for (Entry<Integer, ItemStack> entry: rest.entrySet()) {
								spare = entry.getValue();
							}
							
							if (QuickMarket.getInstance().isPrisonUtilsInstalled()) {
								chest_backpacks:
								for (ItemStack is: chest.getInventory().getContents()) {
									if (Backpacks.isBackPack(is)) {
										Inventory inv = Backpacks.getInventory(is);
										rest = inv.addItem(spare);
										Backpacks.saveBackpack(inv, is);
										if (rest.isEmpty()) break chest_backpacks;
										else {
											for (Entry<Integer, ItemStack> entry: rest.entrySet()) {
												spare = entry.getValue();
											}
										}
									}
								}
							}
							
							// Check if the Items could not fit into any Backpacks either
							if (!rest.isEmpty()) {
								spare = rest.get(rest.keySet().toArray(new Integer[rest.size()]));
								for (Entry<Integer, ItemStack> entry: rest.entrySet()) {
									spare = entry.getValue();
								}
								
								// Return the Items to the Player and assign the Quantity Variable to the amount of successfully sold Items
								quantity = quantity - spare.getAmount();
								p.getInventory().setItem(i, spare);
								
								// Inventory is full
								QuickMarket.getInstance().local.sendTranslation(p, "shops.full-inventory", true);
								full = true;
								// Updating the total Variables
								amount = amount + quantity;
								money = money + this.price * quantity;
								break inventory;
							}
							else p.getInventory().setItem(i, null);
						}
						
						// Updating the total Variables
						amount = amount + quantity;
						money = money + this.price * quantity;
						
						// Uhoh Sell Limit exceeded
						if (type.equals(ShopType.SELL) && amount >= this.amount) break inventory;
					}
					else {
						// Well seems like the Shop Owner is broke.
						QuickMarket.getInstance().local.sendTranslation(p, "shops.insufficient-funds-owner", true);
						break inventory;
					}
				}
				else if (QuickMarket.getInstance().isPrisonUtilsInstalled() && Backpacks.isBackPack(item)) {
					Inventory backpack = Backpacks.getInventory(item);
					for (int j = 0; j < backpack.getContents().length; j++) {
						ItemStack stack = backpack.getContents()[j];
						
						// Check if the current Item matches the Shop's Item
						if (canStack(stack, this.item)) {
							// Get how many Items the Shop Owner can afford
							int quantity = infinite ? stack.getAmount() : (int) (QuickMarket.getInstance().economy.getBalance(Bukkit.getOfflinePlayer(owner)) / this.price);
							if (quantity > stack.getAmount()) quantity = stack.getAmount();
							
							// Check if the Amount of Items a Player can sell exceeds the Sell Limit
							if (type.equals(ShopType.SELL) && amount + quantity > this.amount) quantity = this.amount - amount;
							
							// Check if the Player can actually sell anything at all
							if (quantity > 0) {
								// Get all Items which did not fit in the Chest
								Map<Integer, ItemStack> rest = infinite ? new HashMap<Integer, ItemStack>(): chest.getInventory().addItem(new CustomItem(stack, quantity));
								if (rest.isEmpty()) {
									if (stack.getAmount() - quantity > 0) backpack.setItem(j, new CustomItem(stack, stack.getAmount() - quantity));
									else backpack.setItem(j, null);
								}
								else {
									// Get the spare Items which did not fit into the Inventory
									// Get the spare Items which did not fit into the Inventory
									ItemStack spare = null;
									for (Entry<Integer, ItemStack> entry: rest.entrySet()) {
										spare = entry.getValue();
									}
									
									if (QuickMarket.getInstance().isPrisonUtilsInstalled()) {
										chest_backpacks:
										for (ItemStack is: chest.getInventory().getContents()) {
											if (Backpacks.isBackPack(is)) {
												Inventory inv = Backpacks.getInventory(is);
												rest = inv.addItem(spare);
												Backpacks.saveBackpack(inv, is);
												if (rest.isEmpty()) break chest_backpacks;
												else {
													for (Entry<Integer, ItemStack> entry: rest.entrySet()) {
														spare = entry.getValue();
													}
												}
											}
										}
									}
									
									// Check if the Items could not fit into any Backpacks either
									if (!rest.isEmpty()) {
										spare = rest.get(rest.keySet().toArray(new Integer[rest.size()]));
										for (Entry<Integer, ItemStack> entry: rest.entrySet()) {
											spare = entry.getValue();
										}
										
										// Return the Items to the Player and assign the Quantity Variable to the amount of successfully sold Items
										quantity = quantity - spare.getAmount();
										backpack.setItem(j, spare);
										
										// Inventory is full
										QuickMarket.getInstance().local.sendTranslation(p, "shops.full-inventory", true);
										full = true;
										// Updating the total Variables
										amount = amount + quantity;
										money = money + this.price * quantity;
										break inventory;
									}
									else backpack.setItem(j, null);
								}
								Backpacks.saveBackpack(backpack, item);
								
								// Updating the total Variables
								amount = amount + quantity;
								money = money + this.price * quantity;
								
								// Uhoh Sell Limit exceeded
								if (type.equals(ShopType.SELL) && amount >= this.amount) break inventory;
							}
							else {
								// Well seems like the Shop Owner is broke.
								QuickMarket.getInstance().local.sendTranslation(p, "shops.insufficient-funds-owner", true);
								break inventory;
							}
						}
					}
					
					Backpacks.saveBackpack(backpack, item);
				}
			}
		
			// Check if the Player sold something
			if (amount > 0) {
				// Send a Message to the Player
				QuickMarket.getInstance().local.sendTranslation(p, "shops.sold", false, new Variable("{MONEY}", "$" + DoubleHandler.getFancyDouble(money)), new Variable("{AMOUNT}", String.valueOf(amount)));
				// Send a Message to the Shop Owner later
				refreshTmp(p.getName(), money, amount, ShopType.SELL);
				// Reward the Player with Money
				QuickMarket.getInstance().economy.depositPlayer(p, money);
				// Withdraw Money from the Shop Owner
				if (!infinite) {
					QuickMarket.getInstance().economy.withdrawPlayer(Bukkit.getOfflinePlayer(owner), money);
				}
				// Update the Player's Inventory
				PlayerInventory.update(p);
				// Update the "Used" Stat
				this.used = used + amount;
			}
			else if (!full) QuickMarket.getInstance().local.sendTranslation(p, "shops.full-inventory", true);
			break;
		}
		default:
			break;
		}
	}

	private boolean isBuyMenuEnabled() {
		return isMarket() ? QuickMarket.getInstance().cfg.getBoolean("markets.buy-menu"): QuickMarket.getInstance().cfg.getBoolean("shops.buy-menu");
	}

	private void openBuyMenu(Player p, final int amount) {
		ChestMenu menu = new ChestMenu("&9How many do you want to buy?");
		
		menu.addItem(3, new CustomItem(Material.REDSTONE, "&7Amount: &b" + amount, "", "&7Left Click: &r+1", "&7Shift + Left Click: &r+16", "&7Right Click: &r-1", "&7Shift + Right Click: &r-16"));
		menu.addMenuClickHandler(3, new MenuClickHandler() {
			
			@Override
			public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
				int i = amount;
				if (action.isRightClicked()) i = i - (action.isShiftClicked() ? 16: 1);
				else i = i + (action.isShiftClicked() ? 16: 1);
				if (i < 1) i = 1;
				openBuyMenu(p, i);
				return false;
			}
		});
		
		menu.addItem(4, new CustomItem(this.item.getType(), "&r" + StringUtils.formatItemName(item, false), "", "&7Left Click: &rBuy &e" + amount + " " + StringUtils.formatItemName(item, false)));
		menu.addMenuClickHandler(4, (player, slot, item, action) -> {
			handleTransaction(p, amount);
			return false;
		});
		
		menu.addItem(5, new CustomItem(Material.REDSTONE, "&7Amount: &b" + amount, "", "&7Left Click: &r+32", "&7Shift + Left Click: &r+64", "&7Right Click: &r-32", "&7Shift + Right Click: &r-64"));
		menu.addMenuClickHandler(5, (player, slot, item, action) -> {
			int i = amount;
			if (action.isRightClicked()) i = i - (action.isShiftClicked() ? 64: 32);
			else i = i + (action.isShiftClicked() ? 64: 32);
			if (i < 1) i = 1;
			openBuyMenu(player, i);
			return false;
		});
		
		menu.open(p);
	}

	private void refreshTmp(String name, double price, int amount, ShopType type) {
		switch (type) {
		case BUY: {
			int items = getSummary().temp_itemsB.containsKey(name) ? getSummary().temp_itemsB.get(name): 0;
			getSummary().temp_itemsB.put(name, items + amount);
			double money = getSummary().temp_moneyB.containsKey(name) ? getSummary().temp_moneyB.get(name): 0;
			getSummary().temp_moneyB.put(name, money + price);
			break;
		}
		case SELL_ALL:
		case SELL: {
			int items = getSummary().temp_itemsS.containsKey(name) ? getSummary().temp_itemsS.get(name): 0;
			getSummary().temp_itemsS.put(name, items + amount);
			double money = getSummary().temp_moneyS.containsKey(name) ? getSummary().temp_moneyS.get(name): 0;
			getSummary().temp_moneyS.put(name, money + price);
			break;
		}
		default:
			break;
		}
	}

	public static enum ShopType {
		
		BUY("&lBuy: &r"),
		SELL("&lSell: &r"),
		SELL_ALL("&lSell: &r");
		
		String sign;
		
		private ShopType(String string) {
			this.sign = string;
		}
		
		public String getSignMessage() {
			return ChatColor.translateAlternateColorCodes('&', sign);
		}
	}

	public void store() {
		if (display != null) {
			display.remove();
		}
		
		if (new File("data-storage/QuickMarket/shops/" + chest.getWorld().getUID().toString() + "_" + chest.getBlock().getX() + "_" + chest.getBlock().getY() + "_" + chest.getBlock().getZ() + ".shop").exists()) {
			new File("data-storage/QuickMarket/shops/" + chest.getWorld().getUID().toString() + "_" + chest.getBlock().getX() + "_" + chest.getBlock().getY() + "_" + chest.getBlock().getZ() + ".shop").delete();
		}
		Config cfg = new Config("data-storage/QuickMarket/shops/" + chest.getWorld().getUID().toString() + ";" + chest.getBlock().getX() + ";" + chest.getBlock().getY() + ";" + chest.getBlock().getZ() + ".shop");
		
		try {
			cfg.setValue("chest", chest.getLocation());
			cfg.setValue("sign", sign.getLocation());
			cfg.setValue("type", type.toString());
			
			if (owner != null) {
				cfg.setValue("owner", owner.toString());
				cfg.setValue("name", player);
			}
			else cfg.setValue("owner", null);
			
			cfg.setValue("price", price);
			cfg.setValue("item", new ItemStack(item));
			cfg.setValue("amount", amount);
			cfg.setValue("used", String.valueOf(used));
			cfg.setValue("infinite", infinite);
			cfg.setValue("disabled", disabled);
			cfg.setValue("market", isMarket());
			
			saveFile(cfg);
		}
		catch(Exception x) {
			System.err.println("[QuickMarket] ERROR: Could not save a Shop");
		}
		
		for (int i = 0; i < 24; i++) {
			cfg.setValue("schedule." + i, schedule[i]);
		}
		
		cfg.save();
	}
	
	public void openEditor(Player p) {
		setEditMode(true);
		p.playSound(p.getLocation(), Soundboard.getLegacySounds("BLOCK_WOOD_BUTTON_CLICK_ON", "WOOD_CLICK"), 1F, 1F);
		ChestMenu menu = new ChestMenu("&eShop Editor");
		
		menu.addMenuCloseHandler((player) -> setEditMode(false));
		
		menu.addItem(0, new CustomItem(this.item.getType(), "&r" + StringUtils.formatItemName(item, false), "", "&7Left Click: &rChange Item to the Item held in your main Hand"));
		menu.addMenuClickHandler(0, (player, slot, item, action) -> {
			if (player.getInventory().getItemInMainHand() != null && p.getInventory().getItemInMainHand().getType() != null && p.getInventory().getItemInMainHand().getType() != Material.AIR) {
				setItem(p.getInventory().getItemInMainHand());
				update(true);
				openEditor(p);
			}
			return false;
		});
		
		menu.addItem(1, new CustomItem(type == ShopType.SELL ? Material.DIAMOND: (type.equals(ShopType.SELL_ALL) ? Material.GOLD_INGOT: Material.EMERALD), "&rType: &b" + StringUtils.format(type.toString()), "", "&7Left Click: &rToggle State"));
		menu.addMenuClickHandler(1, (player, slot, item, action) -> {
			toggleType();
			openEditor(p);
			return false;
		});
		
		if (type != ShopType.SELL_ALL) {
			menu.addItem(2, new CustomItem(Material.GLOWSTONE_DUST, "&7Amount: &b" + amount, "", "&7Left Click: &r+1", "&7Shift + Left Click: &r+16", "&7Right Click: &r-1", "&7Shift + Right Click: &r-16"));
			menu.addMenuClickHandler(2, (player, slot, item, action) -> {
				int amount = getAmount();
				if (action.isRightClicked()) amount = amount - (action.isShiftClicked() ? 16: 1);
				else amount = amount + (action.isShiftClicked() ? 16: 1);
				if (amount < 1) amount = 1;
				setAmount(amount);
				openEditor(p);
				return false;
			});
		}
		else {
			menu.addItem(2, new CustomItem(Material.GLOWSTONE_DUST, "&7Amount: &bAll"));
			menu.addMenuClickHandler(2, (player, slot, item, action) -> false);
		}
		
		menu.addItem(isMarket() ? 7: 5, new CustomItem(Material.CLOCK, "&eSchedule"));
		menu.addMenuClickHandler(isMarket() ? 7: 5, (player, slot, item, action) -> {
			openSchedule(p);
			return false;
		});
		
		try {
			String total = type.equals(ShopType.BUY) ? ("&rTotal Income: &6$" + DoubleHandler.getFancyDouble(price * used)): ("&rTotal Outgoings: &6$" + DoubleHandler.getFancyDouble(price * used));
			menu.addItem(3, new CustomItem(CustomSkull.getItem("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTQ2ZGNkYjAzYzRmZTI1ZDRiYzA2MTdlMmQ5MjZlZDkxY2IzZGE0OWQ3YjFmODlhZTlmMjAyMDE2M2ExZWY5In19fQ=="), "&7Usage", "", "&rThis Shop has been used", "&ra total Amount of &e" + used + " &rtimes", total));
			menu.addMenuClickHandler(3, (player, slot, item, action) -> false);
			
			if (p.hasPermission("QuickMarket.shop.infinite")) {
				menu.addItem(6, new CustomItem(CustomSkull.getItem("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjMzNjQxMWUyMWJhNWJhZTQxZGE0ZDBkYTIzYjcxOTExOGYxYzc5YjYxYzMwYmJmMGE1YWNhZjQ1M2ExYSJ9fX0="), "&rInfinite: " + (infinite ? "&2&l\u2714": "&4&l\u2718"), "&c&lAdmin ONLY", "", "&7Left Click: &rToggle State"));
				menu.addMenuClickHandler(6, (player, slot, item, action) -> {
					toggleInfinity();
					update(false);
					openEditor(p);
					return false;
				});
			}
			
			menu.addItem(isMarket() ? 8: 7, new CustomItem(CustomSkull.getItem(disabled ? "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Y0NmMzMWQ2ZWU2ZWE2MTlmNzJlNzg1MjMyY2IwNDhhYjI3MDQ2MmRiMGNiMTQ1NDUxNDQzNjI1MWMxYSJ9fX0=": "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYxZTViMzMzYzJhMzg2OGJiNmE1OGI2Njc0YTI2MzkzMjM4MTU3MzhlNzdlMDUzOTc3NDE5YWYzZjc3In19fQ=="), "&rEnabled: " + (disabled ? "&4&l\u2718": "&2&l\u2714"), "", "&7Left Click: &rToggle State"));
			menu.addMenuClickHandler(isMarket() ? 8: 7, (player, slot, item, action) -> {
				toggleState();
				openEditor(player);
				return false;
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!isMarket()) {
			menu.addItem(8, new CustomItem(Material.BARRIER, "&4Delete Shop", "", "&rClick to delete your Shop"));
			menu.addMenuClickHandler(8, (player, slot, item, action) -> {
				delete();
				player.closeInventory();
				return false;
			});
		}
		
		menu.addItem(9, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+0.1", "&7Shift + Left Click: &r+1", "&7Right Click: &r-0.1", "&7Shift + Right Click: &r-1"));
		menu.addMenuClickHandler(9, getPriceHandler(0.1, 1));
		
		menu.addItem(10, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+10", "&7Shift + Left Click: &r+100", "&7Right Click: &r-10", "&7Shift + Right Click: &r-100"));
		menu.addMenuClickHandler(10, getPriceHandler(10, 100));
		
		menu.addItem(11, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+1K", "&7Shift + Left Click: &r+10K", "&7Right Click: &r-1K", "&7Shift + Right Click: &r-10K"));
		menu.addMenuClickHandler(11, getPriceHandler(1000, 10000));
		
		menu.addItem(12, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+100K", "&7Shift + Left Click: &r+1M", "&7Right Click: &r-100K", "&7Shift + Right Click: &r-1M"));
		menu.addMenuClickHandler(12, getPriceHandler(100000, 1000000));
		
		menu.addItem(13, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+10M", "&7Shift + Left Click: &r+100M", "&7Right Click: &r-10M", "&7Shift + Right Click: &r-100M"));
		menu.addMenuClickHandler(13, getPriceHandler(10000000, 100000000));
		
		menu.addItem(14, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+1B", "&7Shift + Left Click: &r+10B", "&7Right Click: &r-1B", "&7Shift + Right Click: &r-10B"));
		menu.addMenuClickHandler(14, getPriceHandler(1000000000, 10000000000D));
		
		menu.addItem(15, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+100B", "&7Shift + Left Click: &r+1T", "&7Right Click: &r-100B", "&7Shift + Right Click: &r-1T"));
		menu.addMenuClickHandler(15, getPriceHandler(100000000000D, 1000000000000D));
		
		menu.addItem(16, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+10T", "&7Shift + Left Click: &r+100T", "&7Right Click: &r-10T", "&7Shift + Right Click: &r-100T"));
		menu.addMenuClickHandler(16, getPriceHandler(10000000000000D, 100000000000000D));
		
		menu.addItem(17, new CustomItem(Material.GOLD_INGOT, "&7Price: &6$" + DoubleHandler.getFancyDouble(price), "", "&7Left Click: &r+1Q", "&7Shift + Left Click: &r+10Q", "&7Right Click: &r-1Q", "&7Shift + Right Click: &r-10Q"));
		menu.addMenuClickHandler(17, getPriceHandler(1000000000000000D, 10000000000000000D));
		
		menu.open(p);
	}
	
	private MenuClickHandler getPriceHandler(double smaller, double bigger) {
		return (p, slot, item, action) -> {
			double price = getPrice();
			
			if (action.isRightClicked()) price = price - (action.isShiftClicked() ? bigger: smaller);
			else price = price + (action.isShiftClicked() ? bigger: smaller);
			
			if (price <= 0) price = 0.1;
			if (price > QuickMarket.getInstance().cfg.getDouble("shops.max-price")) {
				price = QuickMarket.getInstance().cfg.getDouble("shops.max-price");
				p.playSound(p.getLocation(), Soundboard.getLegacySounds("BLOCK_NOTE_BASS", "NOTE_BASS"), 1F, 1F);
				QuickMarket.getInstance().local.sendTranslation(p, "shops.reached-max-price", true);
			}
			
			setPrice(price);
			openEditor(p);
			return false;
		};
	}
	
	public void openSchedule(Player p) {
		ChestMenu menu = new ChestMenu("&eSchedule &8- Time: " + getCurrentHour() + ":XX");
		
		menu.addMenuOpeningHandler((player) -> {
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1F);
		});
		
		menu.addItem(1, new CustomItem(Material.OAK_SIGN, "&e< Back to the Editor", "", "&7Left Click: &rGo Back"));
		menu.addMenuClickHandler(1, (player, slot, item, action) -> {
			openEditor(player);
			return false;
		});
		
		for (int i = 1; i <= 24; i++) {
			try {
				menu.addItem(i + 8, new CustomItem(CustomSkull.getItem(schedule[i - 1] ? "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Y0NmMzMWQ2ZWU2ZWE2MTlmNzJlNzg1MjMyY2IwNDhhYjI3MDQ2MmRiMGNiMTQ1NDUxNDQzNjI1MWMxYSJ9fX0=": "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYxZTViMzMzYzJhMzg2OGJiNmE1OGI2Njc0YTI2MzkzMjM4MTU3MzhlNzdlMDUzOTc3NDE5YWYzZjc3In19fQ=="), "&rEnabled at " + i + ":XX " + (schedule[i - 1] ? "&4&l\u2718": "&2&l\u2714"), "", "&7Left Click: &rToggle State"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			menu.addMenuClickHandler(i + 8, (player, slot, item, action) -> {
				schedule[slot - 9] = !schedule[slot - 9];
				openSchedule(p);
				return false;
			});
		}
		
		menu.open(p);
	}

	private void setEditMode(boolean state) {
		this.editing = state;
	}
	
	public boolean isOpen() {
		return !schedule[getCurrentHour()] && !disabled;
	}

	private int getCurrentHour() {
		return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	}

	public void toggleType() {
		switch (type) {
			case BUY:
				this.type = ShopType.SELL;
				break;
			case SELL:
				this.type = ShopType.SELL_ALL;
				break;
			case SELL_ALL:
				this.type = ShopType.BUY;
				break;
			default:
				break;
		}
		
		update(false);
	}

	public void toggleState() {
		this.disabled = !disabled;
	}

	public void toggleInfinity() {
		this.infinite = !infinite;
	}

	public void setPrice(double price) {
		this.price = DoubleHandler.fixDouble(price);
		update(false);
	}

	public double getPrice() {
		return price;
	}

	public void setAmount(int amount) {
		this.amount = amount;
		update(false);
	}

	public int getAmount() {
		return this.amount;
	}
	
	public void delete() {
		this.delete(false);
	}

	public void delete(boolean iterator) {
		if (!iterator) shops.remove(this);
		
		if (sign != null) {
			signs.remove(sign.getBlock());
			sign.getBlock().breakNaturally();
		}
		
		if (chest != null) {
			chests.remove(chest.getBlock());
			
			File file = new File("data-storage/QuickMarket/shops/" + chest.getWorld().getUID().toString() + "_" + chest.getBlock().getX() + "_" + chest.getBlock().getY() + "_" + chest.getBlock().getZ());
			if (file.exists()) file.delete();
			
			List<PlayerShop> list = chunks.get(chest.getWorld().getUID().toString() + "_" + chest.getChunk().getX() + "_" + chest.getChunk().getZ());
			if (list != null) {
				list.remove(this);
				if (list.isEmpty()) chunks.remove(chest.getWorld().getUID().toString() + "_" + chest.getChunk().getX() + "_" + chest.getChunk().getZ());
				else chunks.put(chest.getWorld().getUID().toString() + "_" + chest.getChunk().getX() + "_" + chest.getChunk().getZ(), list);
			}
		}
		
		if (display != null) {
			display.remove();
		}
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}

	public boolean isOwner(Player p) {
		if (owner == null) return false;
		return p.hasPermission("QuickMarket.shop.bypass") || p.getUniqueId().equals(owner);
	}
	
	public boolean isInfinite() {
		return this.infinite;
	}
	
	/**
	 * This method compares two instances of {@link ItemStack} and checks
	 * whether their {@link Material} and {@link ItemMeta} match.
	 * 
	 * @param a	{@link ItemStack} One
	 * @param b {@link ItemStack} Two
	 * @return
	 */
	public static boolean canStack(ItemStack a, ItemStack b) {
		if (a == null || b == null) return false;
		
		if (!a.getType().equals(b.getType())) return false;
		if (a.hasItemMeta() != b.hasItemMeta()) return false;
		
		if (a.hasItemMeta()) {
			ItemMeta aMeta = a.getItemMeta(), bMeta = b.getItemMeta();
			
			if (aMeta instanceof Damageable != bMeta instanceof Damageable) return false;
			if (aMeta instanceof Damageable) {
				if (((Damageable) aMeta).getDamage() != ((Damageable) bMeta).getDamage()) return false;
			}

			if (aMeta instanceof LeatherArmorMeta != bMeta instanceof LeatherArmorMeta) return false;
			if (aMeta instanceof LeatherArmorMeta) {
				if (!((LeatherArmorMeta) aMeta).getColor().equals(((LeatherArmorMeta) bMeta).getColor())) return false;
			}
			
			if (aMeta.hasDisplayName() != bMeta.hasDisplayName()) return false;
			if (aMeta.hasDisplayName()) {
				if (!aMeta.getDisplayName().equals(bMeta.getDisplayName())) return false;
			}

			if (aMeta.hasLore() != bMeta.hasLore()) return false;
			if (aMeta.hasLore()) {
				if (aMeta.getLore().size() != bMeta.getLore().size()) return false;
				
				for (int i = 0; i < aMeta.getLore().size(); i++) {
					if (!aMeta.getLore().get(i).equals(bMeta.getLore().get(i))) return false;
				}
			}
		}
		
		return true;
	}

	public Item getDisplayItem() {
		return display;
	}
	
	public boolean isMarket() {
		return this instanceof PlayerMarket;
	}
	
	public void update() {}
	public void saveFile(Config cfg) {}

	public UUID getOwner() {
		return this.owner;
	}
	
}
