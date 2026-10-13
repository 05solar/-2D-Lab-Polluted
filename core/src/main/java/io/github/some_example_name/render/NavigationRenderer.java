package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.RoomPathfinder;
import java.util.List;

/**
 * 우상단 미니맵(프레임 PNG 테두리) + 대문으로 향하는 바닥 발자국(PNG) + 미니맵 아래 귀환 버튼(PNG).
 * 버튼/F로 토글한 경로를 바닥 발자국으로 보여준다. 버튼 클릭 판정은 {@link #buttonHit}로 노출한다.
 */
public final class NavigationRenderer {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera hudCamera = new OrthographicCamera();
    // 마지막으로 그린 버튼의 화면 사각형(좌상단 원점): x, yTop, w, h
    private float btnX, btnYTop, btnW, btnH;

    /** 호환용 유지(이전 이동 트레일 API). 현재는 경로 발자국을 쓰므로 아무 것도 누적하지 않는다. */
    public void updateFootsteps(float delta, boolean moving, float x, float y) { }

    public void render(Camera worldCamera, LaboratoryRoom room, float px, float py,
                       float bodyWidth, float bodyHeight, List<RoomPathfinder.Cell> path,
                       List<Monster> monsters, int gateX, int gateY,
                       TextureRegion frame, TextureRegion footprint, TextureRegion button,
                       boolean routeActive) {
        // --- 월드 좌표: 대문으로 향하는 발자국(PNG)을 바닥에 일정 간격·방향으로 배치 ---
        if (path != null && path.size() > 1) {
            batch.setProjectionMatrix(worldCamera.combined);
            batch.begin();
            batch.setColor(1f, 1f, 1f, .9f);
            float spacing = 1.1f, side = .34f; // 발자국 간격/한 쌍 크기(월드 타일)
            float carry = 0f;
            for (int i = 1; i < path.size(); i++) {
                float ax = path.get(i - 1).x + .5f, ay = path.get(i - 1).y + .5f;
                float bx = path.get(i).x + .5f, by = path.get(i).y + .5f;
                float segLen = (float) Math.hypot(bx - ax, by - ay);
                if (segLen < 1e-4f) continue;
                float dirX = (bx - ax) / segLen, dirY = (by - ay) / segLen;
                float rot = MathUtils.atan2(dirY, dirX) * MathUtils.radiansToDegrees - 90f; // 기본 북향 보정
                for (float d = carry; d < segLen; d += spacing) {
                    float fx = ax + dirX * d, fy = ay + dirY * d;
                    batch.draw(footprint, fx - side / 2f, fy - side / 2f, side / 2f, side / 2f,
                        side, side, 1f, 1f, rot);
                }
                carry = (carry - segLen) % spacing;
                if (carry < 0) carry += spacing;
            }
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // --- 화면 좌표: 미니맵 그리드 + 프레임 + 마커, 그 아래 귀환 버튼 ---
        int sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        hudCamera.setToOrtho(false, sw, sh);
        hudCamera.update();
        float square = Math.min(240f, Math.min(sw, sh) * 0.28f);
        float margin = 12f;
        float x0 = sw - margin - square, y0 = sh - margin - square; // bottom-left of minimap square
        float inset = square * 0.085f;                               // 프레임 테두리 두께
        float area = square - inset * 2f;
        float rw = room.widthInTiles(), rh = room.heightInTiles();
        float scale = area / Math.max(rw, rh);
        float gx0 = x0 + inset + (area - rw * scale) / 2f;
        float gy0 = y0 + inset + (area - rh * scale) / 2f;

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.015f, .02f, .03f, .9f);
        shapes.rect(x0 + inset * .5f, y0 + inset * .5f, square - inset, square - inset);
        for (int y = 0; y < rh; y++) for (int x = 0; x < rw; x++) {
            shapes.setColor(room.blocksActorAtCell(x, y, bodyWidth, bodyHeight)
                ? new Color(.52f, .24f, .22f, 1f) : new Color(.22f, .27f, .27f, 1f));
            shapes.rect(gx0 + x * scale, gy0 + y * scale, scale + .5f, scale + .5f);
        }
        // 대문(귀환 목적지) 마커
        shapes.setColor(1f, .68f, .2f, 1f);
        shapes.rect(gx0 + gateX * scale - scale, gy0 + gateY * scale - scale * .5f, scale * 2f, scale * 1.2f);
        for (Monster m : monsters)
            if (m.isAlive() && m.type() == io.github.some_example_name.combat.MonsterType.TEAM_LEADER) {
                shapes.setColor(Color.PURPLE);
                shapes.circle(gx0 + m.feetX() * scale, gy0 + m.feetY() * scale, Math.max(2f, scale * .65f), 8);
            }
        shapes.setColor(Color.GREEN);
        shapes.circle(gx0 + px * scale, gy0 + py * scale, Math.max(2f, scale * .7f), 8);
        shapes.end();

        // 버튼 위치(미니맵 아래), 프레임·버튼 PNG 그리기
        btnW = square;
        btnH = square * (button.getRegionHeight() / (float) button.getRegionWidth());
        btnX = x0;
        float btnY0 = y0 - 8f - btnH; // bottom-left (y up)
        btnYTop = sh - (btnY0 + btnH);
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        batch.draw(frame, x0, y0, square, square);
        batch.setColor(routeActive ? new Color(.6f, 1f, 1f, 1f) : Color.WHITE); // 활성 시 강조
        batch.draw(button, btnX, btnY0, btnW, btnH);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** 화면 좌표(좌상단 원점, Gdx.input 기준)가 귀환 버튼 위인지. */
    public boolean buttonHit(float screenX, float screenYTop) {
        return screenX >= btnX && screenX <= btnX + btnW
            && screenYTop >= btnYTop && screenYTop <= btnYTop + btnH;
    }

    public void dispose() { shapes.dispose(); batch.dispose(); }
}
