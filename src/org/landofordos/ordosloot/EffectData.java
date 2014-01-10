package org.landofordos.ordosloot;

public class EffectData {

    UniqueEffect effect;
    int level;

    public EffectData(UniqueEffect ench, int level) {
        this.effect = ench;
        this.level = level;
    }

    public UniqueEffect getEffect() {
        return effect;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null) {
            if (obj instanceof UniqueEffect) {
                return (effect.equals(obj));
            }
            if (obj instanceof EffectData) {
                EffectData effDat = (EffectData) obj;
                return ((effect.equals(effDat.getEffect()) && (level == effDat.getLevel())));
            }
        }
        return false;
    }
}
