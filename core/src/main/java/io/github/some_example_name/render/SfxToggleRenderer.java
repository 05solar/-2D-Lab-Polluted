package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * 효과음 켜기/끄기 버튼(sfx_toggle_ui_v1)을 화면 좌표에 그린다. 상태 이미지는 호출 측이
 * 현재 sfxEnabled에 맞춰 넘기고(켜짐=청록 음파, 꺼짐=주황 빗금), 클릭 영역은 마지막으로
 * 그린 보이는 버튼 사각형과 정확히 일치한다. 배경음악 설정과는 무관하다.
 */
public final class SfxToggleRenderer {

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    // 마지막으로 그린 버튼의 화면 사각형(bottom-left 원점) — 클릭 판정용.
    private float x, y, size;

    /** @param rightMargin 화면 오른쪽 끝에서의 여백 @param topOffset 화면 위쪽 끝에서 버튼 상단까지의 거리 */
    public void render(TextureRegion state, float rightMargin, float topOffset, float buttonSize) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        size = buttonSize;
        x = sw - rightMargin - size;
        y = sh - topOffset - size;
        batch.begin();
        batch.draw(state, x, y, size, size);
        batch.end();
    }

    /** 화면 좌표(좌상단 원점, Gdx.input 기준)가 버튼 위인지. */
    public boolean buttonHit(float screenX, float screenYTop) {
        float yTop = Gdx.graphics.getHeight() - (y + size);
        return screenX >= x && screenX <= x + size
            && screenYTop >= yTop && screenYTop <= yTop + size;
    }

    public void dispose() { batch.dispose(); }
}
