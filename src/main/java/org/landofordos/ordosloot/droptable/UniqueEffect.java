package org.landofordos.ordosloot.droptable;

/**
 * @author HSAR A enum setting unique effects that can be applied to items.
 * 
 *         Actual game effect implementation is in the UniqueListener class.
 */
public enum UniqueEffect {

    INFINITE_DURABILITY, //
    DAMAGE_RESISTANCE, // implemented
    HEALTH_BOOST, // implemented
    BLINDNESS, // implemented
    HUNGER, // implemented
    WEAKNESS, // implemented
    JUMP_HEIGHT, // implemented
    SPEED, // implemented
    NIGHT_VISION, // implemented
    FIRE_RESISTANCE, // implemented
    POISON, // implemented
    LIFE_LEECH, // apply REGENERATION when damaging enemies
    HONOURBOUND; // cannot sheathe until enemy killed

    public static UniqueEffect getByName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
