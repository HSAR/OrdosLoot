package org.landofordos.ordosloot;

import org.bukkit.enchantments.Enchantment;

public class EnchantmentData {
	
	Enchantment ench;
	int level;
	boolean override;
	
	public EnchantmentData(Enchantment ench, int level) {
		this(ench, level, false);
	}
	
	public EnchantmentData(Enchantment ench, int level, boolean overrideDefaultLimits) {
		this.ench = ench;
		this.level = level;
		this.override = overrideDefaultLimits;
	}
	
	public Enchantment getEnchantment() {
		return ench;
	}
	
	public int getLevel() {
		return level;
	}
	
	public boolean isOP() {
		return override;
	}

}
