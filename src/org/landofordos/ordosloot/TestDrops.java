package org.landofordos.ordosloot;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TestDrops {

	HashMap<Material, Integer> drops;

	// loot generation tables
	protected QualityTable qualityTable;
	protected DropTable dropTable;
	// weapons and armour are seperate for obvious reasons
	protected NameTable wep_prefTable;
	protected NameTable wep_coreTable;
	protected NameTable wep_suffTable;
	protected NameTable arm_prefTable;
	protected NameTable arm_coreTable;
	protected NameTable arm_suffTable;

	protected Logger logger;

	private Random rng;

	public TestDrops() {
		logger = Logger.getLogger(TestDrops.class.getName());
		try {
			this.loadDropTable();
			this.loadWeaponNameTables();
			this.loadArmourNameTables();
		} catch (IOException e) {
			e.printStackTrace();
		}
		rng = new Random();
		drops = new HashMap<Material, Integer>();
		Map<Quality, Double> qualityDropRates = new HashMap<Quality, Double>();
		for (Quality q : Quality.values()) {
			double rarity = 0.25;
			qualityDropRates.put(q, rarity);
		}
		qualityTable = new QualityTable(qualityDropRates);
	}

	private void loadDropTable() throws IOException {
		// format is MaterialID,weight,{quality1&quality2}
		// ============
		File dropFile = new File("items.txt");
		// ============
		dropTable = new DropTable();
		String[] dropStrings = readFile(dropFile.getPath(), Charset.defaultCharset());
		for (String dropString : dropStrings) {
			String[] dropData = dropString.split(",");
			if (dropData.length != 3) {
				logger.log(Level.SEVERE, "Format error in drops file.");
			} else {
				// valid quality list
				String[] validQualities = splitConfigList(dropData[2]);
				List<Quality> quals = new ArrayList<Quality>();
				for (String validQuality : validQualities) {
					Quality q = Quality.valueOf(validQuality);
					if (q != null) {
						quals.add(q);
					} else {
						logger.log(Level.SEVERE, "Drop data could not be loaded from file.");
					}
				}
				Material m = Material.getMaterial(dropData[0]);
				// check Material validity
				if (m == null) {
					logger.log(Level.SEVERE, "Material ID " + dropData[0] + " could not be loaded from file.");
				} else {
					dropTable.addDrop(m, Integer.parseInt(dropData[1]), quals);
				}
			}
		}
	}

	private void loadWeaponNameTables() throws IOException {
		// ============
		File wep_prefixFile = new File("weapon_prefixes.txt");
		File wep_coreFile = new File("weapon_cores.txt");
		File wep_suffixFile = new File("weapon_suffixes.txt");
		// ============
		wep_prefTable = new NameTable();
		String[] prefixStrings = readFile(wep_prefixFile.getPath(), Charset.defaultCharset());
		for (String prefixString : prefixStrings) {
			// format is name,weight,{itemtype1&itemtype2},{enchantment1=level&enchantment2=level}
			String[] prefixData = prefixString.split(",");
			if (prefixData.length != 4) {
				logger.log(Level.SEVERE, "Format error in prefixes file.");
			} else {
				// valid item list
				String[] validItems = splitConfigList(prefixData[2]);
				List<Material> ites = new ArrayList<Material>();
				for (String validItem : validItems) {
					Material m = Material.getMaterial(validItem);
					if (m != null) {
						ites.add(m);
					} else {
						logger.log(Level.SEVERE, "Material ID " + validItem + " could not be loaded from file.");
					}
				}
				// enchantment list
				String[] enchPairs = splitConfigList(prefixData[3]);
				List<EnchantmentData> enchs = new ArrayList<EnchantmentData>();
				wep_prefTable.addName(prefixData[0], Integer.parseInt(prefixData[1]), ites, enchs);
			}
		}
		// ============
		wep_coreTable = new NameTable();
		String[] coreStrings = readFile(wep_coreFile.getPath(), Charset.defaultCharset());
		// create an array of unprocessed raw strings but splitting along newline chars
		for (String coreString : coreStrings) {
			// format is name,weight,{itemtype1&itemtype2},{enchantment1=level&enchantment2=level}
			String[] coreData = coreString.split(",");
			if (coreData.length != 4) {
				logger.log(Level.SEVERE, "Format error in cores file.");
			} else {
				// valid item list
				String[] validItems = splitConfigList(coreData[2]);
				List<Material> ites = new ArrayList<Material>();
				for (String validItem : validItems) {
					Material m = Material.getMaterial(validItem);
					if (m != null) {
						ites.add(m);
					} else {
						logger.log(Level.SEVERE, "Material ID " + validItem + " could not be loaded from file.");
					}
				}
				// enchantment list
				String[] enchPairs = splitConfigList(coreData[3]);
				List<EnchantmentData> enchs = new ArrayList<EnchantmentData>();
				wep_coreTable.addName(coreData[0], Integer.parseInt(coreData[1]), ites, enchs);
			}
		}
		// ============
		wep_suffTable = new NameTable();
		String[] suffixStrings = readFile(wep_suffixFile.getPath(), Charset.defaultCharset());
		for (String suffixString : suffixStrings) {
			// format is name,weight,{itemtype1&itemtype2},{enchantment1=level&enchantment2=level}
			String[] suffixData = suffixString.split(",");
			if (suffixData.length != 4) {
				logger.log(Level.SEVERE, "Format error in suffixes file.");
				logger.log(Level.INFO, suffixString);
			} else {
				// valid item list
				String[] validItems = splitConfigList(suffixData[2]);
				List<Material> ites = new ArrayList<Material>();
				for (String validItem : validItems) {
					Material m = Material.getMaterial(validItem);
					if (m != null) {
						ites.add(m);
					} else {
						logger.log(Level.SEVERE, "Material ID " + validItem + " could not be loaded from file.");
					}
				}
				// enchantment list
				String[] enchPairs = splitConfigList(suffixData[3]);
				List<EnchantmentData> enchs = new ArrayList<EnchantmentData>();
				wep_suffTable.addName(suffixData[0], Integer.parseInt(suffixData[1]), ites, enchs);
			}
		}

	}

	private void loadArmourNameTables() throws IOException {
		// ============
		File arm_prefixFile = new File("armour_prefixes.txt");
		File arm_coreFile = new File("armour_cores.txt");
		File arm_suffixFile = new File("armour_suffixes.txt");
		// ============
		arm_prefTable = new NameTable();
		String[] prefixStrings = readFile(arm_prefixFile.getPath(), Charset.defaultCharset());
		for (String prefixString : prefixStrings) {
			// format is name,weight,{itemtype1&itemtype2},{enchantment1=level&enchantment2=level}
			String[] prefixData = prefixString.split(",");
			if (prefixData.length != 4) {
				logger.log(Level.SEVERE, "Format error in prefixes file.");
			} else {
				// valid item list
				String[] validItems = splitConfigList(prefixData[2]);
				List<Material> ites = new ArrayList<Material>();
				for (String validItem : validItems) {
					Material m = Material.getMaterial(validItem);
					if (m != null) {
						ites.add(m);
					} else {
						logger.log(Level.SEVERE, "Material ID " + validItem + " could not be loaded from file.");
					}
				}
				// enchantment list
				String[] enchPairs = splitConfigList(prefixData[3]);
				List<EnchantmentData> enchs = new ArrayList<EnchantmentData>();
				arm_prefTable.addName(prefixData[0], Integer.parseInt(prefixData[1]), ites, enchs);
			}
		}
		// ============
		arm_coreTable = new NameTable();
		String[] coreStrings = readFile(arm_coreFile.getPath(), Charset.defaultCharset());
		for (String coreString : coreStrings) {
			// format is name,weight,{itemtype1&itemtype2},{enchantment1=level&enchantment2=level}
			String[] coreData = coreString.split(",");
			if (coreData.length != 4) {
				logger.log(Level.SEVERE, "Format error in cores file.");
			} else {
				// valid item list
				String[] validItems = splitConfigList(coreData[2]);
				List<Material> ites = new ArrayList<Material>();
				for (String validItem : validItems) {
					Material m = Material.getMaterial(validItem);
					if (m != null) {
						ites.add(m);
					} else {
						logger.log(Level.SEVERE, "Material ID " + validItem + " could not be loaded from file.");
					}
				}
				// enchantment list
				String[] enchPairs = splitConfigList(coreData[3]);
				List<EnchantmentData> enchs = new ArrayList<EnchantmentData>();
				arm_coreTable.addName(coreData[0], Integer.parseInt(coreData[1]), ites, enchs);
			}
		}
		// ============
		arm_suffTable = new NameTable();
		String[] suffixStrings = readFile(arm_suffixFile.getPath(), Charset.defaultCharset());
		for (String suffixString : suffixStrings) {
			// format is name,weight,{itemtype1&itemtype2},{enchantment1=level&enchantment2=level}
			String[] suffixData = suffixString.split(",");
			if (suffixData.length != 4) {
				logger.log(Level.SEVERE, "Format error in suffixes file.");
			} else {
				// valid item list
				String[] validItems = splitConfigList(suffixData[2]);
				List<Material> ites = new ArrayList<Material>();
				for (String validItem : validItems) {
					Material m = Material.getMaterial(validItem);
					if (m != null) {
						ites.add(m);
					} else {
						logger.log(Level.SEVERE, "Material ID " + validItem + " could not be loaded from file.");
					}
				}
				// enchantment list
				String[] enchPairs = splitConfigList(suffixData[3]);
				List<EnchantmentData> enchs = new ArrayList<EnchantmentData>();
				arm_suffTable.addName(suffixData[0], Integer.parseInt(suffixData[1]), ites, enchs);
			}
		}

	}

	private EnchantmentData getEnchantment(String ench, String level) {
		Enchantment e = Enchantment.getByName(ench);
		if (e != null) {
			if (level.endsWith("*")) {
				// if input ends with * generate a random level up to the input.
				StringBuilder sb = new StringBuilder(level);
				sb.setLength(sb.length() - 1);
				return new EnchantmentData(e, Integer.parseInt(sb.toString()), false, true);
			} else {
				return new EnchantmentData(e, Integer.parseInt(level));
			}
		} else {
			logger.log(Level.SEVERE, "Enchantment " + ench + " could not be loaded from file.");
			return null;
		}
	}

	/**
	 * Splits a config bracketed list {like&this} into "like" and "this" WARNING: DOES NOT CHECK FOR VALID INPUT
	 * 
	 * @param input
	 *            The string to split
	 * @return properly formatted individual strings
	 */
	private String[] splitConfigList(String input) {
		StringBuilder sb = new StringBuilder(input.trim());
		// remove leading and trailing brackets
		sb.setLength(sb.length() - 1);
		sb.deleteCharAt(0);
		return sb.toString().split("&");

	}

	static String[] readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		// create an array of unprocessed raw strings by splitting along newline chars
		String[] fileContentArray = encoding.decode(ByteBuffer.wrap(encoded)).toString().split("\n");
		// add to fileContentList only if a string is not a comment // like this
		List<String> fileContentList = new ArrayList<String>();
		for (String s : fileContentArray) {
			if ((!s.trim().startsWith("//")) && (!(s.trim().length() == 0))) {
				fileContentList.add(s);
			}
		}
		return fileContentList.toArray(new String[fileContentList.size()]);
	}

	/**
	 * Generates a new dropped item with the parameterised quality.
	 * 
	 * @param quality
	 *            The quality of the item
	 * @return An ItemStack object with appropriate ItemMeta - description, name, enchantments.
	 */
	public ItemStack getNewDroppedItem(Quality quality) {
		if (quality != null) {
			// unique items have a seperate generation
			if (quality.equals(Quality.unique)) {
				return new ItemStack(Material.BEACON);
			}
			// decide the dropped item
			DropTable validDropTable = dropTable.filterByQuality(quality);
			double seed = rng.nextDouble();
			DropTableEntry dte = validDropTable.getDropFromTable(seed);
			if (dte != null) {
				ItemStack item = new ItemStack(dte.getMaterial());
				String name = "";
				// filtered copies of the prefix, core and suffix generation tables.
				NameTable prefTable = null;
				NameTable coreTable = null;
				NameTable suffTable = null;
				// placeholders for the prefix, core and suffix components.
				NameTableEntry pref = null;
				NameTableEntry core = null;
				NameTableEntry suff = null;
				EnchantQueue enchsToAdd = new EnchantQueue();
				switch (dte.getType()) {
				case armour:
					prefTable = arm_prefTable.filterByMaterial(dte.getMaterial());
					coreTable = arm_coreTable.filterByMaterial(dte.getMaterial());
					suffTable = arm_suffTable.filterByMaterial(dte.getMaterial());
					break;
				case weapon:
					prefTable = wep_prefTable.filterByMaterial(dte.getMaterial());
					coreTable = wep_coreTable.filterByMaterial(dte.getMaterial());
					suffTable = wep_suffTable.filterByMaterial(dte.getMaterial());
					break;
				default:
					break;
				}
				switch (quality) {
				case legendary:
					pref = prefTable.getNameFromTable(rng.nextDouble());
				case rare:
					suff = suffTable.getNameFromTable(rng.nextDouble());
				case uncommon:
					core = coreTable.getNameFromTable(rng.nextDouble());
					break;
				default:
					break;
				}
				if (core != null) {
					for (EnchantmentData ench : core.getEnchantments()) {
						enchsToAdd.addEnchant(ench.getEnchantment(), ench.getLevel());
					}
					name = core.getName();
				} else {
					// no core = no drop
					return null;
				}
				if (pref != null) {
					for (EnchantmentData ench : pref.getEnchantments()) {
						enchsToAdd.addEnchant(ench.getEnchantment(), ench.getLevel());
					}
					name = pref.getName() + name;
				}
				if (suff != null) {
					for (EnchantmentData ench : suff.getEnchantments()) {
						enchsToAdd.addEnchant(ench.getEnchantment(), ench.getLevel());
					}
					name = name + suff.getName();
				}
				// apply queued enchantments
				for (EnchantmentData ed : enchsToAdd.finaliseEnchantments()) {
				}
				// set name
				// apply metadata changes and return item
				return item;
			}
		}
		return null;
	}

	public void genDrop() {
		double dropVal = rng.nextDouble();
		Quality droppedQual = qualityTable.checkQuality(dropVal);
		ItemStack item = getNewDroppedItem(droppedQual);
		Material m = item.getType();
		if (drops.get(m) == null) {
			drops.put(m, 1);
		} else {
			drops.put(m, drops.get(m) + 1);
		}
	}

	public void printResults() {
		int[] dropVals = new int[3];
		dropVals[0] = 0; // bows
		dropVals[1] = 0; // swords
		dropVals[2] = 0; // axes
		for (Map.Entry<Material, Integer> dropType : drops.entrySet()) {
			if (dropType.getKey().toString().contains("BOW")) {
				dropVals[0] = dropVals[0] + dropType.getValue();
			}
			if (dropType.getKey().toString().contains("SWORD")) {
				dropVals[1] = dropVals[1] + dropType.getValue();
			}
			if (dropType.getKey().toString().contains("AXE")) {
				dropVals[2] = dropVals[2] + dropType.getValue();
			}
		}
		System.out.println("====");
		System.out.println("BOWS :" + dropVals[0]);
		System.out.println("AXES :" + dropVals[1]);
		System.out.println("SWORDS :" + dropVals[2]);
	}

	public static void main(String[] args) {
		TestDrops td = new TestDrops();
		for (int i = 0; i < 200; i++) {
			td.genDrop();
		}
		td.printResults();
	}

}
