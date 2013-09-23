package org.landofordos.ordosloot;

import java.util.List;

import org.bukkit.Material;

public class NameTableEntry {

	String name;
	List<EnchantmentData> enchantments;
	List<Material> validMats;
	int weight;
	
	public NameTableEntry(String name, int weight, List<Material> validMats, List<EnchantmentData> enchs) {
		this.name = name;
		this.weight = weight;
		this.validMats = validMats;
		this.enchantments = enchs;
	}

	public String getName() {
		return name;
	}

	public int getWeight() {
		return weight;
	}

	public List<Material> getValidMaterials() {
		return validMats;
	}

	public List<EnchantmentData> getEnchantments() {
		return enchantments;
	}

}