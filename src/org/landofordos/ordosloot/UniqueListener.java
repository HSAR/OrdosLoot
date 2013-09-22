package org.landofordos.ordosloot;

import java.util.List;
import java.util.Random;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class UniqueListener implements Listener {

	private OrdosLoot plugin;
	private List<String> uniques;
	// RNG
	Random rng = new Random();

	public UniqueListener(OrdosLoot plugin) {
		this.plugin = plugin;
		uniques = plugin.getUniqueItemNames();
	}

	@EventHandler(priority = EventPriority.NORMAL)
	// EventPriority.NORMAL by default
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
		// check for player as damage source
		Entity damagerEntity = event.getDamager();
		if (damagerEntity instanceof Player) {
			Player player = (Player) damagerEntity;
			PlayerInventory inventory = player.getInventory();
			// #TODO: Check if I can just look into Player.getHeldItem()
			for (ItemStack is : inventory) {
				if ((is != null) && (is.hasItemMeta())) {
					ItemMeta im = is.getItemMeta();
					if (im.hasDisplayName()) {
						// check whether the item's display name is one we are tracking
						for (String uniqueItemName : uniques) {
							if (im.getDisplayName().equals(uniqueItemName)) {
								UniqueTableEntry ute = plugin.getUniqueItemData(im.getDisplayName());
								// #XXX: CHECK #1 - INFINITE DURABILITY
								if (ute.getEffects().contains(UniqueEffect.INFINITE_DURABILITY)) {
									is.setDurability((short) 1000);
								}
							}
						}
					}
				}
			}
		}
		if (event.getEntityType().equals(EntityType.PLAYER)) {
			Player player = (Player) event.getEntity();
			PlayerInventory inventory = player.getInventory();
			// check in the player's inventory to see if there's a unique item we're tracking
			for (ItemStack is : inventory) {
				if ((is != null) && (is.hasItemMeta())) {
					ItemMeta im = is.getItemMeta();
					if (im.hasDisplayName()) {
						// check whether the item's display name is one we are tracking
						for (String uniqueItemName : uniques) {
							if (im.getDisplayName().equals(uniqueItemName)) {
								UniqueTableEntry ute = plugin.getUniqueItemData(im.getDisplayName());
								// #XXX: CHECK #2 - BLOCK CHANCE 5% and 10%
								if (ute.getEffects().contains(UniqueEffect.DAMAGE_ABSORB_5)) {
									// 5% chance to block
									if (rng.nextInt(100) < 5) {
										event.setCancelled(true);
									}
								}
								if (ute.getEffects().contains(UniqueEffect.DAMAGE_ABSORB_10)) {
									// 10% chance to block
									if (rng.nextInt(100) < 10) {
										event.setCancelled(true);
									}
								}
							}
						}
					}
				}
			}

		}
	}
}