package me.ghosttypes.reaper.modules.combat;

import net.minecraft.block.PillarBlock;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.Wrapper;
import com.google.common.util.concurrent.AtomicDouble;
import it.unimi.dsi.fastutil.ints.*;
import me.ghosttypes.reaper.modules.ML;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import me.ghosttypes.reaper.util.player.InvHelper;
import me.ghosttypes.reaper.util.player.ItemHelper;
import me.ghosttypes.reaper.util.world.BlockHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.item.BedItem;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.tick.Tick;

import java.util.List;

public class AutoBedCraft extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWait = settings.createGroup("Pause");
    private final SettingGroup sgAuto = settings.createGroup("Auto (Buggy)");
    private final Setting<Boolean> disableAfter = sgGeneral.add(new BoolSetting.Builder().name("disable-after").description("Toggle off after filling your inv with beds.").defaultValue(false).build());
    private final Setting<Boolean> disableNoMats = sgGeneral.add(new BoolSetting.Builder().name("disable-on-no-mats").description("Toggle off if you run out of material.").defaultValue(false).build());
    private final Setting<Boolean> closeAfter = sgGeneral.add(new BoolSetting.Builder().name("close-after").description("Close the crafting GUI after filling.").defaultValue(true).build());

    private final Setting<Boolean> automatic = sgAuto.add(new BoolSetting.Builder().name("automatic").description("Automatically place/search for and open crafting tables when you're out of beds.").defaultValue(false).build());
    private final Setting<Boolean> antiDesync = sgAuto.add(new BoolSetting.Builder().name("anti-desync").description("Try to prevent inventory desync.").defaultValue(false).build());
    private final Setting<Boolean> debug = sgAuto.add(new BoolSetting.Builder().name("debug").description("Don't enable").defaultValue(false).build());
    private final Setting<Boolean> chatInfo = sgAuto.add(new BoolSetting.Builder().name("chat-info").description("Alerts you in chat when auto refill is starting.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyHole = sgAuto.add(new BoolSetting.Builder().name("in-hole-only").description("Only auto refill while in a hole.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyGround = sgAuto.add(new BoolSetting.Builder().name("on-ground-only").description("Only auto refill while on the ground.").defaultValue(false).build());
    private final Setting<Boolean> autoWhileMoving = sgAuto.add(new BoolSetting.Builder().name("while-moving").description("Allow auto refill while in motion").defaultValue(false).build());
    private final Setting<Integer> emptySlotsNeeded = sgAuto.add(new IntSetting.Builder().name("required-empty-slots").description("How many empty slots are required for activation.").defaultValue(5).min(1).build());
    private final Setting<Integer> radius = sgAuto.add(new IntSetting.Builder().name("radius").description("How far to search for crafting tables near you.").defaultValue(3).min(1).build());
    private final Setting<Double> minHealth = sgAuto.add(new DoubleSetting.Builder().name("min-health").description("Min health require to activate.").defaultValue(10).min(1).max(36).sliderMax(36).build());

    private final Setting<Boolean> eatPause = sgWait.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses Crystal Aura when eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> drinkPause = sgWait.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses Crystal Aura when drinking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> minePause = sgWait.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses Crystal Aura when mining.")
        .defaultValue(false)
        .build()
    );


    public AutoBedCraft() {
        super(ML.R, "AutoBedCraft", "Crafts bed for u");
    }

    private boolean didRefill = false;
    private boolean startedRefill = false;
    private boolean alertedNoMats = false;


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (PlayerUtils.getTotalHealth() <= minHealth.get()) return;
        if (automatic.get() && isOutOfMaterial() && !alertedNoMats) {
            error("Cannot activate auto mode, no material left.");
            alertedNoMats = true;
        }
        if (automatic.get() && needsRefill() && canRefill(true) && !isOutOfMaterial() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
            FindItemResult craftTable = ItemHelper.findCraftTable();
            if (!craftTable.found()) {
                toggle();
                error("No crafting tables in hotbar!");
                return;
            }
            if (debug.get()) info("Searching for nearby crafting tables");
            BlockPos tablePos;
            tablePos = findCraftingTable();
            if (tablePos == null) {
                if (debug.get()) info("None nearby, placing table and returning.");
                placeCraftingTable(craftTable);
                return;
            }
            if (debug.get()) info("Located usable crafting table, opening and refilling");
            openCraftingTable(tablePos);
            if (chatInfo.get() && !startedRefill) {
                info("Refilling...");
                startedRefill = true;
            }
            didRefill = true;
            return;
        }
        if (didRefill && !needsRefill()) {
            if (chatInfo.get()) info("Refill complete.");
            didRefill = false;
            startedRefill = false;
            if (debug.get()) info("Automatic finished.");
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            if (!canRefill(false)) {
                if (debug.get()) info("Cancelling current refill because canRefill is false");
                mc.player.closeHandledScreen();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            CraftingScreenHandler currentScreenHandler = (CraftingScreenHandler) mc.player.currentScreenHandler;
            if (isOutOfPlanks()) {
                if (chatInfo.get()) error("You are of planks making more!");
                if (disableNoMats.get()) toggle();
                mc.player.closeHandledScreen();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            if (InvHelper.isInventoryFull()) {
                if (disableAfter.get()) toggle();
                if (closeAfter.get()) {
                    mc.player.closeHandledScreen();
                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                }
                if (chatInfo.get() && !automatic.get()) info("Your inventory is full.");
                return;
            }
            List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);
            for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
                for (Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                    if (recipe.getOutput().getItem() instanceof BedItem) {
                        assert mc.interactionManager != null;
                        mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, false);
                        windowClick(currentScreenHandler, 0, SlotActionType.QUICK_MOVE, 1);
                    }
                }
            }
        }
    }
    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (PlayerUtils.shouldPause(minePause.get(), eatPause.get(), drinkPause.get())) return;
    }

    private void placeCraftingTable(FindItemResult craftTable) {
        List<BlockPos> nearbyBlocks = BlockHelper.getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) {
            if (BlockHelper.getBlock(block) == Blocks.AIR) {
                BlockUtils.place(block, craftTable, 0, true);
                break;
            }
        }
    }

    private BlockPos findCraftingTable() {
        List<BlockPos> nearbyBlocks = BlockHelper.getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) if (BlockHelper.getBlock(block) == Blocks.CRAFTING_TABLE) return block;
        return null;
    }

    private void openCraftingTable(BlockPos tablePos) {
        Vec3d tableVec = new Vec3d(tablePos.getX(), tablePos.getY(), tablePos.getZ());
        BlockHitResult table = new BlockHitResult(tableVec, Direction.UP, tablePos, false);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, table);
    }

    private boolean needsRefill() {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!bed.found()) return true;
        return InvHelper.isInventoryFull();
    }


    private boolean canRefill(boolean checkSlots) {
        if (!autoWhileMoving.get() && Wrapper.isPlayerMoving(mc.player)) return false;
        if (autoOnlyHole.get() && !Wrapper.isInHole(mc.player)) return false;
        if (autoOnlyGround.get() && !mc.player.isOnGround()) return false;
        if (InvHelper.isInventoryFull()) return false;
        if (checkSlots) if (InvHelper.getEmptySlots() < emptySlotsNeeded.get()) return false;
        return !(PlayerUtils.getTotalHealth() <= minHealth.get());
    }

    private boolean isOutOfMaterial() {
        FindItemResult wool = InvUtils.find(itemStack -> ItemHelper.wools.contains(itemStack.getItem()));
        FindItemResult plank = InvUtils.find(itemStack -> ItemHelper.planks.contains(itemStack.getItem()));
        FindItemResult craftTable = ItemHelper.findCraftTable();
        if (!craftTable.found()) return true;
        if (!wool.found() || !plank.found()) return true;
        return wool.count() < 3 || plank.count() < 3;
    }

    private boolean isOutOfPlanks() {
        FindItemResult plank = InvUtils.find(itemStack -> ItemHelper.planks.contains(itemStack.getItem()));
        if (!plank.found()) return true;
        return plank.count() < 3;
    }


    private void windowClick(ScreenHandler container, int slot, SlotActionType action, int clickData) {
        assert mc.interactionManager != null;
        mc.interactionManager.clickSlot(container.syncId, slot, clickData, action, mc.player);
    }
}
