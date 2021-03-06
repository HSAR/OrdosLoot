package org.landofordos.ordosloot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.landofordos.ordosloot.droptable.AbstractTableEntry;
import org.landofordos.ordosloot.droptable.DropTable;
import org.landofordos.ordosloot.droptable.DropTableEntry;
import org.landofordos.ordosloot.droptable.DropType;
import org.landofordos.ordosloot.droptable.NameTable;
import org.landofordos.ordosloot.droptable.Quality;
import org.landofordos.ordosloot.droptable.QualityTable;
import org.landofordos.ordosloot.droptable.UniqueEffect;
import org.landofordos.ordosloot.droptable.UniqueListener;
import org.landofordos.ordosloot.droptable.UniqueTable;
import org.landofordos.ordosloot.droptable.UniqueTableEntry;
import org.landofordos.ordosloot.droptable.QualityTable.QualityTableEntry;

public class OrdosLoot extends JavaPlugin implements Listener {

    // Important plugin objects
    private static Server server;
    private static Logger logger;
    private FileConfiguration config;
    //
    private boolean verbose;
    private boolean useDrops;
    private boolean logToFile;
    //
    protected File logFile;
    protected final SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
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
        // log to file? retrieve value from config file.
        logToFile = config.getBoolean("pluginvars.logtofile");
        if (logToFile) {
            logger.info("File log enabled.");
            logFile = new File(this.getDataFolder().getAbsolutePath() + File.separator + "log.txt");
        } else {
            logger.info("File log disabled.");
        }
        // load loot rarities from config file
        double total = 0;
        Map<Quality, Double> qualityDropRates = new HashMap<>();
        for (Quality q : Quality.values()) {
            if (verbose) {
                // logger.info(q.toString().toLowerCase() + " [" + config.getString("rarities." + q.toString().toLowerCase()) + "]");
            }
            double rarity = Double.parseDouble(config.getString("rarities." + q.toString().toLowerCase()));
            total += rarity;
            qualityDropRates.put(q, rarity);
        }
        reloadTables();
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
                List<Quality> quals = new ArrayList<>();
                for (String validQuality : validQualities) {
                    Quality q = Quality.getByName(validQuality);
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
                logger.log(Level.SEVERE, "Format error in prefixes file. Violation: '" + prefixString + "'");
            } else {
                // valid item list
                String[] validItems = splitConfigList(prefixData[2]);
                List<Material> ites = new ArrayList<>();
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
                List<EnchantmentData> enchs = new ArrayList<>();
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
                logger.log(Level.SEVERE, "Format error in cores file. Violation: '" + coreString + "'");
            } else {
                // valid item list
                String[] validItems = splitConfigList(coreData[2]);
                List<Material> ites = new ArrayList<>();
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
                List<EnchantmentData> enchs = new ArrayList<>();
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
                logger.log(Level.SEVERE, "Format error in suffixes file. Violation: '" + suffixString + "'");
            } else {
                // valid item list
                String[] validItems = splitConfigList(suffixData[2]);
                List<Material> ites = new ArrayList<>();
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
                List<EnchantmentData> enchs = new ArrayList<>();
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
                List<Material> ites = new ArrayList<>();
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
                List<EnchantmentData> enchs = new ArrayList<>();
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
                List<Material> ites = new ArrayList<>();
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
                List<EnchantmentData> enchs = new ArrayList<>();
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
                List<Material> ites = new ArrayList<>();
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
                List<EnchantmentData> enchs = new ArrayList<>();
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
                List<String> desc = new ArrayList<>(Arrays.asList(splitConfigList(uniqueData[2])));
                // item type
                Material m = Material.getMaterial(uniqueData[3]);
                if (m == null) {
                    logger.log(Level.SEVERE, "Material ID " + uniqueData[3] + " could not be loaded from file.");
                }
                // effect list
                String[] effectData = splitConfigList(uniqueData[4]);
                List<EffectData> effs = new ArrayList<>();
                // if empty, skip
                for (String effectPair : effectData) {
                    if (!effectPair.equals("")) {
                        String[] effectLine = effectPair.split("=");
                        UniqueEffect ue = UniqueEffect.valueOf(effectLine[0]);
                        if (ue != null) {
                            effs.add(new EffectData(ue, Integer.parseInt(effectLine[1])));
                        } else {
                            logger.log(Level.SEVERE, "Unique effect " + effectLine[0] + " could not be loaded from file.");
                        }
                    }
                }
                // enchantment list
                String[] enchPairs = splitConfigList(uniqueData[5]);
                List<EnchantmentData> enchs = new ArrayList<>();
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
                    List<Material> matList = new ArrayList<>(1);
                    matList.add(m);
                    uniqueTable.addUnique(uniqueData[0], Integer.parseInt(uniqueData[1]), desc, matList, effs, enchs);
                }
            }
        }

    }

    private void reloadTables() {
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
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 1) {
            if (args[0].equals("reload")) {
                if (sender.hasPermission("ordosloot.reloadconfig")) {
                    sender.sendMessage(ChatColor.YELLOW + "Configuration reloaded.");
                    verbose = config.getBoolean("pluginvars.verboselogging");
                    useDrops = config.getBoolean("pluginvars.enabled");
                    reloadTables();
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload OrdosLoot's config.");
                    return true;
                }
            }
            if (args[0].equals("toggle")) {
                if (sender.hasPermission("ordosloot.ingametoggle")) {
                    useDrops = !useDrops;
                    sender.sendMessage("Enchanted loot: " + useDrops);
                    // save to config
                    config.set("enabled", useDrops);
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to toggle OrdosLoot.");
                    return true;
                }
            }
            if (args[0].equals("metadata")) {
                if (sender instanceof Player) {
                    if (sender.hasPermission("ordosloot.giveloot")) {
                        Player player = (Player) sender;
                        ItemStack item = player.getItemInHand();
                        if (item != null && item.getItemMeta().getDisplayName().endsWith(ChatColor.RESET.toString())) {
                            ItemMeta meta = item.getItemMeta();
                            meta = applyPluginMetaData(meta);
                            item.setItemMeta(meta);
                        }
                        player.sendMessage(ChatColor.YELLOW + "Item metadata check now "
                                + player.getItemInHand().getItemMeta().getDisplayName().endsWith(ChatColor.RESET.toString()));
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to generate OrdosLoot items.");
                        return true;
                    }
                } else {
                    sender.sendMessage("Cannot do this as you are not a player.");
                    return true;
                }
            }
        }
        // command to give player generated loot of a given quality and material.
        // /ordosloot give QUALITY [ITEMTYPE]
        if (args.length > 1) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if ((player.hasPermission("ordosloot.giveloot")) && (args[0].equalsIgnoreCase("give"))) {
                    Quality qual = Quality.getByName(args[1]);
                    Material mat = null;
                    if (args.length > 2) {
                        mat = Material.getMaterial(args[2]);
                    }
                    if (qual != null) {
                        if (qual.equals(Quality.UNIQUE)) {
                            if (args.length > 2) {
                                // uniques are deterministic and, well, unique, so add by name.
                                ItemStack unique = this.generateUniqueDrop(uniqueTable.getUniqueByName(args[2].replaceAll("_", " ")));
                                if (unique != null) {
                                    player.getInventory().addItem(unique);
                                } else {
                                    player.sendMessage(ChatColor.RED + "Unique item not found.");
                                }
                                return true;
                            } else {
                                player.getInventory().addItem(this.generateNewUniqueDrop(rng.nextDouble()));
                                return true;
                            }
                        } else {
                            if ((mat != null) && (DropType.getType(mat) != null)) {
                                ItemStack item = this.generateItem(DropType.getType(mat), mat, qual);
                                player.getInventory().addItem(item);
                                logger.info(qual + " item \"" + item.getItemMeta().getDisplayName() + "\"" + " given to player "
                                        + player.getName() + ".");
                                if (logToFile) {
                                    this.logNewLootDrop(qual, item, player, null);
                                }
                                return true;
                            } else {
                                sender.sendMessage(ChatColor.RED + "Invalid value for ITEMTYPE.");
                                return true;
                            }
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Invalid value for QUALITY.");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to generate OrdosLoot items.");
                    return true;
                }
            } else {
                sender.sendMessage("Cannot do this as you are not a player.");
                return true;
            }
        }
        return false;
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
            if (quality.equals(Quality.UNIQUE)) {
                return generateNewUniqueDrop(rng.nextDouble());
            }
            // decide the dropped item
            DropTable validDropTable = dropTable.filterByQuality(quality);
            DropTableEntry dte = validDropTable.getDropFromTable(rng.nextDouble());
            if (dte != null) {
                return generateItem(dte.getType(), dte.getMaterial(), quality);
            }
        }
        return null;
    }

    public ItemStack generateItem(DropType type, Material material, Quality quality) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String name = "";
        // filtered copies of the prefix, core and suffix generation tables.
        NameTable prefTable = null;
        NameTable coreTable = null;
        NameTable suffTable = null;
        // placeholders for the prefix, core and suffix components.
        AbstractTableEntry pref = null;
        AbstractTableEntry core = null;
        AbstractTableEntry suff = null;
        EnchantQueue enchsToAdd = new EnchantQueue();
        switch (type) {
        case armour:
            prefTable = arm_prefTable.filterByMaterial(material);
            coreTable = arm_coreTable.filterByMaterial(material);
            suffTable = arm_suffTable.filterByMaterial(material);
            break;
        case weapon:
            prefTable = wep_prefTable.filterByMaterial(material);
            coreTable = wep_coreTable.filterByMaterial(material);
            suffTable = wep_suffTable.filterByMaterial(material);
            break;
        default:
            break;
        }
        switch (quality) {
        case LEGENDARY:
            suff = suffTable.getNameFromTable(rng.nextDouble());
        case RARE:
            pref = prefTable.getNameFromTable(rng.nextDouble());
        case UNCOMMON:
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
        meta = applyPluginMetaData(meta);
        return item;
    }

    private ItemStack generateNewUniqueDrop(double val) {
        // Draw and generate a new unique.
        return generateUniqueDrop(uniqueTable.getUniqueFromTable(rng.nextDouble()));
    }

    private ItemStack generateUniqueDrop(UniqueTableEntry ute) {
        if (ute != null) {
            // get itemtype and retrieve metadata store
            ItemStack item = new ItemStack(ute.getItemType());
            ItemMeta meta = item.getItemMeta();
            // set name, description and enchants
            meta.setDisplayName(Quality.UNIQUE.getColor() + ute.getName());
            meta.setLore(ute.getDesc());
            for (EnchantmentData ench : ute.getEnchantments()) {
                meta.addEnchant(ench.getEnchantment(), ench.getLevel(), true);
            }
            // set unbreakable, if necessary
            if (ute.getEffects().contains(new EffectData(UniqueEffect.INFINITE_DURABILITY, 1))) {
                meta.spigot().setUnbreakable(true);
            }
            meta = applyPluginMetaData(meta);
            item.setItemMeta(meta);
            return item;
        } else {
            return null;
        }
    }

    private ItemMeta applyPluginMetaData(ItemMeta meta) {
        if (!meta.getDisplayName().endsWith(ChatColor.RESET.toString())) {
            meta.setDisplayName(meta.getDisplayName() + ChatColor.RESET);
        }
        return meta;
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

    public UniqueTable getUniqueTable() {
        return uniqueTable;
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

    protected void logNewLootDrop(Quality quality, ItemStack item, Player player, LivingEntity entKilled) {
        PrintStream printstream = null;
        try {
            printstream = new PrintStream(new FileOutputStream(logFile, true));
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "File log was enabled, but log file could not be written to.");
        }
        String dateStamp = dateformat.format((new Date()).getTime());
        StringBuilder sb = new StringBuilder("[");
        sb.append(dateStamp);
        sb.append("]: ");
        sb.append(quality.toString());
        sb.append(" ").append(item.getType().toString()).append(" \"");
        sb.append(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
        sb.append("\" dropped for ");
        sb.append(player.getName());
        if (entKilled != null) {
            sb.append(" from ");
            sb.append(entKilled.getType());
        } else {
            sb.append(" (SPAWNED)");
        }
        printstream.println(sb.toString());
        // close file
        printstream.close();
    }

    /**
     * Returns the plugin's valuation of an item. Items with lots of durability are worth exponentially more than near-dead ones.
     * 
     * @param item
     *            The item to consider
     * @return double value between 0 and 1 representing its considered value
     */
    public double valueOfItem(ItemStack item) {
        // first check it is a valid ordosloot item - color check
        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();
        // check properties match up
        if (meta.hasEnchants()) {
            // if (meta.hasEnchants() && meta.getDisplayName.endsWith(ChatColor.RESET.toString()) { // enable this once migration is
            // complete
            if (displayName.contains("" + Quality.UNIQUE.getColor())) {
                ItemStack testUnique = generateUniqueDrop(getUniqueItemData(ChatColor.stripColor(displayName)));
                // #TODO: Check this works
                if (!testUnique.getItemMeta().equals(meta)) {
                    return 0d;
                }
            }

            // if the name color doesn't match the color of any quality, everything will be 0 anyway.
            double value = 0;
            // now that it's all verified, work out the value of the item
            for (Quality q : Quality.values()) {
                if (displayName.contains(q.getColor().toString())) {
                    double rarity = Double.parseDouble(config.getString("rarities." + q.toString().toLowerCase()));
                    // base value = 1/rarity
                    value = 1.0d / rarity;
                }
            }

            // weight by durability
            double durabilityModifier = item.getDurability() / item.getType().getMaxDurability();
            // square durability so that more durability is polynomially better
            value = value * (durabilityModifier * durabilityModifier);

            if (verbose) {
                logger.info("Valued item '" + meta.getDisplayName() + "' with durability " + item.getDurability() + " / "
                        + item.getType().getMaxDurability() + " at " + value);
            }
            return value;
        } else {
            return 0d;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    // EventPriority.NORMAL by default
    public void onEntityDeath(final EntityDeathEvent event) {
        LivingEntity ent = event.getEntity();
        if ((ent instanceof Monster) && (ent != null)) {
            // #TODO: Implement player permission check before dropping loot.
            Player player = ent.getKiller();
            if (player != null) {
                // We now know that a player (and not a mob spawner, or any other damage type) killed the mob.
                // Randomly roll for item quality based on values loaded from file.
                double dropVal = rng.nextDouble();
                // Roll extra times if the user has a enchanted weapon with looting+
                ItemStack itemInHand = player.getItemInHand();
                if (itemInHand != null) {
                    if (itemInHand.containsEnchantment(Enchantment.LOOT_BONUS_MOBS)) {
                        for (int i = 0; i < itemInHand.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS); i++) {
                            // take the lowest value (lower == better)
                            double newDropVal = rng.nextDouble();
                            dropVal = Math.min(dropVal, newDropVal);
                        }
                    }
                }
                Quality droppedQual = qualityTable.checkQuality(dropVal);
                if (droppedQual != null) {
                    Location loc = ent.getLocation();
                    World world = loc.getWorld();
                    ItemStack item = getNewDroppedItem(droppedQual);
                    if (item != null) {
                        world.dropItemNaturally(loc, item);
                        if (verbose) {
                            logger.info(droppedQual + " item \"" + ChatColor.stripColor(item.getItemMeta().getDisplayName()) + "\""
                                    + " dropped (" + dropVal + ")");
                        }
                        if (logToFile) {
                            this.logNewLootDrop(droppedQual, item, player, ent);
                        }
                    } else {
                        if (verbose) {
                            logger.info("Could not generate item.");
                        }
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

    public static String[] readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        // create an array of unprocessed raw strings by splitting along newline chars
        String[] fileContentArray = encoding.decode(ByteBuffer.wrap(encoded)).toString().split("\n");
        // add to fileContentList only if a string is not a comment // like this
        List<String> fileContentList = new ArrayList<>();
        for (String s : fileContentArray) {
            if ((!s.contains("//")) && (!(s.trim().length() == 0))) {
                fileContentList.add(s);
            }
        }
        return fileContentList.toArray(new String[fileContentList.size()]);
    }

    // Monitor players renaming items to make sure they don't try to create items with the same colors as OrdosLoot items.
    // Currently disabled so that players can actually repair items
    // @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.ANVIL && event.getSlotType() == SlotType.RESULT) {
            Inventory anvil = event.getInventory();
            ItemStack anvilResult = anvil.getItem(event.getSlot());
        }
    }
}
