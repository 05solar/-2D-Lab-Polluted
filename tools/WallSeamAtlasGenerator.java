import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * V2 원본 아틀라스에서 연결 가능한 64px 파생 아틀라스를 결정적으로 만든다.
 * 금속 본체의 내부 픽셀만 반복하므로 직선 벽의 양끝 캡과 셀 경계 여백은 사라진다.
 * 원본 PNG는 수정하지 않는다. 실행: javac -d build/tools tools/WallSeamAtlasGenerator.java;
 * java -cp build/tools WallSeamAtlasGenerator assets/laboratory_tiles_v2
 */
public final class WallSeamAtlasGenerator {
    private static final int TILE = 64;
    private static final int N = 1, E = 2, S = 4, W = 8;
    private static BufferedImage sourceWall;
    private static BufferedImage sourceStructure;
    private static BufferedImage horizontal;
    private static BufferedImage vertical;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("에셋 디렉터리 경로 필요");
        File dir = new File(args[0]);
        sourceWall = read(new File(dir, "lab_wall_autotiles_v2_64.png"));
        sourceStructure = read(new File(dir, "lab_structure_doors_v2_64.png"));
        horizontal = straight(true);
        vertical = straight(false);

        BufferedImage walls = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int[] wallMasks = {
            E | W, E | W, N | S, N | S,
            E, W, S, N,
            E | S, W | S, E | N, W | N,
            E | S, W | S, E | N, W | N
        };
        for (int i = 0; i < wallMasks.length; i++) {
            BufferedImage tile = connected(wallMasks[i], i >= 12);
            if (i == 1 || i == 3) damage(tile, i == 1 ? 1 : 3);
            if (i >= 4 && i <= 7) cap(tile, i);
            paste(walls, tile, i);
        }
        write(walls, new File(dir, "lab_wall_connected_v3_64.png"));

        BufferedImage structures = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int[] junctionMasks = {E | S | W, N | E | W, N | S | W, N | E | S, N | E | S | W};
        for (int i = 0; i < 5; i++) paste(structures, connected(junctionMasks[i], false), i);
        paste(structures, specialty(5, true), 5);
        paste(structures, specialty(6, false), 6);
        paste(structures, specialty(7, true), 7);
        paste(structures, specialty(14, true), 14);
        paste(structures, specialty(15, false), 15);
        paste(structures, door(true, false), 8);
        paste(structures, door(true, true), 9);
        paste(structures, door(false, false), 10);
        paste(structures, door(false, true), 11);
        paste(structures, horizontalJamb(true), 12);
        paste(structures, horizontalJamb(false), 13);
        write(structures, new File(dir, "lab_structure_connected_v3_64.png"));

        BufferedImage verticalJambs = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
        paste(verticalJambs, verticalJamb(true), 0);
        paste(verticalJambs, verticalJamb(false), 1);
        write(verticalJambs, new File(dir, "lab_vertical_jambs_v3_64.png"));
    }

    private static BufferedImage read(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null || image.getWidth() != 256 || image.getHeight() != 256)
            throw new IOException("256x256 원본 아틀라스 필요: " + file);
        return image;
    }

    private static void write(BufferedImage image, File file) throws IOException {
        if (!ImageIO.write(image, "png", file)) throw new IOException("PNG 쓰기 실패: " + file);
    }

    private static BufferedImage tile(BufferedImage atlas, int index) {
        return atlas.getSubimage(index % 4 * TILE, index / 4 * TILE, TILE, TILE);
    }

    private static void paste(BufferedImage atlas, BufferedImage tile, int index) {
        int columns = atlas.getWidth() / TILE;
        int ox = index % columns * TILE, oy = index / columns * TILE;
        for (int y = 0; y < TILE; y++)
            for (int x = 0; x < TILE; x++)
                atlas.setRGB(ox + x, oy + y, tile.getRGB(x, y));
    }

    private static BufferedImage blank() {
        return new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
    }

    private static BufferedImage straight(boolean isHorizontal) {
        BufferedImage original = tile(sourceWall, isHorizontal ? 0 : 2);
        BufferedImage out = blank();
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                // 16px 대칭 주기: 0과 63의 몸체 단면이 픽셀 단위로 동일하다.
                int axis = isHorizontal ? x : y;
                int step = axis % 16;
                int sample = (isHorizontal ? 16 : 13) + Math.min(step, 15 - step);
                out.setRGB(x, y, isHorizontal ? original.getRGB(sample, y)
                    : original.getRGB(x, sample));
            }
        }
        return out;
    }

    private static BufferedImage connected(int mask, boolean inner) {
        BufferedImage out = blank();
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                if (((mask & W) != 0 && x <= 35) || ((mask & E) != 0 && x >= 28))
                    over(out, x, y, horizontal.getRGB(x, y));
                if (((mask & N) != 0 && y <= 35) || ((mask & S) != 0 && y >= 28))
                    over(out, x, y, vertical.getRGB(x, y));
            }
        }
        if (Integer.bitCount(mask) >= 2 && mask != (E | W) && mask != (N | S)) {
            BufferedImage joint = tile(sourceStructure, 4);
            // 금속 결합부만 겹쳐 원본의 외곽 여백은 가져오지 않는다.
            for (int y = 25; y <= 38; y++)
                for (int x = 25; x <= 38; x++)
                    over(out, x, y, joint.getRGB(x, y));
            if (inner) {
                for (int y = 28; y <= 35; y++)
                    for (int x = 28; x <= 35; x++) {
                        int argb = out.getRGB(x, y);
                        int a = argb >>> 24;
                        if (a > 0) {
                            int r = (int) (((argb >>> 16) & 255) * 0.86f);
                            int g = (int) (((argb >>> 8) & 255) * 0.86f);
                            int b = (int) ((argb & 255) * 0.86f);
                            out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                        }
                    }
            }
        }
        return out;
    }

    private static void damage(BufferedImage out, int index) {
        BufferedImage damaged = tile(sourceWall, index);
        for (int y = 15; y < 49; y++)
            for (int x = 16; x < 49; x++)
                over(out, x, y, damaged.getRGB(x, y));
    }

    private static void cap(BufferedImage out, int index) {
        BufferedImage original = tile(sourceWall, index);
        if (index == 4 || index == 5) {
            int sourceX = index == 4 ? 7 : 47;
            for (int y = 11; y < 51; y++)
                for (int dx = 0; dx < 15; dx++)
                    over(out, 24 + dx, y, original.getRGB(sourceX + dx, y));
        } else {
            int sourceY = index == 6 ? 2 : 37;
            for (int dy = 0; dy < 14; dy++)
                for (int x = 4; x < 52; x++)
                    over(out, x, 25 + dy, original.getRGB(x, sourceY + dy));
        }
    }

    private static BufferedImage door(boolean isHorizontal, boolean open) {
        BufferedImage original = tile(sourceStructure, (isHorizontal ? 8 : 10) + (open ? 1 : 0));
        BufferedImage out = blank();
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int base = isHorizontal ? horizontal.getRGB(x, y) : vertical.getRGB(x, y);
                boolean edgeStrip = isHorizontal ? (x <= 7 || x >= 57) : (y <= 7 || y >= 56);
                if (edgeStrip)
                    over(out, x, y, base);
                // 열린 문 가운데의 불투명한 검은 채움은 제거하고 바닥을 노출한다.
                boolean aperture = open && (isHorizontal
                    ? (x >= 20 && x <= 43 && y >= 22 && y <= 43)
                    : (x >= 20 && x <= 43 && y >= 20 && y <= 45));
                if (!edgeStrip && !aperture) over(out, x, y, original.getRGB(x, y));
            }
        }
        if (!isHorizontal && open) {
            // 원본 문턱은 셀 맨 아래에 있어 접합 단면과 충돌한다. 내부로 옮겨 보존한다.
            for (int x = 20; x <= 43; x++) {
                over(out, x, 52, original.getRGB(x, 62));
                over(out, x, 53, original.getRGB(x, 63));
            }
        }
        return out;
    }

    private static BufferedImage horizontalJamb(boolean left) {
        BufferedImage out = connected(E | W, false);
        BufferedImage cap = tile(sourceWall, left ? 5 : 4);
        int sourceX = left ? 47 : 7;
        int destinationX = left ? 45 : 4;
        for (int y = 11; y < 51; y++)
            for (int dx = 0; dx < 15; dx++)
                over(out, destinationX + dx, y, cap.getRGB(sourceX + dx, y));
        return out;
    }

    private static BufferedImage specialty(int index, boolean isHorizontal) {
        BufferedImage out = connected(isHorizontal ? E | W : N | S, false);
        BufferedImage original = tile(sourceStructure, index);
        for (int y = 13; y <= 50; y++)
            for (int x = 13; x <= 50; x++)
                over(out, x, y, original.getRGB(x, y));
        return out;
    }

    private static BufferedImage verticalJamb(boolean top) {
        BufferedImage out = connected(N | S, false);
        BufferedImage cap = tile(sourceWall, top ? 6 : 7);
        int sourceY = top ? 2 : 37;
        int destinationY = top ? 44 : 7;
        for (int dy = 0; dy < 14; dy++)
            for (int x = 4; x < 52; x++)
                over(out, x, destinationY + dy, cap.getRGB(x, sourceY + dy));
        return out;
    }

    private static void over(BufferedImage target, int x, int y, int argb) {
        if ((argb >>> 24) != 0) target.setRGB(x, y, argb);
    }
}
