package me.ghosttypes.reaper.util.render;

import me.ghosttypes.reaper.util.world.BlockHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class AutomationUtils {

    public static ArrayList<Vec3d> surroundPositions = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 0, 0));
        add(new Vec3d(-1, 0, 0));
        add(new Vec3d(0, 0, 1));
        add(new Vec3d(0, 0, -1));
    }};


    public static boolean isAnvilBlock(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.ANVIL || BlockHelper.getBlock(pos) == Blocks.CHIPPED_ANVIL || BlockHelper.getBlock(pos) == Blocks.DAMAGED_ANVIL;
    }

    public static boolean isWeb(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.COBWEB || BlockHelper.getBlock(pos) == Block.getBlockFromItem(Items.STRING);
    }


    public static boolean isWebbed(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        if (isWeb(pos)) return true;
        return isWeb(pos.up());
    }

    public static boolean isTrapBlock(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.OBSIDIAN || BlockHelper.getBlock(pos) == Blocks.ENDER_CHEST;
    }

    public static boolean isSurroundBlock(BlockPos pos) {
        //some apes use anchors as surround blocks cus it has the same blast resistance as obsidian
        return BlockHelper.getBlock(pos) == Blocks.OBSIDIAN || BlockHelper.getBlock(pos) == Blocks.ENDER_CHEST || BlockHelper.getBlock(pos) == Blocks.RESPAWN_ANCHOR;
    }

    public static boolean canCrystal(PlayerEntity p) {
        BlockPos tpos = p.getBlockPos();
        for (Vec3d sp : surroundPositions) {
            BlockPos sb = tpos.add(sp.x, sp.y, sp.z);
            if (BlockHelper.getBlock(sb) == Blocks.AIR) return true;
        }
        return false;
    }
}
