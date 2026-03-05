package org.konata.cvi;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import cubiomes.ffm.Cubiomes;
import cubiomes.ffm.Pos;
import cubiomes.ffm.StructureConfig;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.konata.cvi.cubiomes.CubiomesUtils;
import org.konata.cvi.cubiomes.datatypes.VarPos;
import org.konata.cvi.interfaces.WaypointExt;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author IzumiiKonata
 * Date: 2026/3/4 22:32
 */
@UtilityClass
public class CVICommand {

    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, cbx) -> {
            dispatcher
                .register(
                    // /cvi
                    ClientCommandManager
                        .literal("cvi")
                        // /cvi setseed <seed>
                        .then(
                            ClientCommandManager
                                .literal("setseed")
                                .then(
                                    ClientCommandManager.argument(
                                        "seed",
                                        LongArgumentType.longArg()
                                    )
                                    .executes(CVICommand::setSeed)
                                )
                        )
                        // /cvi find <structure> [range(in chunks), def: 8]
                        .then(
                            ClientCommandManager
                                .literal("find")
                                .then(
                                    ClientCommandManager
                                        .argument(
                                        "structure",
                                            StringArgumentType.word()
                                        )
                                        .suggests(CVICommand::structureSuggest)
                                        .executes(ctx -> findStructure(ctx, 8))
                                        .then(
                                            ClientCommandManager
                                                .argument(
                                                    "range",
                                                    IntegerArgumentType.integer(1, 2048)
                                                )
                                                .executes(ctx -> findStructure(ctx, IntegerArgumentType.getInteger(ctx, "range")))
                                        )
                                )
                        )
                        // /cvi addwaypoints
                        .then(
                            ClientCommandManager
                                .literal("addwaypoints")
                                .executes(CVICommand::addWaypoints)
                        )
                );
        });
    }

    private static int addWaypoints(CommandContext<FabricClientCommandSource> ctx) {

        if (foundStructures.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal(ChatFormatting.RED + "No structure found!"));
            return 0;
        }

        WaypointManager waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        Identifier dimensionType = Minecraft.getInstance().level.dimension().identifier();

        for (StructureContainer varPos : foundStructures) {
            int x = varPos.x;
            int z = varPos.z;

            if (waypointContains(x, z, dimensionType))
                continue;

            TreeSet<DimensionContainer> dimensions = new TreeSet<>();
            dimensions.add(new DimensionContainer(Minecraft.getInstance().level.dimensionType(), dimensionType.getPath(), dimensionType));
            Waypoint wp = new Waypoint(varPos.structure.name().toLowerCase(), x, z, 64, true, 1, 1, 1, "", "", dimensions);
            ((WaypointExt) wp).cvi$setCVIWaypoint(true);
            waypointManager.addWaypoint(wp);
        }

        return 1;
    }

    private boolean waypointContains(int x, int z, Identifier dimensionType) {
        WaypointManager waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();

        return waypointManager.getWaypoints().stream().anyMatch(waypoint -> waypoint.x == x && waypoint.z == z && waypoint.dimensions.stream().anyMatch(container -> container.Identifier.equals(dimensionType)));
    }

    private int setSeed(CommandContext<FabricClientCommandSource> ctx) {
        long seed = LongArgumentType.getLong(ctx, "seed");
        ctx.getSource().sendFeedback(Component.literal("Set seed to " + seed));
        CubiomesVoxelmapIntegration.worldInfo.seed = seed;

        return 1;
    }

    class StructureContainer {
        public final EnumStructure structure;
        public final int x, z;

        public StructureContainer(EnumStructure structure, int x, int z) {
            this.structure = structure;
            this.x = x;
            this.z = z;
        }
    }

    final List<StructureContainer> foundStructures = new CopyOnWriteArrayList<>();

    private int findStructure(CommandContext<FabricClientCommandSource> ctx, int range) {

        if (CubiomesVoxelmapIntegration.worldInfo.seed == 0) {
            ctx.getSource().sendFeedback(Component.literal(ChatFormatting.RED + "Seed not set! Use /cvi setseed <seed> to set seed"));
            return 0;
        }

        String structure = StringArgumentType.getString(ctx, "structure");
        EnumStructure structureEnum = EnumStructure.valueOf(structure.toUpperCase());

        Holder<DimensionType> dimensionTypeHolder = Minecraft.getInstance().level.dimensionTypeRegistration();

        if (!canStructureGenerateInDim(structureEnum, dimensionTypeHolder)) {
            ctx.getSource().sendFeedback(Component.literal(ChatFormatting.RED + "Structure \"" + structureEnum.name().toLowerCase() + "\" cannot generate in dimension \"" + dimensionTypeHolder.getRegisteredName() + "\"!"));
            return 0;
        }

        Arena arena = Arena.ofShared();
        List<VarPos> pos = new ArrayList<>();

        MemorySegment structureConfig = StructureConfig.allocate(arena);

        if (CubiomesUtils.getStructureConfig_override(mapStructure(structureEnum), CubiomesVoxelmapIntegration.worldInfo.mc, structureConfig) == 0) {
            ctx.getSource().sendFeedback(Component.literal(ChatFormatting.RED + "Structure config not valid!"));
            return 0;
        }

        Vec3 position = Minecraft.getInstance().player.position();
        int x = (int) position.x;
        int z = (int) position.z;

        int fromX = x - range * 16;
        int toX = x + range * 16;
        int fromZ = z - range * 16;
        int toZ = z + range * 16;

        new Thread(() -> {
            try {
                CubiomesUtils.getStructs(arena, pos, structureConfig, structureEnum, CubiomesVoxelmapIntegration.worldInfo, mapDimension(dimensionTypeHolder), fromX, fromZ, toX, toZ, false);

                foundStructures.clear();
                for (VarPos p : pos) {
                    foundStructures.add(new StructureContainer(structureEnum, Pos.x(p.pos), Pos.z(p.pos)));
                }

                Minecraft.getInstance().schedule(() -> {
                    ctx.getSource().sendFeedback(Component.literal("Found " + pos.size() + " \"" + structureEnum.name() + "\" within " + range + " chunks"));

                    if (!foundStructures.isEmpty()) {
                        MutableComponent clickHere = Component.literal("[Click Here]");
                        clickHere.setStyle(
                            Style.EMPTY
                                .withBold(true)
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent.RunCommand("cvi addwaypoints"))
                        );
                        MutableComponent msg = Component.literal("Add them into VoxelMap's waypoints ");
                        ctx.getSource().sendFeedback(msg.append(clickHere));
                    }
                });
            } finally {
                arena.close();
            }
        }).start();

        return 1;
    }

    private int mapDimension(Holder<DimensionType> dimensionTypeHolder) {
        if (dimensionTypeHolder.is(BuiltinDimensionTypes.OVERWORLD)) {
            return Cubiomes.DIM_OVERWORLD();
        } else if (dimensionTypeHolder.is(BuiltinDimensionTypes.NETHER)) {
            return  Cubiomes.DIM_NETHER();
        } else if (dimensionTypeHolder.is(BuiltinDimensionTypes.END)) {
            return Cubiomes.DIM_END();
        } else {
            return Cubiomes.none();
        }
    }

    private boolean canStructureGenerateInDim(EnumStructure structure, Holder<DimensionType> dimensionTypeHolder) {
        if (dimensionTypeHolder.is(BuiltinDimensionTypes.OVERWORLD)) {
            return structure.canGenerateInOverworld;
        } else if (dimensionTypeHolder.is(BuiltinDimensionTypes.NETHER)) {
            return structure.canGenerateInNether;
        } else if (dimensionTypeHolder.is(BuiltinDimensionTypes.END)) {
            return structure.canGenerateInEnd;
        } else {
            return false;
        }
    }

    private int mapStructure(EnumStructure structure) {
        return switch (structure) {
            case PILLAGER_OUTPOST -> Cubiomes.Outpost();
            case MINESHAFT -> Cubiomes.Mineshaft();
            case MANSION -> Cubiomes.Mansion();
            case TEMPLE -> Cubiomes.Jungle_Temple();
            case DESERT_PYRAMID -> Cubiomes.Desert_Pyramid();
            case IGLOO -> Cubiomes.Igloo();
            case SHIPWRECK -> Cubiomes.Shipwreck();
            case SWAMP_HUT -> Cubiomes.Swamp_Hut();
//            case STRONGHOLD -> Cubiomes.placeholder();
            case MONUMENT -> Cubiomes.Monument();
            case OCEAN_RUIN -> Cubiomes.Ocean_Ruin();
            case FORTRESS -> Cubiomes.Fortress();
            case END_CITY, END_CITY_WITH_SHIP -> Cubiomes.End_City();
            case BURIED_TREASURE -> Cubiomes.Treasure();
            case BASTION_REMNANT -> Cubiomes.Bastion();
            case VILLAGE -> Cubiomes.Village();
            case RUINED_PORTAL -> Cubiomes.Ruined_Portal();
            case RUINED_PORTAL_NETHER -> Cubiomes.Ruined_Portal_N();
            case ANCIENT_CITY -> Cubiomes.Ancient_City();
            case TRAIL_RUINS -> Cubiomes.Trail_Ruins();
            case TRIAL_CHAMBERS -> Cubiomes.Trial_Chambers();
        };
    }

    private CompletableFuture<Suggestions> structureSuggest(CommandContext<FabricClientCommandSource> commandSourceStackCommandContext, SuggestionsBuilder suggestionsBuilder) {
        return CompletableFuture.completedFuture(
            Suggestions.merge(
                suggestionsBuilder.getInput(),
                Stream.of(EnumStructure.values())
                    .map(EnumStructure::name)
                    .map(String::toLowerCase)
                    .map(suggestionsBuilder::suggest)
                    .map(SuggestionsBuilder::build)
                    .collect(Collectors.toList())
            )
        );
    }

}
