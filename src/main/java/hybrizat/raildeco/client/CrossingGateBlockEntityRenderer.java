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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * ???????????/???????????OCR / ?????? Addon????????
 * ????????????? 32 ????????????????????????
 *
 * <p>??????????+X ??????????????????????
 * ????????????????????????????????????????????
 */
public class CrossingGateBlockEntityRenderer implements BlockEntityRenderer<CrossingGateBlockEntity> {
    /** ?????????????? */
    private static final float ARM_UP_ANGLE = 90.0F;
    /** ???????????????? */
    private static final float ARM_DOWN_ANGLE = 0.0F;

    /** ? LENGTH(1~8) ????????????????????????? crgt_a1m_l ~ crgt_a8m_l */
    private static final float[] ARM_LENGTHS = {19.5F, 29.5F, 45.5F, 61.5F, 77.5F, 92.5F, 108.5F, 124.5F};

    /** ??????? = ???? + ?? (8, 0, 9.25)? */
    private static final float HINGE_X = 8.5F;
    private static final float HINGE_Y = 17.5F;
    private static final float HINGE_Z = 12.25F;

    /** ?????????? 2.7x1.3 ?????????y 15-19, z 11.25-12.75??? 0.1 ?????? */
    private static final float ARM_H = 2.7F, ARM_W = 1.3F, ARM_Y = 16.15F;
    /** ??? Z ????? 2.75 + 9.25? */
    private static final float ARM_Z_CENTER = 12.0F;

    /** ???????2048 ???uv = ?? / 128?????? 447~450 ???? 7px = 7 ?? */
    private static final float STRIPE_U0 = 1.0F / 128.0F;
    private static final float STRIPE_PERIOD = 7.0F;
    private static final float STRIPE_V0 = 447.0F / 128.0F;
    private static final float STRIPE_V1 = 450.0F / 128.0F;
    /** ?????/??/?????? (98,22) ??? */
    private static final float BLACK_U = 98.0F / 128.0F;
    private static final float BLACK_V = 22.0F / 128.0F;

    private static boolean debugLogged = false;

    private final RandomSource random = RandomSource.create();
    private TextureAtlasSprite sprite;

    private long lastChangeTick = -1L;
    private boolean lastPowered = false;

    public CrossingGateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(CrossingGateBlockEntity blockEntity) {
        return true;
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
            angle = Mth.lerp(easeOutCubic(progress), ARM_UP_ANGLE, ARM_DOWN_ANGLE);
        } else {
            angle = Mth.lerp(easeInOutCubic(progress), ARM_DOWN_ANGLE, ARM_UP_ANGLE);
        }

        int length = state.getValue(CrossingGateBlock.LENGTH);
        Direction facing = state.getValue(CrossingGateBlock.FACING);

        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel bodyModel = modelManager.getModel(standaloneModel("block/crossing_gate_body_short"));
        BakedModel lampGlowModel = modelManager.getModel(standaloneModel("block/crossing_gate_lamp_glow"));

        if (!debugLogged) {
            debugLogged = true;
            int quads = countQuads(bodyModel, state) + countQuads(lampGlowModel, state);
            RailDeco.LOGGER.info("[RailDeco] crossing gate renderer active at {}; bodyMissing={}; lampMissing={}; totalQuads={}; length={}",
                    blockEntity.getBlockPos(),
                    bodyModel == modelManager.getMissingModel(),
                    lampGlowModel == modelManager.getMissingModel(),
                    quads,
                    length);
        }

        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(RailDeco.id("block/crossing_gate"));
        }

        VertexConsumer vertices = buffer.getBuffer(RenderType.solid());

        pose.pushPose();
        // ??? +X?????? ?????????????FACING ????
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot() - 90.0F));
        pose.translate(-0.5, 0.0, -0.5);

        // ?????????????????0~1?????????
        // ????? + ?? + ????
        renderBakedModel(bodyModel, state, pose, vertices, packedLight, packedOverlay);

        // ???????????????????????????????????????
        if (powered) {
            boolean leftOn = ((long) time / CrossingGateBlockEntity.FLASH_PERIOD) % 2 == 0;
            if (leftOn) {
                pose.pushPose();
                pose.translate(8.0F / 16.0F, 16.0F / 16.0F, 9.25F / 16.0F);
                renderBakedModel(lampGlowModel, state, pose, vertices, LightTexture.FULL_BRIGHT, packedOverlay);
                pose.popPose();
            }
        }

        // ????????????????0~16 = 1 ??????? 1/16?cutout ???? mipmap ??????
        VertexConsumer armVertices = buffer.getBuffer(RenderType.cutout());
        pose.pushPose();
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        pose.translate(HINGE_X, HINGE_Y, HINGE_Z);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-HINGE_X, -HINGE_Y, -HINGE_Z);
        drawArm(armVertices, pose, length, packedLight, packedOverlay);
        pose.popPose();

        pose.popPose();
    }

    /**
     * ????????????? +X ???????? 7 ??????????/????
     * ???????? ? 6 ??????????/??????????????
     */
    private void drawArm(VertexConsumer vertices, PoseStack pose, int length, int packedLight, int packedOverlay) {
        float len = ARM_LENGTHS[length - 1];
        float y0 = ARM_Y;
        float y1 = y0 + ARM_H;
        float z0 = ARM_Z_CENTER - ARM_W / 2.0F;
        float z1 = ARM_Z_CENTER + ARM_W / 2.0F;
        float x0 = HINGE_X;
        float x1 = x0 + len;

        // ??????????????????????????
        for (float xs = x0; xs < x1; xs += STRIPE_PERIOD) {
            float xe = Math.min(xs + STRIPE_PERIOD, x1);
            float u0 = STRIPE_U0;
            float u1 = STRIPE_U0 + (xe - xs) / 128.0F;
            // ???+Z?
            quad(vertices, pose, packedLight, packedOverlay, 0.0F, 0.0F, 1.0F,
                    xs, y0, z1, xe, y0, z1, xe, y1, z1, xs, y1, z1,
                    u0, STRIPE_V1, u1, STRIPE_V1, u1, STRIPE_V0, u0, STRIPE_V0);
            // ???-Z??u ?? x ??
            quad(vertices, pose, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F,
                    xe, y0, z0, xs, y0, z0, xs, y1, z0, xe, y1, z0,
                    u1, STRIPE_V1, u0, STRIPE_V1, u0, STRIPE_V0, u1, STRIPE_V0);
        }

        // ?/?/???????????????
        drawBox(vertices, pose, packedLight, packedOverlay, x0, y0, z0, x1, y1, z1,
                BLACK_U, BLACK_V, false);

        // ??/?????? crgt_a6m+ ? shadankanL ?????????????
        if (length >= 6) {
            drawBox(vertices, pose, packedLight, packedOverlay, -3.0F, 16.0F, 11.75F, 6.0F, 17.0F, 12.75F,
                    BLACK_U, BLACK_V, true);
            drawBox(vertices, pose, packedLight, packedOverlay, -2.5F, 15.0F, 11.5F, 0.0F, 20.0F, 12.75F,
                    BLACK_U, BLACK_V, true);
            drawBox(vertices, pose, packedLight, packedOverlay, 0.5F, 15.0F, 11.5F, 3.0F, 20.0F, 12.75F,
                    BLACK_U, BLACK_V, true);
        }
    }

    /** ?????????includeNS=false ????????????????????? */
    private void drawBox(VertexConsumer vertices, PoseStack pose, int packedLight, int packedOverlay,
                         float x0, float y0, float z0, float x1, float y1, float z1,
                         float u, float v, boolean includeNS) {
        quad(vertices, pose, packedLight, packedOverlay, 0.0F, 1.0F, 0.0F,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u, v, u, v, u, v, u, v);
        quad(vertices, pose, packedLight, packedOverlay, 0.0F, -1.0F, 0.0F,
                x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u, v, u, v, u, v, u, v);
        quad(vertices, pose, packedLight, packedOverlay, 1.0F, 0.0F, 0.0F,
                x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, u, v, u, v, u, v, u, v);
        quad(vertices, pose, packedLight, packedOverlay, -1.0F, 0.0F, 0.0F,
                x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, u, v, u, v, u, v, u, v);
        if (includeNS) {
            quad(vertices, pose, packedLight, packedOverlay, 0.0F, 0.0F, 1.0F,
                    x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u, v, u, v, u, v, u, v);
            quad(vertices, pose, packedLight, packedOverlay, 0.0F, 0.0F, -1.0F,
                    x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u, v, u, v, u, v, u, v);
        }
    }

    private void quad(VertexConsumer vertices, PoseStack pose, int packedLight, int packedOverlay,
                      float nx, float ny, float nz,
                      float x0, float y0, float z0, float x1, float y1, float z1,
                      float x2, float y2, float z2, float x3, float y3, float z3,
                      float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        addVertex(vertices, pose, x0, y0, z0, u0, v0, nx, ny, nz, packedLight, packedOverlay);
        addVertex(vertices, pose, x1, y1, z1, u1, v1, nx, ny, nz, packedLight, packedOverlay);
        addVertex(vertices, pose, x2, y2, z2, u2, v2, nx, ny, nz, packedLight, packedOverlay);
        addVertex(vertices, pose, x3, y3, z3, u3, v3, nx, ny, nz, packedLight, packedOverlay);
    }

    private void addVertex(VertexConsumer vertices, PoseStack pose,
                           float x, float y, float z, float u, float v,
                           float nx, float ny, float nz, int packedLight, int packedOverlay) {
        vertices.addVertex(pose.last(), x, y, z)
                .setUv(sprite.getU(u / 16.0F), sprite.getV(v / 16.0F))
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setNormal(pose.last(), nx, ny, nz)
                .setLight(packedLight)
                .setOverlay(packedOverlay);
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

    /** ??????? */
    private static float easeOutCubic(float p) {
        float q = 1.0F - p;
        return 1.0F - q * q * q;
    }

    /** ??????? */
    private static float easeInOutCubic(float p) {
        return p < 0.5F ? 4.0F * p * p * p : 1.0F - (float) Math.pow(-2.0F * p + 2.0F, 3.0F) / 2.0F;
    }

    private static ModelResourceLocation standaloneModel(String path) {
        return new ModelResourceLocation(RailDeco.id(path), ModelResourceLocation.STANDALONE_VARIANT);
    }
}
