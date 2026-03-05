package org.konata.cvi.mixin;

import com.mamiyaotaru.voxelmap.util.Waypoint;
import org.konata.cvi.interfaces.WaypointExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Waypoint.class)
public class WaypointMixin implements WaypointExt {

	@Unique
    public boolean cvi$isCVIWaypoint = false;

	@Override
	public boolean cvi$isCVIWaypoint() {
		return cvi$isCVIWaypoint;
	}

	@Override
	public void cvi$setCVIWaypoint(boolean cviWaypoint) {
		cvi$isCVIWaypoint = cviWaypoint;
	}
}