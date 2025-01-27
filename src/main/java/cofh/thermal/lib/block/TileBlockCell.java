package cofh.thermal.lib.block;

import cofh.core.block.TileBlock4Way;
import cofh.core.tileentity.TileCoFH;
import cofh.lib.tileentity.ITileCallback;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import java.util.function.Supplier;

public class TileBlockCell extends TileBlock4Way {

    public TileBlockCell(Properties builder, Supplier<? extends TileCoFH> supplier) {

        super(builder, supplier);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {

        return true;
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {

        if (state.getLightEmission() > 0) {
            return state.getLightEmission();
        }
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof ITileCallback) {
            return ((ITileCallback) tile).getLightValue();
        }
        return state.getLightEmission();
    }

}
