package com.hybri.raildeco.client;

import com.hybri.raildeco.RailDeco;
import com.hybri.raildeco.block.CrossingGateBlock;
import com.hybri.raildeco.block.entity.CrossingGateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 踏切遮断机渲染器。
 *
 * <p>动画完全在客户端根据方块状态与游戏时间推导，无需同步 NBT：
 * 当客户端观察到 {@code POWERED} 状态翻转时记录时间戳，之后按
 * 4.0s（落下）/ 6.1s（抬起）插值计算遮断杆角度。
 */
public class CrossingGateBlockEntityRenderer implements BlockEntityRenderer<CrossingGateBlockEntity> {
    /** 遮断杆完全落下时的角度（模型空间，杆沿 +Z 方向伸出）。 */
    private static final float ARM_CLOSED_ANGLE = 85.0F;
    /** 配重摆幅比例（参考 Addon：配重反向小幅摆动）。 */
    private static final float COUNTERWEIGHT_RATIO = 0.2F;

    /** 模型空间中的铰点（像素坐标，16px = 1 格）。 */
    private static final float PIVOT_X = 8.0F;
    private static final float PIVOT_Y = 18.0F;
    private static final float PIVOT_Z = 8.0F;

    private final RandomSource random = RandomSource.create();

    private long lastChangeTick = -1L;
    private boolean lastPowered = false;

    public CrossingGateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrossingGateBlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = blockEntity.getBlockState();

        boolean powered = state.getValue(CrossingGateBlock.POWERED);
        float time = (float) level.getGameTime() + partialTick;

        if (powered != lastPowered) {
            lastPowered = powered;
            lastChangeTick = level.getGameTime();
        }

        float progress;
        if (lastChangeTick < 0L) {
            // 区块加载时不知道状态改变时刻，直接快照到目标姿态
            progress = powered ? 1.0F : 0.0F;
        } else {
            int duration = powered ? CrossingGateBlockEntity.DOWN_TICKS : CrossingGateBlockEntity.UP_TICKS;
            progress = Mth.clamp((time - lastChangeTick) / (float) duration, 0.0F, 1.0F);
        }

        float angle;
        if (powered) {
            angle = ARM_CLOSED_ANGLE * easeOutCubic(progress);
        } else {
            angle = ARM_CLOSED_ANGLE * (1.0F - easeInOutCubic(progress));
        }

        int length = state.getValue(CrossingGateBlock.LENGTH);
        Direction facing = state.getValue(CrossingGateBlock.FACING);

        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel poleModel = modelManager.getModel(standaloneModel("block/crossing_gate_pole"));
        BakedModel armModel = modelManager.getModel(standaloneModel("block/crossing_gate_arm"));
        BakedModel counterweightModel = modelManager.getModel(standaloneModel("block/crossing_gate_counterweight"));
        BakedModel lampGlowModel = modelManager.getModel(standaloneModel("block/crossing_gate_lamp_glow"));

        VertexConsumer vertices = buffer.getBuffer(RenderType.cutoutMipped());

        pose.pushPose();
        // 与 blockstate 的 facing 旋转保持一致：模型 +Z 为遮断杆伸出方向
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        pose.translate(-0.5, 0.0, -0.5);
        // 模型 JSON 使用 16px = 1 格坐标系
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        // 立柱 + 灯罩 + 警铃（静态）
        renderBakedModel(poleModel, state, pose, vertices, packedLight, packedOverlay);

        // 遮断杆：绕铰点沿 X 轴向下旋转，并按 LENGTH 拉长
        pose.pushPose();
        pose.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.scale(1.0F, 1.0F, length);
        renderBakedModel(armModel, state, pose, vertices, packedLight, packedOverlay);
        pose.popPose();

        // 配重：位于铰点后方（-Z），反向小幅摆动
        pose.pushPose();
        pose.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        pose.mulPose(Axis.XP.rotationDegrees(angle * COUNTERWEIGHT_RATIO));
        pose.translate(0.0F, 0.0F, -8.0F);
        renderBakedModel(counterweightModel, state, pose, vertices, packedLight, packedOverlay);
        pose.popPose();

        // 警灯：通电时左右交替闪烁（全亮度）
        if (powered) {
            boolean leftOn = ((long) time / CrossingGateBlockEntity.FLASH_PERIOD) % 2 == 0;
            float lampX = leftOn ? 3.3F : 11.3F;
            pose.pushPose();
            pose.translate(lampX, 30.6F, 7.0F);
            renderBakedModel(lampGlowModel, state, pose, vertices, LightTexture.FULL_BRIGHT, packedOverlay);
            pose.popPose();
        }

        pose.popPose();
    }

    private void renderBakedModel(BakedModel model, BlockState state, PoseStack pose, VertexConsumer vertices,
                                  int packedLight, int packedOverlay) {
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, direction, random, ModelData.EMPTY, null)) {
                vertices.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay, true);
            }
        }
    }

    /** 下落：先快后慢。 */
    private static float easeOutCubic(float p) {
        float q = 1.0F - p;
        return 1.0F - q * q * q;
    }

    /** 抬起：缓慢起步、缓慢收尾。 */
    private static float easeInOutCubic(float p) {
        return p < 0.5F ? 4.0F * p * p * p : 1.0F - (float) Math.pow(-2.0F * p + 2.0F, 3.0F) / 2.0F;
    }

    private static ModelResourceLocation standaloneModel(String path) {
        return new ModelResourceLocation(RailDeco.id(path), ModelResourceLocation.STANDALONE_VARIANT);
    }
}