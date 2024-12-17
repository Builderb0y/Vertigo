By default, minecraft keeps entire chunks synced with players. Which is problematic when using mods or data packs which make the world taller. Big Globe for example makes the overworld 2048 blocks tall. The reason why this is problematic is two-fold:
1. It means there's a lot more data to sync from the server to the client in multiplayer, which increases bandwidth requirements.
2. It means there's more blocks the client needs to keep track of, which increases memory usage.

I didn't find a mod that can solve these problems during my brief search on Modrinth, so I made my own.

Vertigo attempts to solve these issues by only syncing chunk sections that are near the player vertically. In other words, by lying to the client about what blocks are where. If a chunk section is too far above or below the player, the server will tell the client that the section is empty. Or in other words, full of air.

# Mod compatibility

Because the client does not know about all the blocks in a chunk, it is expected that other mods may try to sync data in sections the client doesn't know about, with undefined results. To help reduce some of these problems, Vertigo does the following:
* Overwrite `PlayerLookup.tracking(ServerWorld, BlockPos)` to not include players which are out of vertical range of the input position. If your mod uses this method to send update packets to players, it will probably work just fine.
	* `PlayerLookup.tracking(BlockEntity)` delegates to `tracking(ServerWorld, BlockPos)`, so this tracking method should work just fine too.
	* All other tracking methods are unchanged.
* Include an API which allows:
	* Querying whether or not a specific player knows about a specific chunk section.
	* Listing players which know about a specific chunk section.

Despite this, I do expect some mods to have issues still. If you are an end user, please do not expect this mod to work out-of-the-box with every other mod just yet. Some mods will need to make changes in order to work with Vertigo. If you are a mod developer and think that a conflict is on my end, feel free to open an issue about it.