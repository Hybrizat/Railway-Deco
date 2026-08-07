package hybrizat.raildeco.client;

import hybrizat.raildeco.RailDeco;
import hybrizat.raildeco.block.CrossingGateBlock;
import hybrizat.raildeco.block.entity.CrossingGateBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 踏切遮断机渲染器（造型参考「大宫铁道踏切 Addon / OCR」）。
 *
 * <p>模型坐标限制：Minecraft 方块模型元素必须在 0~16 之间，
 * 因此立柱/灯罩拆成上下两组模型，由本渲染器拼装。
 */
public class CrossingGateBlockEntityRenderer implements BlockEntityRenderer<CrossingGateBlockEntity> {
    /** 遮断杆完全落下时的角度（模型空间，杆沿 +Z 方向伸出）。 */
    private static final float ARM_CLOSED_ANGLE = 85.0F;

    private static boolean debugLogged = false;

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
        BakedModel lowerModel = modelManager.getModel(standaloneModel("block/crossing_gate_lower"));
        BakedModel upperModel = modelManager.getModel(standaloneModel("block/crossing_gate_upper"));
        BakedModel armModel = modelManager.getModel(standaloneModel("block/crossing_gate_arm"));
        BakedModel lampGlowModel = modelManager.getModel(standaloneModel("block/crossing_gate_lamp_glow"));

        if (!debugLogged) {
            debugLogged = true;
            int quads = countQuads(lowerModel, state) + countQuads(upperModel, state)
                + countQuads(armModel, state) + countQuads(lampGlowModel, state);
            RailDeco.LOGGER.info("[RailDeco] crossing gate renderer active at {}; missing={}/{}/{}/{}; totalQuads={}",
                blockEntity.getBlockPos(),
                lowerModel == modelManager.getMissingModel(),
                upperModel == modelManager.getMissingModel(),
                armModel == modelManager.getMissingModel(),
                lampGlowModel == modelManager.getMissingModel(),
                quads);
        }

        VertexConsumer vertices = buffer.getBuffer(RenderType.solid());

        pose.pushPose();
        // 与 blockstate 的 facing 旋转保持一致：模型 +Z 为遮断杆伸出方向
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        pose.translate(-0.5, 0.0, -0.5);
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        // 底座 + 立柱下半
        renderBakedModel(lowerModel, state, pose, vertices, packedLight, packedOverlay);

        // 立柱上半 + 警灯罩 + 配重（整体上移 12px）
        pose.pushPose();
        pose.translate(0.0F, 12.0F, 0.0F);
        renderBakedModel(upperModel, state, pose, vertices, packedLight, packedOverlay);
        pose.popPose();

        // 遮断杆：从灯罩前缘伸出，绕 X 轴旋转（+Z 尖端向下），按 LENGTH 拉长
        pose.pushPose();
        pose.translate(8.0F, 17.5F, 10.0F);
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.scale(1.0F, 1.0F, length);
        renderBakedModel(armModel, state, pose, vertices, packedLight, packedOverlay);
        pose.popPose();

        // 警灯：通电时左右交替闪烁（全亮度），位于灯罩两侧
        if (powered) {
            boolean leftOn = ((long) time / CrossingGateBlockEntity.FLASH_PERIOD) % 2 == 0;
            float lampX = leftOn ? 3.8F : 11.7F;
            pose.pushPose();
            pose.translate(lampX, 18.0F, 8.0F);
            renderBakedModel(lampGlowModel, state, pose, vertices, LightTexture.FULL_BRIGHT, packedOverlay);
            pose.popPose();
        }

        pose.popPose();
    }

    /** ?????? cullface ?????? null??????????????????? */
    private static final Direction[] RENDER_DIRECTIONS = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private int countQuads(BakedModel model, BlockState state) {
        int count = 0;
        for (Direction direction : RENDER_DIRECTIONS) {
            random.setSeed(42L);
            count += model.getQuads(state, direction, random, ModelData.EMPTY, null).size();
        }
        return count;
    }

    private void renderBakedModel(BakedModel model, BlockState state, PoseStack pose, VertexConsumer vertices,
                                  int packedLight, int packedOverlay) {
        for (Direction direction : RENDER_DIRECTIONS) {
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