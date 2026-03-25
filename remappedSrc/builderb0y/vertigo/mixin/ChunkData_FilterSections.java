package builderb0y.vertigo.mixin;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.vertigo.VersionUtil;
import builderb0y.vertigo.VertigoInternals;
import builderb0y.vertigo.compat.ValkyrienSkiesCompat;

@Mixin(ClientboundLevelChunkPacketData.class)
public class ChunkData_FilterSections {

	@ModifyReceiver(method = "calculateChunkSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;getSerializedSize()I"))
	private static LevelChunkSection vertigo_modifySize(LevelChunkSection section, @Local(index = 4) int index, @Local(argsOnly = true) LevelChunk chunk) {
		return vertigo_checkY(chunk.getPos(), chunk.getSectionYFromSectionIndex(index)) ? section : VertigoInternals.EMPTY_SECTION;
	}

	@ModifyReceiver(method = "extractChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;write(Lnet/minecraft/network/FriendlyByteBuf;)V"))
	private static LevelChunkSection vertigo_modifySection(LevelChunkSection section, FriendlyByteBuf buf, @Local(index = 4) int index, @Local(argsOnly = true) LevelChunk chunk) {
		return vertigo_checkY(chunk.getPos(), chunk.getSectionYFromSectionIndex(index)) ? section : VertigoInternals.EMPTY_SECTION;
	}

	@WrapWithCondition(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean vertigo_filterBlockEntity(List<?> list, Object element, @Local(argsOnly = true) LevelChunk chunk) {
		return vertigo_checkY(chunk.getPos(), ((BlockEntityInfo)(element)).y >> 4);
	}

	@Unique
	private static boolean vertigo_checkY(ChunkPos chunkPos, int sectionY) {
		if (!ValkyrienSkiesCompat.isInShipyard(chunkPos)) {
			ServerPlayer player = VertigoInternals.SYNCING_PLAYER.get();
			if (player != null) {
				int playerSectionY = player.getBlockY() >> 4;
				int playerViewDistance = VersionUtil.getViewDistance(player);
				return Math.abs(sectionY - playerSectionY) <= playerViewDistance;
			}
		}
		return true;
	}
}