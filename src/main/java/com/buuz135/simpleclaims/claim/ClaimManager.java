package com.buuz135.simpleclaims.claim;

import com.buuz135.simpleclaims.FileUtils;
import com.buuz135.simpleclaims.claim.chunk.ChunkInfo;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyInfoStorage;
import com.buuz135.simpleclaims.claim.tracking.ModifiedTracking;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.util.BsonUtil;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;

public class ClaimManager {

    private static final ClaimManager INSTANCE = new ClaimManager();

    private HashMap<String, PartyInfo> parties;
    private HashMap<String, HashMap<String, ChunkInfo>> chunks;
    private boolean needsMapUpdate;
    private boolean isDirty;
    private Thread savingThread;
    private HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("SimpleClaims");

    public static ClaimManager getInstance() {
        return INSTANCE;
    }

    private ClaimManager() {
        this.parties = new HashMap<>();
        this.chunks = new HashMap<>();
        this.needsMapUpdate = false;
        this.isDirty = false;

        FileUtils.ensureMainDirectory();

        try {
            var partyPath = FileUtils.ensureFile(FileUtils.PARTY_PATH, "{}");
            var loadedParties = RawJsonReader.readSync(partyPath.toPath(), PartyInfoStorage.CODEC, HytaleLogger.getLogger());
            for (PartyInfo party : loadedParties.infos()) {
                this.parties.put(party.getId().toString(), party);
            }
        } catch (IOException e) {
            logger.at(Level.SEVERE).log("LOADING PARTY FILE ERROR");
            logger.at(Level.SEVERE).log(e.getMessage());
            //throw new RuntimeException(e);
            // TODO Create the file again
        }

        try {
            var claimPath = FileUtils.ensureFile(FileUtils.CLAIM_PATH, "{}");
            var loadedClaims = RawJsonReader.readSync(claimPath.toPath(), ChunkInfo.DimensionStorage.CODEC, HytaleLogger.getLogger());
            for (ChunkInfo.ChunkInfoStorage chunkInfoStorage : loadedClaims.getChunkInfoStorages()) {
                var hashMap = new HashMap<String, ChunkInfo>();
                for (ChunkInfo chunkInfo : chunkInfoStorage.getChunkInfos()) {
                    hashMap.put(ChunkInfo.formatCoordinates(chunkInfo.getChunkX(), chunkInfo.getChunkZ()), chunkInfo);
                }
                this.chunks.put(chunkInfoStorage.getDimension(), hashMap);
            }
        } catch (IOException e) {
            logger.at(Level.SEVERE).log("LOADING CLAIM FILE ERROR");
            logger.at(Level.SEVERE).log(e.getMessage());
            //throw new RuntimeException(e);
            // TODO Create the file again
        }

        this.savingThread = new Thread(() -> {
            while (true) {
                if (isDirty) {
                    isDirty = false;
                    logger.at(Level.INFO).log("Saving data...");
                    FileUtils.ensureMainDirectory();

                    try {
                        var partyPath = FileUtils.ensureFile(FileUtils.PARTY_PATH, "{}");
                        BsonUtil.writeSync(partyPath.toPath(), PartyInfoStorage.CODEC, new PartyInfoStorage(this.parties.values().toArray(new PartyInfo[0])), HytaleLogger.getLogger());
                    } catch (IOException e) {
                        logger.at(Level.SEVERE).log(e.getMessage());
                    }

                    try {
                        var claimPath = FileUtils.ensureFile(FileUtils.CLAIM_PATH, "{}");
                        var dimensionalStorages = new ArrayList<ChunkInfo.ChunkInfoStorage>();
                        this.chunks.forEach((dimension, chunkInfos) -> dimensionalStorages.add(new ChunkInfo.ChunkInfoStorage(dimension, chunkInfos.values().toArray(new ChunkInfo[0]))));
                        BsonUtil.writeSync(claimPath.toPath(), ChunkInfo.DimensionStorage.CODEC, new ChunkInfo.DimensionStorage(dimensionalStorages.toArray(new ChunkInfo.ChunkInfoStorage[0])), HytaleLogger.getLogger());
                    } catch (IOException e) {
                        logger.at(Level.SEVERE).log(e.getMessage());
                    }
                    logger.at(Level.INFO).log("Finished saving data... Eepy time...");
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.at(Level.SEVERE).log("SAVING THREAD ERROR");
                    logger.at(Level.SEVERE).log(e.getMessage());
                }
            }
        });
        this.savingThread.start();

        markDirty();
    }

    public void markDirty() {
        this.isDirty = true;
        setNeedsMapUpdate(true);
    }

    public void addParty(PartyInfo partyInfo){
        this.parties.put(partyInfo.getId().toString(), partyInfo);
    }

    public boolean isAllowedToInteract(Player player, String dimension, int chunkX, int chunkZ, Predicate<PartyInfo> interactMethod){
        // Check if the chunk is claimed and yf the chunk is not claimed return true
        var chunkInfo = getChunkRawCoords(dimension, chunkX, chunkZ);
        if (chunkInfo == null) return true;

        // Check the chunk party interact restrictions and check if the interact method is allowed
        var chunkParty = getPartyById(chunkInfo.getPartyOwner());
        if (chunkParty == null || interactMethod.test(chunkParty)) return true;

        // Get the player party, if the player doesn't have a party then it can't be a member of the claimed chunk party
        var partyFromPlayer = getPartyFromPlayer(player);
        if (partyFromPlayer == null) return false;

        // Check if its the same party
        return chunkInfo.getPartyOwner().equals(partyFromPlayer.getId());
    }

    @Nullable
    public PartyInfo getPartyFromPlayer(Player player){
        return this.parties.values().stream().filter(partyInfo -> partyInfo.isOwnerOrMember(player.getUuid())).findFirst().orElse(null);
    }

    @Nullable
    public PartyInfo getPartyFromPlayer(UUID player){
        return this.parties.values().stream().filter(partyInfo -> partyInfo.isOwnerOrMember(player)).findFirst().orElse(null);
    }

    @Nullable
    public PartyInfo getPartyById(UUID partyId){
        return this.parties.get(partyId.toString());
    }

    public PartyInfo createParty(Player owner){
        var party = new PartyInfo(UUID.randomUUID(), owner.getUuid(), owner.getDisplayName() + "'s Party", owner.getDisplayName() + "'s Party Description", new UUID[0], Color.getHSBColor(new Random().nextFloat(), 1, 1).getRGB());
        party.addMember(owner.getUuid());
        party.setCreatedTracked(new ModifiedTracking(owner.getUuid(), owner.getDisplayName(), LocalDateTime.now().toString()));
        party.setModifiedTracked(new ModifiedTracking(owner.getUuid(), owner.getDisplayName(), LocalDateTime.now().toString()));
        this.parties.put(party.getId().toString(), party);
        this.markDirty();
        return party;
    }

    @Nullable
    public ChunkInfo getChunk(String dimension, int chunkX, int chunkZ){
        var chunkInfo = this.chunks.computeIfAbsent(dimension, k -> new HashMap<>());
        var formattedChunk = ChunkInfo.formatCoordinates(chunkX, chunkZ);
        return chunkInfo.getOrDefault(formattedChunk, null);
    }

    @Nullable
    public ChunkInfo getChunkRawCoords(String dimension, int blockX, int blockZ){
        return this.getChunk(dimension, ChunkUtil.chunkCoordinate(blockX), ChunkUtil.chunkCoordinate(blockZ));
    }

    public ChunkInfo claimChunkBy(String dimension, int chunkX, int chunkZ, PartyInfo partyInfo, Player owner){
        var chunkInfo = new ChunkInfo(partyInfo.getId(), chunkX, chunkZ);
        var chunkDimension = this.chunks.computeIfAbsent(dimension, k -> new HashMap<>());
        chunkDimension.put(ChunkInfo.formatCoordinates(chunkX, chunkZ), chunkInfo);
        chunkInfo.setCreatedTracked(new ModifiedTracking(owner.getUuid(), owner.getDisplayName(), LocalDateTime.now().toString()));
        this.markDirty();
        return chunkInfo;
    }

    public ChunkInfo claimChunkByRawCoords(String dimension, int blockX, int blockZ, PartyInfo partyInfo, Player owner){
        return this.claimChunkBy(dimension, ChunkUtil.chunkCoordinate(blockX), ChunkUtil.chunkCoordinate(blockZ), partyInfo, owner);
    }

    public boolean hasEnoughClaimsLeft(PartyInfo partyInfo){
        int maxAmount = 25;
        int currentAmount = 0;
        for (String dimension : this.chunks.keySet()) {
            for (ChunkInfo value : this.chunks.get(dimension).values()) {
                if (value.getPartyOwner().equals(partyInfo.getId())) currentAmount++;
                if (currentAmount >= maxAmount) return false;
            }
        }
        return true;
    }

    public int getAmountOfClaims(PartyInfo partyInfo) {
        int currentAmount = 0;
        for (String dimension : this.chunks.keySet()) {
            for (ChunkInfo value : this.chunks.get(dimension).values()) {
                if (value.getPartyOwner().equals(partyInfo.getId())) currentAmount++;

            }
        }
        return currentAmount;
    }

    public void unclaim(String dimension, int chunkX, int chunkZ){
        this.chunks.computeIfAbsent(dimension, k -> new HashMap<>()).remove(ChunkInfo.formatCoordinates(chunkX, chunkZ));
        this.markDirty();
    }

    public void unclaimRawCoords(String dimension, int blockX, int blockZ){
        this.unclaim(dimension, ChunkUtil.chunkCoordinate(blockX), ChunkUtil.chunkCoordinate(blockZ));
    }

    public boolean needsMapUpdate() {
        return needsMapUpdate;
    }

    public void setNeedsMapUpdate(boolean needsMapUpdate) {
        this.needsMapUpdate = needsMapUpdate;
    }
}
