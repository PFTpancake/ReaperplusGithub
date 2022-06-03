package me.ghosttypes.reaper.util;

import me.ghosttypes.reaper.util.player.FindItemResult;
import me.ghosttypes.reaper.util.helper.Members;
import me.ghosttypes.reaper.util.helper.Stats;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class VectorUtils {
    public static MinecraftClient mc;
    public static Stats scores;
    public static Screen screen;
    public static int previousSlot = -1;

    public static void init() {
        mc = MinecraftClient.getInstance();

        screen = null;
    }

    public static void postInit() {
        scores = new Stats(10);


    }


    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dX = x2 - x1;
        double dY = y2 - y1;
        double dZ = z2 - z1;

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(Vec3d vec1, Vec3d vec2) {
        double dX = vec2.x - vec1.x;
        double dY = vec2.y - vec1.y;
        double dZ = vec2.z - vec1.z;

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distance(BlockPos pos1, BlockPos pos2) {
        double dX = pos2.getX() - pos1.getX();
        double dY = pos2.getY() - pos1.getY();
        double dZ = pos2.getZ() - pos1.getZ();

        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public static double distanceXZ(Vec3d pos1, Vec3d pos2) {
        double dX = pos1.getX() - pos2.getX();
        double dZ = pos1.getZ() - pos2.getZ();

        return MathHelper.sqrt((float) (dX * dX + dZ * dZ));
    }

    public static double distanceXZ(double x1, double x2, double z1, double z2) {
        double dX = x1 - x2;
        double dZ = z1 - z2;

        return MathHelper.sqrt((float) (dX * dX + dZ * dZ));
    }

    public static double distanceY(Vec3d pos1, Vec3d pos2) {
        return Math.abs(pos2.y - pos1.y);
    }

    public static double distanceY(double y1, double y2) {
        return Math.abs(y1 - y2);
    }


    // Blocks

    public static Block getBlock(BlockPos pos) {
        if (pos == null) return null;
        return mc.world.getBlockState(pos).getBlock();
    }

    public static BlockState getBlockState(BlockPos pos) {
        if (pos == null) return null;
        return mc.world.getBlockState(pos);
    }

    public static VoxelShape getCollision(BlockPos pos) {
        return getBlockState(pos).getCollisionShape(mc.world, pos);
    }

    public static boolean isSolid(BlockPos pos) {
        return getBlockState(pos).isSolidBlock(mc.world, pos);
    }

    private static final Vec3d hitPos = new Vec3d(0.0D, 0.0D, 0.0D);

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority) {
        return place(blockPos, findItemResult, rotationPriority, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority) {
        return place(blockPos, findItemResult, rotate, rotationPriority, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean checkEntities) {
        return place(blockPos, findItemResult, rotate, rotationPriority, true, checkEntities);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority, boolean checkEntities) {
        return place(blockPos, findItemResult, true, rotationPriority, true, checkEntities);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities) {
        return place(blockPos, findItemResult, rotate, rotationPriority, swingHand, checkEntities, true, false);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack) {
        return place(blockPos, findItemResult, rotate, rotationPriority, swingHand, checkEntities, swapBack, false);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack, boolean random) {
        if (findItemResult.isOffhand()) {
            return place(blockPos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, rotate, rotationPriority, swingHand, checkEntities, swapBack, random);
        } else {
            return findItemResult.isHotbar() && place(blockPos, Hand.MAIN_HAND, findItemResult.getSlot(), rotate, rotationPriority, swingHand, checkEntities, swapBack, random);
        }
    }

    public static boolean place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack, boolean offsetRandom) {
        if (slot >= 0 && slot <= 8 || slot == 45) {
            if (!canPlace(blockPos, checkEntities)) {
                return false;
            } else {
                Vec3d vec = getHitPos(blockPos, offsetRandom);

                ((IVec3d) hitPos).set(vec.x, vec.y, vec.z);
                BlockPos neighbour = getNeighbourPos(blockPos);
                Direction side = getSide(blockPos);

                if (rotate) {
                    Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> {
                        InvUtils.swap(slot, swapBack);

                        place(new BlockHitResult(hitPos, side, neighbour, false), hand, swingHand);

                        if (swapBack) InvUtils.swapBack();
                    });
                } else {
                    InvUtils.swap(slot, swapBack);

                    place(new BlockHitResult(hitPos, side, neighbour, false), hand, swingHand);

                    if (swapBack) InvUtils.swapBack();
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public static void place(BlockHitResult blockHitResult, Hand hand, boolean swing) {
        if (hand != null && blockHitResult != null && mc.world.getWorldBorder().contains(blockHitResult.getBlockPos()) && mc.player.getStackInHand(hand).getItem() instanceof BlockItem) {
            boolean wasSneaking = mc.player.input.sneaking;
            mc.player.input.sneaking = false;

            ActionResult result = mc.interactionManager.interactBlock(mc.player, mc.world, hand, blockHitResult);

            if (result.shouldSwingHand()) {
                if (swing) mc.player.swingHand(hand);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }

            mc.player.input.sneaking = wasSneaking;
        }
    }

    public static Vec3d getHitPos(BlockPos blockPos, boolean offsetRandom) {
        Random random = new Random();

        double px = random.nextDouble(0.9) + 0.5;
        double py = random.nextDouble(0.9) + 0.5;
        double pz = random.nextDouble(0.9) + 0.5;

        double x = offsetRandom ? px : 0.5;
        double y = offsetRandom ? py : 0.5;
        double z = offsetRandom ? pz : 0.5;

        ((IVec3d) hitPos).set(blockPos.getX() + x, (double) blockPos.getY() + y, (double) blockPos.getZ() + z);
        Direction side = getPlaceSide(blockPos);

        if (side != null) {
            hitPos.add((double) side.getOffsetX() * 0.5D, (double) side.getOffsetY() * 0.5D, (double) side.getOffsetZ() * 0.5D);
        }

        return hitPos;
    }

    public static BlockPos getNeighbourPos(BlockPos blockPos) {
        Direction side = getPlaceSide(blockPos);
        BlockPos neighbour;

        if (side == null) {
            neighbour = blockPos;
        } else {
            neighbour = blockPos.offset(side.getOpposite());
        }

        return neighbour;
    }

    public static Direction getSide(BlockPos blockPos) {
        Direction side = getPlaceSide(blockPos);

        return side == null ? Direction.UP : side;
    }

    public static boolean canPlace(BlockPos pos) {
        return canPlace(pos, true);
    }

    public static boolean canPlace(BlockPos pos, boolean checkEntities) {
        return canPlace(pos, Blocks.OBSIDIAN.getDefaultState(), checkEntities);
    }

    public static boolean canPlace(BlockPos pos, BlockState state, boolean checkEntities) {
        if (pos == null || mc.world == null) return false;
        if (!World.isValid(pos)) return false;
        if (!getBlockState(pos).getMaterial().isReplaceable()) return false;

        return checkEntities ? mc.world.canPlace(state, pos, ShapeContext.absent()) : getBlockState(pos).getMaterial().isReplaceable();
    }

    public static Direction getPlaceSide(BlockPos blockPos) {
        Direction[] var1 = Direction.values();
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            Direction side = var1[var3];
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();
            BlockState state = MeteorClient.mc.world.getBlockState(neighbor);
            if (!state.isAir() && !isClickable(state.getBlock()) && state.getFluidState().isEmpty()) {
                return side2;
            }
        }

        return null;
    }

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractButtonBlock || block instanceof AbstractPressurePlateBlock || block instanceof BlockWithEntity || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof DoorBlock || block instanceof NoteBlock || block instanceof TrapdoorBlock;
    }

    public static ArrayList<BlockPos> getPositionsAroundPlayer(double range) {
        return getPositionsAroundEntity(mc.player, range);
    }

    public static ArrayList<BlockPos> getPositionsAroundEntity(Entity entity, double range) {
        double pX = entity.getX() - 0.5;
        double pY = entity.getY();
        double pZ = entity.getZ() - 0.5;

        int minX = (int) Math.floor(pX - range);
        int minY = (int) Math.floor(pY - range);
        int minZ = (int) Math.floor(pZ - range);

        int maxX = (int) Math.floor(pX + range);
        int maxY = (int) Math.floor(pY + range);
        int maxZ = (int) Math.floor(pZ + range);

        double rangeSq = Math.pow(range, 2);

        ArrayList<BlockPos> positions = new ArrayList<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (VectorUtils.distance(pX, pY, pZ, x, y, z) <= rangeSq) {
                        BlockPos pos = new BlockPos(x, y, z);

                        if (World.isValid(pos)) positions.add(pos);
                    }
                }
            }
        }

        return positions;
    }

    public static double[] directionSpeed(float speed) {
        float forward = mc.player.input.movementForward;
        float side = mc.player.input.movementSideways;
        float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw);

        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (side < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            side = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }

        final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        final double posX = forward * speed * cos + side * speed * sin;
        final double posZ = forward * speed * sin - side * speed * cos;

        return new double[]{posX, posZ};
    }

    // Movement

    private void packetJump(boolean onGround) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.41999998688698, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.75319998052120, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.00133597911214, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.16610926093821, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.24918707874468, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.17675927506424, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.02442408821369, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.79673560066871, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.49520087700593, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.1212968405392, z, onGround));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
    }

    // Finding Items

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (isGood.test(mc.player.getOffHandStack())) {
        }

        if (isGood.test(mc.player.getMainHandStack())) {
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        return find(isGood, 0, mc.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        int slot = -1, count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }


        return null;
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;

        mc.player.getInventory().selectedSlot = slot;
        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        return true;
    }

    public static boolean swapBack() {
        if (previousSlot == -1) return false;

        boolean return_ = swap(previousSlot, false);
        previousSlot = -1;
        return return_;
    }


    public static List<String> members = new ArrayList<>();

    public static void members() {
        Thread thread = new Thread(() -> {
            try {
                members = Members.getUserNames();
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        thread.start();
    }
}
