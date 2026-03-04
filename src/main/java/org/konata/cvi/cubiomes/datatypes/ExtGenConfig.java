package org.konata.cvi.cubiomes.datatypes;

import cubiomes.ffm.Cubiomes;

/**
 * @author IzumiiKonata
 * Date: 2026/3/4 22:12
 */
public class ExtGenConfig {

    public boolean experimentalVers, estimateTerrain, saltOverride;
    public long[] salts;

    public ExtGenConfig() {
        salts = new long[Cubiomes.FEATURE_NUM()];
        this.reset();
    }

    public void reset() {
        experimentalVers = false;
        estimateTerrain = true;
        saltOverride = false;
        for (int i = 0; i < Cubiomes.FEATURE_NUM(); i++)
            salts[i] = ~(long)0;
    }

}
