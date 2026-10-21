package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * 스킬 패널 열기 버튼(슬롯 프레임 + 스킬 아이콘)을 화면 좌표에 그린다. 효과음 토글 버튼과
 * 같은 배치 규칙(좌상단 기준, 고정 px)이고 클릭 영역은 마지막으로 그린 사각형과 일치한다.
 */
public final class SkillMenuButtonRenderer {

    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private float x, y, size;

    /** @param leftX 버튼 왼쪽 가장자리(화면 px) @param topOffset 화면 위쪽 끝에서 버튼 상단까지 */
    public void render(TextureRegion frame, TextureRegion icon,
                       float leftX, float topOffset, float buttonSize) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        camera.setToOrtho(false, sw, sh);
        batch.setProjectionMatrix(camera.combined);
        size = buttonSize;
        x = leftX;
        y = sh - topOffset - size;
        batch.begin();
        batch.draw(frame, x, y, size, size);
        float pad = size * 0.18f;
        batch.draw(icon, x + pad, y + pad, size - pad * 2, size - pad * 2);
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
