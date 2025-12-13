package com.buuz135.simpleclaims.claim.tracking;

import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverride;
import com.buuz135.simpleclaims.codecs.CustomCodecs;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.UUID;

public class ModifiedTracking {

    public static final BuilderCodec<ModifiedTracking> CODEC = BuilderCodec.builder(ModifiedTracking.class, ModifiedTracking::new)
            .append(new KeyedCodec<>("UserUUID", Codec.UUID_STRING),
                    (modifiedTracking, id, extraInfo) -> modifiedTracking.user_uuid = id,
                    (modifiedTracking, extraInfo) -> modifiedTracking.user_uuid).add()
            .append(new KeyedCodec<>("UserName", Codec.STRING),
                    (modifiedTracking, string, extraInfo) -> modifiedTracking.user_name = string,
                    (modifiedTracking, extraInfo) -> modifiedTracking.user_name).add()
            .append(new KeyedCodec<>("Date", Codec.STRING),
                    (modifiedTracking, string, extraInfo) -> modifiedTracking.date = string,
                    (modifiedTracking, extraInfo) -> modifiedTracking.date).add()
            .build();

    private UUID user_uuid;
    private String user_name;
    private String date;

    public ModifiedTracking(UUID user_uuid, String user_name, String date) {
        this.user_uuid = user_uuid;
        this.user_name = user_name;
        this.date = date;
    }

    public ModifiedTracking() {
        this(UUID.randomUUID(), "-", "");
    }
}
