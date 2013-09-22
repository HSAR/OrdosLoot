package org.landofordos.ordosloot;

import java.util.List;

import org.bukkit.Material;

public class DropTableEntry {

	private DropType dropType;
	private Material itemType;
	private int weight;
	List<Quality> validQuals;

	public DropTableEntry(Material itemType, int weight, List<Quality> validQualities) {
		if ((itemType.toString().contains("SWORD")) || (itemType.toString().contains("AXE"))) {
			this.dropType = DropType.weapon;
		}
		if ((itemType.toString().contains("HELMET")) || (itemType.toString().contains("CHESTPLATE"))
				|| (itemType.toString().contains("LEGGINGS")) || (itemType.toString().contains("BOOTS"))) {
			this.dropType = DropType.armour;
		}
		this.itemType = itemType;
		this.weight = weight;
		this.validQuals = validQualities;
	}

	public DropType getType() {
		return dropType;
	}

	public Material getMaterial() {
		return itemType;
	}

	public List<Quality> getValidQualities() {
		return validQuals;
	}

	public int getWeight() {
		return weight;
	}
}
