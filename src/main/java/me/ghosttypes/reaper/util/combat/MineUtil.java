package me.ghosttypes.reaper.util.combat;

import me.ghosttypes.reaper.util.Wrapper;
import me.ghosttypes.reaper.util.player.Automationutils;
import me.ghosttypes.reaper.util.player.InvUtil;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MineUtil {

    public static Executor mineExecutor = Executors.newCachedThreadPool();
    public static boolean override = false;
    public static boolean retoggleCA = false;

    // global method to handle packet mining with whatever MineMode is set in the specific module, rather than put this in each module that uses it
    public static void handlePacketMine(BlockPos targetBlock, MineMode mineMode, boolean rotate, int rotatePrio) {
        boolean canHandle = false;
        long waitTime = 0;
        FindItemResult pick = InvUtil.findPick();
        if (pick.found()) {
            canHandle = true;
            waitTime = (long) getBlockBreakingSpeed(pick.slot(), mc.world.getBlockState(targetBlock));
            int currentSlot = mc.player.getInventory().selectedSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pick.slot()));
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
        }
        if (mineMode == MineMode.Server) { // regular packet mine
            Automationutils.doPacketMine(targetBlock, rotate, rotatePrio);
        }
        if (mineMode == MineMode.Bypass_Strict) { // bypass for servers like ecme where packet mining is fucky
            Automationutils.doPacketMine(targetBlock, rotate, rotatePrio);
            Automationutils.doPacketMine(targetBlock, false, 0);
        }
        if (mineMode == MineMode.Bypass_Strong) { // bypass for servers with pretty shitty AC, but where you can't instamine. About as fast as you can go without getting kicked for packet spam or instamine patch
            Automationutils.doPacketMine(targetBlock, rotate, rotatePrio);
            Automationutils.doPacketMine(targetBlock, false, 0);
            Automationutils.doPacketMine(targetBlock, false, 0);
        }
        if (canHandle) {
            long finalWaitTime = waitTime; // wait however long it takes to mine the target block on a thread
            mineExecutor.execute(() -> handlePostPacketMine(targetBlock, pick, finalWaitTime));
            //handlePostPacketMine(targetBlock, pick, waitTime); lol i had this here twice probably caused the desync too
        }
    }

    // second part of global packet mining handler
    public static void handlePostPacketMine(BlockPos targetBlock, FindItemResult pick, long waitTime) {
        // wait before handling
        try {Thread.sleep(waitTime);} catch (Exception ignored) {}
        // make sure the pick is still valid
        if (!pick.found()) return;
        if (!pick.isHotbar()) return;
        override = true;
        // update slot
        // TODO: See if the second packet is somehow causing slot desync, this util seems to break eating?
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pick.slot()));
        //mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        // send final packet + hand swing
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, targetBlock, Direction.UP));
        Wrapper.swingHand(false);
        override = false;
    }

    public static void handleModules(boolean disable) {
        CrystalAura ca = Modules.get().get(CrystalAura.class);
        retoggleCA = false;
        if (ca.isActive() && disable) {
            retoggleCA = true;
            ca.toggle();
        }
        if (!ca.isActive() && !disable && retoggleCA) ca.toggle();
    }

    public static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(block);
        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getStack(slot);
            int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool);
            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }
        if (StatusEffectUtil.hasHaste(mc.player)) speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
            speed *= k;
        }
        if (mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(mc.player)) speed /= 5.0F;
        if (!mc.player.isOnGround()) speed /= 5.0F;
        return speed;
    }

    public enum MineMode {
        Client,
        Server,
        Bypass_Strict,
        Bypass_Strong
    }

    public enum BlockRotate {
        None,
        Place,
        Break,
        Both
    }
}
