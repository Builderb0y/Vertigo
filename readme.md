By default, Minecraft keeps entire chunks synced with players, which is problematic when using mods or data packs which make the world taller. Big Globe for example makes the overworld 2048 blocks tall. The reason why this is problematic is two-fold:
1. It means there's a lot more data to sync from the server to the client in multiplayer, which increases bandwidth requirements.
2. It means there's more blocks the client needs to keep track of, which increases memory usage.

Minecraft started delaying chunk syncing in 1.21 to adapt to the player's internet connection, and while this does help a lot,
* A lot of mods are still on MC 1.20.1, and
* The problem is still not solved completely. The player is less likely to time out now, but chunk syncing is overall slower.

I didn't find a mod that can solve all of the above problems during my brief search on Modrinth, so I made my own.

Vertigo attempts to solve these issues by only syncing chunk sections that are near the player vertically. In other words, by lying to the client about what blocks are where. If a chunk section is too far above or below the player, the server will tell the client that the section is empty. Or in other words, full of air.

# Mod compatibility

Because the client does not know about all the blocks in a chunk, it is expected that other mods may try to sync data in sections the client doesn't know about, with undefined results. To help reduce some of these problems, Vertigo does the following:
* Overwrite `PlayerLookup.tracking(ServerWorld, BlockPos)` in Fabric's networking API to not include players which are out of vertical range of the input position. If your mod uses this method to send update packets to players, it will probably work just fine.
	* `PlayerLookup.tracking(BlockEntity)` delegates to `tracking(ServerWorld, BlockPos)`, so this tracking method should work just fine too.
	* All other tracking methods are unchanged.
* Include an API which allows:
	* Querying whether or not a specific player knows about a specific chunk section.
	* Listing players which know about a specific chunk section.

Despite this, I do expect some mods to have issues still. If you are an end user, please do not expect this mod to work out-of-the-box with every other mod just yet. Some mods will need to make changes in order to work with Vertigo. If you are a mod developer and think that a conflict is on my end, feel free to open an issue about it.

# Q&A

Q: Does it help with vanilla/non-tall worlds?  
A: Not really, unless you have a very small view distance.

Q: Does this mod need to be installed on both the client and the server to work?  
A: Yes.

Q: Will things break if I only have it installed on one side?  
A: Hopefully not.

Q: Does Vertigo affect any server-side mechanics? Like mob spawning or block ticking?  
A: No.

Q: What counts as "near the player vertically"?  
A: The vertical sync distance is the same as the horizontal sync distance, which is the same as your view distance (or the server's view distance; whichever one is smaller).