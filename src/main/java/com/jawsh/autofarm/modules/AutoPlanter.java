package com.jawsh.autofarm.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class AutoPlanter extends Module {
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

    // Mode: normal planting vs pure composter feeding
    private enum Mode {
        PlantCrops,
        CompostersOnly
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

    // General settings
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Horizontal range around you.")
        .defaultValue(4)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> actionsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("actions-per-tick")
        .description("Maximum plant/composter actions per tick.")
        .defaultValue(3)
        .min(1)
        .sliderMax(32)
        .build()
    );

    private final Setting<ActivationMode> activationMode = sgGeneral.add(new EnumSetting.Builder<ActivationMode>()
        .name("activation-mode")
        .description("When the module is allowed to run.")
        .defaultValue(ActivationMode.Always)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Plant crops or only feed composters for bonemeal.")
        .defaultValue(Mode.PlantCrops)
        .build()
    );

    // Crop filter (applies to both planting and composter feeding)
    private final Setting<FilterMode> filterMode = sgCrops.add(new EnumSetting.Builder<FilterMode>()
        .name("crop-filter")
        .description("Controls which crops are planted / fed into composters.")
        .defaultValue(FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> wheat = sgCrops.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
        .name("wheat")
        .description("Allow wheat / wheat seeds.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> carrots = sgCrops.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
        .name("carrots")
        .description("Allow carrots.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> potatoes = sgCrops.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
        .name("potatoes")
        .description("Allow potatoes.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> beetroot = sgCrops.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
        .name("beetroot")
        .description("Allow beetroot / beetroot seeds.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    private final Setting<Boolean> netherWart = sgCrops.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
        .name("nether-wart")
        .description("Allow nether wart.")
        .defaultValue(true)
        .visible(() -> filterMode.get() != FilterMode.Off)
        .build()
    );

    public AutoPlanter() {
        super(Categories.Player, "Auto Planter",
            "Automatically plants crops on farmland/soul sand, or feeds composters for bonemeal.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || mc.player == null || mc.world == null || mc.interactionManager == null) return;

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

        if (mode.get() == Mode.CompostersOnly) {
            handleCompostersOnly();
        } else {
            handlePlanting();
        }
    }

    // ============================================================
    // Normal planting mode: ONLY interacts with farmland/soul sand
    // ============================================================

    private void handlePlanting() {
        List<BlockPos> spots = findPlantSpots();
        if (spots.isEmpty()) return;

        int planted = 0;

        for (BlockPos cropPos : spots) {
            if (planted >= actionsPerTick.get()) break;

            BlockPos soilPos = cropPos.down();
            BlockState soilState = mc.world.getBlockState(soilPos);
            Block soil = soilState.getBlock();

            FindItemResult seed = findSeedForSoil(soil);
            if (seed == null || !seed.found()) continue;

            // Hold the correct item
            InvUtils.swap(seed.slot(), false);

            // Right-click the TOP of the soil block (farmland or soul sand)
            useOnTop(soilPos);

            planted++;
        }
    }

    /**
     * Finds empty spots where we can plant (air above farmland / soul sand).
     * Composter blocks are completely ignored in this mode.
     */
    private List<BlockPos> findPlantSpots() {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos cropPos = center.add(x, dy, z);

                    BlockState state = mc.world.getBlockState(cropPos);
                    if (!state.isAir()) continue;  // only plant into air

                    BlockPos soilPos = cropPos.down();
                    BlockState soilState = mc.world.getBlockState(soilPos);
                    Block soilBlock = soilState.getBlock();

                    // Valid soil: farmland (overworld crops) or soul sand (nether wart)
                    if (soilBlock != Blocks.FARMLAND && soilBlock != Blocks.SOUL_SAND) continue;

                    // Do we actually have something we can plant here?
                    if (!hasAnyAllowedSeedForSoil(soilBlock)) continue;

                    result.add(cropPos);
                }
            }
        }

        return result;
    }

    private boolean hasAnyAllowedSeedForSoil(Block soil) {
        FindItemResult res = findSeedForSoil(soil);
        return res != null && res.found();
    }

    /**
     * Select a seed/crop for this soil type, respecting whitelist/blacklist.
     */
    private FindItemResult findSeedForSoil(Block soil) {
        // Overworld crops on farmland
        if (soil == Blocks.FARMLAND) {
            FindItemResult res;

            if (isAllowedCropType(CropType.Wheat)) {
                res = InvUtils.findInHotbar(Items.WHEAT_SEEDS);
                if (res.found()) return res;
            }

            if (isAllowedCropType(CropType.Carrot)) {
                res = InvUtils.findInHotbar(Items.CARROT);
                if (res.found()) return res;
            }

            if (isAllowedCropType(CropType.Potato)) {
                res = InvUtils.findInHotbar(Items.POTATO);
                if (res.found()) return res;
            }

            if (isAllowedCropType(CropType.Beetroot)) {
                res = InvUtils.findInHotbar(Items.BEETROOT_SEEDS);
                if (res.found()) return res;
            }

            return null;
        }

        // Nether wart on soul sand
        if (soil == Blocks.SOUL_SAND) {
            if (!isAllowedCropType(CropType.NetherWart)) return null;
            FindItemResult res = InvUtils.findInHotbar(Items.NETHER_WART);
            return res != null && res.found() ? res : null;
        }

        return null;
    }

    // ============================================================
    // Composter-only mode: ONLY interacts with composters
    // ============================================================

    private void handleCompostersOnly() {
        List<BlockPos> composters = findComposters();
        if (composters.isEmpty()) return;

        int actions = 0;

        for (BlockPos pos : composters) {
            if (actions >= actionsPerTick.get()) break;

            FindItemResult seed = findSeedForComposter();
            if (seed == null || !seed.found()) break; // nothing left to feed

            // Hold the chosen item
            InvUtils.swap(seed.slot(), false);

            // Right-click the top of the composter
            useOnTop(pos);

            actions++;
        }
    }

    private List<BlockPos> findComposters() {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = center.add(x, dy, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() == Blocks.COMPOSTER) {
                        result.add(pos);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Pick any allowed crop/seed to feed into a composter.
     */
    private FindItemResult findSeedForComposter() {
        FindItemResult res;

        if (isAllowedCropType(CropType.Wheat)) {
            res = InvUtils.findInHotbar(Items.WHEAT_SEEDS);
            if (res.found()) return res;
        }

        if (isAllowedCropType(CropType.Carrot)) {
            res = InvUtils.findInHotbar(Items.CARROT);
            if (res.found()) return res;
        }

        if (isAllowedCropType(CropType.Potato)) {
            res = InvUtils.findInHotbar(Items.POTATO);
            if (res.found()) return res;
        }

        if (isAllowedCropType(CropType.Beetroot)) {
            res = InvUtils.findInHotbar(Items.BEETROOT_SEEDS);
            if (res.found()) return res;
        }

        if (isAllowedCropType(CropType.NetherWart)) {
            res = InvUtils.findInHotbar(Items.NETHER_WART);
            if (res.found()) return res;
        }

        return null;
    }

    // ============================================================
    // Shared helpers
    // ============================================================

    /**
     * Simulates a right-click on the TOP face of the given block.
     */
    private void useOnTop(BlockPos pos) {
        Vec3d hitPos = Vec3d.ofCenter(pos).add(0, 0.5, 0);  // middle of top face
        BlockHitResult hit = new BlockHitResult(hitPos, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isAllowedCropType(CropType type) {
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
        else return !enabled;  // Blacklist
    }
}
