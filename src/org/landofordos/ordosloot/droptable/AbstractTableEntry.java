package org.landofordos.ordosloot;

import java.util.List;

import org.bukkit.Material;

public abstract class AbstractTableEntry {

    protected String name;
    protected List<EnchantmentData> enchantments;
    protected int weight;
    protected List<Material> itemTypes;

    public AbstractTableEntry(String name, int weight, List<Material> itemTypes, List<EnchantmentData> enchantments) {
        super();
        this.name = name;
        this.enchantments = enchantments;
        this.weight = weight;
        this.itemTypes = itemTypes;
    }

    public String getName() {
    	return name;
    }

    public int getWeight() {
    	return weight;
    }

    public List<EnchantmentData> getEnchantments() {
    	return enchantments;
    }

    public List<Material> getItemTypes() {
        return itemTypes;
    }

}
