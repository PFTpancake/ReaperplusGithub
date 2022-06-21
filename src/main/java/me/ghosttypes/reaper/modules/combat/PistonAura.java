package me.ghosttypes.reaper.modules.combat;


import me.ghosttypes.reaper.modules.ML;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PistonAura extends Module {
    private enum Stage {
        SEARCHING,
        CRYSTAL,
        REDSTONE,
        BREAKING
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgToggles = settings.createGroup("Toggles");
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder().name("target-range").description("Target range").defaultValue(3).min(1).max(6).sliderRange(1, 6).build());
    private final Setting<Double> minimalDamage = sgGeneral.add(new DoubleSetting.Builder().name("minimal-damage").description("Minimal damage for the target to be valid").defaultValue(12).min(0).max(100).sliderRange(0, 100).build());
    private final Setting<Integer> actionInterval = sgGeneral.add(new IntSetting.Builder().name("action-interval").description("Delay between actions").defaultValue(0).min(0).max(10).sliderRange(0, 10).build());
    private final Setting<Integer> switchDelay = sgGeneral.add(new IntSetting.Builder().name("delay-between-switch").description("Delay between swapping inventory slots").defaultValue(3).min(0).max(10).sliderRange(0, 10).build());

    private final Setting<Boolean> swing = sgToggles.add(new BoolSetting.Builder().name("swing").description("Swing hand").defaultValue(true).build());
    private final Setting<Boolean> disableWhenNone = sgToggles.add(new BoolSetting.Builder().name("disable-when-none").description("Disables the module when out of resources").defaultValue(true).build());
    private final Setting<Boolean> strict = sgToggles.add(new BoolSetting.Builder().name("strict").description("Prevents module from working while moving").defaultValue(false).build());
    private final Setting<Boolean> antiSuicide = sgToggles.add(new BoolSetting.Builder().name("anti-suicide").description("Prevents you from dying (doesnt seem to work)").defaultValue(false).build());
    private final Setting<Boolean> mine = sgToggles.add(new BoolSetting.Builder().name("mine").description("Mines redstone blocks (slower but more reliable)").defaultValue(false).build());
    private final Setting<Boolean> torchSupport = sgToggles.add(new BoolSetting.Builder().name("torch-support").description("whether to place support blocks for redstone torches").defaultValue(true).build());
    private final Setting<Boolean> crystalSupport = sgToggles.add(new BoolSetting.Builder().name("crystal-support").description("whether to place support blocks for end crystals").defaultValue(true).build());
    private final Setting<Boolean> debugRender = sgToggles.add(new BoolSetting.Builder().name("render-place-positions").description("whether to render placement positions").defaultValue(true).build());
    private final Setting<Boolean> pauseOnEat = sgToggles.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgToggles.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking potions.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgToggles.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining blocks.").defaultValue(true).build());


    public PistonAura() {
        super(ML.R, "PistonAura", "the best PistonAura ");
    }

    private Stage stage;
    private Runnable postAction;
    private PlayerEntity target;
    private BlockPos facePos;
    private Direction faceOffset;
    private BlockPos crystalPos;
    private EndCrystalEntity crystal;
    private BlockPos pistonPos;
    private BlockPos torchPos;
    private BlockPos currentMining;
    private boolean skipPiston;
    private boolean canSupport;
    private boolean canCrystalSupport;
    private boolean hasRotated;
    private boolean changePickItem;
    private boolean mining;
    private int pickPrevSlot;
    private int miningTicks;
    private int tickCounter;
    private int delayAfterSwitch;

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) return;
        resetAll();
    }

    @Override
    public void onDeactivate() {
        resetAll();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (debugRender.get()) {
            if (facePos != null) {
                event.renderer.box(facePos, new SettingColor(255, 255, 255, 255), new SettingColor(255, 255, 255, 255), ShapeMode.Both, 0);
            }
            if (crystalPos != null) {
                event.renderer.box(crystalPos, new SettingColor(255, 255, 255, 255), new SettingColor(255, 0, 0, 255), ShapeMode.Both, 0);
            }
            if (pistonPos != null) {
                event.renderer.box(pistonPos, new SettingColor(255, 255, 255, 255), new SettingColor(0, 176, 255, 255), ShapeMode.Both, 0);
            }
            if (torchPos != null) {
                event.renderer.box(torchPos, new SettingColor(255, 255, 255, 255), new SettingColor(255, 72, 0, 255), ShapeMode.Both, 0);
            }
        }
    }


    @EventHandler
    public void onTickPre(TickEvent.Pre event) {
        if (disableWhenNone.get()) {
            var redstoneSlot = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH).found();
            var pistonSlot = InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON).found();
            var crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL).found();
            if (!redstoneSlot || !pistonSlot || !crystalSlot) {
                info("Out of materials.");
                toggle();
                return;
            }
        }
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (torchPos != null && crystalPos == null && !mining) mine(torchPos, false);
        if (miningTicks < 10 && mining) miningTicks++;
        if (miningTicks >= 10) {
            mine(currentMining, true);
            miningTicks = 0;
        }
        if (!mining) {
            if (tickCounter < actionInterval.get()) {
                tickCounter++;
            }
            if (tickCounter < actionInterval.get()) {
                return;
            }
            if (postAction == null) handleAction();
        }
    }

    @EventHandler
    public void onTickPost(TickEvent.Post event) {
        if (postAction == null || mining) {
            if (torchPos != null && isAir(torchPos)) mining = false;
            return;
        }
        if (stage == Stage.SEARCHING && pistonPos != null && faceOffset != null) {
            if (!hasRotated) {
                float yaw = switch (faceOffset.getOpposite()) {
                    case EAST -> 90;
                    case SOUTH -> 180;
                    case WEST -> -90;
                    default -> 0;
                };
                Rotations.rotate(yaw, 0, () -> hasRotated = true);
            } else {
                tickCounter = 0;
                postAction.run();
                postAction = null;
                handleAction();
            }
        } else {
            tickCounter = 0;
            postAction.run();
            postAction = null;
            handleAction();
        }
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet && target != null && packet.getStatus() == 3) {
            var deadEntity = packet.getEntity(mc.world);
            if (deadEntity instanceof PlayerEntity && target.equals(deadEntity)) {
                stage = Stage.SEARCHING;
            }
        }
        if (event.packet instanceof BlockUpdateS2CPacket packet && torchPos != null) {
            if (packet.getPos().equals(torchPos) && packet.getState().isAir()) {
                miningTicks = 0;
                currentMining = null;
                mining = false;
                torchPos = null;
            }
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            delayAfterSwitch = switchDelay.get();
        }
    }

    private void handleAction() {
        assert mc.player != null;
        assert mc.world != null;
        if (strict.get() && mc.player.getVelocity().length() > 9.0E-4D) return;

        switch (stage) {
            case SEARCHING -> {
                for (PlayerEntity candidate : getTargets()) {
                    if (evaluateTarget(candidate)) {
                        target = candidate;
                        if (skipPiston) {
                            stage = Stage.CRYSTAL;
                            skipPiston = false;
                            return;
                        }
                        FindItemResult fir = InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON);
                        if (!mine.get() && torchPos != null && mc.world.getBlockState(torchPos).emitsRedstonePower()) {
                            return;
                        }
                        var prevSlot = mc.player.getInventory().selectedSlot;
                        var changeItem = prevSlot != fir.slot();

                        if (changeItem) {
                            InvUtils.swap(fir.slot(), false);
                        }
                        postAction = () -> {
                            var yaw = switch (faceOffset.getOpposite()) {
                                case EAST -> 90;
                                case SOUTH -> 180;
                                case WEST -> -90;
                                default -> 0;
                            };
                            Rotations.rotate(yaw, 0, () -> {
                                BlockUtils.place(pistonPos, fir, false, 0, swing.get(), false);
                                hasRotated = false;
                            });
                            stage = Stage.CRYSTAL;
                        };
                        return;
                    }
                }
            }
            case CRYSTAL -> {
                crystal = getCrystalAtPos(crystalPos);

                if (crystal != null && pistonPos != null) {
                    stage = Stage.REDSTONE;
                    return;
                } else if (!canPlaceCrystal(crystalPos.down(), canCrystalSupport)) {
                    stage = Stage.SEARCHING;
                    return;
                }
                FindItemResult fir = InvUtils.findInHotbar(Items.END_CRYSTAL);
                FindItemResult crystalSupport = null;
                if (canCrystalSupport) {
                    crystalSupport = InvUtils.findInHotbar(Items.OBSIDIAN);
                }
                var prevSlot = mc.player.getInventory().selectedSlot;
                var changeItem = prevSlot != fir.slot();

                if (changeItem) {
                    InvUtils.swap(fir.slot(), false);
                }
                FindItemResult finalCrystalSupport = crystalSupport;
                postAction = () -> {
                    if (canCrystalSupport && crystalPos != null) {
                        assert finalCrystalSupport != null;
                        BlockUtils.place(crystalPos.down(), finalCrystalSupport, false, 0, swing.get(), false);
                        canCrystalSupport = false;
                    }
                    BlockUtils.place(crystalPos, fir, false, 0, swing.get(), false);
                    stage = Stage.REDSTONE;
                };
            }
            case REDSTONE -> {
                if (facePos == null || torchPos == null || !mc.world.getBlockState(torchPos).getMaterial().isReplaceable()) {
                    stage = Stage.SEARCHING;
                    return;
                }
                FindItemResult fir = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH);
                FindItemResult supportBlock = null;
                if (canSupport) {
                    supportBlock = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() > 600);
                }
                var prevSlot = mc.player.getInventory().selectedSlot;
                var changeItem = prevSlot != fir.slot();

                if (changeItem) {
                    InvUtils.swap(fir.slot(), false);
                }
                FindItemResult finalSupportBlock = supportBlock;
                postAction = () -> {
                    if (canSupport && torchPos != null) {
                        assert finalSupportBlock != null;
                        BlockUtils.place(torchPos.down(), finalSupportBlock, false, 0, swing.get(), false);
                        canSupport = false;
                    }
                    BlockUtils.place(torchPos, fir, false, 0, swing.get(), false);
                    stage = Stage.BREAKING;
                };
            }
            case BREAKING -> {
                if (!delayCheck()) return;
                crystal = getCrystalAtPos(crystalPos);
                if (crystalPos != null && crystal != null) {
                    if (!(getBlock(pistonPos) instanceof PistonBlock)) stage = Stage.SEARCHING;
                    var velocityCheck = crystal.getVelocity().length() == 0;
                    if (crystal.age > 5 && velocityCheck) stage = Stage.SEARCHING;
                    boolean blastResistantAtFace = getBlock(facePos).getBlastResistance() > 600;
                    double offsetForBlastResistant = blastResistantAtFace ? 0 : 0.5;
                    var damage = DamageUtils.crystalDamage(target, new Vec3d(facePos.getX() + 0.5, facePos.getY() + offsetForBlastResistant, facePos.getZ() + 0.5), false, null, blastResistantAtFace);
                    System.out.println(damage);
                    if (damage < minimalDamage.get() && !pistonHeadBlocking(pistonPos)) {
                        return;
                    }
                    postAction = () -> {
                        breakCrystal(crystal);
                        if (mine.get() && torchPos != null && torchPos.equals(pistonPos.down())) mine(torchPos, false);
                        faceOffset = null;
                        facePos = null;
                        crystalPos = null;
                        pistonPos = null;
                        target = null;
                        stage = Stage.SEARCHING;
                    };
                } else if ((pistonPos != null && pistonHeadBlocking(pistonPos)) || (crystalPos != null && !mc.world.getBlockState(crystalPos).getMaterial().isReplaceable())) {
                    postAction = () -> {
                        if (mine.get() && torchPos != null && torchPos.equals(pistonPos.down())) mine(torchPos, false);
                        faceOffset = null;
                        facePos = null;
                        crystalPos = null;
                        pistonPos = null;
                        target = null;
                        stage = Stage.SEARCHING;
                    };
                }
            }
        }
    }

    private boolean pistonHeadBlocking(BlockPos pos) {
        for (var direction : Direction.Type.HORIZONTAL) {
            var offset = pos.offset(direction);
            if (getBlock(offset) == Blocks.PISTON_HEAD) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateTarget(PlayerEntity candidate) {
        var tempFacePos = new BlockPos(candidate.getPos()).up();
        if (evaluateTarget(tempFacePos, candidate)) {
            return true;
        }
        if (evaluateTarget(tempFacePos.up(), candidate)) {
            return true;
        }
        return evaluateTarget(tempFacePos.up(2), candidate);
    }

    private boolean evaluateTarget(BlockPos tempFacePos, PlayerEntity candidate) {
        assert mc.interactionManager != null;
        assert mc.player != null;
        assert mc.world != null;
        BlockPos tempCrystalPos = null;
        BlockPos tempPistonPos = null;
        BlockPos tempTorchPos = null;
        Direction offset = null;
        var crystalList = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(tempFacePos).contract(0.2), Entity::isAlive);
        EndCrystalEntity blockingCrystal = null;
        for (var crystal : crystalList) {
            var velocityCheck = crystal.getVelocity().length() == 0;
            if (crystal.age > 5 && velocityCheck) {
                blockingCrystal = crystal;
                break;
            }
        }
        if (blockingCrystal != null) {
            if (delayCheck()) breakCrystal(blockingCrystal);
            return false;
        }
        skipPiston = false;
        canSupport = false;
        canCrystalSupport = false;
        for (var faceOffset : Direction.Type.HORIZONTAL) {
            // Crystal placement stuff + anti-suicide.
            var potentialCrystal = tempFacePos.offset(faceOffset);
            var potCrystalState = mc.world.getBlockState(potentialCrystal);
            if (!EntityUtils.intersectsWithEntity(new Box(potentialCrystal), Entity::isLiving)) {
                FindItemResult supportBlock = InvUtils.findInHotbar(Items.OBSIDIAN);
                if (!supportBlock.found() || !crystalSupport.get()) continue;
                if (potCrystalState.getMaterial().isReplaceable() || potCrystalState.isAir()) canCrystalSupport = true;
            }
            if (!canPlaceCrystal(potentialCrystal.down(), canCrystalSupport)) continue;
            boolean blastResistantAtFace = getBlock(tempFacePos).getBlastResistance() > 600;
            double offsetForBlastResistant = blastResistantAtFace ? 0 : 0.5;
            var calculatedCrystalPos = new Vec3d(tempFacePos.getX() + 0.5, tempFacePos.getY() + offsetForBlastResistant, tempFacePos.getZ() + 0.5);
            var damage = DamageUtils.crystalDamage(candidate, calculatedCrystalPos, false, null, blastResistantAtFace);

            if (damage < minimalDamage.get()) continue;

            if (antiSuicide.get()) {
                var selfDamage = DamageUtils.crystalDamage(mc.player, Vec3d.ofCenter(potentialCrystal));
                if (selfDamage >= EntityUtils.getTotalHealth(mc.player)) continue;
            }

            // Piston placement stuff.
            var potentialPiston = tempFacePos.offset(faceOffset, 2);
            var pistonState = mc.world.getBlockState(potentialPiston);
            skipPiston = getBlock(potentialPiston) instanceof PistonBlock;
            if (getDistanceFromPlayer(potentialPiston) > mc.interactionManager.getReachDistance()) continue;
            if (!pistonState.isAir() && !pistonState.getMaterial().isReplaceable() && !skipPiston) continue;
            if (pistonState.isAir() && EntityUtils.intersectsWithEntity(new Box(potentialPiston), Entity::isLiving))
                continue;

            // Redstone placement stuff.
            Item redstone = null;
            FindItemResult firT = InvUtils.findInHotbar(Items.REDSTONE_TORCH);
            FindItemResult firB = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
            if (firT.found() && firB.found()) redstone = firT.slot() > firB.slot() ? Items.REDSTONE_BLOCK : Items.REDSTONE_TORCH;
            if (firT.found() && !firB.found()) redstone = Items.REDSTONE_TORCH;

            var places = new BlockPos[mine.get() ? 2 : 1];
            places[0] = potentialPiston.offset(faceOffset);
            if (mine.get()) places[1] = potentialPiston.offset(Direction.DOWN);
            for (var potentialRedstone : places) {
                if (getDistanceFromPlayer(potentialRedstone) > mc.interactionManager.getReachDistance()) continue;

                var state = mc.world.getBlockState(potentialRedstone);
                if (!state.isAir()) {
                    if (!state.getMaterial().isReplaceable()) continue;
                    if (!state.emitsRedstonePower()) continue;
                    if (pistonState.isAir()) continue;
                    if (!mining || !potentialRedstone.equals(torchPos)) continue;
                }
                if (EntityUtils.intersectsWithEntity(new Box(potentialRedstone), Entity::isLiving)) continue;
                if (potentialRedstone == places[0] && redstone != null && redstone == Items.REDSTONE_TORCH) {
                    FindItemResult supportBlock = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() > 600);
                    if (!supportBlock.found() || !torchSupport.get()) continue;
                    var downPos = potentialRedstone.down();
                    if (getDistanceFromPlayer(downPos) > mc.interactionManager.getReachDistance()) continue;

                    var supportState = mc.world.getBlockState(downPos);
                    if (!supportState.isAir()) {
                        if (!supportState.isFullCube(mc.world, downPos)) continue;
                        if (supportState.getBlock().getBlastResistance() <= 600) continue;
                    }
                    if (EntityUtils.intersectsWithEntity(new Box(downPos), Entity::isLiving)) continue;
                    canSupport = true;
                }
                tempTorchPos = potentialRedstone;
                break;
            }
            if (tempTorchPos == null) continue;
            tempCrystalPos = potentialCrystal;
            tempPistonPos = potentialPiston;
            offset = faceOffset;
            break;
        }

        if (tempCrystalPos != null) {
            faceOffset = offset;
            facePos = tempFacePos;
            crystalPos = tempCrystalPos;
            crystal = getCrystalAtPos(crystalPos);
            pistonPos = tempPistonPos;
            torchPos = tempTorchPos;
            return true;
        }
        return false;
    }

    private double getDistanceFromPlayer(BlockPos pos) {
        assert mc.player != null;
        return Math.sqrt(mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)));
    }

    private EndCrystalEntity getCrystalAtPos(BlockPos pos) {
        assert mc.world != null;
        var middlePos = Vec3d.ofCenter(pos);
        var crystalList = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos).contract(0.5), Entity::isAlive);
        if (crystalList.isEmpty()) return null;
        if (crystalList.size() == 1) return crystalList.get(0);
        EndCrystalEntity nearestCrystal = null;
        for (var crystal : crystalList) {
            if (nearestCrystal == null) nearestCrystal = crystal;
            if (crystal.squaredDistanceTo(middlePos) < nearestCrystal.squaredDistanceTo(middlePos)) {
                nearestCrystal = crystal;
            }
        }
        return nearestCrystal;
    }

    private boolean canPlaceCrystal(BlockPos blockPos, boolean support) {
        assert mc.world != null;
        var blockState = mc.world.getBlockState(blockPos);
        var blockPosUp = blockPos.up();
        if (!(blockState.isOf(Blocks.BEDROCK) || blockState.isOf(Blocks.OBSIDIAN)) && !support) return false;
        if (!mc.world.getBlockState(blockPosUp).isAir()) return false;
        var x = blockPosUp.getX();
        var y = blockPosUp.getY();
        var z = blockPosUp.getZ();
        return mc.world.getOtherEntities(null, new Box(x, y, z, x + 1, y + 2, z + 1)).isEmpty();
    }

    private boolean isAir(BlockPos pos) {
        assert mc.world != null;
        return mc.world.getBlockState(pos).isAir();
    }

    private List<PlayerEntity> getTargets() {
        assert mc.world != null;
        assert mc.player != null;
        var players = mc.world.getEntitiesByClass(PlayerEntity.class, new Box(mc.player.getBlockPos()).expand(targetRange.get()), Predicate.not(PlayerEntity::isMainPlayer));
        if (players.isEmpty()) return players;
        return players.stream()
            .filter(LivingEntity::isAlive)
            .filter(playerEntity -> Friends.get().shouldAttack(playerEntity))
            .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
            .collect(Collectors.toList());
    }

    private Block getBlock(BlockPos bp) {
        assert mc.world != null;
        return mc.world.getBlockState(bp).getBlock();
    }

    private boolean isOffhand() {
        assert mc.player != null;
        return mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
    }

    private void breakCrystal(Entity crystal) {
        if (crystal == null) return;
        assert mc.interactionManager != null;
        assert mc.player != null;
        Hand hand = isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        if (swing.get()) mc.player.swingHand(hand);
        else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
    }

    private boolean delayCheck() {
        if (delayAfterSwitch > 0) {
            delayAfterSwitch--;
            return false;
        }
        return true;
    }

    private void mine(BlockPos blockPos, boolean override) {
        if (blockPos == null) return;
        assert mc.world != null;
        assert mc.player != null;
        assert mc.interactionManager != null;
        var state = mc.world.getBlockState(blockPos);
        if ((!mining && state.getHardness(null, null) >= 0 && !state.isAir()) || override) {

            var pickaxe = InvUtils.findInHotbar(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
            pickPrevSlot = mc.player.getInventory().selectedSlot;
            changePickItem = pickaxe.slot() != pickPrevSlot;
            if (changePickItem) {
                InvUtils.swap(pickaxe.slot(), false);
            }

            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.WEST));
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.WEST));
            mining = true;
            currentMining = blockPos;
            if (mc.interactionManager.getCurrentGameMode().isCreative() && blockPos.equals(torchPos)) {
                mining = false;
                torchPos = null;
            }
        }
    }

    private void resetAll() {
        stage = Stage.SEARCHING;
        postAction = null;
        target = null;
        facePos = null;
        faceOffset = null;
        crystalPos = null;
        crystal = null;
        skipPiston = false;
        pistonPos = null;
        torchPos = null;
        hasRotated = false;
        changePickItem = false;
        mining = false;
        canSupport = false;
        canCrystalSupport = false;
        currentMining = null;
        pickPrevSlot = -1;
        miningTicks = 0;
        tickCounter = 0;
        delayAfterSwitch = 0;
    }
}
