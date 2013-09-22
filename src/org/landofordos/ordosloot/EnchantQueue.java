package org.landofordos.ordosloot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;

/**
 * @author HSAR A temporary holding class designed to hold all the enchantments to be put onto an item, thereby allowing additive (and
 *         subtractive) enchantments.
 */
public class EnchantQueue {

	private Map<Enchantment, Integer> enchMap;

	public EnchantQueue() {
		enchMap = new HashMap<Enchantment, Integer>();
		// store all enchantments at 0 to the map at the start
		for (Enchantment e : Enchantment.values()) {
			enchMap.put(e, 0);
		}
	}

	public void addEnchant(Enchantment ench, int level) {
		enchMap.put(ench, enchMap.get(ench) + level);
	}

	public List<EnchantmentData> finaliseEnchantments() {
		List<EnchantmentData> result = new ArrayList<EnchantmentData>();
		for (Enchantment e : Enchantment.values()) {
			int level = enchMap.get(e);
			// if we have an enchantment to apply...
			if (level > 0) {
				// if it's larger than the limit use the limit
				result.add(new EnchantmentData(e, getMin(level, e.getMaxLevel())));
			}
		}
		return result;
	}

	public static int getMin(int a, int b) {
		return (a < b ? a : b);
	}

}
