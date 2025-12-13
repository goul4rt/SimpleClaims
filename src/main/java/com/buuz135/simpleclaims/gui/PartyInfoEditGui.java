package com.buuz135.simpleclaims.gui;

import com.buuz135.simpleclaims.claim.ClaimManager;
import com.buuz135.simpleclaims.claim.party.PartyInfo;
import com.buuz135.simpleclaims.claim.party.PartyOverride;
import com.buuz135.simpleclaims.claim.party.PartyOverrides;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.CustomPageLifetime;
import com.hypixel.hytale.protocol.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.loader.util.ColorUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PartyInfoEditGui extends InteractiveCustomUIPage<PartyInfoEditGui.PartyInfoData> {

    private final PartyInfo info;
    private String name;
    private String description;
    private int requestingConfirmation;

    public PartyInfoEditGui(@NonNullDecl PlayerRef playerRef, PartyInfo info) {
        super(playerRef, CustomPageLifetime.CanDismiss, PartyInfoData.CODEC);
        this.info = info;
        this.name = info.getName();
        this.description = info.getDescription();
        this.requestingConfirmation = -1;
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl PartyInfoData data) {
        super.handleDataEvent(ref, store, data);
        if (data.name != null) {
            this.name = data.name;
        }
        if (data.description != null) {
            this.description = data.description;
        }
        if (data.save != null) {
            this.info.setName(this.name);
            this.info.setDescription(this.description);
            ClaimManager.getInstance().markDirty();
        }
        if (data.cancel != null) {
            this.close();
        }
        if (data.removeButtonAction != null) {
            var split = data.removeButtonAction.split(":");
            var action = split[0];
            var index = Integer.parseInt(split[1]);
            if (action.equals("Click")){
                this.requestingConfirmation = index;
            }
            if (action.equals("Delete")){
                this.info.removeMember(this.info.getMembers()[index]);
                ClaimManager.getInstance().markDirty();
            }
            if (action.equals("PlaceBlocksSetting")){
                this.info.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_PLACE_BLOCKS, new PartyOverride.PartyOverrideValue("bool", !this.info.isBlockPlaceEnabled())));
            }
            if (action.equals("BreakBlocksSetting")){
                this.info.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_BREAK_BLOCKS, new PartyOverride.PartyOverrideValue("bool", !this.info.isBlockBreakEnabled())));
            }
            if (action.equals("InteractBlocksSetting")){
                this.info.setOverride(new PartyOverride(PartyOverrides.PARTY_PROTECTION_INTERACT, new PartyOverride.PartyOverrideValue("bool", !this.info.isBlockInteractEnabled())));
            }
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.build(ref, commandBuilder, eventBuilder, store);
            this.sendUpdate(commandBuilder, eventBuilder, true);
            return;
        }
        if (data.claimColor != null) {
            try {
                this.info.setColor(0xFF000000 | Integer.parseInt(data.claimColor.substring(1,7), 16));
            } catch (NumberFormatException e) {
                System.out.println("Invalid color");
            }

        }
        this.sendUpdate();
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        var player = store.getComponent(ref, PlayerRef.getComponentType());
        var playerCanModify = this.info.isOwner(player.getUuid());
        uiCommandBuilder.append("Pages/SimpleClaimsEditParty.ui");
        uiCommandBuilder.set("#PartyInfo #PartyNameField.Value", this.info.getName());
        uiCommandBuilder.set("#PartyInfo #PartyNameField.IsReadOnly", !playerCanModify);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PartyNameField", EventData.of("@Name", "#PartyNameField.Value"), false);
        uiCommandBuilder.set("#PartyInfo #PartyDescriptionField.Value", this.info.getDescription());
        uiCommandBuilder.set("#PartyInfo #PartyDescriptionField.IsReadOnly", !playerCanModify);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PartyDescriptionField", EventData.of("@Description", "#PartyDescriptionField.Value"), false);
        for (int i = 0; i < this.info.getMembers().length; i++) {
            uiCommandBuilder.append("#MemberEntries", "Pages/PartyMemberListEntry.ui");
            var isOwner = this.info.isOwner(this.info.getMembers()[i]);
            uiCommandBuilder.set("#MemberEntries[" + i + "] #MemberName.Text", ClaimManager.getInstance().getPlayerNameTracker().getPlayerName(this.info.getMembers()[i]));
            uiCommandBuilder.set("#MemberEntries[" + i + "] #MemberRole.Text", isOwner ? "Owner" : "Member");
            if (!isOwner) {
                uiCommandBuilder.set("#MemberEntries[" + i + "] #MemberRole.Background.Color", "#1a8dec83");
                uiCommandBuilder.set("#MemberEntries[" + i + "] #MemberRole.OutlineColor", "#1a8decde");
            }
            if (!playerCanModify || isOwner){
                //uiCommandBuilder.remove("#Members[" + i + "] #RemoveMemberButton");
            } else {
                //uiEventBuilder.addEventBinding(CustomUIEventBindingType.SlotMouseExited, "#Members[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Left:" + i), false);
                if (this.requestingConfirmation == i) {
                    uiCommandBuilder.set("#MemberEntries[" + i + "] #RemoveMemberButton.Text", "Are you sure?");
                    uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#MemberEntries[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Delete:" + i), false);
                    uiEventBuilder.addEventBinding(CustomUIEventBindingType.MouseExited, "#MemberEntries[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Click:-1"), false);
                } else {
                    uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#MemberEntries[" + i + "] #RemoveMemberButton", EventData.of("RemoveButtonAction", "Click:" + i), false);
                }
            }
        }
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveChangesButton", EventData.of("Save", "true"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton", EventData.of("Cancel", "true"), false);

        uiCommandBuilder.set("#PlaceBlocksSetting #CheckBox.Value", this.info.isBlockPlaceEnabled());
        uiCommandBuilder.set("#BreakBlocksSetting #CheckBox.Value", this.info.isBlockBreakEnabled());
        uiCommandBuilder.set("#InteractBlocksSetting #CheckBox.Value",this.info.isBlockInteractEnabled());

        uiCommandBuilder.set("#ClaimColorPickerGroup #ClaimColorPicker.Value", String.format("#%06X", (0xFFFFFF & this.info.getColor())));

        uiCommandBuilder.set("#ClaimedChunksInfo #ClaimedChunksCount.Text", ClaimManager.getInstance().getAmountOfClaims(this.info)+ "");
        uiCommandBuilder.set("#ClaimedChunksInfo #MaxChunksCount.Text",this.info.getMaxClaimAmount() + "");

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PlaceBlocksSetting #CheckBox", EventData.of("RemoveButtonAction", "PlaceBlocksSetting:0"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BreakBlocksSetting #CheckBox", EventData.of("RemoveButtonAction", "BreakBlocksSetting:0"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InteractBlocksSetting #CheckBox", EventData.of("RemoveButtonAction", "InteractBlocksSetting:0"), false);

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ClaimColorPicker", EventData.of("@ClaimColor", "#ClaimColorPicker.Value"), false);
    }

    public static class PartyInfoData {
        static final String KEY_NAME = "@Name";
        static final String KEY_DESCRIPTION = "@Description";
        static final String KEY_SAVE = "Save";
        static final String KEY_CANCEL = "Cancel";
        static final String KEY_REMOVE_BUTTON_ACTION = "RemoveButtonAction";
        static final String KEY_CLAIM_COLOR = "@ClaimColor";
        public static final BuilderCodec<PartyInfoData> CODEC = BuilderCodec.<PartyInfoData>builder(PartyInfoData.class, PartyInfoData::new)
                .addField(new KeyedCodec<>(KEY_NAME, Codec.STRING), (searchGuiData, s) -> searchGuiData.name = s, searchGuiData -> searchGuiData.name)
                .addField(new KeyedCodec<>(KEY_DESCRIPTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.description = s, searchGuiData -> searchGuiData.description)
                .addField(new KeyedCodec<>(KEY_SAVE, Codec.STRING), (searchGuiData, s) -> searchGuiData.save = s, searchGuiData -> searchGuiData.save)
                .addField(new KeyedCodec<>(KEY_CANCEL, Codec.STRING), (searchGuiData, s) -> searchGuiData.cancel = s, searchGuiData -> searchGuiData.cancel)
                .addField(new KeyedCodec<>(KEY_REMOVE_BUTTON_ACTION, Codec.STRING), (searchGuiData, s) -> searchGuiData.removeButtonAction = s, searchGuiData -> searchGuiData.removeButtonAction)
                .addField(new KeyedCodec<>(KEY_CLAIM_COLOR, Codec.STRING), (searchGuiData, s) -> searchGuiData.claimColor = s, searchGuiData -> searchGuiData.claimColor)

                .build();

        private String name;
        private String description;
        private String save;
        private String cancel;
        private String removeButtonAction;
        private String claimColor;

        @Override
        public String toString() {
            return "PartyInfoData{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", save='" + save + '\'' +
                    ", cancel='" + cancel + '\'' +
                    ", removeButtonAction='" + removeButtonAction + '\'' +
                    ", claimColor='" + claimColor + '\'' +
                    '}';
        }
    }
}
