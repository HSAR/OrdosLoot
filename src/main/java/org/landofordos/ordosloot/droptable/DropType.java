package org.landofordos.ordosloot.droptable;

import org.bukkit.Material;

public enum DropType {
	
	weapon, armour;

	public static DropType getType(Material itemType) {
		if ((itemType.toString().contains("SWORD")) || (itemType.toString().contains("AXE")) || (itemType.toString().contains("BOW"))) {
			return DropType.weapon;
		}
		if ((itemType.toString().contains("HELMET")) || (itemType.toString().contains("CHESTPLATE"))
				|| (itemType.toString().contains("LEGGINGS")) || (itemType.toString().contains("BOOTS"))) {
			return DropType.armour;
		}
		return null;
	}
}
