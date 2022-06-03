package me.ghosttypes.reaper.util.player;

import me.ghosttypes.reaper.util.Wrapper;
import me.ghosttypes.reaper.util.world.BlockHelper;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Automationutils {

    public static int rotateTimer = 0;

    public static ArrayList<Vec3d> surroundPositions = new ArrayList<>() {{
        add(new Vec3d(1, 0, 0));
        add(new Vec3d(-1, 0, 0));
        add(new Vec3d(0, 0, 1));
        add(new Vec3d(0, 0, -1));
    }};

    public static ArrayList<Vec3d> selfTrapPositions = new ArrayList<>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    public static BlockPos getSelfTrapBlock(PlayerEntity p) {
        BlockPos tpos = p.getBlockPos();
        for (Vec3d sp : selfTrapPositions) {
            BlockPos sb = tpos.add(sp.x, sp.y, sp.z);
            if (isTrapBlock(sb)) return sb;
        }
        return null;
    }

    public static ArrayList<BlockPos> getSurroundBlocks(PlayerEntity p) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        BlockPos tpos = p.getBlockPos();
        for (Vec3d sp : surroundPositions) {
            BlockPos sb = tpos.add(sp.x, sp.y, sp.z);
            if (isTrapBlock(sb)) blocks.add(sb);
        }
        if (blocks.isEmpty()) return null;
        return blocks;
    }

    public static boolean isAnvilBlock(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.ANVIL || BlockHelper.getBlock(pos) == Blocks.CHIPPED_ANVIL || BlockHelper.getBlock(pos) == Blocks.DAMAGED_ANVIL;
    }

    public static boolean isWeb(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.COBWEB || BlockHelper.getBlock(pos) == Block.getBlockFromItem(Items.STRING);
    }

    public static boolean isSelfTrapped(PlayerEntity p) {
        BlockPos tpos = p.getBlockPos();
        for (Vec3d sp : selfTrapPositions) {
            BlockPos sb = tpos.add(sp.x, sp.y, sp.z);
            if (!BlockHelper.isAir(sb)) return false;
        }
        return true;
    }

    public static boolean isBurrowed(PlayerEntity p, boolean holeCheck) {
        BlockPos pos = p.getBlockPos();
        if (holeCheck && !Wrapper.isInHole(p)) return false;
        return BlockHelper.getBlock(pos) == Blocks.ENDER_CHEST || BlockHelper.getBlock(pos) == Blocks.OBSIDIAN || isAnvilBlock(pos);
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

    public static BlockPos getOpenPos(PlayerEntity p) {
        BlockPos tpos = p.getBlockPos();
        for (Vec3d sp : surroundPositions) {
            BlockPos sb = tpos.add(sp.x, sp.y, sp.z);
            if (BlockHelper.getBlock(sb) == Blocks.AIR) return sb;
        }
        return null;
    }

    public static boolean canCrystal(PlayerEntity p) {
        BlockPos tpos = p.getBlockPos();
        for (Vec3d sp : surroundPositions) {
            BlockPos sb = tpos.add(sp.x, sp.y, sp.z);
            if (BlockHelper.getBlock(sb) == Blocks.AIR) return true;
        }
        return false;
    }

    public static BlockPos getStringPos(PlayerEntity p) {
        BlockPos tpos = p.getBlockPos();
        for (Vec3d sp : surroundPositions) {
            BlockPos sb = tpos.add(sp.x, sp.y, sp.z);
            if (isWeb(sb)) return sb;
        }
        return null;
    }

    public static void mineWeb(PlayerEntity p, int swordSlot, boolean rotate, int priority) {
        if (p == null || swordSlot == -1) return;
        BlockPos pos = p.getBlockPos();
        BlockPos webPos = null;
        if (isWeb(pos)) webPos = pos;
        if (isWeb(pos.up())) webPos = pos.up();
        if (isWeb(pos.up(2))) webPos = pos.up(2);
        if (webPos == null) return;
        Wrapper.updateSlot(swordSlot);
        doRegularMine(webPos, rotate, priority);
    }

    public static void doPacketMine(BlockPos targetPos, boolean rotate, int priority) {
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(targetPos), Rotations.getPitch(targetPos), priority, () -> sendPacketMine(targetPos));
        } else {
            sendPacketMine(targetPos);
        }
    }

    public static void doRegularMine(BlockPos targetPos, boolean rotate, int priority) {
        boolean doRotate = false;
        if (rotate && rotateTimer <= 0) {
            rotateTimer = 5;
            doRotate = true;
        } else {
            rotateTimer--;
        }
        if (doRotate) {
            Rotations.rotate(Rotations.getYaw(targetPos), Rotations.getPitch(targetPos), priority, () -> sendRegularMine(targetPos));
        } else {
            sendRegularMine(targetPos);
        }
    }

    public static void sendPacketMine(BlockPos targetPos) {
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, Direction.UP));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, Direction.UP));
        //Wrapper.swingHand(false);
    }

    public static void sendRegularMine(BlockPos targetPos) {
        mc.interactionManager.updateBlockBreakingProgress(targetPos, Direction.UP);
        Wrapper.swingHand(false);
    }

}
