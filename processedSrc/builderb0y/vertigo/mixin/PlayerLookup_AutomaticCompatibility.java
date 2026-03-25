package builderb0y.vertigo.mixin;

import java.util.Collection;
import java.util.Objects;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import builderb0y.vertigo.api.VertigoAPI;

@Mixin(PlayerLookup.class)
public class PlayerLookup_AutomaticCompatibility {

	/**
	@author Builderb0y
	@reason attempt to stop other mods from syncing data
	related to blocks that the client doesn't have loaded.

	MODDERS: if you want to sync the data anyway,
	even if the client doesn't have this position loaded,
	use {@link PlayerLookup#tracking(ServerWorld, ChunkPos)} instead.
	*/
	@Overwrite
	public static Collection<ServerPlayerEntity> tracking(ServerWorld world, BlockPos pos) {
		Objects.requireNonNull(world, "The world cannot be null");
		Objects.requireNonNull(pos, "BlockPos cannot be null");
		return VertigoAPI.getPlayersTrackingBlock(world, pos).toList();
	}
}