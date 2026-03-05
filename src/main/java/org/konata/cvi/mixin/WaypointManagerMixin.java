package org.konata.cvi.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import org.konata.cvi.interfaces.WaypointExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.BufferedReader;
import java.io.File;
import java.util.TreeSet;

/**
 * @author IzumiiKonata
 * Date: 2026/3/5 20:42
 */
@Mixin(WaypointManager.class)
public class WaypointManagerMixin {

    private static final ThreadLocal<Boolean> CVI_FLAG =
            ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "loadWaypointsExtensible",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;trim()Ljava/lang/String;",
                    ordinal = 1,
                    shift = At.Shift.BY,
                    by = 2
            )
    )
    private void loadWaypointsExtensible(String worldNameStandard, CallbackInfoReturnable<Boolean> cir, @Local(name = "key") String key, @Local(name = "value") String value) {
        if (key.equals("cvi")) {
            CVI_FLAG.set(value.equals("true"));
        }
    }

    @Inject(method = "loadWaypoint", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;contains(Ljava/lang/Object;)Z"))
    private void loadWaypoint(String name, int x, int z, int y, boolean enabled, float red, float green, float blue, String suffix, String world, TreeSet<DimensionContainer> dimensions, CallbackInfo ci, @Local(name = "newWaypoint") Waypoint wp) {
        if (CVI_FLAG.get()) {
            ((WaypointExt) wp).cvi$setCVIWaypoint(true);
            CVI_FLAG.remove();
        }
    }

    @ModifyArg(
            method = "saveWaypoints",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V",
                    ordinal = 3
            )
    )
    private String saveWaypoints(String s, @Local(name = "pt") Waypoint pt) {
        s += ",cvi:" + ((WaypointExt) pt).cvi$isCVIWaypoint();
        return s;
    }

}
