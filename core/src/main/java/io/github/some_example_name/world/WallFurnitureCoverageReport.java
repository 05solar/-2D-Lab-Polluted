package io.github.some_example_name.world;

/** Measured visible alpha coverage for one usable room wall. */
public final class WallFurnitureCoverageReport {
    private final String roomId;
    private final String wall;
    private final float usableLength;
    private final float coveredLength;

    public WallFurnitureCoverageReport(String roomId, String wall,
                                       float usableLength, float coveredLength) {
        this.roomId = roomId;
        this.wall = wall;
        this.usableLength = usableLength;
        this.coveredLength = Math.min(usableLength, coveredLength);
    }
    public String roomId() { return roomId; }
    public String wall() { return wall; }
    public float usableLength() { return usableLength; }
    public float coveredLength() { return coveredLength; }
    public float ratio() { return usableLength <= 0f ? 1f : coveredLength / usableLength; }
    public boolean meetsTarget() { return ratio() >= .85f; }
}
