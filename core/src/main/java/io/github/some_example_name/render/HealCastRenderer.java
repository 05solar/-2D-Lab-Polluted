package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.some_example_name.asset.SkillAssets;

/**
 * 회복 캐스트 연출(heal_cast_sheet 12프레임, 1/15s, 0.8s 1회 — 반복 금지).
 * 셀은 128×128 고정 사각형이며 발 앵커 (64,108)을 플레이어 발 위치에 연결한다.
 * 효과는 플레이어를 따라다니고(effectFollowsActor) 캐릭터 위에 알파 0.7로 덧그린다.
 * 성공 이벤트에서 한 번 시작하며 HP는 여기서 바꾸지 않는다(시각 전용 — 쿨다운과 분리).
 */
public final class HealCastRenderer {

    private static final float FRAME_SECONDS = 1f / 15f;
    private static final float ALPHA = 0.7f;
    // 캐릭터 화면 키 1타일 기준: VFX 셀을 1.4타일로(카탈로그 권고 48px 캐릭터→64~80px VFX 비율).
    private static final float DRAW_SIZE = 1.4f;
    private static final float FOOT_X = 64f / 128f;          // 셀 안 발 앵커 X 비율
    private static final float FOOT_Y_FROM_BOTTOM = (128f - 108f) / 128f; // 하단에서 앵커까지

    private final SkillAssets assets;
    private float age = Float.MAX_VALUE;

    public HealCastRenderer(SkillAssets assets) { this.assets = assets; }

    public void play() { age = 0f; }

    public void update(float delta) {
        if (age != Float.MAX_VALUE) age += delta;
    }

    public boolean playing() {
        return age < FRAME_SECONDS * SkillAssets.HEAL_FRAMES;
    }

    /** 플레이어 스프라이트를 그린 직후 호출(같은 배치, 통상 알파 블렌딩). */
    public void render(SpriteBatch batch, float playerFeetX, float playerFeetY) {
        if (!playing()) return;
        int frame = (int) (age / FRAME_SECONDS);
        TextureRegion region = assets.healFrame(frame);
        float x = playerFeetX - DRAW_SIZE * FOOT_X;
        float y = playerFeetY - DRAW_SIZE * FOOT_Y_FROM_BOTTOM;
        batch.setColor(1f, 1f, 1f, ALPHA);
        batch.draw(region, x, y, DRAW_SIZE, DRAW_SIZE);
        batch.setColor(Color.WHITE);
    }
}
