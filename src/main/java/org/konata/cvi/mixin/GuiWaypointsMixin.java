package org.konata.cvi.mixin;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.konata.cvi.CubiomesVoxelmapIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author IzumiiKonata
 * Date: 2026/3/5 20:24
 */
@Mixin(GuiWaypoints.class)
public abstract class GuiWaypointsMixin extends GuiScreenMinimap {

//    @Unique
//    private Button showCVIWaypointsButton;

    @Shadow
    protected abstract void sort();

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setFocused(Z)V"))
    public void init(CallbackInfo ci) {
        this.addRenderableWidget((new Button.Builder(Component.literal("Show CVI Waypoints: " + CubiomesVoxelmapIntegration.showCVIWaypoints), (button) -> {
            CubiomesVoxelmapIntegration.showCVIWaypoints = !CubiomesVoxelmapIntegration.showCVIWaypoints;
            button.setMessage(Component.literal("Show CVI Waypoints: " + CubiomesVoxelmapIntegration.showCVIWaypoints));
            this.sort();
        })).bounds(this.getWidth() / 2 + 80 + 74 + 4, this.getHeight() - 28, 74, 20).build());
    }

}
