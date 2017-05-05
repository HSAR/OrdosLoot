package org.landofordos.ordosloot;

import java.util.Random;

import org.bukkit.enchantments.Enchantment;

public class EnchantmentData {

	Enchantment ench;
	int level;
	boolean override;
	boolean random;

	public EnchantmentData(Enchantment ench, int level) {
		this(ench, level, false, false);
	}

	public EnchantmentData(Enchantment ench, int level, boolean overrideDefaultLimits, boolean random) {
		this.ench = ench;
		this.level = level;
		this.override = overrideDefaultLimits;
	}

	public Enchantment getEnchantment() {
		return ench;
	}

	public int getLevel() {
		if (!random) {
			return level;
		} else {
			return (new Random()).nextInt(level + 1);
		}
	}

	public boolean isOP() {
		return override;
	}

}
