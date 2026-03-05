package org.konata.cvi.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import org.konata.cvi.CubiomesVoxelmapIntegration;
import org.konata.cvi.interfaces.WaypointExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @author IzumiiKonata
 * Date: 2026/3/5 20:30
 */
@Mixin(targets = "com.mamiyaotaru.voxelmap.gui.GuiSlotWaypoints")
public class GuiSlotWaypointsMixin {

    @ModifyExpressionValue(method = "updateFilter", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
    public boolean updateFilter(boolean original, String filter, @Local(name = "waypoint") Waypoint waypoint) {
        return original && !shouldHideWaypoint(waypoint);
    }

    @Unique
    private boolean shouldHideWaypoint(Waypoint wp) {
        return !CubiomesVoxelmapIntegration.showCVIWaypoints && ((WaypointExt) wp).cvi$isCVIWaypoint();
    }

}
