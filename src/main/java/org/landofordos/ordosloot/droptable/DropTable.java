package org.landofordos.ordosloot.droptable;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

public class DropTable {

	List<DropTableEntry> dropTable;

	/**
	 * Creates a DropTable object which is responsible for storing and generating random base items.
	 */
	public DropTable() {
		// initialise table objects
		dropTable = new ArrayList<>();
	}

	public int size() {
		return dropTable.size();
	}

	public void addDrop(Material itemType, int weight, List<Quality> validQualities) {
		for (int i = 0; i < weight; i++) {
			// add into the table $weight number of times.
			dropTable.add(new DropTableEntry(itemType, weight, validQualities));
		}
	}

	public void addEntry(DropTableEntry dte) {
		// add into the table $weight number of times.
		dropTable.add(dte);
	}

	/**
	 * Returns a new table with only the modifiers that apply to the specified quality
	 * 
	 * @param filterQual
	 *            Quality to filter for
	 * @return a new NameTable with only valid items for the quality selected
	 */
	public DropTable filterByQuality(Quality filterQual) {
		DropTable result = new DropTable();
		for (DropTableEntry dte : dropTable) {
			if (dte.getValidQualities().contains(filterQual)) {
				result.addEntry(dte);
			}
		}
		return result;
	}

	/**
	 * Takes a seed value and draws a NameTableEntry accordingly.
	 * 
	 * @param val
	 *            - the seed value (between 0 and 1).
	 * @return A NameTableEntry object with the string name and the enchantments to apply.
	 */
	public DropTableEntry getDropFromTable(double val) {
		if (dropTable.size() > 0) {
			int intVal = (int) ((val) * dropTable.size());
			// System.out.println(dropTable.get(intVal).getMaterial().toString());
			return dropTable.get(intVal);
		} else {
			return null;
		}
	}

	public static Material[] getValidItems() {
		return new Material[]{ Material.BOW, Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, Material.STONE_SWORD,
				Material.WOOD_SWORD, Material.DIAMOND_AXE, Material.IRON_AXE, Material.GOLD_AXE, Material.STONE_AXE, Material.WOOD_AXE,
				Material.DIAMOND_HELMET, Material.IRON_HELMET, Material.CHAINMAIL_HELMET, Material.GOLD_HELMET, Material.LEATHER_HELMET,
				Material.DIAMOND_CHESTPLATE, Material.IRON_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.GOLD_CHESTPLATE,
				Material.LEATHER_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.IRON_LEGGINGS, Material.CHAINMAIL_LEGGINGS,
				Material.GOLD_LEGGINGS, Material.LEATHER_LEGGINGS, Material.DIAMOND_BOOTS, Material.IRON_BOOTS, Material.CHAINMAIL_BOOTS,
				Material.GOLD_BOOTS, Material.LEATHER_BOOTS, };
	}

}
