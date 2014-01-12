package org.landofordos.ordosloot;

import java.util.HashMap;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class UniqueListener implements Listener {

    private OrdosLoot plugin;
    private UniqueTable uniques;
    // RNG
    Random rng = new Random();

    // effect data values
    HashMap<String, Boolean> honourBound;

    public UniqueListener(OrdosLoot plugin) {
        this.plugin = plugin;
        uniques = plugin.getUniqueTable();
        honourBound = new HashMap<String, Boolean>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    // EventPriority.NORMAL by default
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            // check for player as damage source
            Entity damagerEntity = event.getDamager();
            if (damagerEntity instanceof Player) {
                Player player = (Player) damagerEntity;
                PlayerInventory inventory = player.getInventory();
                for (ItemStack is : inventory) {
                    if ((is != null) && (is.hasItemMeta())) {
                        ItemMeta im = is.getItemMeta();
                        if (im.hasLore()) {
                            // check if the item is a unique
                            UniqueTableEntry ute = uniques.getByItemStack(player.getItemInHand());
                            if (ute != null) {
                                for (EffectData ed : ute.getEffects()) {
                                    switch (ed.getEffect()) {
                                    case INFINITE_DURABILITY:
                                        is.setDurability((short) 1000);
                                        break;
                                    case LIFE_LEECH:
                                        // heal player for level % of the damage done, each health point takes 50 ticks to heal (at amp = 0)
                                        int ticksToApply = (int) (event.getDamage() * (ed.getLevel() / 100)) * 50;
                                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticksToApply, 0, false),
                                                true);
                                        break;
                                    default:
                                        break;
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
                        if (im.hasLore()) {
                            // check if the item is a unique
                            UniqueTableEntry ute = uniques.getByItemStack(player.getItemInHand());
                            if (ute != null) {

                                // NOT HERE ANY MORE
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    // EventPriority.NORMAL by default
    public void onEntityDeath(final EntityDeathEvent event) {
        EntityDamageEvent ede = event.getEntity().getLastDamageCause();
        if (ede instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent) ede;
            Entity damagerEntity = edbee.getDamager();
            if (damagerEntity instanceof Player) {
                Player player = (Player) damagerEntity;
                PlayerInventory inventory = player.getInventory();
                for (ItemStack is : inventory) {
                    if ((is != null) && (is.hasItemMeta())) {
                        ItemMeta im = is.getItemMeta();
                        if (im.hasLore()) {
                            // check if the item is a unique
                            UniqueTableEntry ute = uniques.getByItemStack(player.getItemInHand());
                            if (ute != null) {
                                for (EffectData ed : ute.getEffects()) {
                                    switch (ed.getEffect()) {
                                    case HONOURBOUND:
                                        plugin.getLogger().info("Player released from honourbound effect.");
                                        honourBound.put(player.getName(), false);
                                    default:
                                        break;
                                    }
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
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        for (ItemStack is : inventory) {
            if ((is != null) && (is.hasItemMeta())) {
                ItemMeta im = is.getItemMeta();
                if (im.hasLore()) {
                    // check if the item is a unique
                    UniqueTableEntry ute = uniques.getByItemStack(player.getItemInHand());
                    if (ute != null) {
                        if (ute.getEffects().contains(UniqueEffect.INFINITE_DURABILITY)) {
                            is.setDurability((short) 1000);
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
            UniqueTableEntry ute = uniques.getByItemStack(player.getItemInHand());
            // Prevent users from placing items with enchantments or unique items.
            if ((im.hasEnchants()) || (ute != null)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        // #TODO: Add all effects to queue, check if cancelled before adding them
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItem(event.getNewSlot());
            // ADD EFFECTS ON ITEM IN HAND
            if ((itemInHand != null) && (itemInHand.hasItemMeta())) {
                ItemMeta im = itemInHand.getItemMeta();
                // items must have lore to be a unique
                if (im.hasLore()) {
                    // check if the item is a unique
                    UniqueTableEntry ute = uniques.getByItemStack(itemInHand);
                    if (ute != null) {
                        for (EffectData ed : ute.getEffects()) {
                            switch (ed.getEffect()) {
                            case DAMAGE_RESISTANCE:
                                player.addPotionEffect(
                                        new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, ed.getLevel() - 1, false),
                                        true);
                                break;
                            case HEALTH_BOOST:
                                player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE,
                                        ed.getLevel() - 1, false), true);
                                break;
                            case BLINDNESS:
                                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false), true);
                                break;
                            case HUNGER:
                                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false), true);
                                break;
                            case WEAKNESS:
                                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false), true);
                                break;
                            case JUMP_HEIGHT:
                                player.addPotionEffect(
                                        new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, ed.getLevel() - 1, false), true);
                                break;
                            case SPEED:
                                // speed = 1 means amplifier = 0
                                // speed = -1 means amplifier = -1
                                if (ed.getLevel() > 0) {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, ed.getLevel() - 1,
                                            false), true);
                                } else {
                                    player.addPotionEffect(
                                            new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, ed.getLevel(), false), true);
                                }
                                break;
                            case NIGHT_VISION:
                                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE,
                                        ed.getLevel() - 1, false), true);
                                break;
                            case FIRE_RESISTANCE:
                                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE,
                                        ed.getLevel() - 1, false), true);
                                break;
                            case POISON:
                                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false), true);
                                break;
                            case HONOURBOUND:
                                honourBound.put(player.getName(), true);
                                break;
                            default:
                                break;
                            }
                        }

                    }
                }
            }
            // REMOVE EFFECTS WHEN ITEM NOT IN HAND
            ItemStack itemLastInHand = player.getInventory().getItem(event.getPreviousSlot());
            if ((itemLastInHand != null) && (itemLastInHand.hasItemMeta())) {
                ItemMeta im = itemLastInHand.getItemMeta();
                // items must have lore to be a unique
                if (im.hasLore()) {
                    // check if the item is a unique
                    UniqueTableEntry ute = uniques.getByItemStack(itemLastInHand);
                    if (ute != null) {
                        for (EffectData ed : ute.getEffects()) {
                            switch (ed.getEffect()) {
                            case DAMAGE_RESISTANCE:
                                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                                break;
                            case HEALTH_BOOST:
                                player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
                                break;
                            case BLINDNESS:
                                player.removePotionEffect(PotionEffectType.BLINDNESS);
                                break;
                            case HUNGER:
                                player.removePotionEffect(PotionEffectType.HUNGER);
                                break;
                            case WEAKNESS:
                                player.removePotionEffect(PotionEffectType.WEAKNESS);
                                break;
                            case JUMP_HEIGHT:
                                player.removePotionEffect(PotionEffectType.JUMP);
                                break;
                            case SPEED:
                                player.removePotionEffect(PotionEffectType.SPEED);
                                break;
                            case NIGHT_VISION:
                                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                                break;
                            case FIRE_RESISTANCE:
                                player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                                break;
                            case POISON:
                                player.removePotionEffect(PotionEffectType.POISON);
                                break;
                            case HONOURBOUND:
                                Boolean value = honourBound.get(player.getName());
                                if (value != null) {
                                    if (value.booleanValue()) {
                                        event.setCancelled(true);
                                    }
                                }
                                break;
                            default:
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
