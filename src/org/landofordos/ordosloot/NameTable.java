package org.landofordos.ordosloot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

public class NameTable {

	List<NameTableEntry> nameTable;

	/**
	 * Creates a PrefixTable object which is responsible for storing and generating random prefixes for an item.
	 */
	public NameTable() {
		// initialise table objects
		nameTable = new ArrayList<NameTableEntry>();
	}

	public int size() {
		return nameTable.size();
	}

	public void addName(String name, int weight, List<Material> validMats, List<EnchantmentData> enchs) {
		for (int i = 0; i < weight; i++) {
			// add into the table $weight number of times.
			nameTable.add(new NameTableEntry(name, weight, validMats, enchs));
		}
	}

	public void addEntry(NameTableEntry nte) {
		nameTable.add(nte);
	}

	/**
	 * Returns a new table with only the modifiers that apply to one item type
	 * 
	 * @param filterMat
	 *            Material object which will be used to filter
	 * @return a new NameTable with only valid modifiers for the filterMat Material.
	 */
	public NameTable filterByMaterial(Material filterMat) {
		NameTable result = new NameTable();
		for (NameTableEntry nte : nameTable) {
			if (nte.getItemTypes().contains(filterMat)) {
				result.addEntry(nte);
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
	public AbstractTableEntry getNameFromTable(double val) {
		if (nameTable.size() > 0) {
			int intVal = (int) (val * nameTable.size());
			return nameTable.get(intVal);
		} else {
			return null;
		}
	}
}
