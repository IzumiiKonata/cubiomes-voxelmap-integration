package org.konata.cvi;

/**
 * @author IzumiiKonata
 * Date: 2026/3/4 22:56
 */
public enum EnumStructure {

    PILLAGER_OUTPOST,
    MINESHAFT,
    MANSION,
    TEMPLE,
    DESERT_PYRAMID,
    IGLOO,
    SHIPWRECK,
    SWAMP_HUT,
    STRONGHOLD,
    MONUMENT,
    OCEAN_RUIN,
    FORTRESS(false, true, false),
    END_CITY(false, false, true),
    END_CITY_WITH_SHIP(false, false, true),
    BURIED_TREASURE,
    BASTION_REMNANT,
    VILLAGE,
    RUINED_PORTAL,
    RUINED_PORTAL_NETHER(false, true, false),
    ANCIENT_CITY,
    TRAIL_RUINS,
    TRIAL_CHAMBERS;

    public final boolean canGenerateInOverworld, canGenerateInNether, canGenerateInEnd;

    EnumStructure() {
        this(true, false, false);
    }

    EnumStructure(boolean canGenerateInOverworld, boolean canGenerateInNether, boolean canGenerateInEnd) {
        this.canGenerateInOverworld = canGenerateInOverworld;
        this.canGenerateInNether = canGenerateInNether;
        this.canGenerateInEnd = canGenerateInEnd;
    }

}
