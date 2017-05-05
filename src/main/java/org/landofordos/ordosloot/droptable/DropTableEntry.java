package org.landofordos.ordosloot.droptable;

import java.util.List;

import org.bukkit.Material;

public class DropTableEntry {

	private DropType dropType;
	private Material itemType;
	private int weight;
	List<Quality> validQuals;

	public DropTableEntry(Material itemType, int weight, List<Quality> validQualities) {
		dropType = DropType.getType(itemType);
		if (dropType == null) {
			throw new RuntimeException("Improper type argument given to DropTableEntry");
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
