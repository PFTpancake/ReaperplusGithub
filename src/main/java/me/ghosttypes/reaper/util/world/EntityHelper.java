package me.ghosttypes.reaper.util.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityHelper {


    public static void hitEntity(Entity entity) {
        if (entity == null) return;
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public static EndCrystalEntity getNearbyCrystal(BlockPos startPos, int range) {
        List<EndCrystalEntity> crystals = getNearbyCrystals(startPos, range);
        if (crystals == null) return null;
        if (crystals.isEmpty()) return null;
        return crystals.get(0);
    }

    public static List<EndCrystalEntity> getNearbyCrystals(BlockPos startPos, int range) {
        ArrayList<EndCrystalEntity> nearbyCrystals = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                if (BlockHelper.distanceBetween(startPos, entity.getBlockPos()) <= range) nearbyCrystals.add((EndCrystalEntity) entity);
            }
        }
        return nearbyCrystals;
    }
}
