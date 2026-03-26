package builderb0y.vertigo.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import builderb0y.vertigo.api.VertigoAPI;

@Mixin(ChunkHolder.class)
public class ChunkHolder_FilterSections {

	@ModifyExpressionValue(method = "broadcastChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder$PlayerProvider;getPlayers(Lnet/minecraft/world/level/ChunkPos;Z)Ljava/util/List;", ordinal = 1))
	private List<ServerPlayer> vertigo_storeOriginalList(
		List<ServerPlayer> original,
		@Share("originalPlayerList") LocalRef<List<ServerPlayer>> store
	) {
		store.set(original);
		return original;
	}

	@ModifyVariable(method = "broadcastChanges", at = @At(value = "CONSTANT", args = "nullValue=true"), index = 3)
	private List<ServerPlayer> vertigo_filterPlayers(
		List<ServerPlayer> current,
		@Share("originalPlayerList") LocalRef<List<ServerPlayer>> original,
		@Local(argsOnly = true) LevelChunk chunk,
		@Local(index = 4) int index
	) {
		List<ServerPlayer> toFilter = original.get();
		if (toFilter.isEmpty()) return toFilter;
		List<ServerPlayer> newList = null;
		for (ServerPlayer player : toFilter) {
			if (
				VertigoAPI.isSectionLoaded(
					player,
					chunk.getPos().x(),
					chunk.getSectionYFromSectionIndex(index),
					chunk.getPos().z()
				)
			) {
				if (newList == null) newList = new ArrayList<>(toFilter.size());
				newList.add(player);
			}
		}
		return newList != null ? newList : Collections.emptyList();
	}
}