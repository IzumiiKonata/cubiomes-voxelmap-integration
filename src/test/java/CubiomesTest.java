import cubiomes.ffm.Cubiomes;
import cubiomes.ffm.StructureConfig;
import org.konata.cvi.cubiomes.CubiomesUtils;
import org.konata.cvi.cubiomes.datatypes.VarPos;
import org.konata.cvi.cubiomes.datatypes.WorldInfo;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/**
 * @author IzumiiKonata
 * Date: 2026/3/4 22:17
 */
public class CubiomesTest {

    public static void main(String[] args) {

        try (Arena arena = Arena.ofConfined()) {
            List<VarPos> pos = new ArrayList<>();

            WorldInfo wi = new WorldInfo();
            wi.seed = 1136L;

            MemorySegment structureConfig = StructureConfig.allocate(arena);

            if (CubiomesUtils.getStructureConfig_override(Cubiomes.Village(), wi.mc, structureConfig) == 0)
                return;

            CubiomesUtils.getStructs(arena, pos, structureConfig, wi, Cubiomes.DIM_OVERWORLD(), -23169, -23169, 23169, 23169, false);

            System.out.println(pos.size());
        }

    }

}
