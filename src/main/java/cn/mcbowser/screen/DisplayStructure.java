package cn.mcbowser.screen;

import cn.mcbowser.McBowser;
import cn.mcbowser.block.DisplayPanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record DisplayStructure(BlockPos origin, Direction facing, int width, int height) {
    public static final int MIN_WIDTH = 2;
    public static final int MIN_HEIGHT = 2;
    public static final int MAX_WIDTH = 16;
    public static final int MAX_HEIGHT = 9;

    public static DisplayStructure find(Level level, BlockPos clicked) {
        BlockState clickedState = level.getBlockState(clicked);
        if (!clickedState.is(McBowser.DISPLAY_PANEL.get())) return null;

        Direction facing = clickedState.getValue(DisplayPanelBlock.FACING);
        Direction right = facing.getClockWise();
        BlockPos bottomLeft = clicked;
        while (matches(level, bottomLeft.relative(right.getOpposite()), facing)) bottomLeft = bottomLeft.relative(right.getOpposite());
        while (matches(level, bottomLeft.below(), facing)) bottomLeft = bottomLeft.below();

        int width = 0;
        while (width < MAX_WIDTH && matches(level, bottomLeft.relative(right, width), facing)) width++;
        int height = 0;
        while (height < MAX_HEIGHT && matches(level, bottomLeft.above(height), facing)) height++;
        if (width < MIN_WIDTH || height < MIN_HEIGHT) return null;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!matches(level, bottomLeft.relative(right, x).above(y), facing)) return null;
            }
        }
        return new DisplayStructure(bottomLeft.immutable(), facing, width, height);
    }

    private static boolean matches(Level level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.is(McBowser.DISPLAY_PANEL.get()) && state.getValue(DisplayPanelBlock.FACING) == facing;
    }
}
