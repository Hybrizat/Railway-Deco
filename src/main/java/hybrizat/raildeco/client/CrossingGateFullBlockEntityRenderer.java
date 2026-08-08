package hybrizat.raildeco.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderStateShard;
import hybrizat.raildeco.RailDeco;
import hybrizat.raildeco.block.CrossingGateFullBlock;
import hybrizat.raildeco.block.entity.CrossingGateFullBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ??????? OCR ? cr1???????????????? Bedrock ?????
 * ??????????????????????????????????
 * ??????/??????
 */
public class CrossingGateFullBlockEntityRenderer implements BlockEntityRenderer<CrossingGateFullBlockEntity> {

    /** ???? = ???? + ?????????????????????? */
    private static float offsetX = 8.0F;
    private static float offsetY = 0.0F;
    private static float offsetZ = 8.0F;

    private static final List<Cube> STATIC_CUBES = new ArrayList<>();
    private static final List<Cube> LIGHT_ON_LEFT = new ArrayList<>();
    private static final List<Cube> LIGHT_ON_RIGHT = new ArrayList<>();
    private static final List<Cube> LIGHT_OFF_LEFT = new ArrayList<>();
    private static final List<Cube> LIGHT_OFF_RIGHT = new ArrayList<>();
    private static boolean parsed = false;

    /** ?????????????????????? */
    private static final RenderType LIGHT_TRANSLUCENT = RenderType.create(
            "rail_deco_cr1_lamp_translucent",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            262144,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setOverlayState(RenderType.OVERLAY)
                    .createCompositeState(true));

    /** ?????? cutout ???????/??????????????????????? */
    private static final RenderType NO_CULL_CUTOUT = RenderType.create(
            "rail_deco_cr1_cutout_nocull",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            262144,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_CUTOUT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(RenderType.NO_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setOverlayState(RenderType.OVERLAY)
                    .createCompositeState(false));

    private TextureAtlasSprite sprite;

    public CrossingGateFullBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(CrossingGateFullBlockEntity blockEntity) {
        return true;
    }

    private static final class Cube {
        final float[] origin = new float[3];
        final float[] size = new float[3];
        final float[] pivot = new float[3];
        final float[] rotation = new float[3];
        /** direction(lowercase) -> [u1,v1,u2,v2] in texture pixels */
        final List<String[]> faces = new ArrayList<>();
        final List<float[]> faceUvs = new ArrayList<>();
    }

    private static synchronized void ensureParsed() {
        if (parsed) {
            return;
        }
        parsed = true;
        try {
            ResourceLocation geoId = RailDeco.id("models/entity/cr1.geo.json");
            try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(geoId)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray geos = root.getAsJsonArray("minecraft:geometry");
                JsonObject geo = geos.get(0).getAsJsonObject();
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
                for (JsonElement boneEl : geo.getAsJsonArray("bones")) {
                    JsonObject bone = boneEl.getAsJsonObject();
                    String boneName = bone.get("name").getAsString().toLowerCase(Locale.ROOT);
                    if (boneName.startsWith("meiban")) {
                        // ??????????????
                        continue;
                    }
                    boolean isKeihyo = boneName.startsWith("keihyo");
                    boolean isLightOn = boneName.startsWith("lightlon") || boneName.startsWith("lightron");
                    boolean isLightOff = boneName.startsWith("lightloff") || boneName.startsWith("lightroff");
                    boolean isLeft = boneName.startsWith("lightlon") || boneName.startsWith("lightloff");
                    JsonElement cubesEl = bone.get("cubes");
                    if (cubesEl == null || !cubesEl.isJsonArray()) {
                        continue;
                    }
                    for (JsonElement cubeEl : cubesEl.getAsJsonArray()) {
                        JsonObject c = cubeEl.getAsJsonObject();
                        Cube cube = new Cube();
                        float[] origin = readFloat3(c, "origin");
                        float[] size = readFloat3(c, "size");
                        float[] pivot = readFloat3(c, "pivot");
                        float[] rotation = readFloat3(c, "rotation");
                        // ?????keihyo????????? X ??????????????
                        // ??? x/z ??????????????
                        if (isKeihyo) {
                            origin = new float[]{origin[2], origin[1], origin[0]};
                            size = new float[]{size[2], size[1], size[0]};
                            pivot = new float[]{pivot[2], pivot[1], pivot[0]};
                            rotation = new float[]{0.0F, 0.0F, rotation[0]};
                            // ????? 0.05 -> 0.5????????
                            if (size[2] < 0.5F) {
                                float delta = (0.5F - size[2]) / 2.0F;
                                origin[2] -= delta;
                                size[2] = 0.5F;
                            }
                        }
                        System.arraycopy(origin, 0, cube.origin, 0, 3);
                        System.arraycopy(size, 0, cube.size, 0, 3);
                        System.arraycopy(pivot, 0, cube.pivot, 0, 3);
                        System.arraycopy(rotation, 0, cube.rotation, 0, 3);
                        JsonObject uv = c.getAsJsonObject("uv");
                        float[] keihyoStripeUv = null;
                        for (String dir : new String[]{"north", "east", "south", "west", "up", "down"}) {
                            JsonElement faceEl = uv.get(dir);
                            if (faceEl == null) {
                                continue;
                            }
                            JsonObject face = faceEl.getAsJsonObject();
                            float[] uvOrigin = readFloat2(face, "uv");
                            float[] uvSize = readFloat2(face, "uv_size");
                            String mappedDir = dir;
                            if (isKeihyo) {
                                // x<->z ????north<->west, south<->east
                                mappedDir = switch (dir) {
                                    case "north" -> "west";
                                    case "south" -> "east";
                                    case "east" -> "south";
                                    case "west" -> "north";
                                    default -> dir;
                                };
                                // ????????????????????east???????????
                                if (dir.equals("east")) {
                                    keihyoStripeUv = new float[]{uvOrigin[0], uvOrigin[1], uvOrigin[0] + uvSize[0], uvOrigin[1] + uvSize[1]};
                                } else if (dir.equals("west")) {
                                    if (keihyoStripeUv != null) {
                                        uvOrigin = new float[]{keihyoStripeUv[0], keihyoStripeUv[1]};
                                        uvSize = new float[]{keihyoStripeUv[2] - keihyoStripeUv[0], keihyoStripeUv[3] - keihyoStripeUv[1]};
                                    }
                                }
                            }
                            cube.faces.add(new String[]{mappedDir});
                            cube.faceUvs.add(new float[]{uvOrigin[0], uvOrigin[1], uvOrigin[0] + uvSize[0], uvOrigin[1] + uvSize[1]});
                        }
                        // ????????
                        float x0 = origin[0], y0 = origin[1], z0 = origin[2];
                        float x1 = origin[0] + size[0], y1 = origin[1] + size[1], z1 = origin[2] + size[2];
                        minX = Math.min(minX, x0); maxX = Math.max(maxX, x1);
                        minY = Math.min(minY, y0); maxY = Math.max(maxY, y1);
                        minZ = Math.min(minZ, z0); maxZ = Math.max(maxZ, z1);
                        if (isLightOn) {
                            (isLeft ? LIGHT_ON_LEFT : LIGHT_ON_RIGHT).add(cube);
                        } else if (isLightOff) {
                            (isLeft ? LIGHT_OFF_LEFT : LIGHT_OFF_RIGHT).add(cube);
                        } else {
                            STATIC_CUBES.add(cube);
                        }
                    }
                }
                if (minX < maxX && minY < maxY) {
                    offsetX = 8.0F - (minX + maxX) / 2.0F;
                    offsetZ = 8.0F - (minZ + maxZ) / 2.0F;
                    offsetY = -minY;
                }
                RailDeco.LOGGER.info("[RailDeco] cr1 geometry loaded: static={} lightOnL={} lightOnR={} lightOffL={} lightOffR={} bounds=({},{},{})-({},{},{})",
                        STATIC_CUBES.size(), LIGHT_ON_LEFT.size(), LIGHT_ON_RIGHT.size(), LIGHT_OFF_LEFT.size(), LIGHT_OFF_RIGHT.size(),
                        minX, minY, minZ, maxX, maxY, maxZ);
            }
        } catch (Exception e) {
            RailDeco.LOGGER.error("[RailDeco] failed to parse cr1 geometry", e);
        }
    }

    private static float[] readFloat3(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonArray()) {
            return new float[]{0, 0, 0};
        }
        JsonArray arr = el.getAsJsonArray();
        return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
    }

    private static float[] readFloat2(JsonObject obj, String key) {
        JsonArray arr = obj.getAsJsonArray(key);
        return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat()};
    }

    @Override
    public void render(CrossingGateFullBlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        ensureParsed();

        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(RailDeco.id("block/crossing_gate_full"));
        }

        boolean powered = blockEntity.getBlockState().getValue(CrossingGateFullBlock.POWERED);
        float time = (float) level.getGameTime() + partialTick;
        boolean phaseA = ((long) time / CrossingGateFullBlockEntity.FLASH_PERIOD) % 2 == 0;

        Direction facing = blockEntity.getBlockState().getValue(CrossingGateFullBlock.FACING);

        VertexConsumer vertices = buffer.getBuffer(NO_CULL_CUTOUT);

        pose.pushPose();
        // ??? +X ? ?????????????FACING ????
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing.getOpposite().toYRot()));
        pose.translate(-0.5, 0.0, -0.5);
        pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        // ?????????
        for (Cube cube : STATIC_CUBES) {
            drawCube(vertices, pose, cube, packedLight, packedOverlay, 0.0F);
        }
        // ????????????????????????????????????
        // ??????????????????????????????????
        // ?????????? <-> ?????????????
        VertexConsumer lightVertices = buffer.getBuffer(LIGHT_TRANSLUCENT);
        boolean leftLit = powered && phaseA;
        boolean rightLit = powered && !phaseA;
        drawLamp(lightVertices, pose, LIGHT_ON_LEFT, LIGHT_OFF_LEFT, leftLit, packedLight, packedOverlay);
        drawLamp(lightVertices, pose, LIGHT_ON_RIGHT, LIGHT_OFF_RIGHT, rightLit, packedLight, packedOverlay);

        pose.popPose();
    }

    /** ?????????????????????????????lit=true ???????????? */
    private void drawLamp(VertexConsumer vertices, PoseStack pose,
                          List<Cube> onCubes, List<Cube> offCubes, boolean lit,
                          int packedLight, int packedOverlay) {
        int light = lit ? LightTexture.FULL_BRIGHT : packedLight;
        if (lit) {
            // ?????z ?? 0???????z ?? +1 -> ? -1.1 ?? -0.1 ?????????? -1.1?
            for (Cube cube : offCubes) {
                drawCube(vertices, pose, cube, packedLight, packedOverlay, 0.0F);
            }
            for (Cube cube : onCubes) {
                drawCube(vertices, pose, cube, light, packedOverlay, 0.0F);
            }
        } else {
            // ?????z ?? +1 -> ? -1.1 ?? -0.1???????z ?? -1 -> ? -0.1 ?? -1.1?
            for (Cube cube : onCubes) {
                drawCube(vertices, pose, cube, packedLight, packedOverlay, -1.0F);
            }
            for (Cube cube : offCubes) {
                drawCube(vertices, pose, cube, packedLight, packedOverlay, 1.0F);
            }
        }
    }

    private void drawCube(VertexConsumer vertices, PoseStack pose, Cube cube, int packedLight, int packedOverlay,
                          float zOffset) {
        float[] o = cube.origin, s = cube.size, p = cube.pivot, r = cube.rotation;
        // 8 ??????????
        float[][] corners = new float[8][3];
        int idx = 0;
        for (int dz = 0; dz <= 1; dz++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dx = 0; dx <= 1; dx++) {
                    float x = o[0] + dx * s[0];
                    float y = o[1] + dy * s[1];
                    float z = o[2] + dz * s[2];
                    float[] v = {x, y, z};
                    rotateAround(v, 0, r[0], p);
                    rotateAround(v, 1, r[1], p);
                    rotateAround(v, 2, r[2], p);
                    v[0] += offsetX;
                    v[1] += offsetY;
                    v[2] += offsetZ + zOffset;
                    corners[idx++] = v;
                }
            }
        }
        for (int i = 0; i < cube.faces.size(); i++) {
            String dir = cube.faces.get(i)[0];
            float[] uv = cube.faceUvs.get(i);
            // u0,v0 = ????????? 0~16 ?? UV??? / 128?2048 ???
            float u0 = uv[0] / 128.0F, v0 = uv[1] / 128.0F;
            float u1 = uv[2] / 128.0F, v1 = uv[3] / 128.0F;
            switch (dir) {
                case "south" -> quad(vertices, pose, packedLight, packedOverlay, 0, 0, 1,
                        corners[4], corners[5], corners[7], corners[6], u0, v1, u1, v1, u1, v0, u0, v0);
                case "north" -> quad(vertices, pose, packedLight, packedOverlay, 0, 0, -1,
                        corners[1], corners[0], corners[2], corners[3], u0, v1, u1, v1, u1, v0, u0, v0);
                case "east" -> quad(vertices, pose, packedLight, packedOverlay, 1, 0, 0,
                        corners[5], corners[7], corners[3], corners[1], u0, v1, u0, v0, u1, v0, u1, v1);
                case "west" -> quad(vertices, pose, packedLight, packedOverlay, -1, 0, 0,
                        corners[0], corners[2], corners[6], corners[4], u0, v1, u0, v0, u1, v0, u1, v1);
                case "up" -> quad(vertices, pose, packedLight, packedOverlay, 0, 1, 0,
                        corners[2], corners[6], corners[7], corners[3], u0, v0, u0, v1, u1, v1, u1, v0);
                case "down" -> quad(vertices, pose, packedLight, packedOverlay, 0, -1, 0,
                        corners[4], corners[0], corners[1], corners[5], u0, v0, u0, v1, u1, v1, u1, v0);
            }
        }
    }

    private void rotateAround(float[] v, int axis, float deg, float[] pivot) {
        if (deg == 0.0F) {
            return;
        }
        float a = (float) Math.toRadians(deg);
        float c = Mth.cos(a), s = Mth.sin(a);
        float x = v[0] - pivot[0], y = v[1] - pivot[1], z = v[2] - pivot[2];
        switch (axis) {
            case 0 -> { // X
                float ny = y * c - z * s;
                float nz = y * s + z * c;
                v[1] = ny + pivot[1];
                v[2] = nz + pivot[2];
            }
            case 1 -> { // Y
                float nx = x * c + z * s;
                float nz = -x * s + z * c;
                v[0] = nx + pivot[0];
                v[2] = nz + pivot[2];
            }
            default -> { // Z
                float nx = x * c - y * s;
                float ny = x * s + y * c;
                v[0] = nx + pivot[0];
                v[1] = ny + pivot[1];
            }
        }
    }

    private void quad(VertexConsumer vertices, PoseStack pose, int packedLight, int packedOverlay,
                      float nx, float ny, float nz,
                      float[] a, float[] b, float[] c, float[] d,
                      float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        addVertex(vertices, pose, a[0], a[1], a[2], u0, v0, nx, ny, nz, packedLight, packedOverlay);
        addVertex(vertices, pose, b[0], b[1], b[2], u1, v1, nx, ny, nz, packedLight, packedOverlay);
        addVertex(vertices, pose, c[0], c[1], c[2], u2, v2, nx, ny, nz, packedLight, packedOverlay);
        addVertex(vertices, pose, d[0], d[1], d[2], u3, v3, nx, ny, nz, packedLight, packedOverlay);
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
}
