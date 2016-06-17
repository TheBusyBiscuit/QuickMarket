package me.mrCookieSlime.QuickMarket;

import java.util.Iterator;

import me.minebuilders.clearlag.events.EntityRemoveEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ClearLaggIntegration implements Listener {
	
	public ClearLaggIntegration(QuickMarket plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void oNEntityRemove(EntityRemoveEvent e) {
		Iterator<Entity> iterator = e.getEntityList().iterator();
		while (iterator.hasNext()) {
			Entity n = iterator.next();
			if (n instanceof Item) {
				if (n.hasMetadata("quickmarket_item")) iterator.remove();
			}
		}
	}
}
