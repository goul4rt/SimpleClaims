package com.buuz135.simpleclaims.systems.tick;

import com.buuz135.simpleclaims.Main;
import com.buuz135.simpleclaims.claim.ClaimManager;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapLoadException;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WorldMapUpdateTickingSystem extends TickingSystem<ChunkStore> {

    private int tick = 0;

    public WorldMapUpdateTickingSystem() {

    }

    @Override
    public void tick(float v, int i, @NonNullDecl Store<ChunkStore> store) {
        ++tick;
        if (tick > 20 * 5) { //every 5 seconds
            if (ClaimManager.getInstance().needsMapUpdate()) {
                for (World world : Main.WORLDS.values()) {
                    if (ClaimManager.getInstance().getMapUpdateQueue().containsKey(world.getName())) {
                        world.getWorldMapManager().clearImagesInChunks(ClaimManager.getInstance().getMapUpdateQueue().get(world.getName()));
                        for (PlayerRef playerRef : world.getPlayerRefs()) {
                            var player = world.getEntityStore().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
                            player.getWorldMapTracker().clearChunks(ClaimManager.getInstance().getMapUpdateQueue().get(world.getName()));
                        }
                    }
                }
                ClaimManager.getInstance().getMapUpdateQueue().clear();
                ClaimManager.getInstance().setNeedsMapUpdate(false);
            }
            tick = 0;
        }
    }



}
