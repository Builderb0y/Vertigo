package builderb0y.vertigo.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.api.VertigoAPI;

@Mixin(ChunkHolder.class)
public class ChunkHolder_FilterSections {

	@ModifyExpressionValue(method = "flushUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkHolder$PlayersWatchingChunkProvider;getPlayersWatchingChunk(Lnet/minecraft/util/math/ChunkPos;Z)Ljava/util/List;", ordinal = 1))
	private List<ServerPlayerEntity> vertigo_storeOriginalList(
		List<ServerPlayerEntity> original,
		@Share("originalPlayerList") LocalRef<List<ServerPlayerEntity>> store
	) {
		store.set(original);
		return original;
	}

	@ModifyVariable(method = "flushUpdates", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 1), index = 3)
	private List<ServerPlayerEntity> vertigo_filterPlayers(
		List<ServerPlayerEntity> current,
		@Share("originalPlayerList") LocalRef<List<ServerPlayerEntity>> original,
		@Local(argsOnly = true) WorldChunk chunk,
		@Local(index = 4) int index
	) {
		List<ServerPlayerEntity> toFilter = original.get();
		if (toFilter.isEmpty()) return toFilter;
		List<ServerPlayerEntity> newList = null;
		for (ServerPlayerEntity player : toFilter) {
			if (
				VertigoAPI.isSectionLoaded(
					player,
					chunk.getPos().x,
					chunk.sectionIndexToCoord(index),
					chunk.getPos().z
				)
			) {
				if (newList == null) newList = new ArrayList<>(toFilter.size());
				newList.add(player);
			}
		}
		return newList != null ? newList : Collections.emptyList();
	}
}