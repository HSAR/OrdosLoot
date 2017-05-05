package org.landofordos.ordosloot.droptable;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.landofordos.ordosloot.EffectData;
import org.landofordos.ordosloot.EnchantmentData;

public class UniqueTable {

    List<UniqueTableEntry> uniqueTable;

    /**
     * Creates a UniqueTable object which is responsible for storing and generating unique-rarity items.
     */
    public UniqueTable() {
        // initialise table objects
        uniqueTable = new ArrayList<>();
    }

    public int size() {
        return uniqueTable.size();
    }

    public void addUnique(String name, int weight, List<String> desc, List<Material> itemTypes, List<EffectData> effects,
            List<EnchantmentData> enchs) {
        for (int i = 0; i < weight; i++) {
            // add into the table $weight number of times.
            uniqueTable.add(new UniqueTableEntry(name, weight, desc, itemTypes, effects, enchs));
        }
    }

    public void addEntry(UniqueTableEntry ute) {
        for (int i = 0; i < ute.getWeight(); i++) {
            // add into the table $weight number of times.
            uniqueTable.add(ute);
        }
    }

    public List<String> getUniqueItemNames() {
        List<String> result = new ArrayList<>();
        for (UniqueTableEntry ute : uniqueTable) {
            if (!result.contains(ute.getName())) {
                result.add(ute.getName());
            }
        }
        return result;
    }

    /**
     * Takes a seed value and draws a NameTableEntry accordingly.
     * 
     * @param val
     *            - the seed value (between 0 and 1).
     * @return A NameTableEntry object with the string name and the enchantments to apply, null if no items in table.
     */
    public UniqueTableEntry getUniqueFromTable(double val) {
        if (uniqueTable.size() > 0) {
            int intVal = (int) (val * uniqueTable.size());
            return uniqueTable.get(intVal);
        } else {
            return null;
        }
    }

    /**
     * Returns the data held for the specified unique item.
     * 
     * @param name
     *            the name of the item
     * @return the UniqueTableEntry object holding data for the unique, or null if not found.
     */
    public UniqueTableEntry getUniqueByName(String name) {
        for (UniqueTableEntry ute : uniqueTable) {
            // full or partial match,
            if ((ute.getName().equalsIgnoreCase(name)) || (ute.getName().toLowerCase().contains(name.toLowerCase()))) {
                return ute;
            }
        }
        return null;
    }

    public UniqueTableEntry getByItemStack(ItemStack i) {
        for (UniqueTableEntry ute : uniqueTable) {
            if (ute.equals(i)) {
                return ute;
            }
        }
        return null;
    }

    public static Material[] getValidQualities() {
        return new Material[]{ Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, Material.STONE_SWORD, Material.WOOD_SWORD,
                Material.DIAMOND_AXE, Material.IRON_AXE, Material.GOLD_AXE, Material.STONE_AXE, Material.WOOD_AXE, Material.DIAMOND_HELMET,
                Material.IRON_HELMET, Material.CHAINMAIL_HELMET, Material.GOLD_HELMET, Material.LEATHER_HELMET,
                Material.DIAMOND_CHESTPLATE, Material.IRON_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.GOLD_CHESTPLATE,
                Material.LEATHER_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.IRON_LEGGINGS, Material.CHAINMAIL_LEGGINGS,
                Material.GOLD_LEGGINGS, Material.LEATHER_LEGGINGS, Material.DIAMOND_BOOTS, Material.IRON_BOOTS, Material.CHAINMAIL_BOOTS,
                Material.GOLD_BOOTS, Material.LEATHER_BOOTS, };
    }
}
