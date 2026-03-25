package builderb0y.vertigo;

import net.minecraft.entity.Entity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;

public class VersionUtil {

	public static int getViewDistance(ServerPlayerEntity player) {
		                           
			return player.getViewDistance();
		     
                                                                  
        
	}

	public static ChunkSection newEmptyChunkSection(DynamicRegistryManager registries) {
		//using an anonymous subclass fixes compatibility with the AntiXray mod.
		                           
			return new ChunkSection(net.minecraft.world.chunk.PalettesFactory.fromRegistryManager(registries)) {};
		                             
                                                                         
       
                                                                  
        
	}

	public static int blockMinYInclusive(HeightLimitView view) {
		return view.getBottomY();
	}

	public static int sectionMinYInclusive(HeightLimitView view) {
		return view.getBottomSectionCoord();
	}

	public static int blockMaxYExclusive(HeightLimitView view) {
		return view.getBottomY() + view.getHeight();
	}

	public static int sectionMaxYExclusive(HeightLimitView view) {
		return blockMaxYExclusive(view) >> 4;
	}

	public static int blockMaxYInclusive(HeightLimitView view) {
		return blockMaxYExclusive(view) - 1;
	}

	public static int sectionMaxYInclusive(HeightLimitView view) {
		return sectionMaxYExclusive(view) - 1;
	}

	public static World getWorld(Entity entity) {
		                           
			return entity.getEntityWorld();
		     
                            
        
	}
}