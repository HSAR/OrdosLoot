package org.landofordos.ordosloot;

import java.util.List;

import org.bukkit.Material;

public class NameTableEntry extends AbstractTableEntry {

    public NameTableEntry(String name, int weight, List<Material> itemTypes, List<EnchantmentData> enchs) {
        super(name, weight, itemTypes, enchs);
    }

}