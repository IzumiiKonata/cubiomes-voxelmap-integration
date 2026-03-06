package org.konata.cvi.cubiomes;

import cubiomes.ffm.*;
import lombok.experimental.UtilityClass;
import org.konata.cvi.EnumStructure;
import org.konata.cvi.cubiomes.datatypes.ExtGenConfig;
import org.konata.cvi.cubiomes.datatypes.VarPos;
import org.konata.cvi.cubiomes.datatypes.WorldInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import static cubiomes.ffm.Cubiomes.MASK48;

/**
 * @author IzumiiKonata
 * Date: 2026/3/4 21:21
 */
@UtilityClass
public class CubiomesUtils {

    static ExtGenConfig g_extgen = new ExtGenConfig();

    public int getStructureConfig_override(
            int stype,
            int mc,
            /* Type: StructureConfig */
            MemorySegment sconf
    ) {
        if (mc == Integer.MAX_VALUE)
            mc = 0;

        int ok = Cubiomes.getStructureConfig(stype, mc, sconf);
        if (ok != 0 && g_extgen.saltOverride)
        {
            int salt = (int) g_extgen.salts[stype];
            if (salt <= Cubiomes.MASK48())
                cubiomes.ffm.StructureConfig.salt(sconf, salt);;
        }
        return ok;
    }

    public void getStructs(
            Arena arena,
            List<VarPos> out,
            /* Type: StructureConfig */
            final MemorySegment sconf,
            EnumStructure structure,
            WorldInfo wi,
            int dim,
            int x0, int z0,
            int x1, int z1,
            boolean nogen
    ) {
        int si0 = Math.floorDiv(x0, StructureConfig.regionSize(sconf) * 16);
        int sj0 = Math.floorDiv(z0, StructureConfig.regionSize(sconf) * 16);
        int si1 = Math.floorDiv((x1-1), StructureConfig.regionSize(sconf) * 16);
        int sj1 = Math.floorDiv((z1-1), StructureConfig.regionSize(sconf) * 16);

        // Type: Generator
        MemorySegment g = Generator.allocate(arena);

        if (!nogen) {
            Cubiomes.setupGenerator(g, wi.mc, wi.large ? 1 : 0);
            Cubiomes.applySeed(g, dim, wi.seed);
        }

        for (int i = si0; i <= si1; i++)
        {
            for (int j = sj0; j <= sj1; j++)
            {
                // Type: Pos
                MemorySegment p = Pos.allocate(arena);
                int ok = Cubiomes.getStructurePos(StructureConfig.structType(sconf), wi.mc, wi.seed, i, j, p);
                if (ok == 0)
                    continue;

                if (Pos.x(p) >= x0 && Pos.x(p) < x1 && Pos.z(p) >= z0 && Pos.z(p) < z1)
                {
                    VarPos vp = new VarPos(arena, p, StructureConfig.structType(sconf), structure);
                    if (nogen)
                    {
                        out.add(vp);
                        continue;
                    }
                    int id = Cubiomes.isViableStructurePos(StructureConfig.structType(sconf), g, Pos.x(p), Pos.z(p), 0);
                    if (id == 0)
                        continue;

                    // Type: Piece[1024]
                    MemorySegment pieces = Piece.allocateArray(1024, arena);

                    if (StructureConfig.structType(sconf) == Cubiomes.End_City())
                    {
                        // Type: SurfaceNoise
                        MemorySegment sn = SurfaceNoise.allocate(arena);
                        Cubiomes.initSurfaceNoise(sn, Cubiomes.DIM_END(), wi.seed);
                        int y = Cubiomes.isViableEndCityTerrain(g, sn, Pos.x(p), Pos.z(p));
                        if (y == 0)
                            continue;

                        int n = Cubiomes.getEndCityPieces(pieces, wi.seed, Pos.x(p) >> 4, Pos.z(p) >> 4);

                        if (n != 0)
                        {
                            vp.pieces.clear();
                            for (int sz = 0; sz < n; sz++) {
                                MemorySegment slice =
                                        pieces.asSlice(sz * Piece.sizeof(), Piece.sizeof());
                                vp.pieces.add(slice);
                            }

                            if (structure == EnumStructure.END_CITY_WITH_SHIP) {
                                boolean hasShip = false;

                                for (MemorySegment piece : vp.pieces) {
                                    if (Piece.name(piece).getString(0).equals("ship")) {
                                        hasShip = true;
                                        break;
                                    }
                                }

                                if (!hasShip)
                                    continue;
                            }

//                            vp.pieces.assign(pieces, pieces + n);
                            cubiomes.ffm.Pos3.y(Piece.bb0(vp.pieces.getFirst()), y); // height of end city pieces are relative to surface
                        }
                    }
                    else if (StructureConfig.structType(sconf) == Cubiomes.Ruined_Portal() || StructureConfig.structType(sconf) == Cubiomes.Ruined_Portal_N())
                    {
                        id = Cubiomes.getBiomeAt(g, 4, (Pos.x(p) >> 2) + 2, 0, (Pos.z(p) >> 2) + 2);
                    }
                    else if (StructureConfig.structType(sconf) == Cubiomes.Fortress())
                    {
                        int n = Cubiomes.getFortressPieces(pieces, 1024,
                                wi.mc, wi.seed, Pos.x(p) >> 4, Pos.z(p) >> 4);
                        vp.pieces.clear();
                        for (int sz = 0; sz < n; sz++) {
                            MemorySegment slice =
                                    pieces.asSlice(sz * Piece.sizeof(), Piece.sizeof());
                            vp.pieces.add(slice);
                        }
                    }
                    else if (Generator.mc(g) >= Cubiomes.MC_1_18())
                    {
                        if (g_extgen.estimateTerrain &&
                                Cubiomes.isViableStructureTerrain(StructureConfig.structType(sconf), g, Pos.x(p), Pos.z(p)) == 0)
                        {
                            continue;
                        }
                    }

                    Cubiomes.getVariant(vp.v, StructureConfig.structType(sconf), wi.mc, wi.seed, Pos.x(p), Pos.z(p), id);
                    out.add(vp);
                }
            }
        }
    }

    public static void loadNatives() {
        File libFile;
        String libFileName = "/libcubiomes.dll";

        try {
            libFile = File.createTempFile("lib", null);
            libFile.deleteOnExit();
        } catch (IOException iOException) {
            throw new UnsatisfiedLinkError("Failed to create temp file");
        }

        byte[] arrayOfByte = new byte[2048];

        try {
            InputStream inputStream = CubiomesUtils.class.getResourceAsStream(libFileName);

            if (inputStream == null) {
                throw new UnsatisfiedLinkError(String.format("Failed to open lib file: %s", libFileName));
            }

            try (FileOutputStream fileOutputStream = new FileOutputStream(libFile)) {
                int size;
                while ((size = inputStream.read(arrayOfByte)) != -1) {
                    fileOutputStream.write(arrayOfByte, 0, size);
                }
            } catch (Throwable throwable) {
                try {
                    inputStream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }

        } catch (IOException exception) {
            throw new UnsatisfiedLinkError(String.format("Failed to copy file: %s", exception.getMessage()));
        }

        try {
            System.load(libFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

}
