package io.github.some_example_name.collision;

/** 충돌 해결 후의 위치(박스 좌하단)와 축별 충돌 여부. */
public final class CollisionResult {

    public final float x;
    public final float y;
    public final boolean hitX;
    public final boolean hitY;

    public CollisionResult(float x, float y, boolean hitX, boolean hitY) {
        this.x = x;
        this.y = y;
        this.hitX = hitX;
        this.hitY = hitY;
    }
}
