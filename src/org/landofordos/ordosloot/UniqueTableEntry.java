package org.landofordos.ordosloot;

import java.util.List;

import org.bukkit.Material;

public class UniqueTableEntry {

	private String name;
	private List<String> desc;
	private int weight;
	private List<UniqueEffect> effects;
	private List<EnchantmentData> enchantments;

	public UniqueTableEntry(String name, int weight, List<String> desc, Material itemType, List<UniqueEffect> effects, List<EnchantmentData> enchs) {
		this.name = name;
		this.desc = desc;
		this.weight = weight;
		this.effects = effects;
		this.enchantments = enchs;
	}

	public String getName() {
		return name;
	}
	
	public List<String> getDesc() {
		return desc;
	}

	public int getWeight() {
		return weight;
	}

	public List<UniqueEffect> getEffects() {
		return effects;
	}

	public List<EnchantmentData> getEnchantments() {
		return enchantments;
	}
}
