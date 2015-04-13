package org.landofordos.ordosloot;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class UniqueTableEntry extends AbstractTableEntry {

    private List<String> desc;
    private List<EffectData> effects;

    /**
     * @param name
     *            - Name of the item.
     * @param weight
     * @param desc
     * @param itemTypes
     * @param effects
     * @param enchs
     * 
     *            UniqueTable entry with all the data required to create a unique item.
     */
    public UniqueTableEntry(String name, int weight, List<String> desc, List<Material> itemTypes, List<EffectData> effects,
            List<EnchantmentData> enchs) {
        super(name, weight, itemTypes, enchs);
        this.desc = desc;
        this.effects = effects;
    }

    public List<String> getDesc() {
        return desc;
    }

    public Material getItemType() {
        return itemTypes.get(0);
    }

    public List<EffectData> getEffects() {
        return effects;
    }

    /**
     * @param item
     *            - ItemStack representation of this UniqueTableEntry
     * @return true if the item is a valid representation, false otherwise.
     * 
     *         Fast and simple check - name, which might be edited by malicious users, and lore, which cannot.
     */
    public boolean equals(ItemStack item) {
        ItemMeta im = item.getItemMeta();
        if ((im != null) && (im.hasDisplayName()) & (im.hasLore())) {
            if (im.getDisplayName().equalsIgnoreCase(Quality.UNIQUE.getColor() + name)) {
                if (im.getLore().equals(desc)) {
                    return true;
                }
            }
        }
        return false;
    }
}
