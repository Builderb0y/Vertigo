package builderb0y.vertigo.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkData.BlockEntityData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.vertigo.VersionUtil;
import builderb0y.vertigo.Vertigo;
import builderb0y.vertigo.compat.ValkyrienSkiesCompat;

@Mixin(ChunkData.class)
public class ChunkData_FilterSections {

	@ModifyReceiver(method = "getSectionsPacketSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;getPacketSize()I"))
	private static ChunkSection vertigo_modifySize(ChunkSection section, @Local(index = 4) int index, @Local(argsOnly = true) WorldChunk chunk) {
		return vertigo_checkY(chunk.getPos(), chunk.sectionIndexToCoord(index)) ? section : Vertigo.EMPTY_SECTION;
	}

	@ModifyReceiver(method = "writeSections", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;toPacket(Lnet/minecraft/network/PacketByteBuf;)V"))
	private static ChunkSection vertigo_modifySection(ChunkSection section, PacketByteBuf buf, @Local(index = 4) int index, @Local(argsOnly = true) WorldChunk chunk) {
		return vertigo_checkY(chunk.getPos(), chunk.sectionIndexToCoord(index)) ? section : Vertigo.EMPTY_SECTION;
	}

	@WrapWithCondition(method = "<init>(Lnet/minecraft/world/chunk/WorldChunk;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean vertigo_filterBlockEntity(List<?> list, Object element, @Local(argsOnly = true) WorldChunk chunk) {
		return vertigo_checkY(chunk.getPos(), ((BlockEntityData)(element)).y >> 4);
	}

	@Unique
	private static boolean vertigo_checkY(ChunkPos chunkPos, int sectionY) {
		if (!ValkyrienSkiesCompat.isInShipyard(chunkPos)) {
			ServerPlayerEntity player = Vertigo.SYNCING_PLAYER.get();
			if (player != null) {
				int playerSectionY = player.getBlockY() >> 4;
				int playerViewDistance = VersionUtil.getViewDistance(player);
				return Math.abs(sectionY - playerSectionY) <= playerViewDistance;
			}
		}
		return true;
	}
}