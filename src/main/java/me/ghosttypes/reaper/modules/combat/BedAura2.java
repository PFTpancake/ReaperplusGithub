package me.ghosttypes.reaper.modules.combat;


import me.ghosttypes.reaper.util.Wrapper;
import me.ghosttypes.reaper.util.combat.MineUtil;
import me.ghosttypes.reaper.util.player.Automationutils;
import me.ghosttypes.reaper.util.player.InvUtil;
import me.ghosttypes.reaper.util.world.BlockHelper;
import me.ghosttypes.reaper.util.world.EntityHelper;
import me.ghosttypes.reaper.modules.ML;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BedAura2 extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelayHole = settings.createGroup("HoleDelay");
    private final SettingGroup sgDelayMoving = settings.createGroup("MovingDelay");
    private final SettingGroup sgRotation = settings.createGroup("Rotation");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgPopOverride = settings.createGroup("PopOverride");
    private final SettingGroup sgTrap = settings.createGroup("Trap");
    private final SettingGroup sgAutoMove = settings.createGroup("Inventory");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").description("Spam chat with shit you won't understand.").defaultValue(false).build());
    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("The delay between placing beds in ticks.").defaultValue(9).min(0).sliderMax(20).build());
    public final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder().name("strict-direction").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<BreakHand> breakHand = sgGeneral.add(new EnumSetting.Builder<BreakHand>().name("break-hand").description("Which hand to break beds with.").defaultValue(BreakHand.Offhand).build());

    // Delays
    public final Setting<Integer> placeDelayHole = sgDelayHole.add(new IntSetting.Builder().name("place-delay-hole").description("The delay between placing beds in ticks if the target is in a hole.").defaultValue(9).min(0).sliderMax(20).build());
    public final Setting<Integer> placeDelayMoving = sgDelayHole.add(new IntSetting.Builder().name("place-delay-moving").description("The delay between placing beds in ticks if the target is moving.").defaultValue(7).min(0).sliderMax(20).build());

    public final Setting<Integer> breakDelayHole = sgDelayMoving.add(new IntSetting.Builder().name("break-delay-hole").description("The delay between placing beds in ticks if the target is in a hole.").defaultValue(9).min(0).sliderMax(20).build());
    public final Setting<Integer> breakDelayMoving = sgDelayMoving.add(new IntSetting.Builder().name("break-delay-moving").description("The delay between placing beds in ticks if the target is moving.").defaultValue(7).min(0).sliderMax(20).build());

    // Rotation
    public final Setting<MineUtil.BlockRotate> blockRotation = sgRotation.add(new EnumSetting.Builder<MineUtil.BlockRotate>().name("block-rotations").description("How to rotate on block place/break.").defaultValue(MineUtil.BlockRotate.None).build());
    public final Setting<Integer> rotatePrio = sgRotation.add(new IntSetting.Builder().name("rotate-priority").description("Rotation priority").defaultValue(50).min(1).sliderMax(100).max(100).build());

    // Targeting
    public final Setting<Boolean> predictMovement = sgTargeting.add(new BoolSetting.Builder().name("predict").description("Predict where to place next.").defaultValue(false).build());
    public final Setting<Boolean> predictIgnoreElytra = sgTargeting.add(new BoolSetting.Builder().name("ignore-elytra").description("Ignore predict if you or the target is in an elytra.").defaultValue(false).build());
    public final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder().name("target-range").description("The range at which players can be targeted.").defaultValue(4).min(0).sliderMax(5).build());
    public final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to filter the players to target.").defaultValue(SortPriority.LowestHealth).build());
    public final Setting<Double> minDamage = sgTargeting.add(new DoubleSetting.Builder().name("min-damage").description("The minimum damage to inflict on your target.").defaultValue(7).min(0).max(36).sliderMax(36).build());
    public final Setting<Double> maxSelfDamage = sgTargeting.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum damage to inflict on yourself.").defaultValue(7).min(0).max(36).sliderMax(36).build());
    public final Setting<Boolean> antiSuicide = sgTargeting.add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    // Pop Override
    public final Setting<Boolean> popOverride = sgPopOverride.add(new BoolSetting.Builder().name("pop-override").description("Ignore max self damage if the target pops and you wont.").defaultValue(false).build());
    public final Setting<Double> popOverridePreHP = sgPopOverride.add(new DoubleSetting.Builder().name("min-health").description("How much health you must have.").defaultValue(10).min(1).max(36).sliderMax(36).build());
    public final Setting<Double> popOverridePostHP = sgPopOverride.add(new DoubleSetting.Builder().name("min-health-after").description("How much health you must have after placing.").defaultValue(6).min(1).max(36).sliderMax(36).build());

    // Inventory
    public final Setting<Boolean> autoMove = sgAutoMove.add(new BoolSetting.Builder().name("auto-move").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
    public final Setting<Integer> autoMoveSlot = sgAutoMove.add(new IntSetting.Builder().name("auto-move-slot").description("The slot auto move moves beds to.").defaultValue(9).min(1).max(9).sliderMin(1).sliderMax(9).visible(autoMove::get).build());
    public final Setting<Boolean> autoSwitch = sgAutoMove.add(new BoolSetting.Builder().name("auto-switch").description("Switches to and from beds automatically.").defaultValue(true).build());
    public final Setting<Boolean> restoreOnDisable = sgAutoMove.add(new BoolSetting.Builder().name("restore-on-disable").description("Put whatever was in your auto move slot back after disabling.").defaultValue(true).build());
    public final Setting<Integer> minBeds = sgAutoMove.add(new IntSetting.Builder().name("min-beds").description("How many beds are required in your inventory to place.").defaultValue(2).min(1).build());

    // Trap
    public final Setting<Boolean> autoTrap = sgTrap.add(new BoolSetting.Builder().name("auto-trap").description("Prevent the target from escaping before placing beds.").defaultValue(true).build());
    public final Setting<Boolean> autoTrapHoleOnly = sgTrap.add(new BoolSetting.Builder().name("hole-only").description("Only trap the target if they are in a hole.").defaultValue(false).build());
    public final Setting<Boolean> autoTrapHold = sgTrap.add(new BoolSetting.Builder().name("hold").description("Wait for the target to be trapped before placing beds.").defaultValue(false).build());
    public final Setting<Boolean> autoTrapBypassObby = sgTrap.add(new BoolSetting.Builder().name("bypass-on-no-obby").description("Will place normally rather than stopping if you're out of obby.").defaultValue(false).build());

    // Automation
    public final Setting<Boolean> disableOnNoBeds = sgAutomation.add(new BoolSetting.Builder().name("disable-on-no-beds").description("Disable if you run out of beds.").defaultValue(false).build());
    public final Setting<Boolean> breakBlockingCrystals = sgAutomation.add(new BoolSetting.Builder().name("break-blocking-crystals").description("Break crystals blocking bed placement.").defaultValue(true).build());
    public final Setting<Integer> crystalDelay = sgAutoMove.add(new IntSetting.Builder().name("crystal-delay").description("Delay in ticks between breaking crystals in the way of bed placements.").defaultValue(2).min(1).build());
    public final Setting<MineUtil.MineMode> mineMode = sgTargeting.add(new EnumSetting.Builder<MineUtil.MineMode>().name("mine-mode").description("How to mine blocks.").defaultValue(MineUtil.MineMode.Server).build());
    public final Setting<Boolean> breakBurrow = sgAutomation.add(new BoolSetting.Builder().name("break-burrow").description("Break target's burrow automatically.").defaultValue(true).build());
    public final Setting<Boolean> breakWeb = sgAutomation.add(new BoolSetting.Builder().name("break-web").description("Break target's webs/string automatically.").defaultValue(true).build());
    public final Setting<Boolean> renderAutomation = sgAutomation.add(new BoolSetting.Builder().name("render-break").description("Render mining self-trap/burrow.").defaultValue(false).build());
    public final Setting<ShapeMode> shapeModeAutomation = sgAutomation.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How burrow blocks/webs are rendered.").defaultValue(ShapeMode.Both).build());
    public final Setting<SettingColor> sideColorAutomation = sgAutomation.add(new ColorSetting.Builder().name("side-color-automation").description("The side color.").defaultValue(new SettingColor(156, 56, 56,75)).build());
    public final Setting<SettingColor> lineColorAutomation = sgAutomation.add(new ColorSetting.Builder().name("line-color-automation").description("The line color.").defaultValue(new SettingColor(156, 56, 56)).build());

    // Safety
    public final Setting<Boolean> disableOnSafety = sgSafety.add(new BoolSetting.Builder().name("disable-on-safety").description("Disable BedAuraPlus when safety activates.").defaultValue(true).build());
    public final Setting<Double> safetyHP = sgSafety.add(new DoubleSetting.Builder().name("safety-hp").description("What health safety activates at.").defaultValue(10).min(1).max(36).sliderMax(36).build());
    public final Setting<Boolean> safetyGapSwap = sgSafety.add(new BoolSetting.Builder().name("swap-to-gap").description("Swap to egaps after activating safety.").defaultValue(false).build());
    public final Setting<Boolean> safetyRage = sgSafety.add(new BoolSetting.Builder().name("rage").description("Ignore safety if your current target will pop with the next placement.").defaultValue(false).build());

    // Pause
    public final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnCa = sgPause.add(new BoolSetting.Builder().name("pause-on-ca").description("Pause while Crystal Aura is active.").defaultValue(false).build());
    public final Setting<Boolean> pauseOnCraft = sgPause.add(new BoolSetting.Builder().name("pause-on-crafting").description("Pauses while you're in a crafting table.").defaultValue(false).build());

    // Render
    public final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("render-swing").description("Render hand swings").defaultValue(true).build());
    public final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render-beds").description("Render bed placement.").defaultValue(true).build());
    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How bed placements are rendered.").defaultValue(ShapeMode.Both).build());
    public final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(114, 11, 135,75)).build());
    public final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(114, 11, 135)).build());
    public final Setting<Boolean> renderPops = sgRender.add(new BoolSetting.Builder().name("render-pops").description("Use a different color for beds that will pop the target.").defaultValue(true).build());
    public final Setting<SettingColor> sideColorPop = sgRender.add(new ColorSetting.Builder().name("side-color-pop").description("The side color.").defaultValue(new SettingColor(156, 56, 56,75)).build());
    public final Setting<SettingColor> lineColorPop = sgRender.add(new ColorSetting.Builder().name("line-color-pop").description("The line color.").defaultValue(new SettingColor(156, 56, 56)).build());

    public final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder().name("render-damage").description("Render bed damage.").defaultValue(true).build());
    public final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render-mode").description("Where to render bed damage.").defaultValue(RenderMode.Center).build());
    public final Setting<Integer> roundDamage = sgRender.add(new IntSetting.Builder().name("round-damage").description("Round damage to x decimal places.").defaultValue(2).min(0).max(3).sliderMax(3).build());
    public final Setting<Double> damageScale = sgRender.add(new DoubleSetting.Builder().name("damage-scale").description("The scale of the damage text.").defaultValue(1.4).min(0).max(5.0).sliderMax(5.0).build());
    public final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder().name("damage-color").description("The color of the damage text.").defaultValue(new SettingColor(15, 255, 211)).build());


    private CardinalDirection direction;
    private PlayerEntity target;
    private BlockPos placePos, breakPos, trapPos;
    private double nextDamage;
    private int timer, breakTimer, webTimer, crystalTimer;
    private boolean isBurrowMining, safetyToggled, notifiedBurrow;
    private Item ogItem;

    public BedAura2() {
        super(ML.R, "BedAura2", "the best bed aura2");
    }

    @Override
    public void onActivate() {
        // timers should be 0 so it instantly places and breaks on enable, then they will reset to the delay in the next tick
        breakTimer = 0;
        timer = 0;
        crystalTimer = crystalDelay.get();
        target = null;
        safetyToggled = false;
        direction = CardinalDirection.North;
        ogItem = Wrapper.getItemFromSlot(autoMoveSlot.get() - 1);
        if (ogItem instanceof BedItem) ogItem = null; //ignore if we already have a bed there.
    }

    @Override
    public void onDeactivate() {
        // check if the module was toggled from safety
        if (safetyToggled) {
            if (safetyGapSwap.get()) {
                FindItemResult gap = InvUtil.findEgap();
                if (gap.found()) mc.player.getInventory().selectedSlot = gap.slot();
            }
        }
        // check if we should restore the original item that was in the refill slot before activation
        if (!safetyToggled && restoreOnDisable.get() && ogItem != null) {
            FindItemResult ogItemInv = InvUtils.find(ogItem);
            if (ogItemInv.found()) InvUtils.move().from(ogItemInv.slot()).toHotbar(autoMoveSlot.get() - 1);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Check if monke user is in the correct dimension
        if (mc.world.getDimension().isBedWorking()) {
            error("Beds won't work here monke.");
            toggle();
            return;
        }

        // check if the user is above safety hp
        if (PlayerUtils.getTotalHealth() <= safetyHP.get()) {
            boolean skip = false;
            // rage check
            if (safetyRage.get()) {
                if (target != null) if (EntityUtils.getTotalHealth(target) - nextDamage <= 0) skip = true;
            }
            if (disableOnSafety.get() && !skip) {
                // disable if disableOnSafety is on
                safetyToggled = true;
                toggle();
            }
            // pause if disableOnSafety is off
            if (!skip) return;
        }


        // check if we need to pause
        if (BedCode.shouldPause(debug.get())) return;
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        // targeting
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = null;
            placePos = null;
            breakPos = null;
            return;
        }


        // auto move
        if (autoMove.get()) {
            // find a bed
            FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            // if we found one move it to the refill slot
            if (bed.found() && bed.slot() != autoMoveSlot.get() - 1) InvUtils.move().from(bed.slot()).toHotbar(autoMoveSlot.get() - 1);
            // if we didn't find one, check if the user has disableOnNoBeds on
            if (!bed.found() && disableOnNoBeds.get()) {
                toggle();
                return;
            }
        }

        // find a place pos if we don't have any beds nearby to break
        if (breakPos == null) placePos = findPlace(target);

        // auto trap
        if (autoTrap.get() && BlockUtils.canPlace(target.getBlockPos().up(2))) {
            if (debug.get()) info("Checking auto trap");
            boolean doTrap = true;
            if (placePos != null) {
                if (autoTrapHoleOnly.get() && !Wrapper.isInHole(target)) doTrap = false;
            } else {
                doTrap = false;
            }
            if (doTrap) {
                if (debug.get()) info("Doing trap");
                FindItemResult obby = InvUtil.findObby();
                if (obby.found()) {
                    // rotation check
                    boolean rotate = false;
                    switch (blockRotation.get()) { case Place, Both -> rotate = true;}
                    if (debug.get()) info("Placing trap block");
                    boolean placed = BlockUtils.place(target.getBlockPos().up(2), obby, rotate, rotatePrio.get(), true, true, true);
                    if (!placed && autoTrapHold.get()) {
                        if (debug.get()) info("Didn't place, holding because trapHold=true");
                        return;
                    }
                } else {
                    if (debug.get()) info("No obby to trap with");
                    if (!autoTrapBypassObby.get()) return;
                    if (debug.get()) info("Ignoring because bypassObby=true");
                }
            }
        }

        // burrow breaking
        if (Automationutils.isBurrowed(target, false) && breakBurrow.get()) {
            // rotation check
            boolean rotate = false;
            switch (blockRotation.get()) { case Break, Both -> rotate = true;}
            // only want to return here if we are using a packet mode and already sent the mine packets
            if (mineMode.get() != MineUtil.MineMode.Client) {
                if (isBurrowMining) {
                    if (debug.get()) info("Already packet burrow mining, returning");
                    return;
                }
            }
            FindItemResult pick = InvUtil.findPick();
            if (pick.found()) {
                // alert the user
                if (!notifiedBurrow) {
                    notifiedBurrow = true;
                }
                if (mineMode.get() != MineUtil.MineMode.Client) { // check current mine mode
                    if (debug.get()) info("Doing packet burrow mine");
                    MineUtil.handlePacketMine(target.getBlockPos(), mineMode.get(), rotate, rotatePrio.get()); // handle packet mining
                } else {
                    if (debug.get()) info("Doing client burrow mine");
                    Wrapper.updateSlot(pick.slot());
                    Automationutils.doRegularMine(target.getBlockPos(), rotate, rotatePrio.get()); // handle client mining
                }
                isBurrowMining = true;
                return;
            }
        } else {
            // check if burrow mining is complete
            if (isBurrowMining) {
                notifiedBurrow = false;
                isBurrowMining = false;
            }
        }

        // check if burrow mining is complete
        //if (isBurrowMining && !AutomationUtils.isBurrowed(target, false)) {
        //    notifiedBurrow = false;
        //    isBurrowMining = false;
        //}

        // web breaking
        if (placePos == null && Automationutils.isWebbed(target) && breakWeb.get()) {
            if (debug.get()) info("Doing web mine");
            // check if we are already burrow mining
            if (isBurrowMining) {
                if (debug.get()) info("Already burrow mining, returning");
                return;
            }
            FindItemResult sword = InvUtil.findSword();
            if (sword.found()) {
                // rotation check
                boolean rotate = false;
                switch (blockRotation.get()) { case Break, Both -> rotate = true;}
                Wrapper.updateSlot(sword.slot());
                if (webTimer <= 0) { // webTimer is to prevent getting stuck on apes spamming webs or string
                    webTimer = 100;
                } else {
                    webTimer--;
                }
                Automationutils.mineWeb(target, sword.slot(), rotate, rotatePrio.get()); // always use client mode for string/web. Useless to packet mine since it breaks fast.
                return;
            }
        }

        // Breaking crystals preventing bed placement
        if (breakBlockingCrystals.get() && placePos != null) {
            if (crystalTimer <= 0) {
                //TODO: don't hardcode this
                EndCrystalEntity nearestCrystal = EntityHelper.getNearbyCrystal(target.getBlockPos(), 2); // get the nearest crystal
                if (nearestCrystal != null) {
                    if (BlockHelper.distanceBetween(placePos, nearestCrystal.getBlockPos()) <= 2) EntityHelper.hitEntity(nearestCrystal); // if its within 2 blocks of our current bed place pos, break it
                }
            } else {
                crystalTimer--;
            }

        }

        // Place
        if (timer <= 0 && placeBed(placePos)) {
            placePos = null;
            if (Wrapper.isPlayerMoving(target) && !Wrapper.isInHole(target)) {
                if (debug.get()) info("Using moving place delay");
                timer = placeDelayMoving.get();
            } else {
                if (debug.get()) info("Using hole place delay");
                timer = placeDelayHole.get();
            }
            //timer = delay.get();
        }
        else {
            timer--;
        }

        // Break
        if (breakTimer <= 0) {
            if (breakPos == null) breakPos = findBreak();
            breakBed(breakPos);
            if (!Wrapper.isPlayerMoving(target) || Wrapper.isInHole(target)) {
                if (debug.get()) info("Using hole break delay");
                breakTimer = breakDelayHole.get();
            } else {
                if (debug.get()) info("Using moving break delay");
                breakTimer = breakDelayMoving.get();
            }
        } else {
            breakTimer--;
        }
    }


    // Check if we should predict the target's movement
    private boolean shouldPredict(PlayerEntity target) {
        if (predictIgnoreElytra.get() && target.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) return false;
        // only predict if the target is in motion, and outside a hole
        return Wrapper.isPlayerMoving(target) && !Wrapper.isInHole(target);
    }


    private boolean shouldPopOverride(PlayerEntity target, double selfDmg, double targetDmg) {
        if (!popOverride.get()) return false;
        if (PlayerUtils.getTotalHealth() >= popOverridePreHP.get()) {
            // check if the target damage will pop them
            //double targetHealth = target.getHealth() + target.getAbsorptionAmount();
            double targetHealthAfterDamage = EntityUtils.getTotalHealth(target) - targetDmg;
            // check that the user's health after placing will be greater than postHP
            double healthAfter = PlayerUtils.getTotalHealth() - selfDmg;
            return targetHealthAfterDamage <= 0 && healthAfter >= popOverridePostHP.get();
        }
        return false;
    }

    private BlockPos findPlace(PlayerEntity target) {
        if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) return null;
        // loop from players feet -> body -> head
        for (int index = 0; index < 3; index++) {
            int i = index == 0 ? 1 : index == 1 ? 0 : 2;
            // loop through each direction
            for (CardinalDirection dir : CardinalDirection.values()) {
                // strict direction check
                if (strictDirection.get()
                    && dir.toDirection() != mc.player.getHorizontalFacing()
                    && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;
                // starting Position
                BlockPos centerPos = target.getBlockPos().up(i);
                // predict
                if (predictMovement.get() && shouldPredict(target)) {
                    double plusX = Math.round(target.getVelocity().x);
                    double plusY = Math.round(target.getVelocity().y);
                    double plusZ = Math.round(target.getVelocity().z);
                    centerPos = centerPos.add(plusX, plusY, plusZ);
                    //if (debug.get()) ChatUtils.info("predicting next position to " + centerPos.getX() + "," + centerPos.getY() + "," + centerPos.getZ());
                }
                // store damages
                double headSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
                double offsetSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos.offset(dir.toDirection())));
                double targetDamage = DamageUtils.bedDamage(target, Utils.vec3d(centerPos));
                // store the damage for rendering
                nextDamage = targetDamage;
                // store the potential place pos
                BlockPos placePos = centerPos.offset((direction = dir).toDirection());
                // messy code that will be fixed after I ensure everything works
                // first , check if we can place a bed
                if (mc.world.getBlockState(centerPos).getMaterial().isReplaceable() && BlockUtils.canPlace(centerPos.offset(dir.toDirection()))) {
                    // check if we should ignore maxSelfDamage
                    boolean popOverride = shouldPopOverride(target, headSelfDamage, targetDamage);
                    if (popOverride) { // if we are doing pop override
                        if (antiSuicide.get()) { // check if the bed will kill/pop the user, if they have anti suicide on
                            if (PlayerUtils.getTotalHealth() - headSelfDamage > 0) { // if it won't return the placePos
                                return placePos;
                            } else { // return null if it will
                                return null;
                            }
                        } else { // if the user doesn't have anti suicide, return the place pos even if it will pop them
                            return placePos;
                        }
                    } else { // if we aren't doing pop override, check the damages to the user & minDamage
                        if (targetDamage >= minDamage.get()) {
                            if (offsetSelfDamage <= maxSelfDamage.get() && headSelfDamage <= maxSelfDamage.get()) { // continue if the damages are below maxSelfDmg
                                if (antiSuicide.get()) { // check if the bed will kill/pop the user, if they have anti suicide on
                                    if (PlayerUtils.getTotalHealth() - headSelfDamage > 0) { // if it won't return the placePos
                                        return placePos;
                                    } else { // return null if it will
                                        return null;
                                    }
                                }
                            } else { // return null if the damages exceed maxSelfDmg
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                }
                // meteor's current (and cleaner) checks
                //if (mc.world.getBlockState(centerPos).getMaterial().isReplaceable()
                //    && BlockUtils.canPlace(centerPos.offset(dir.toDirection()))
                //    && DamageUtils.bedDamage(target, Utils.vec3d(centerPos)) >= minDamage.get()
                //    && offsetSelfDamage < maxSelfDamage.get()
                //    && headSelfDamage < maxSelfDamage.get()
                //    && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0)
                //    && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0)) {
                //    return centerPos.offset((direction = dir).toDirection());
                //}
            }
        }
        // return null if no valid placements were found
        return null;
    }

    private BlockPos findBreak() {
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (!(blockEntity instanceof BedBlockEntity)) continue;
            BlockPos bedPos = blockEntity.getPos();
            Vec3d bedVec = Utils.vec3d(bedPos);
            // store damages
            double selfDamage = DamageUtils.bedDamage(mc.player, bedVec);
            double targetDamage = DamageUtils.bedDamage(target, bedVec);
            // first check that we can reach to break the bed
            if (PlayerUtils.distanceTo(bedVec) <= mc.interactionManager.getReachDistance()) {
                // this shit is messy but oh well it will be fixed once i ensure it all works
                // check if we should ignore maxSelfDamage
                boolean popOverride = shouldPopOverride(target, selfDamage, targetDamage);
                if (popOverride) { // if we are ignoring maxSelfDamage just do the anti suicide check
                    if (antiSuicide.get()) {
                        if (PlayerUtils.getTotalHealth() - selfDamage > 0) {
                            return bedPos;
                        } else {
                            return null;
                        }
                    } else {
                        return bedPos;
                    }
                } else { // if we aren't ignoring maxSelfDamage, check damages to the user + anti suicide
                    if (selfDamage <= maxSelfDamage.get()) {
                        if (antiSuicide.get()) { // check anti suicide
                            if (PlayerUtils.getTotalHealth() - selfDamage > 0) {
                                return bedPos;
                            } else { // return null if it will pop/kill the user
                                return null;
                            }
                        } else { // return the pos if the user doesn't have anti suicide on
                            return bedPos;
                        }
                    } else { // return null if the damage exceeds maxSelfDamage
                        return null;
                    }
                }
            }

            // meteors current (and cleaner) checks
            //if (PlayerUtils.distanceTo(bedVec) <= mc.interactionManager.getReachDistance()
            //  && DamageUtils.bedDamage(target, bedVec) >= minDamage.get()
            //    && DamageUtils.bedDamage(mc.player, bedVec) < maxSelfDamage.get()
            //    && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - DamageUtils.bedDamage(mc.player, bedVec) > 0)) {
            //    return bedPos;
            //}
        } // return null if no beds were found
        return null;
    }

    private boolean placeBed(BlockPos pos) {
        if (pos == null) return false;
        FindItemResult bed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (bed.getHand() == null && !autoSwitch.get()) return false;
        double yaw = switch (direction) {
            case East -> 90;
            case South -> 180;
            case West -> -90;
            default -> 0;
        };
        Rotations.rotate(yaw, Rotations.getPitch(pos), () -> {
            BlockUtils.place(pos, bed, false, 0, swing.get(), true);
            breakPos = pos;
            placePos = null;
        });
        return true;
    }

    private void breakBed(BlockPos pos) {
        if (pos == null) return;
        breakPos = null;
        if (!(mc.world.getBlockState(pos).getBlock() instanceof BedBlock)) return;
        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) mc.player.setSneaking(false);
        Hand bHand;
        // set which hand we should break the bed with
        if (breakHand.get() == BreakHand.Mainhand) {
            bHand = Hand.MAIN_HAND;
        } else {
            bHand = Hand.OFF_HAND;
        }
        //mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket()); might switch to this or add it as a setting idk
        mc.interactionManager.interactBlock(mc.player, mc.world, bHand, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, false));
        mc.player.setSneaking(wasSneaking);
    }


    // Bed Rendering
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && placePos != null && breakPos == null) {
            int x = placePos.getX();
            int y = placePos.getY();
            int z = placePos.getZ();
            Color sideColor1 = sideColor.get();
            Color lineColor1 = lineColor.get();
            // check if we should render with the pop color
            if (target != null) {
                if (renderPops.get() && (target.getHealth() + target.getAbsorptionAmount()) - nextDamage <= 0) {
                    sideColor1 = sideColorPop.get();
                    lineColor1 = lineColorPop.get();
                }
            }
            // render the placement
            switch (direction) {
                case North -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, sideColor1, lineColor1, shapeMode.get(), 0);
                case South -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor1, lineColor1, shapeMode.get(), 0);
                case East -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor1, lineColor1, shapeMode.get(), 0);
                case West -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, sideColor1, lineColor1, shapeMode.get(), 0);
            }
            // render burrow / web / string mining
            if (renderAutomation.get() && target != null) {
                boolean rendering = Automationutils.isBurrowed(target, false);
                if (Automationutils.isWebbed(target)) rendering = true;
                if (rendering) event.renderer.box(target.getBlockPos(), sideColorAutomation.get(), lineColorAutomation.get(), shapeModeAutomation.get(), 0);
            }
        }
    }

    // Bed Damage Rendering
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (renderDamage.get() && placePos != null && breakPos == null) {
            BlockPos renderPos = new BlockPos(0, 0, 0);
            // render damage in the center pos
            if (renderMode.get() == RenderMode.Center) {
                renderPos = placePos.offset(direction.toDirection().getOpposite());
            } else {
                // render damage on the current direction offset
                renderPos = placePos;
            }
            // store xyz for current break pos
            int x = renderPos.getX();
            int y = renderPos.getY();
            int z = renderPos.getZ();
            Vec3 bedVec = new Vec3();
            bedVec.set(x, y, z);
            if (NametagUtils.to2D(bedVec, damageScale.get())) {
                NametagUtils.begin(bedVec);
                TextRenderer.get().begin(1.0, false, true);
                // get the current place pos damage (calculated and stored from BedUtil find place methods)
                double damage = nextDamage;
                String damageText = "0";
                // handle damage rounding
                switch (roundDamage.get()) {
                    case 0 -> { damageText = String.valueOf(Math.round(damage)); }
                    case 1 -> { damageText = String.valueOf(Math.round(damage * 10.0) / 10.0); }
                    case 2 -> { damageText = String.valueOf(Math.round(damage * 100.0) / 100.0); }
                    case 3 -> { damageText = String.valueOf(Math.round(damage * 1000.0) / 1000.0); }
                }
                // render the damage
                final double w = TextRenderer.get().getWidth(damageText) / 2.0;
                TextRenderer.get().render(damageText, -w, 0.0, damageColor.get());
                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }


    public enum RenderMode {
        Center,
        Offset
    }


    public enum BreakHand {
        Mainhand,
        Offhand
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
