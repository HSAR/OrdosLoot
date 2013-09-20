package org.landofordos.ordosloot;

public enum Quality {

	ordinary("Ordinary", "ffffff"), uncommon("Uncommon", "23aa00"), rare("Rare", "1ba9ab"), legendary("Legendary", "e4a40a"), unique(
			"Unique", "af6025");

	private String value;
	private String colorCode;

	Quality(String value, String colorCode) {
		this.value = value;
		if (colorCode.length() != 6) {
			throw new IllegalArgumentException();
		} else {
			this.colorCode = colorCode;			
		}
	}

	public String getValue() {
		return value;
	}
	
	public String getColor() {
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
