package cn.mcbowser.client.render;

import de.keksuccino.rinku.RinkuBrowser;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class DisplayPanelRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public int width;
    public int height;
    public @Nullable RinkuBrowser browser;
}
