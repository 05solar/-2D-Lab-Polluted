package io.github.some_example_name.world;

/** 위치와 방향이 고정된 문. 상태 한 곳에서 충돌과 시각 ID를 함께 결정한다. */
public final class Door {
    public enum Orientation { HORIZONTAL, VERTICAL }

    private final int x;
    private final int y;
    private final Orientation orientation;
    private DoorState state;

    public Door(int x, int y, Orientation orientation, DoorState state) {
        if (orientation == null || state == null) throw new IllegalArgumentException("문 방향/상태 누락");
        this.x = x;
        this.y = y;
        this.orientation = orientation;
        this.state = state;
    }

    public int x() { return x; }
    public int y() { return y; }
    public Orientation orientation() { return orientation; }
    public DoorState state() { return state; }
    public void setState(DoorState next) {
        if (next == null) throw new IllegalArgumentException("문 상태 누락");
        state = next;
    }

    public TileType tileType() {
        return state == DoorState.OPEN ? TileType.DOORWAY_OPEN : TileType.DOOR_CLOSED;
    }

    public String visualId() {
        String axis = orientation == Orientation.HORIZONTAL ? "horizontal" : "vertical";
        return axis + (state == DoorState.OPEN ? "_door_open" : "_door_closed");
    }
}
