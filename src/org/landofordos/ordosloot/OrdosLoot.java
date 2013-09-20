package org.landofordos.ordosloot;

import java.util.ArrayList;
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
	Random rng = new Random();
	// drop vars
	protected QualityTable qualityTable;

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

	@EventHandler(priority = EventPriority.NORMAL)
	// EventPriority.NORMAL by default
	public void onEntityDeath(final EntityDeathEvent event) {
		if (event.getEntity() instanceof Monster) {
			Monster monsterEnt = (Monster) event.getEntity();
			if (monsterEnt.getKiller() == null) {
				return;
			} else {
				// #TODO: Implement player permission check before dropping loot.
				Player player = monsterEnt.getKiller();
				// We now know that a player (and not a mob spawner, or any other damage type) killed the mob.
				// Randomly roll for item quality based on values loaded from file.
				double dropVal = rng.nextDouble();
				Quality droppedQual = qualityTable.checkQuality(dropVal);
				if (droppedQual != null) {
					if (verbose) {
						logger.info("Drop awarded at " + droppedQual + " (" + dropVal + ")");
					}
				} else {
					logger.info("No item dropped.");
				}
				Location loc = monsterEnt.getLocation();
				World world = loc.getWorld();
				ItemStack item = new ItemStack(Material.STONE_SWORD);
				ItemMeta meta = item.getItemMeta();
				List<String> desc = new ArrayList<String>();
				desc.add("Beyond the sharpened edge of the shield...");
				meta.setLore(desc);
				meta.setDisplayName(ChatColor.getByChar(droppedQual.getColor()) + "Ancient Sword");
				item.setItemMeta(meta);
				// #TODO: Custom names.
				// #TODO: Random material type.
				// #TODO: Enchant item.
				// #TODO: All drops should have sharpness or protection with a level based on tier.
				world.dropItemNaturally(loc, item);

			}
		}
	}
}