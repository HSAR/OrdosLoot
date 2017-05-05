package org.landofordos.ordosloot.droptable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QualityTable {

	public class QualityTableEntry {

		Quality quality;
		double percentage;

		public QualityTableEntry(Quality quality, double percentage) {
			this.quality = quality;
			this.percentage = percentage;
		}

		public double getPerc() {
			return percentage;
		}

		public Quality getQuality() {
			return quality;
		}

	}

	Map<Quality, Double> dropRates;

	public List<QualityTableEntry> dropTable;

	public QualityTable(Map<Quality, Double> dropRates) {
		// Need to do this in the right order
		double total = 0d;
		dropTable = new ArrayList<QualityTableEntry>(5);
		for (Quality q : Quality.getQualities()) {
			double rarity = dropRates.get(q);
			total += rarity;
			dropTable.add(new QualityTableEntry(q, total));
		}
	}

	/**
	 * Returns a Quality based on a given double. Lower values yield better loot.
	 * 
	 * @param dropValue - seed value to check.
	 * @return Corresponding quality listed in the table. Null if no quality at this value (no drop given, basically).
	 */
	public Quality checkQuality(double dropValue) {
		// lower drop value = better loot given (this avoids having to calculate the "empty" space)
		for (int i = 0; i < dropTable.size(); i++) {
			if (dropValue < dropTable.get(i).getPerc()) {
				Quality droppedQuality = dropTable.get(i).getQuality();
				return droppedQuality;
			}
		}
		return null;
	}

}
