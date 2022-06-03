package me.ghosttypes.reaper.modules.render;

import me.ghosttypes.reaper.modules.ML;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;

public class KillFX extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEffect = settings.createGroup("Effect");


    // General


    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Won't summon particles when you die.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Won't summon particles when friends die.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> playEffectOnPop = sgGeneral.add(new BoolSetting.Builder()
        .name("play-effect-on-pop")
        .description("Plays a effect when a player pops a totem.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> playEffectOnDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("play-effect-on-death")
        .description("Plays a effect when a player dies.")
        .defaultValue(true)
        .build()
    );


    // Effect


    private final Setting<Integer> effectAmount = sgEffect.add(new IntSetting.Builder()
        .name("effect-amount")
        .description("How many lightning bolts to spawn.")
        .defaultValue(1)
        .sliderMin(1)
        .sliderMax(5)
        .min(1)
        .noSlider()
        .build()
    );

    private final Setting<Boolean> silentLightning = sgEffect.add(new BoolSetting.Builder()
        .name("silent-lightning")
        .description("Makes the lightning bold silent.")
        .defaultValue(false)
        .build()
    );

    public KillFX() {
        super(ML.R, "KillFX", "Cool Things");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 35 && playEffectOnPop.get() || packet.getStatus() == 3 && playEffectOnDeath.get()) {
                Entity entity = packet.getEntity(mc.world);

                if (entity instanceof PlayerEntity
                    && (!ignoreSelf.get() || ignoreSelf.get() && entity != mc.player)
                    && (!ignoreFriends.get() || ignoreFriends.get() && Friends.get() != null && !Friends.get().isFriend((PlayerEntity) entity))) {

                    Vec3d pos = entity.getPos();

                    for (int i = 0; i < effectAmount.get(); i++) {
                        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
                        lightning.setPos(pos.x, pos.y, pos.z);
                        lightning.setSilent(silentLightning.get());
                        lightning.refreshPositionAfterTeleport(pos);

                        mc.world.addEntity(lightning.getId(), lightning);
                    }
                }
            }
        }
    }
}
