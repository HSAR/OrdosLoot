package org.landofordos.ordosloot;

import org.bukkit.ChatColor;

public enum Quality {

	ordinary("Ordinary", ChatColor.WHITE), uncommon("Uncommon", ChatColor.DARK_GREEN), rare("Rare", ChatColor.DARK_AQUA), legendary(
			"Legendary", ChatColor.YELLOW), unique("Unique", ChatColor.GOLD);

	private String value;
	private ChatColor colorCode;

	Quality(String value, ChatColor colorCode) {
		this.value = value;
		this.colorCode = colorCode;
	}

	public String getValue() {
		return value;
	}

	public ChatColor getColor() {
		return colorCode;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static Quality getEnum(String value) {

		if (value == null)
			throw new IllegalArgumentException();

		for (Quality v : values())
			if (value.equalsIgnoreCase(v.getValue()))
				return v;

		throw new IllegalArgumentException();

	}

	public static Quality[] getQualities() { // hard-coded to return them in the correct order for use in table generation
		Quality[] qualities = { unique, legendary, rare, uncommon, ordinary };
		return qualities;
	}
}
