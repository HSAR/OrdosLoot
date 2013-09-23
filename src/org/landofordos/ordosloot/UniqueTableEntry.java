package org.landofordos.ordosloot;

import java.util.List;

import org.bukkit.Material;

public class UniqueTableEntry {

	private String name;
	private List<String> desc;
	private int weight;
	private List<UniqueEffect> effects;
	private List<EnchantmentData> enchantments;
	private Material itemType;

	public UniqueTableEntry(String name, int weight, List<String> desc, Material itemType, List<UniqueEffect> effects, List<EnchantmentData> enchs) {
		this.name = name;
		this.weight = weight;
		this.desc = desc;
		this.itemType = itemType;
		this.effects = effects;
		this.enchantments = enchs;
	}

	public String getName() {
		return name;
	}

	public int getWeight() {
		return weight;
	}
	
	public List<String> getDesc() {
		return desc;
	}
	
	public Material getItemType() {
		return itemType;
	}

	public List<UniqueEffect> getEffects() {
		return effects;
	}

	public List<EnchantmentData> getEnchantments() {
		return enchantments;
	}
}
