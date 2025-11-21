package com.jawsh.autofarm.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFarm extends Module {
    private enum FilterMode {
        Off,
        Whitelist,
        Blacklist
    }

    private enum ActivationMode {
        Always,
        WhileSneaking,
        WhileNotSneaking
    }

    private enum RunMode {
        Continuous,
        SingleRun
    }

    private enum CropType {
        Wheat,
        Carrot,
        Potato,
        Beetroot,
        NetherWart
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCrops = settings.createGroup("Crops");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General settings
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Horizontal range to farm around you.")
        .defaultValue(4)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Maximum crops harvested per farming cycle.")
        .defaultValue(3)
        .min(1)
        .sliderMax(16)
        .build()
    );

    // Delay between farming cycles (0 by default for max responsiveness)
    private final Setting<Integer> minDelay = sgGeneral.add(new IntSetting.Builder()
        .name("min-delay")
        .description("Minimum delay between farming cycles in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> maxDelay = sgGeneral.add(new IntSetting.Builder()
        .name("max-delay")
        .description("Maximum delay between farming cycles in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(40)
        .build()
    );

    private final Setting<Boolean> replant = sgGeneral.add(new BoolSetting.Builder()
        .name("replant")
        .description("Replant crops after harvesting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stopWhenLowSeeds = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-when-low-seeds")
        .description("Skip harvesting when you don't have enough seeds to replant.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minSeeds = sgGeneral.add(new IntSetting.Builder()
        .name("min-seeds")
        .description("Minimum seeds/crops to keep before harvesting.")
        .defaultValue(4)
        .min(0)
        .sliderMax(64)
        .visible(stopWhenLowSeeds::get)
        .build()
    );

    private final Setting<Boolean> stopWhenFull = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-when-full")
        .description("Stop farming when your inventory has no empty slots.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ActivationMode> activationMode = sgGeneral.add(new EnumSetting.Builder<ActivationMode>()
        .name("activation-mode")
        .description("When the module is allowed to run.")
        .defaultValue(ActivationMode.Always)
        .build()
    );

    private final Setting<RunMode> runMode = sgGeneral.add(new EnumSetting.Builder<RunMode>()
        .name("run-mode")
        .description("Continuous farming or single sweep.")
        .defaultValue(RunMode.Continuous)
        .build()
    );

    // Crop filter
    private final Setting<FilterMode> filterMode = sgCrops.add(new EnumSetting.Builder<FilterMode>()
        .name("crop-filter")
        .description("Controls which crops will be farmed.")
        .defaultValue(FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> wheat = sgCrops.add(new BoolSetting.Builder()
        .name("wheat")
        .description("Include wheat.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> carrots = sgCrops.add(new BoolSetting.Builder()
        .name("carrots")
        .description("Include carrots.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> potatoes = sgCrops.add(new BoolSetting.Builder()
        .name("potatoes")
        .description("Include potatoes.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> beetroot = sgCrops.add(new BoolSetting.Builder()
        .name("beetroot")
        .description("Include beetroot.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> netherWart = sgCrops.add(new BoolSetting.Builder()
        .name("nether-wart")
        .description("Include nether wart.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    // Render
    private final Setting<Boolean> renderTargets = sgRender.add(new BoolSetting.Builder()
        .name("render-targets")
        .description("Render boxes around crops that will be harvested.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> targetColor = sgRender.add(new ColorSetting.Builder()
        .name("target-color")
        .description("Color of the target crop boxes.")
        .defaultValue(new SettingColor(0, 255, 0, 50))
        .build()
    );

    // State
    private int delayTimer = 0;
    private boolean hasFarmedThisRun = false;
    private final List<BlockPos> lastTargets = new ArrayList<>();

    public AutoFarm() {
        super(Categories.Player, "Auto Farmer",
            "Breaks fully-grown crops around you and replants them if seeds are available.");
    }

    @Override
    public void onActivate() {
        delayTimer = 0;
        hasFarmedThisRun = false;
        lastTargets.clear();
    }

    @Override
    public void onDeactivate() {
        lastTargets.clear();
        hasFarmedThisRun = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || mc.player == null || mc.world == null) return;

        // Sneak / activation mode
        switch (activationMode.get()) {
            case WhileSneaking:
                if (!mc.player.isSneaking()) return;
                break;
            case WhileNotSneaking:
                if (mc.player.isSneaking()) return;
                break;
            case Always:
            default:
                break;
        }

        // Inventory full check
        if (stopWhenFull.get() && !hasEmptySlot()) return;

        // Require a hoe somewhere in the hotbar, but DON'T auto-switch
        FindItemResult hoe = InvUtils.findInHotbar(stack -> stack.getItem() instanceof HoeItem);
        if (!hoe.found()) return;

        // Handle delay between farming cycles
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        List<BlockPos> targets = findMatureCrops();
        lastTargets.clear();
        lastTargets.addAll(targets);

        if (targets.isEmpty()) {
            if (runMode.get() == RunMode.SingleRun && hasFarmedThisRun) {
                toggle();
            }
            return;
        }

        int worked = 0;

        for (BlockPos pos : targets) {
            if (worked >= blocksPerTick.get()) break;

            BlockState state = mc.world.getBlockState(pos);
            Block block = state.getBlock();

            if (!isAllowedCrop(block)) continue;
            if (!isMatureCrop(state, block)) continue;

            Item seedItem = getSeedItemFor(block);

            // Seed threshold check
            if (replant.get() && seedItem != null && stopWhenLowSeeds.get()) {
                int totalSeeds = InvUtils.find(seedItem).count();
                if (totalSeeds <= minSeeds.get()) continue;
            }

            // Break crop
            BlockUtils.breakBlock(pos, true);

            // Replant if possible
            if (replant.get()) {
                replantCrop(pos, seedItem);
            }

            worked++;
            hasFarmedThisRun = true;
        }

        if (worked > 0) {
            delayTimer = nextDelay();
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderTargets.get()) return;
        if (lastTargets.isEmpty()) return;

        Color c = targetColor.get();

        for (BlockPos pos : lastTargets) {
            Box box = new Box(pos);
            event.renderer.box(box, c, c, ShapeMode.Both, 0);
        }
    }

    // Scan a generous vertical band around the player [-1..2] for crops
    private List<BlockPos> findMatureCrops() {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        int minY = -1;
        int maxY = 2;

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int dy = minY; dy <= maxY; dy++) {
                    BlockPos pos = center.add(x, dy, z);
                    BlockState state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (!isAllowedCrop(block)) continue;
                    if (!isMatureCrop(state, block)) continue;

                    result.add(pos);
                }
            }
        }

        return result;
    }

    private boolean isMatureCrop(BlockState state, Block block) {
        // Normal overworld crops
        if (block instanceof CropBlock crop) {
            return crop.isMature(state);
        }

        // Nether wart – age 3 is fully grown
        if (block instanceof NetherWartBlock) {
            return state.get(NetherWartBlock.AGE) >= 3;
        }

        return false;
    }

    private CropType getCropType(Block block) {
        if (block == Blocks.WHEAT) return CropType.Wheat;
        if (block == Blocks.CARROTS) return CropType.Carrot;
        if (block == Blocks.POTATOES) return CropType.Potato;
        if (block == Blocks.BEETROOTS) return CropType.Beetroot;
        if (block == Blocks.NETHER_WART) return CropType.NetherWart;
        return null;
    }

    private boolean isAllowedCrop(Block block) {
        CropType type = getCropType(block);
        if (type == null) return false;

        FilterMode mode = filterMode.get();
        if (mode == FilterMode.Off) return true;

        boolean enabled;

        switch (type) {
            case Wheat:
                enabled = wheat.get();
                break;
            case Carrot:
                enabled = carrots.get();
                break;
            case Potato:
                enabled = potatoes.get();
                break;
            case Beetroot:
                enabled = beetroot.get();
                break;
            case NetherWart:
                enabled = netherWart.get();
                break;
            default:
                enabled = false;
                break;
        }

        if (mode == FilterMode.Whitelist) return enabled;
        else return !enabled;
    }

    private Item getSeedItemFor(Block block) {
        if (block == Blocks.WHEAT)       return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS)     return Items.CARROT;
        if (block == Blocks.POTATOES)    return Items.POTATO;
        if (block == Blocks.BEETROOTS)   return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;

        return null;
    }

    private void replantCrop(BlockPos pos, Item seedItem) {
        if (seedItem == null) return;

        BlockPos soilPos = pos.down();
        BlockState soil = mc.world.getBlockState(soilPos);

        boolean soilOk = soil.isOf(Blocks.FARMLAND) || soil.isOf(Blocks.SOUL_SAND);
        if (!soilOk) return;

        if (!mc.world.getBlockState(pos).isAir()) return;

        FindItemResult seeds = InvUtils.findInHotbar(seedItem);
        if (!seeds.found()) return;

        BlockUtils.place(pos, seeds, true, 0, true, true, true);
    }

    private boolean hasEmptySlot() {
        PlayerInventory inv = mc.player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) return true;
        }

        return false;
    }

    private int nextDelay() {
        int min = Math.max(0, minDelay.get());
        int max = Math.max(min, maxDelay.get());

        if (max == 0) return 0;
        if (min == max) return min;

        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
