package builderb0y.vertigo.mixin;

import java.util.Collection;
import java.util.Objects;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import builderb0y.vertigo.api.VertigoAPI;

@Mixin(PlayerLookup.class)
public class PlayerLookup_AutomaticCompatibility {

	/**
	@author Builderb0y
	@reason attempt to stop other mods from syncing data
	related to blocks that the client doesn't have loaded.

	MODDERS: if you want to sync the data anyway,
	even if the client doesn't have this position loaded,
	use {@link PlayerLookup#tracking(ServerLevel, ChunkPos)} instead.
	*/
	@Overwrite
	public static Collection<ServerPlayer> tracking(ServerLevel world, BlockPos pos) {
		Objects.requireNonNull(world, "The world cannot be null");
		Objects.requireNonNull(pos, "BlockPos cannot be null");
		return VertigoAPI.getPlayersTrackingBlock(world, pos).toList();
	}
}