package io.github.some_example_name.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.some_example_name.asset.ExteriorWallAssets;

/**
 * 연구소 2타일 두께 외벽 링(직선 4종 신축 + 바깥 모서리 4종) + 남쪽 8x4 대문(닫힘/열림)을 월드 좌표로 그린다.
 * 직선은 길이 방향으로만 늘리고 두께(2타일)는 유지하며, 남쪽 벽은 대문 폭만큼 비워 실제 개구부를 만든다.
 * 좌표계: ty=0이 남(하단), y가 북으로 증가.
 */
public final class ExteriorWallRenderer {
    private final SpriteBatch batch = new SpriteBatch();
    private final ExteriorWallAssets a;

    public ExteriorWallRenderer(ExteriorWallAssets assets) { this.a = assets; }

    public void render(Camera worldCamera, int w, int h, float gateCenterX, boolean gateOpen) {
        float t = a.wallThicknessTiles;                 // 2
        float gw = a.gateWorldWidthTiles, gh = a.gateWorldHeightTiles; // 8 x 4
        float gateLeft = gateCenterX - gw / 2f;
        float gateRight = gateCenterX + gw / 2f;

        batch.setProjectionMatrix(worldCamera.combined);
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        // 모서리(2x2)
        batch.draw(a.cornerSW(), 0, 0, t, t);
        batch.draw(a.cornerSE(), w - t, 0, t, t);
        batch.draw(a.cornerNW(), 0, h - t, t, t);
        batch.draw(a.cornerNE(), w - t, h - t, t, t);
        // 직선(두께 t 유지, 길이 신축)
        batch.draw(a.north(), t, h - t, w - 2 * t, t);
        batch.draw(a.west(), 0, t, t, h - 2 * t);
        batch.draw(a.east(), w - t, t, t, h - 2 * t);
        // 남쪽 벽은 대문 개구부 좌우로 분리
        float leftW = gateLeft - t;
        if (leftW > 0.01f) batch.draw(a.south(), t, 0, leftW, t);
        float rightW = (w - t) - gateRight;
        if (rightW > 0.01f) batch.draw(a.south(), gateRight, 0, rightW, t);
        // 8x4 대문(anchorPx 하단-중앙 → base y=0, center x=gateCenterX). 닫힘=통로 차단, 열림=통로 투명.
        batch.draw(a.gate(gateOpen), gateCenterX - gw / 2f, 0f, gw, gh);
        batch.end();
    }

    /**
     * 본부↔연구소 대문(8x4, V3)만 그린다. 외벽 링 자체는 V4 벽이 담당하므로 더 이상 그리지 않는다.
     * anchorPx 하단-중앙 → base y=0, center x=gateCenterX. 닫힘=통로 차단, 열림=통로 투명.
     */
    public void renderGate(Camera worldCamera, float gateCenterX, boolean gateOpen) {
        float gw = a.gateWorldWidthTiles, gh = a.gateWorldHeightTiles; // 8 x 4
        batch.setProjectionMatrix(worldCamera.combined);
        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        batch.draw(a.gate(gateOpen), gateCenterX - gw / 2f, 0f, gw, gh);
        batch.end();
    }

    public void dispose() { batch.dispose(); }
}
