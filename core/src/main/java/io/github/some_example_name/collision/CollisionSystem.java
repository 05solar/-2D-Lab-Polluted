package io.github.some_example_name.collision;

import com.badlogic.gdx.math.Rectangle;

/**
 * AABB(축 정렬 박스) 대 타일 고체 충돌을 축별로 해결한다. 순수 계산이라 화면 없이 테스트 가능.
 *
 * X축을 먼저 이동·보정하고 Y축을 이어서 처리하는 표준 방식이다. 한 프레임 이동량이
 * 타일 한 칸보다 작으면(= speed*delta < 1) 벽을 통과하지 않는다. delta 상한이 이를 보장한다.
 */
public class CollisionSystem {

    private static final float EPS = 0.0001f;

    /**
     * @param box 현재 박스(좌하단 x,y + w,h). 이 객체는 변경하지 않는다.
     * @param dx  이번 프레임 x 이동량
     * @param dy  이번 프레임 y 이동량
     */
    public CollisionResult resolve(Rectangle box, float dx, float dy, SolidGrid grid) {
        float x = box.x;
        float y = box.y;
        float w = box.width;
        float h = box.height;

        boolean hitX = false;
        boolean hitY = false;

        // --- X축 ---
        x += dx;
        if (dx != 0f) {
            int minTy = (int) Math.floor(y + EPS);
            int maxTy = (int) Math.floor(y + h - EPS);
            if (dx > 0f) {
                int tileX = (int) Math.floor(x + w - EPS);
                if (columnHasSolid(grid, tileX, minTy, maxTy)) {
                    x = tileX - w;
                    hitX = true;
                }
            } else {
                int tileX = (int) Math.floor(x + EPS);
                if (columnHasSolid(grid, tileX, minTy, maxTy)) {
                    x = tileX + 1f;
                    hitX = true;
                }
            }
        }

        // --- Y축 (보정된 x 기준) ---
        y += dy;
        if (dy != 0f) {
            int minTx = (int) Math.floor(x + EPS);
            int maxTx = (int) Math.floor(x + w - EPS);
            if (dy > 0f) {
                int tileY = (int) Math.floor(y + h - EPS);
                if (rowHasSolid(grid, tileY, minTx, maxTx)) {
                    y = tileY - h;
                    hitY = true;
                }
            } else {
                int tileY = (int) Math.floor(y + EPS);
                if (rowHasSolid(grid, tileY, minTx, maxTx)) {
                    y = tileY + 1f;
                    hitY = true;
                }
            }
        }

        return new CollisionResult(x, y, hitX, hitY);
    }

    private boolean columnHasSolid(SolidGrid grid, int tileX, int minTy, int maxTy) {
        for (int ty = minTy; ty <= maxTy; ty++) {
            if (grid.isSolid(tileX, ty)) {
                return true;
            }
        }
        return false;
    }

    private boolean rowHasSolid(SolidGrid grid, int tileY, int minTx, int maxTx) {
        for (int tx = minTx; tx <= maxTx; tx++) {
            if (grid.isSolid(tx, tileY)) {
                return true;
            }
        }
        return false;
    }
}
