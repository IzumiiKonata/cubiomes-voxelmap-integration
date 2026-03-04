package org.konata.cvi;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.fabricmc.api.ModInitializer;

import org.konata.cvi.cubiomes.datatypes.WorldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CubiomesVoxelmapIntegration implements ModInitializer {

	public static final String MOD_ID = "cubiomes-voxelmap-integration";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final WorldInfo worldInfo = new WorldInfo();

	@Override
	public void onInitialize() {
		CVICommand.register();
	}

}