package io.github.thebusybiscuit.quickmarket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.github.thebusybiscuit.quickmarket.shop.MarketStand;
import io.github.thebusybiscuit.quickmarket.shop.PlayerMarket;
import io.github.thebusybiscuit.quickmarket.shop.PlayerShop;
import io.github.thebusybiscuit.quickmarket.shop.ShopProtectionLevel;
import io.github.thebusybiscuit.quickmarket.shop.ShopType;

public class MarketListener implements Listener {
	
	public MarketListener(QuickMarket plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	private final BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	private final Map<UUID, Consumer<Block>> link = new HashMap<UUID, Consumer<Block>>();
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent e) {
		// This Event should only be called once!
		if (e.getHand().equals(EquipmentSlot.OFF_HAND)) return;
		
		if (link.containsKey(e.getPlayer().getUniqueId()) && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			link.get(e.getPlayer().getUniqueId()).accept(e.getClickedBlock());
			link.remove(e.getPlayer().getUniqueId());
		}
		else if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (e.getClickedBlock().getBlockData() instanceof WallSign) {
				PlayerShop shop = PlayerShop.signs.get(e.getClickedBlock());
				
				if (shop != null) {
					if (shop.isOwner(e.getPlayer())) shop.openEditor(e.getPlayer());
					else shop.handleTransaction(e.getPlayer(), 0);
				}
				else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
					MarketStand market = MarketStand.map.get(MarketStand.location(e.getClickedBlock().getLocation()));
					if (market != null && market.isOwner(e.getPlayer())) {
						try {
							market.openGUI(e.getPlayer());
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			else if (e.getClickedBlock().getState() instanceof Container) {
				ShopProtectionLevel level = isChestProtected(e.getPlayer(), e.getClickedBlock());
				if (level.equals(ShopProtectionLevel.NO_ACCESS)) {
					e.setCancelled(true);
					QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.no-access", true);
				}
			}
		}
	}
	
	private ShopProtectionLevel isChestProtected(Player p, Block b) {
		PlayerShop shop = PlayerShop.chests.get(b);
		
		if (shop != null) {
			return (p == null ||!shop.isOwner(p) || shop.getOwner() == null) ? ShopProtectionLevel.NO_ACCESS: ShopProtectionLevel.ACCESS;
		}
		else {
			for (BlockFace face: faces) {
				Block block = b.getRelative(face);
				PlayerShop adjacentShop = PlayerShop.chests.get(block);
				if (block.getType().equals(b.getType()) && adjacentShop != null) {
					return (p == null ||!adjacentShop.isOwner(p) || adjacentShop.getOwner() == null) ? ShopProtectionLevel.NO_ACCESS: ShopProtectionLevel.ACCESS;
				}
			}
		}
		return ShopProtectionLevel.NO_SHOP;
	}
	
	@EventHandler
	public void onPickup(EntityPickupItemEvent e) {
		if (e.getItem().hasMetadata("quickmarket_item")) {
			e.setCancelled(true);
		}
		else if (e.getItem().getItemStack().hasItemMeta() && e.getItem().getItemStack().getItemMeta().hasDisplayName()) {
			if (e.getItem().getItemStack().getItemMeta().getDisplayName().startsWith(ChatColor.translateAlternateColorCodes('&', "&6&lQuickMarket Display Item &e"))) {
				e.setCancelled(true);
				e.getItem().remove();
			}
		}
	}
	
	@EventHandler
	public void onPickup(InventoryPickupItemEvent e) {
		if (e.getItem().hasMetadata("quickmarket_item")) {
			e.setCancelled(true);
		}
		else if (e.getItem().getItemStack().hasItemMeta() && e.getItem().getItemStack().getItemMeta().hasDisplayName()) {
			if (e.getItem().getItemStack().getItemMeta().getDisplayName().startsWith(ChatColor.translateAlternateColorCodes('&', "&6&lQuickMarket Display Item &e"))) {
				e.setCancelled(true);
				e.getItem().remove();
			}
		}
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		String chunk = e.getChunk().getWorld().getUID().toString() + "_" + e.getChunk().getX() + "_" + e.getChunk().getZ();
		
		if (PlayerShop.chunks.containsKey(chunk)) {
			List<PlayerShop> shops = PlayerShop.chunks.get(chunk);
			
			if (QuickMarket.getInstance().cfg.getBoolean("options.chunk-notifications")) {
				System.out.println("[QuickMarket] Chunk X:" + e.getChunk().getX() + " Z:" + e.getChunk().getZ() + " has been unloaded, this lead to " + shops.size() + " Shop(s) being temporarily unloaded.");
			}
			
			for (PlayerShop shop: shops) {
				if (shop.getDisplayItem() != null) shop.getDisplayItem().remove();
				shop.setLoaded(false);
			}
		}
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		String chunk = e.getChunk().getWorld().getUID().toString() + "_" + e.getChunk().getX() + "_" + e.getChunk().getZ();
		
		if (PlayerShop.chunks.containsKey(chunk)) {
			List<PlayerShop> shops = PlayerShop.chunks.get(chunk);
			
			if (QuickMarket.getInstance().cfg.getBoolean("options.chunk-notifications")) {
				System.out.println("[QuickMarket] Chunk X:" + e.getChunk().getX() + " Z:" + e.getChunk().getZ() + " has been loaded, this lead to " + shops.size() + " Shop(s) being loaded.");
			}
			
			for (PlayerShop shop: shops) {
				shop.setLoaded(true);
				shop.update(true);
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		PlayerShop shop = PlayerShop.chests.get(e.getBlock());
		
		if (shop != null) {
			e.setCancelled(true);
			if (shop.isOwner(e.getPlayer())) {
				if (shop.isMarket()) {
					QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.delete-via-sign", true);
				}
				else {
					QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.delete-via-sign", true);
				}
			}
			else {
				QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.no-access", true);
			}
		}
		else {
			shop = PlayerShop.signs.get(e.getBlock());
			if (shop != null) {
				e.setCancelled(true);
				if (shop.isOwner(e.getPlayer())) {
					if (shop.isMarket()) {
						QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.delete-via-sign", true);
					}
					else {
						QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.delete-via-sign", true);
					}
				}
				else {
					QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.no-access", true);
				}
			}
			else {
				MarketStand market = MarketStand.map.get(MarketStand.location(e.getBlock().getLocation()));
				if (market != null && e.getPlayer().hasPermission("QuickMarket.market.admin")) {
					market.delete();
				}
			}
		}
	}
	
	@EventHandler
	public void onSignEdit(SignChangeEvent e) {
		if (!(e.getBlock().getState() instanceof Sign)) return;
		
		Sign sign = (Sign) e.getBlock().getState();
		if (sign.getBlockData() instanceof WallSign) {
			if (e.getLine(0).equalsIgnoreCase(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', QuickMarket.getInstance().cfg.getString("shops.prefix"))))) {
				if (e.getPlayer().hasPermission("QuickMarket.shop.create")) {
					Block chest = e.getBlock().getRelative(((WallSign) sign.getBlockData()).getFacing().getOppositeFace());
					
					if (!(chest.getState() instanceof Container)) {
						QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.not-a-chest", true);
						e.setCancelled(true);
						return;
					}
					else {
						if (!e.getLine(1).matches("[0-9]+")) {
							QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.not-a-valid-amount", true);
							e.setCancelled(true);
							return;
						}
						
						if (!e.getLine(2).matches("[0-9\\.]+")) {
							QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.not-a-valid-price", true);
							e.setCancelled(true);
							return;
						}
						
						double price = Double.valueOf(e.getLine(2));
						ShopType type = null;
						
						if (e.getLine(3).equalsIgnoreCase("sell")) type = ShopType.SELL;
						else if (e.getLine(3).equalsIgnoreCase("buy")) type = ShopType.BUY;
						else if (e.getLine(3).equalsIgnoreCase("sellall")) type = ShopType.SELL_ALL;
						else {
							QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.not-a-valid-type", true);
							e.setCancelled(true);
							return;
						}

						int amount = Integer.parseInt(e.getLine(1));
						if (amount > 0) {
							PlayerShop shop = new PlayerShop(e.getBlock(), chest, e.getPlayer(), amount, price, type);
							e.setCancelled(true);
							shop.update(true);
						}
						else {
							QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.not-a-valid-amount", true);
							e.setCancelled(true);
							return;
						}
					}
				}
				else {
					QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.no-permission", true);
					e.setCancelled(true);
				}
			}
			else if (e.getLine(0).equalsIgnoreCase(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', QuickMarket.getInstance().cfg.getString("markets.prefix"))))) {
				if (e.getPlayer().hasPermission("QuickMarket.market.admin")) {
					Block chest = e.getBlock().getRelative(((WallSign) sign.getBlockData()).getFacing().getOppositeFace());

					if (!(chest.getState() instanceof Container)) {
						QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "shops.not-a-chest", true);
						e.setCancelled(true);
						return;
					}
					else createMarket(e, chest, 1, 100.0, ShopType.BUY);
				}
				else {
					QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.no-permission", true);
					e.setCancelled(true);
				}
			}
			else if (e.getLine(0).equalsIgnoreCase("[MarketStand]")) {
				if (e.getPlayer().hasPermission("QuickMarket.market.admin")) {
					if (!e.getLine(1).matches("[0-9\\.]+")) {
						QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.not-a-valid-price", true);
						e.setCancelled(true);
						return;
					}
					
					double price = Double.valueOf(e.getLine(1));
					e.setCancelled(true);
					new MarketStand(e.getBlock(), price);
				}
				else {
					QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.no-permission", true);
					e.setCancelled(true);
				}
			}
		}
	}

	private void createMarket(final SignChangeEvent e, final Block chest, final int amount, final double price, final ShopType type) {
		e.setCancelled(true);
		QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.link", true);
		
		link.put(e.getPlayer().getUniqueId(), (block) -> {
			if (!MarketStand.map.containsKey(MarketStand.location(block.getLocation()))) {
				QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.link-abort", true);
				return;
			}
			QuickMarket.getInstance().local.sendMessage(e.getPlayer(), "market.link-success", true);
			PlayerMarket shop = new PlayerMarket(MarketStand.location(block.getLocation()), e.getBlock(), chest, e.getPlayer(), amount, price, type);
			shop.update(true);
		});
	}

}
