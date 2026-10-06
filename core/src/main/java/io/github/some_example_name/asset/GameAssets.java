package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.config.AssetPaths;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.render.AnimationSet;

/**
 * 게임 자산의 로딩과 해제를 한곳에서 책임진다. 게임 루트에서 한 번 생성하고 종료 시 한 번 해제한다.
 * 전역 singleton이 아니라 생명주기가 명확한 자원 객체로, 필요한 곳에 전달한다.
 *
 * 1단계에서는 타일셋/플레이어 시트를 직접 로딩한다. (로딩 화면 + AssetManager 도입은 이후 단계)
 */
public class GameAssets implements Disposable {

    // 타일셋은 실제 픽셀 크기와 무관하게 4열 x 4행 격자다. (제공 파일은 1254x1254라 64px 가정이 틀림)
    private static final int TILESET_COLUMNS = 4;
    private static final int TILESET_ROWS = 4;

    private final Texture tilesetTexture;
    private final Texture playerTexture;
    private final TextureRegion[][] tileRegions;
    private final AnimationSet playerAnimations;

    public GameAssets() {
        tilesetTexture = loadNearest(AssetPaths.LAB_TILESET);
        playerTexture = loadNearest(AssetPaths.PLAYER_SHEET);
        // 하드코딩된 64px가 아니라 실제 크기를 열/행으로 나눠 분할한다(비정수 크기도 반올림으로 처리).
        tileRegions = splitIntoGrid(tilesetTexture, TILESET_COLUMNS, TILESET_ROWS);
        Gdx.app.log("GameAssets", "tileset=" + AssetPaths.LAB_TILESET
            + " size=" + tilesetTexture.getWidth() + "x" + tilesetTexture.getHeight()
            + " grid=" + TILESET_COLUMNS + "x" + TILESET_ROWS);
        playerAnimations = loadPlayerAnimations(playerTexture);
    }

    private static Texture loadNearest(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        // Nearest 필터 + ClampToEdge: 타일 경계에서 이웃 조각/바깥 텍셀을 샘플링해 생기는
        // 얇은 선(bleeding)을 줄인다. (렌더러의 반텍셀 인셋과 함께 사용)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return texture;
    }

    /**
     * 텍스처를 columns x rows 격자로 분할한다. 각 셀 경계를 실제 크기 기준으로 반올림해
     * 전체 이미지를 빈틈 없이 덮는다(타일 크기가 정수가 아니어도 안전). 반환은 [row][col].
     */
    private static TextureRegion[][] splitIntoGrid(Texture texture, int columns, int rows) {
        int w = texture.getWidth();
        int h = texture.getHeight();
        TextureRegion[][] grid = new TextureRegion[rows][columns];
        for (int r = 0; r < rows; r++) {
            int y0 = Math.round(r * h / (float) rows);
            int y1 = Math.round((r + 1) * h / (float) rows);
            for (int c = 0; c < columns; c++) {
                int x0 = Math.round(c * w / (float) columns);
                int x1 = Math.round((c + 1) * w / (float) columns);
                grid[r][c] = new TextureRegion(texture, x0, y0, x1 - x0, y1 - y0);
            }
        }
        return grid;
    }

    private static AnimationSet loadPlayerAnimations(Texture sheet) {
        JsonValue root = new JsonReader().parse(Gdx.files.internal(AssetPaths.PLAYER_ANIMATION_DATA));
        int frameW = root.getInt("frameWidth");
        int frameH = root.getInt("frameHeight");
        TextureRegion[][] frames = TextureRegion.split(sheet, frameW, frameH);

        AnimationSet set = new AnimationSet();

        for (JsonValue a = root.get("animations").child; a != null; a = a.next) {
            String name = a.name();
            int row = a.getInt("row");
            int[] frameIndices = a.get("frames").asIntArray();
            float frameDuration = a.getFloat("frameDuration");
            boolean loop = a.getBoolean("loop", false);

            Array<TextureRegion> regions = new Array<>();
            for (int fi : frameIndices) {
                regions.add(frames[row][fi]);
            }
            Animation<TextureRegion> animation = new Animation<>(frameDuration, regions,
                loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
            set.putAnimation(name, animation);

            if (a.has("hitFrames")) {
                set.putHitFrames(name, a.get("hitFrames").asIntArray());
            }
        }

        JsonValue idle = root.get("idleFrames");
        if (idle != null) {
            for (JsonValue d = idle.child; d != null; d = d.next) {
                Direction direction = Direction.valueOf(d.name().toUpperCase());
                int row = d.getInt("row");
                int column = d.getInt("column");
                set.putIdle(direction, frames[row][column]);
            }
        }
        return set;
    }

    public TextureRegion[][] tileRegions() {
        return tileRegions;
    }

    public AnimationSet playerAnimations() {
        return playerAnimations;
    }

    @Override
    public void dispose() {
        tilesetTexture.dispose();
        playerTexture.dispose();
    }
}
