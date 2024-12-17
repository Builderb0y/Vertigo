package builderb0y.vertigo.api;

import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import builderb0y.vertigo.VerticalTrackingManager;

public class VertigoAPI {

	/**
	returns true if the packet containing the blocks for the provided
	section has been sent to the provided player, false otherwise.

	@apiNote minecraft will delay some chunks to prevent timeouts since 1.21.
	as such, even if the section is within range of the player,
	it is not guaranteed to be loaded on the client yet.

	aldo, vertigo's unload distance is one section bigger than its load distance.
	as such, even if the section is outside the range of the player,
	it is not guaranteed to be unloaded yet either.

	if this method is called on the logical client with the main client player entity,
	then this method returns true if the packet containing the
	blocks for the provided section has been received and processed.

	if this method is called on the logical client with a different player
	(for example, another player in multiplayer), then this method returns false.

	this method is not thread-safe, and should ONLY be called from the render thread or the server thread.
	*/
	public static boolean isSectionLoaded(PlayerEntity player, int sectionX, int sectionY, int sectionZ) {
		if (player.getWorld().isClient) {
			return isSectionLoadedClient(player, sectionX, sectionY, sectionZ);
		}
		else {
			VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.get(player);
			return manager != null && manager.isLoaded(sectionX, sectionY, sectionZ);
		}
	}

	@Environment(EnvType.CLIENT)
	private static boolean isSectionLoadedClient(PlayerEntity player, int sectionX, int sectionY, int sectionZ) {
		if (player == MinecraftClient.getInstance().player) {
			return VerticalTrackingManager.CLIENT.isLoaded(sectionX, sectionY, sectionZ);
		}
		else {
			return false;
		}
	}

	public static boolean isSectionLoaded(PlayerEntity player, ChunkSectionPos pos) {
		return isSectionLoaded(player, pos.getSectionX(), pos.getSectionY(), pos.getSectionZ());
	}

	public static boolean isBlockLoaded(PlayerEntity player, int blockX, int blockY, int blockZ) {
		return isSectionLoaded(player, blockX >> 4, blockY >> 4, blockZ >> 4);
	}

	public static boolean isBlockLoaded(PlayerEntity player, BlockPos pos) {
		return isBlockLoaded(player, pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	returns a Stream containing all players who know what
	blocks are in the chunk section at the provided coordinates.
	*/
	public static Stream<ServerPlayerEntity> getPlayersTrackingSection(ServerWorld world, int sectionX, int sectionY, int sectionZ) {
		if (VerticalTrackingManager.PLAYERS.isEmpty()) return Stream.empty();
		List<ServerPlayerEntity> players = world.getPlayers();
		if (players.isEmpty()) return Stream.empty();
		return players.stream().filter((ServerPlayerEntity player) -> {
			VerticalTrackingManager manager = VerticalTrackingManager.PLAYERS.get(player);
			return manager != null && manager.isLoaded(sectionX, sectionY, sectionZ);
		});
	}

	public static Stream<ServerPlayerEntity> getPlayersTrackingSection(ServerWorld world, ChunkSectionPos pos) {
		return getPlayersTrackingSection(world, pos.getSectionX(), pos.getSectionY(), pos.getSectionZ());
	}

	public static Stream<ServerPlayerEntity> getPlayersTrackingBlock(ServerWorld world, int blockX, int blockY, int blockZ) {
		return getPlayersTrackingSection(world, blockX >> 4, blockY >> 4, blockZ >> 4);
	}

	public static Stream<ServerPlayerEntity> getPlayersTrackingBlock(ServerWorld world, BlockPos pos) {
		return getPlayersTrackingBlock(world, pos.getX(), pos.getY(), pos.getZ());
	}
}