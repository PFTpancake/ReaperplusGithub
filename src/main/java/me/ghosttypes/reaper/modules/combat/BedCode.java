package me.ghosttypes.reaper.modules.combat;

import me.ghosttypes.reaper.util.combat.MineUtil;
import me.ghosttypes.reaper.util.misc.ItemCounter;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.screen.CraftingScreenHandler;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BedCode {

    // Check if BedAura needs to be paused
    public static boolean shouldPause(boolean debug) {
        BedAura2 ba = Modules.get().get(BedAura2.class);
        int minBeds = ba.minBeds.get();
        boolean pauseOnCraft = ba.pauseOnCraft.get();
        boolean pauseOnCa = ba.pauseOnCa.get();
        boolean pauseOnEat = ba.pauseOnCa.get();
        boolean pauseOnDrink = ba.pauseOnDrink.get();
        boolean pauseOnMine = ba.pauseOnMine.get();
        // Check eating/drinking/mine
        if (PlayerUtils.shouldPause(pauseOnMine, pauseOnEat, pauseOnDrink)) {
            if (debug) ChatUtils.info("Pausing on mine/eat/drink");
            return true;
        }
        // Check ca
        if (pauseOnCa && Modules.get().get(CrystalAura.class).isActive()) {
            if (debug) ChatUtils.info("Pausing on CA");
            return true;
        }
        // Check if the user is in a crafting table screen
        if (pauseOnCraft && mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            if (debug) ChatUtils.info("Pausing on Crafting Screen");
            return true;
        }
        // Check if mine helper is locking modules
        if (MineUtil.override) {
            if (debug) ChatUtils.info("Pausing on MineHelper override");
            return true;
        }
        // Check if the user has fewer beds than minBeds
        if (ItemCounter.beds() <= minBeds) {
            if (debug) ChatUtils.info("Pausing on minBeds");
            return true;
        } else {
            return false;
        }
    }

}
