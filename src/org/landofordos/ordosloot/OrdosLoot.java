package org.landofordos.ordosloot;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.landofordos.ordosloot.QualityTable.QualityTableEntry;

public class OrdosLoot extends JavaPlugin implements Listener {

	// Important plugin objects
	private static Server server;
	private static Logger logger;
	private FileConfiguration config;
	//
	private boolean verbose;
	private boolean useDrops;
	// RNG
	Random rng;
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
	// table for unique items
	protected UniqueTable uniqueTable;

	public void onDisable() {
		logger.info("Disabled.");
	}

	public void onEnable() {
		// static reference to this plugin and the server
		// plugin = this;
		server = getServer();
		// start the logger
		logger = getLogger();
		// save config to default location if not already there
		this.saveDefaultConfig();
		// Random Number Generator
		rng = new Random();
		//
		// ====== CONFIG LOAD START ======
		//
		// set config var
		config = this.getConfig();
		// first-run initialisation, if necessary
		final boolean firstrun = config.getBoolean("pluginvars.firstrun");
		if (firstrun) {
			// Whatever first run initialisation is required
			config.set("pluginvars.firstrun", false);
			this.saveConfig();
			if (verbose) {
				logger.info("First-run initialisation complete.");
			}
		}
		// verbose logging? retrieve value from config file.
		verbose = config.getBoolean("pluginvars.verboselogging");
		if (verbose) {
			logger.info("Verbose logging enabled.");
		} else {
			logger.info("Verbose logging disabled.");
		}
		// plugin effect enabled? retrieve value from config file.
		useDrops = config.getBoolean("pluginvars.enabled");
		// load loot rarities from config file
		double total = 0;
		Map<Quality, Double> qualityDropRates = new HashMap<Quality, Double>();
		for (Quality q : Quality.values()) {
			if (verbose) {
				// logger.info(q.toString().toLowerCase() + " [" + config.getString("rarities." + q.toString().toLowerCase()) + "]");
			}
			double rarity = Double.parseDouble(config.getString("rarities." + q.toString().toLowerCase()));
			total += rarity;
			qualityDropRates.put(q, rarity);
		}
		try {
			// load available drop items from the file.
			loadDropTable();
			// load name tables from their files; extract them if they do not already exist.
			// #TODO: Allow prefixes to add a description.
			// #TODO: Crafting
			// #TODO: Crafting ideas: use rotten meat for DAMAGE_UNDEAD. Use nether quartz because of difficulty of obtaining.
			loadWeaponNameTables();
			loadArmourNameTables();
			// load unique-quality items from the file.
			loadUniqueTable();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (verbose) {
			logger.info("Loaded " + uniqueTable.size() + " unique items from file.");
		}
		// check that they do not exceed 1 in total
		if (total > 1) {
			logger.log(Level.SEVERE, "Quality table values exceeded permitted limit.");
			useDrops = false;
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		} else {
			qualityTable = new QualityTable(qualityDropRates);
			for (QualityTableEntry qte : qualityTable.dropTable) {
				logger.info(qte.getQuality() + " [" + qte.getPerc() + "]");
			}
		}
		//
		// ====== CONFIG LOAD FINISH ======
		//
		// register events
		server.getPluginManager().registerEvents(this, this);
		server.getPluginManager().registerEvents(new UniqueListener(this), this);
	}

	private void loadDropTable() throws IOException {
		// format is MaterialID,weight,{quality1&quality2}
		// ============
		File dropFile = new File(this.getDataFolder() + "/items.txt");
		if (!dropFile.exists()) {
			saveResource("items.txt", false);
		}
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
		File wep_prefixFile = new File(this.getDataFolder() + "/weapon_prefixes.txt");
		File wep_coreFile = new File(this.getDataFolder() + "/weapon_cores.txt");
		File wep_suffixFile = new File(this.getDataFolder() + "/weapon_suffixes.txt");
		if (!wep_prefixFile.exists()) {
			saveResource("weapon_prefixes.txt", false);
		}
		if (!wep_coreFile.exists()) {
			saveResource("weapon_cores.txt", false);
		}
		if (!wep_suffixFile.exists()) {
			saveResource("weapon_suffixes.txt", false);
		}
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
				for (String enchPair : enchPairs) {
					String[] enchLine = enchPair.split("=");
					enchs.add(getEnchantment(enchLine[0], enchLine[1]));
				}
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
				for (String enchPair : enchPairs) {
					String[] enchLine = enchPair.split("=");
					enchs.add(getEnchantment(enchLine[0], enchLine[1]));
				}
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
				for (String enchPair : enchPairs) {
					String[] enchLine = enchPair.split("=");
					enchs.add(getEnchantment(enchLine[0], enchLine[1]));
				}
				wep_suffTable.addName(suffixData[0], Integer.parseInt(suffixData[1]), ites, enchs);
			}
		}

	}

	private void loadArmourNameTables() throws IOException {
		// ============
		File arm_prefixFile = new File(this.getDataFolder() + "/armour_prefixes.txt");
		File arm_coreFile = new File(this.getDataFolder() + "/armour_cores.txt");
		File arm_suffixFile = new File(this.getDataFolder() + "/armour_suffixes.txt");
		if (!arm_prefixFile.exists()) {
			saveResource("armour_prefixes.txt", false);
		}
		if (!arm_coreFile.exists()) {
			saveResource("armour_cores.txt", false);
		}
		if (!arm_suffixFile.exists()) {
			saveResource("armour_suffixes.txt", false);
		}
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
				for (String enchPair : enchPairs) {
					String[] enchLine = enchPair.split("=");
					enchs.add(getEnchantment(enchLine[0], enchLine[1]));
				}
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
				for (String enchPair : enchPairs) {
					String[] enchLine = enchPair.split("=");
					enchs.add(getEnchantment(enchLine[0], enchLine[1]));
				}
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
				for (String enchPair : enchPairs) {
					String[] enchLine = enchPair.split("=");
					enchs.add(getEnchantment(enchLine[0], enchLine[1]));
				}
				arm_suffTable.addName(suffixData[0], Integer.parseInt(suffixData[1]), ites, enchs);
			}
		}

	}

	private void loadUniqueTable() throws IOException {
		// format is name,weight,{descLine1&descLine2},MaterialID,{unique_effect1&unique_effect2},{ench1&ench2}
		// ============
		File uniqueFile = new File(this.getDataFolder() + "/unique_items.txt");
		if (!uniqueFile.exists()) {
			saveResource("unique_items.txt", false);
		}
		// ============
		uniqueTable = new UniqueTable();
		String[] uniqueStrings = readFile(uniqueFile.getPath(), Charset.defaultCharset());
		for (String uniqueString : uniqueStrings) {
			String[] uniqueData = uniqueString.split(",");
			if (uniqueData.length != 6) {
				logger.log(Level.SEVERE, "Format error in unique item " + uniqueData[0] + ".");
			} else {
				// description
				List<String> desc = new ArrayList<String>(Arrays.asList(splitConfigList(uniqueData[2])));
				// item type
				Material m = Material.getMaterial(uniqueData[3]);
				if (m == null) {
					logger.log(Level.SEVERE, "Material ID " + uniqueData[3] + " could not be loaded from file.");
				}
				// effect list
				String[] effectData = splitConfigList(uniqueData[4]);
				List<UniqueEffect> effs = new ArrayList<UniqueEffect>();
				// if empty, skip
				for (String effect : effectData) {
					if (!effect.equals("")) {
						UniqueEffect ue = UniqueEffect.valueOf(effect);
						if (ue != null) {
							effs.add(ue);
						} else {
							logger.log(Level.SEVERE, "Unique item data could not be loaded from file.");
						}
					}
				}
				// enchantment list
				String[] enchPairs = splitConfigList(uniqueData[5]);
				List<EnchantmentData> enchs = new ArrayList<EnchantmentData>();
				// if empty, skip
				for (String enchPair : enchPairs) {
					if (!enchPair.equals("")) {
						String[] enchLine = enchPair.split("=");
						Enchantment e = Enchantment.getByName(enchLine[0]);
						if (e != null) {
							enchs.add(new EnchantmentData(e, Integer.parseInt(enchLine[1])));
						} else {
							logger.log(Level.SEVERE, "Enchantment " + enchLine[0] + " could not be loaded from file.");
						}
					}
				}
				// validation check before entry
				if ((uniqueData[0] != null) && (Integer.parseInt(uniqueData[1]) > 0) && (desc != null) && (m != null) && (effs != null)
						&& (enchs != null)) {
					uniqueTable.addUnique(uniqueData[0], Integer.parseInt(uniqueData[1]), desc, m, effs, enchs);
				}
			}
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (args.length == 1) {
			if (args[0] == "reload") {
				if (sender.hasPermission("ordosloot.reloadconfig")) {
					verbose = config.getBoolean("pluginvars.verboselogging");
					useDrops = config.getBoolean("pluginvars.enabled");
				} else {
					sender.sendMessage(ChatColor.RED + "You do not have permission to reload OrdosLoot's config.");
					return true;
				}
			}
			if (args[0] == "toggle") {
				if (sender.hasPermission("ordosloot.ingametoggle")) {
					useDrops = !useDrops;
					// save to config
					config.set("enabled", useDrops);
				} else {
					sender.sendMessage(ChatColor.RED + "You do not have permission to toggle OrdosLoot.");
					return true;
				}
			}
		}
		sender.sendMessage("Enchanted loot: " + useDrops);
		return true;
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
				return createUniqueDrop(rng.nextDouble());
			}
			// decide the dropped item
			DropTable validDropTable = dropTable.filterByQuality(quality);
			DropTableEntry dte = validDropTable.getDropFromTable(rng.nextDouble());
			if (dte != null) {
				ItemStack item = new ItemStack(dte.getMaterial());
				ItemMeta meta = item.getItemMeta();
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
					meta.addEnchant(ed.getEnchantment(), ed.getLevel(), false);
				}
				// set name
				meta.setDisplayName(quality.getColor() + name);
				// apply metadata changes and return item
				item.setItemMeta(meta);
				return item;
			}
		}
		return null;
	}

	private ItemStack createUniqueDrop(double val) {
		// Draw and generate a new unique.
		UniqueTableEntry ute = uniqueTable.getUniqueFromTable(rng.nextDouble());
		// get itemtype and retrieve metadata store
		ItemStack item = new ItemStack(ute.getItemType());
		ItemMeta meta = item.getItemMeta();
		// set name, description and enchants
		meta.setDisplayName(Quality.unique.getColor() + ute.getName());
		meta.setLore(ute.getDesc());
		for (EnchantmentData ench : ute.getEnchantments()) {
			meta.addEnchant(ench.getEnchantment(), ench.getLevel(), true);
		}
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Returns the display names of the unique items currently tracked by this plugin.
	 * 
	 * @return A List item holding DisplayNames of the unique items loaded.
	 */
	public List<String> getUniqueItemNames() {
		return uniqueTable.getUniqueItemNames();
	}

	public UniqueTableEntry getUniqueItemData(String name) {
		return uniqueTable.getUniqueByName(name);
	}

	private EnchantmentData getEnchantment(String ench, String level) {
		Enchantment e = Enchantment.getByName(ench);
		if (e != null) {
			try {
				if (level.endsWith("*")) {
					// if input ends with * generate a random level up to the input.
					StringBuilder sb = new StringBuilder(level);
					sb.setLength(sb.length() - 1);
					return new EnchantmentData(e, Integer.parseInt(sb.toString()), false, true);
				} else {
					return new EnchantmentData(e, Integer.parseInt(level));
				}
			} catch (NumberFormatException arg0) {
				logger.log(Level.SEVERE, "Enchantment " + ench + " of level " + level + " could not be loaded from file.");
				return null;
			}
		}
		logger.log(Level.SEVERE, "Enchantment " + ench + " could not be loaded from file.");
		return null;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	// EventPriority.NORMAL by default
	public void onEntityDeath(final EntityDeathEvent event) {
		if (event.getEntity() instanceof Monster) {
			Monster monsterEnt = (Monster) event.getEntity();
			if (monsterEnt.getKiller() == null) {
				return;
			} else {
				// #TODO: Implement player permission check before dropping loot.
				// Player player = monsterEnt.getKiller();
				// We now know that a player (and not a mob spawner, or any other damage type) killed the mob.
				// Randomly roll for item quality based on values loaded from file.
				double dropVal = rng.nextDouble();
				Quality droppedQual = qualityTable.checkQuality(dropVal);
				if (droppedQual != null) {
					if (verbose) {
						logger.info(droppedQual + " dropped (" + dropVal + ")");
					}
					Location loc = monsterEnt.getLocation();
					World world = loc.getWorld();
					ItemStack item = getNewDroppedItem(droppedQual);
					if (item != null) {
						world.dropItemNaturally(loc, item);
					}
				} else {
					if (verbose) {
						logger.info("No item dropped.");
					}
				}
			}
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
			if ((!s.startsWith("//")) && (!(s.trim().length() == 0))) {
				fileContentList.add(s);
			}
		}
		return fileContentList.toArray(new String[fileContentList.size()]);
	}
}