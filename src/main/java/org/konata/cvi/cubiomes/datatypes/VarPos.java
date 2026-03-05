package org.konata.cvi.cubiomes.datatypes;

import cubiomes.ffm.StructureVariant;
import org.konata.cvi.EnumStructure;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/**
 * @author IzumiiKonata
 * Date: 2026/3/4 21:40
 */
public class VarPos {

    /**
     * Type: Pos
     */
    public MemorySegment pos;
    public int type;

    /**
     * Type: StructureVariant
     */
    public MemorySegment v;

    /**
     * Type: Piece
     */
    public List<MemorySegment> pieces;

    public final EnumStructure structure;

    public VarPos(Arena arena, MemorySegment pos, int type, EnumStructure structure) {
        this.pos = pos;
        this.type = type;
        this.structure = structure;
        this.v = StructureVariant.allocate(arena);
        this.pieces = new ArrayList<>();
    }

}
