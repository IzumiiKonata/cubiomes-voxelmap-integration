package org.konata.cvi;

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
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.konata.cvi.cubiomes.CubiomesUtils;
import org.konata.cvi.cubiomes.datatypes.VarPos;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
                        // /cvi set_seed <seed>
                        .then(
                            ClientCommandManager
                                .literal("set_seed")
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
                );
        });
    }

    private int setSeed(CommandContext<FabricClientCommandSource> ctx) {
        long seed = LongArgumentType.getLong(ctx, "seed");
        ctx.getSource().sendFeedback(Component.literal("Set seed to " + seed));
        CubiomesVoxelmapIntegration.worldInfo.seed = seed;

        return 1;
    }

    private int findStructure(CommandContext<FabricClientCommandSource> ctx, int range) {

        if (CubiomesVoxelmapIntegration.worldInfo.seed == 0) {
            ctx.getSource().sendFeedback(Component.literal(ChatFormatting.RED + "Seed not set! Use /cvi seed <seed> to set seed"));
            return 0;
        }

        String structure = StringArgumentType.getString(ctx, "structure");
        EnumStructure structureEnum = EnumStructure.valueOf(structure.toUpperCase());

        try (Arena arena = Arena.ofConfined()) {
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

            CubiomesUtils.getStructs(arena, pos, structureConfig, CubiomesVoxelmapIntegration.worldInfo, Cubiomes.DIM_OVERWORLD(), fromX, fromZ, toX, toZ, false);

            ctx.getSource().sendFeedback(Component.literal("Found " + pos.size() + " \"" + structureEnum.name() + "\" within " + range + " chunks"));
            VarPos first = pos.getFirst();

            ctx.getSource().sendFeedback(Component.literal("First: [" + Pos.x(first.pos) + ", " + Pos.z(first.pos) + "]"));

            return 1;
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
            case END_CITY -> Cubiomes.End_City();
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
