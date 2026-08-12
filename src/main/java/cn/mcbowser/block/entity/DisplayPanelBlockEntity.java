package cn.mcbowser.block.entity;

import cn.mcbowser.McBowser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class DisplayPanelBlockEntity extends BlockEntity {
    private boolean displayEnabled;
    private String browserUrl = "https://www.bilibili.com";
    private String compatibleMediaUrl = "";
    private long mediaStartedAt;

    public DisplayPanelBlockEntity(BlockPos pos, BlockState state) {
        super(McBowser.DISPLAY_PANEL_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isDisplayEnabled() { return displayEnabled; }
    public String getBrowserUrl() { return browserUrl; }
    public String getCompatibleMediaUrl() { return compatibleMediaUrl; }
    public long getMediaStartedAt() { return mediaStartedAt; }

    public void setDisplayEnabled(boolean displayEnabled) {
        this.displayEnabled = displayEnabled;
        setChangedAndSync();
    }

    public void setBrowserUrl(String browserUrl) {
        this.browserUrl = browserUrl;
        this.compatibleMediaUrl = "";
        this.mediaStartedAt = 0L;
        setChangedAndSync();
    }

    public void setCompatibleMedia(String sourceUrl, long startedAt) {
        this.compatibleMediaUrl = sourceUrl;
        this.mediaStartedAt = startedAt;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        displayEnabled = input.getBooleanOr("DisplayEnabled", false);
        browserUrl = input.getStringOr("BrowserUrl", "https://www.bilibili.com");
        compatibleMediaUrl = input.getStringOr("CompatibleMediaUrl", "");
        mediaStartedAt = input.getLongOr("MediaStartedAt", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("DisplayEnabled", displayEnabled);
        output.putString("BrowserUrl", browserUrl);
        output.putString("CompatibleMediaUrl", compatibleMediaUrl);
        output.putLong("MediaStartedAt", mediaStartedAt);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
