package me.ghosttypes.reaper.modules.chat;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.events.DeathEvent;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.Formatter;
import me.ghosttypes.reaper.util.misc.MessageUtil;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.player.Stats;
import me.ghosttypes.reaper.util.services.GlobalManager;
import me.ghosttypes.reaper.util.services.GlobalManager.DeathEntry;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static me.ghosttypes.reaper.util.services.GlobalManager.deathEntries;

public class AutoDiscord extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> ezDelay = sgGeneral.add(new IntSetting.Builder().name("ez-delay").description("How many seconds before sending an ez message.").defaultValue(40).min(1).sliderMax(100).build());
    public final Setting<Boolean> pmEz = sgGeneral.add(new BoolSetting.Builder().name("pm-ez").description("Send the AutoEz message to the player's dm.").defaultValue(false).build());
    private final Setting<List<String>> ezMessages = sgGeneral.add(new StringListSetting.Builder().name("ez-messages").description("Messages to use for AutoEz.").defaultValue(Collections.emptyList()).build());


    public AutoDiscord() {
        super(ML.M, "AutoDiscord", "Get Reaper+ discord poping.");
    }

    @Override
    public void onActivate() {
        if (ezMessages.get().isEmpty()) {
            warning("Your ez message list was empty, using the default message.");
            ezMessages.get().add("Get Reaper+ At https://discord.gg/RprkhD4CC2");
            ezMessages.get().add("If You Suck At PVP You Need Reaper+ c At https://discord.gg/RprkhD4CC2");
            ezMessages.get().add("Get Reaper+ Today https://discord.gg/RprkhD4CC2");

        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (deathEntries.isEmpty()) return;
        ArrayList<DeathEntry> toRemove = new ArrayList<>();
        for (DeathEntry entry : deathEntries) if (entry.getTime() + 2500 < System.currentTimeMillis()) toRemove.add(entry); // remove all entries that are older than 2.5 seconds
        deathEntries.removeIf(toRemove::contains);
    }

    @EventHandler
    public void onKill(DeathEvent.KillEvent event) {
        qEz(event.player);
    }

    public void qEz(PlayerEntity target) {
        if (target == null) return;
        String name = target.getEntityName();
        if (MessageUtil.pendingEZ.contains(name)) return; // no duplicate messages
        Stats.addKill(name);
        String ezMessage = "GG {player}";
        DeathEntry entry = GlobalManager.getDeathEntry(name); // try to get the death entry
        if (entry != null) {
            int pops = entry.getPops();
        } else { ezMessage = getFixedEZ(); } // if no entry, use a 'fixed' message
        ezMessage = ezMessage.replace("{player}", name); // add the player's name
        ezMessage = Formatter.applyPlaceholders(ezMessage); // apply client placeholders
        MessageUtil.sendEzMessage(name, ezMessage, ezDelay.get() * 1000, pmEz.get());
    }


    public String getFixedEZ() {
        for (String m : ezMessages.get()) if (!m.contains("{pops}")) return m;
        return "GG {player}";
    }


}
