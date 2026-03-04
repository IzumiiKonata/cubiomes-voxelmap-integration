package org.konata.cvi.cubiomes.datatypes;

import cubiomes.ffm.Cubiomes;

/**
 * @author IzumiiKonata
 * Date: 2026/3/4 21:53
 */
public class WorldInfo {

    public int mc;
    public boolean large;
    public long seed;
    public int y;

    public WorldInfo() {
        this.reset();
    }

    public void reset() {
        this.mc = Cubiomes.MC_NEWEST();
        this.large = false;
        this.seed = 0;
        this.y = 255;
    }

}
