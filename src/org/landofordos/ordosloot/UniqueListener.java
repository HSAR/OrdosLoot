package org.landofordos.ordosloot;

import java.util.List;
import java.util.Random;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class UniqueListener implements Listener {

    private OrdosLoot plugin;
    private List<String> uniques;
    // RNG
    Random rng = new Random();

    public UniqueListener(OrdosLoot plugin) {
        this.plugin = plugin;
        uniques = plugin.getUniqueItemNames();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    // EventPriority.NORMAL by default
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        // #TODO: Make this work.
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
                                if (ute.getEffects().contains(UniqueEffect.DAMAGE_RESISTANCE)) {

                                    // NOT HERE ANY MORE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // #TODO: Make this work.
        Player player = event.getPlayer();
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack is = player.getItemInHand();
        if ((is != null) && (is.hasItemMeta())) {
            // plugin.getLogger().info("TEST");
            ItemMeta im = is.getItemMeta();
            if (im.hasEnchants()) {
                // #XXX: CHECK #0 - Prevent users from placing unique items.
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItem(event.getNewSlot());
        // ADD EFFECTS ON ITEM IN HAND
        if ((itemInHand != null) && (itemInHand.hasItemMeta())) {
            ItemMeta im = itemInHand.getItemMeta();
            if (im.hasDisplayName()) {
                // check whether the item's display name is one we are tracking
                for (String uniqueItemName : uniques) {
                    if (im.getDisplayName().equals(uniqueItemName)) {
                        UniqueTableEntry ute = plugin.getUniqueItemData(im.getDisplayName());
                        // #XXX: CHECK #3 - DAMAGE RESISTANCE EFFECT
                        for (EffectData ed : ute.getEffects()) {
                            if (ed.equals(UniqueEffect.DAMAGE_RESISTANCE)) {
                                player.addPotionEffect(
                                        new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, ed.getLevel(), false), true);

                            }
                        }
                    }
                }
            }
        }
        // REMOVE EFFECTS WHEN ITEM NOT IN HAND
        ItemStack itemLastInHand = player.getInventory().getItem(event.getPreviousSlot());
        if ((itemLastInHand != null) && (itemLastInHand.hasItemMeta())) {
            ItemMeta im = itemLastInHand.getItemMeta();
            if (im.hasDisplayName()) {
                // check whether the item's display name is one we are tracking
                for (String uniqueItemName : uniques) {
                    if (im.getDisplayName().equals(uniqueItemName)) {
                        UniqueTableEntry ute = plugin.getUniqueItemData(im.getDisplayName());
                        // #XXX: CHECK #3 - DAMAGE RESISTANCE EFFECT
                        for (EffectData ed : ute.getEffects()) {
                            if (ed.equals(UniqueEffect.DAMAGE_RESISTANCE)) {
                                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

                            }
                        }
                    }
                }
            }
        }
    }
}
