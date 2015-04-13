package org.landofordos.ordosloot.droptable;

import java.util.List;

import org.bukkit.Material;
import org.landofordos.ordosloot.EnchantmentData;

public class NameTableEntry extends AbstractTableEntry {

    public NameTableEntry(String name, int weight, List<Material> itemTypes, List<EnchantmentData> enchs) {
        super(name, weight, itemTypes, enchs);
    }

}