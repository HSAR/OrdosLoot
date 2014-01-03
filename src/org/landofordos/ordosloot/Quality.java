package org.landofordos.ordosloot;

import org.bukkit.ChatColor;

public enum Quality {

	ORDINARY("Ordinary", ChatColor.WHITE), UNCOMMON("Uncommon", ChatColor.DARK_GREEN), RARE("Rare", ChatColor.DARK_AQUA), LEGENDARY(
			"Legendary", ChatColor.YELLOW), UNIQUE("Unique", ChatColor.GOLD);

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

	/**
	 * Returns the quality matching the input string.
	 * 
	 * @param name
	 *            Name to match into a Quality
	 * @return Quality matching the name, null if not found.
	 */
	public static Quality getByName(String name) {
		try {
			return Quality.valueOf(name.toUpperCase());
		} catch (IllegalArgumentException arg0) {
			return null;
		}
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
		Quality[] qualities = { UNIQUE, LEGENDARY, RARE, UNCOMMON, ORDINARY };
		return qualities;
	}
}
