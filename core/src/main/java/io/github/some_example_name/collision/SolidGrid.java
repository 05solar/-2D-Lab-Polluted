package io.github.some_example_name.collision;

/** 타일 단위 고체(벽) 여부를 질의하는 최소 인터페이스. 충돌 계산을 월드 구현과 분리한다. */
public interface SolidGrid {

    /** 타일 좌표 (tileX, tileY)가 고체(통과 불가)이면 true. 범위 밖은 고체로 취급한다. */
    boolean isSolid(int tileX, int tileY);

    int widthInTiles();

    int heightInTiles();
}
