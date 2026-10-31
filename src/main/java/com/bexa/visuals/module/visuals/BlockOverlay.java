package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/** Qarab turgan blokni to'ldirilgan rang + kontur bilan ajratadi. */
public class BlockOverlay extends Module {
    private final BoolSetting fill = add(new BoolSetting("Fill", true));
    private final BoolSetting outline = add(new BoolSetting("Outline", true));
    private final NumberSetting fillAlpha = add(new NumberSetting("Fill Alpha", 0.25, 0.0, 1.0, 0.05));
    private final NumberSetting outlineWidth = add(new NumberSetting("Outline Width", 1.5, 0.5, 4.0, 0.1));
    private final ColorSetting color = add(new ColorSetting("Color", ColorUtils.RED));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));

    public BlockOverlay() {
        super("Block Overlay", "Highlights the block you're looking at", Category.VISUALS);
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (mc.world == null || mc.player == null) return;
        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() == HitResult.Type.MISS) return;
        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        Box box = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos);

        if (!Render3D.begin(stack, cam, throughWalls.get())) return;
        try {
            int base = color.getColor();
            if (fill.get()) {
                int fc = ColorUtils.mulAlpha(base, fillAlpha.getF());
                Render3D.box(box.minX - 0.0015, box.minY - 0.0015, box.minZ - 0.0015,
                        box.maxX + 0.0015, box.maxY + 0.0015, box.maxZ + 0.0015, fc, fc);
            }
            if (outline.get()) {
                int oc = ColorUtils.withAlpha(base, 255);
                Render3D.boxLines(box.minX - 0.001, box.minY - 0.001, box.minZ - 0.001,
                        box.maxX + 0.001, box.maxY + 0.001, box.maxZ + 0.001, outlineWidth.get() * 0.0055, oc);
            }
        } finally {
            Render3D.end();
        }
    }
}
