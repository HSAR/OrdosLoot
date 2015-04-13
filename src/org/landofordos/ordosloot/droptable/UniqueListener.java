package org.landofordos.ordosloot.droptable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.landofordos.ordosloot.EffectData;
import org.landofordos.ordosloot.OrdosLoot;

public class UniqueListener implements Listener {

    private OrdosLoot plugin;
    private UniqueTable uniques;
    // RNG
    Random rng = new Random();

    // effects applied by this plugin that can be removed without consequence
    protected HashMap<String, Set<PotionEffectType>> knownEffects;

    // effect data values
    protected static HashMap<String, Boolean> honourBound;

    public UniqueListener(OrdosLoot plugin) {
        this.plugin = plugin;
        uniques = plugin.getUniqueTable();
        //
        knownEffects = new HashMap<String, Set<PotionEffectType>>();
        //
        honourBound = new HashMap<String, Boolean>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    // EventPriority.NORMAL by default
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            // check for player as damage source
            Entity damagerEntity = event.getDamager();
            List<PotionEffect> potionEffectQueue = new ArrayList<PotionEffect>();
            Player player = null;
            if (damagerEntity instanceof Player) {
                player = (Player) damagerEntity;
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
                                        potionEffectQueue.add(new PotionEffect(PotionEffectType.REGENERATION, ticksToApply, 0, false));
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
                player = (Player) event.getEntity();
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
            // add potion effects at the end of execution in case event is set to cancelled by one of the effects
            if (!event.isCancelled()) {
                if (player != null) {
                    for (PotionEffect pEffect : potionEffectQueue) {
                        player.addPotionEffect(pEffect, true);
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
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItem(event.getNewSlot());
            List<PotionEffect> potionEffectQueue = new ArrayList<PotionEffect>();
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
                                potionEffectQueue.add(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE,
                                        ed.getLevel() - 1, false));
                                break;
                            case HEALTH_BOOST:
                                potionEffectQueue.add(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false));
                                break;
                            case BLINDNESS:
                                potionEffectQueue.add(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false));
                                break;
                            case HUNGER:
                                potionEffectQueue
                                        .add(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, ed.getLevel() - 1, false));
                                break;
                            case WEAKNESS:
                                potionEffectQueue.add(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false));
                                break;
                            case JUMP_HEIGHT:
                                potionEffectQueue.add(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, ed.getLevel() - 1, false));
                                break;
                            case SPEED:
                                // speed = 1 means amplifier = 0
                                // speed = -1 means amplifier = -1
                                if (ed.getLevel() > 0) {
                                    potionEffectQueue.add(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, ed.getLevel() - 1,
                                            false));
                                } else {
                                    potionEffectQueue
                                            .add(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, ed.getLevel(), false));
                                }
                                break;
                            case NIGHT_VISION:
                                potionEffectQueue.add(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, ed.getLevel() - 1,
                                        false));
                                break;
                            case FIRE_RESISTANCE:
                                potionEffectQueue.add(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE,
                                        ed.getLevel() - 1, false));
                                break;
                            case POISON:
                                potionEffectQueue
                                        .add(new PotionEffect(PotionEffectType.POISON, Integer.MAX_VALUE, ed.getLevel() - 1, false));
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
            // add potion effects at the end of execution in case event is set to cancelled by one of the effects
            if (!event.isCancelled()) {
                Set<PotionEffectType> knownEffectList = knownEffects.get(player.getName());
                // remove all previous known effects
                if (knownEffectList != null) {
                    for (PotionEffect pe : player.getActivePotionEffects()) {
                        if (pe.getDuration() > 100000) {
                            for (PotionEffectType pet : knownEffectList) {
                                if (pe.getType().equals(pet)) {
                                    player.removePotionEffect(pet);
                                }
                            }
                            knownEffectList = knownEffects.get(player.getName());
                        }
                    }
                } else {
                    knownEffectList = new HashSet<PotionEffectType>(potionEffectQueue.size());
                }
                for (PotionEffect pEffect : potionEffectQueue) {
                    // add new ones
                    player.addPotionEffect(pEffect, true);
                    // register with plugin
                    knownEffectList.add(pEffect.getType());
                }
                // finalise registration
                knownEffects.put(player.getName(), knownEffectList);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    // EventPriority.NORMAL by default
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (!event.isCancelled()) {
        // REMOVE EFFECTS WHEN ITEM DROPPED
            Player player = event.getPlayer();
            ItemStack itemLastInHand = event.getItemDrop().getItemStack();
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
