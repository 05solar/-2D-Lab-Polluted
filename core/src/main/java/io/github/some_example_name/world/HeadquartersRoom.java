package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.some_example_name.collision.ObstacleGrid;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.entity.player.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Safe tent's logical collision, exit and future interaction locations. */
public final class HeadquartersRoom implements ObstacleGrid {
    private static final float PROMPT_RADIUS = 2.5f;
    private static final float WATER_INTERACTION_RADIUS = 2.0f;
    private final boolean[][] walls;
    private final List<Rectangle> footprints;
    private final Vector2 spawn;
    private final Rectangle exit;
    private final HeadquartersProp vending;
    private final HeadquartersProp waterDispenser;
    // 2026-10-20: 강화 트리 = 망치 있는 접이식 작업대, 스킬 창 = 북벽 금속 테이블.
    private final HeadquartersProp upgradeBench;
    private final HeadquartersProp skillTable;
    // 메인 컴퓨터(중앙 터미널 책상) = 보스 단계 선택 입구(2026-10-20).
    private final HeadquartersProp mainComputer;
    private final Map<String, Vector2> interactionPoints;
    private boolean exitFired;

    HeadquartersRoom(boolean[][] walls, List<HeadquartersProp> props, Vector2 spawn,
                     Rectangle exit) {
        this.walls = walls;
        this.spawn = spawn;
        this.exit = exit;
        List<Rectangle> ground = new ArrayList<>();
        HeadquartersProp machine = null;
        HeadquartersProp dispenser = null;
        HeadquartersProp bench = null;
        HeadquartersProp skills = null;
        HeadquartersProp computer = null;
        Map<String, Vector2> points = new LinkedHashMap<>();
        for (HeadquartersProp prop : props) {
            if (prop.footprint() != null) ground.add(prop.footprint());
            if (prop.id().equals("vending_machine")) machine = prop;
            if (prop.id().equals("water_dispenser")) dispenser = prop;
            if (prop.id().equals("folding_workbench")) bench = prop;
            if (prop.id().equals("skill_table")) skills = prop;
            if (prop.id().equals("terminal_desk")) computer = prop;
            points.put(prop.id(), new Vector2(prop.anchorX(), prop.id().equals("water_dispenser")
                ? prop.anchorY() + 0.6f : prop.anchorY() - 0.65f));
        }
        this.footprints = Collections.unmodifiableList(ground);
        this.interactionPoints = Collections.unmodifiableMap(points);
        this.vending = machine;
        this.waterDispenser = dispenser;
        this.upgradeBench = bench;
        this.skillTable = skills;
        this.mainComputer = computer;
    }

    @Override public int widthInTiles() { return walls[0].length; }
    @Override public int heightInTiles() { return walls.length; }
    @Override public boolean isSolid(int x, int y) {
        return x < 0 || y < 0 || x >= widthInTiles() || y >= heightInTiles() || walls[y][x];
    }
    @Override public Iterable<Rectangle> obstacles() { return footprints; }
    public Vector2 spawnPoint() { return new Vector2(spawn); }
    public Rectangle exitArea() { return new Rectangle(exit); }
    public List<Rectangle> furnitureFootprints() { return footprints; }
    public Vector2 interactionPoint(String id) {
        Vector2 point = interactionPoints.get(id);
        return point == null ? null : new Vector2(point);
    }

    /** Fires only when the whole lower collision box crosses south into the outside tile. */
    public boolean crossedSouthExit(float previousFeetY, Player player, float moveY) {
        float threshold = exit.y + 0.5f;
        Rectangle b = player.bounds();
        if (exitFired || moveY >= 0f || previousFeetY <= threshold || b.y > threshold
            || b.y + b.height > exit.y + exit.height
            || b.x < exit.x || b.x + b.width > exit.x + exit.width) return false;
        exitFired = true;
        return true;
    }

    public void resetExitTrigger() { exitFired = false; }

    public boolean canInteractWithVending(float feetX, float feetY) {
        if (vending == null || feetY >= vending.anchorY()) return false;
        float dx = feetX - vending.anchorX();
        float dy = feetY - (vending.anchorY() - 0.5f);
        return dx * dx + dy * dy <= 1.25f * 1.25f;
    }

    /** Only the front approach is active; walls and other props occlude the interaction ray. */
    public boolean canInteractWithVending(Player player) {
        if (player == null || player.isDead() || player.facing() != Direction.UP
            || !canInteractWithVending(player.feetX(), player.feetY())) return false;
        Vector2 target = new Vector2(vending.anchorX(), vending.anchorY() - 0.55f);
        return hasClearApproach(player, target, vending);
    }

    /** Proximity-only prompt visibility; facing and line of sight are not required to show it. */
    public boolean isNearVending(Player player) {
        return player != null && !player.isDead() && vending != null
            && distanceTo(player, vending.anchorX(), vending.anchorY() - 0.55f) <= PROMPT_RADIUS;
    }

    /** Water is approached from the north, facing down; the dispenser itself is not an obstacle to sight. */
    public boolean canInteractWithWaterDispenser(Player player) {
        if (waterDispenser == null || player == null || player.isDead()) return false;
        Vector2 target = interactionPoint("water_dispenser");
        if (distanceTo(player, target.x, target.y) > WATER_INTERACTION_RADIUS) return false;
        return hasClearApproach(player, target, waterDispenser);
    }

    /** Prompt appears throughout the enlarged radius, regardless of facing or obstruction. */
    public boolean isNearWaterDispenser(Player player) {
        if (waterDispenser == null || player == null || player.isDead()) return false;
        Vector2 target = interactionPoint("water_dispenser");
        return distanceTo(player, target.x, target.y) <= PROMPT_RADIUS;
    }

    /** 강화 작업대(망치 있는 접이식 작업대): 남쪽(앞)에서 접근, 막히지 않은 경로 필요. */
    public boolean canInteractWithUpgradeBench(Player player) {
        return canInteractWithFrontProp(player, upgradeBench);
    }

    public boolean isNearUpgradeBench(Player player) {
        return isNearFrontProp(player, upgradeBench);
    }

    /** 스킬 테이블(북벽 금속 벤치): SPACE로 스킬 장착/단축키 등록 창 — 같은 접근 규칙. */
    public boolean canInteractWithSkillTable(Player player) {
        return canInteractWithFrontProp(player, skillTable);
    }

    public boolean isNearSkillTable(Player player) {
        return isNearFrontProp(player, skillTable);
    }

    /** 메인 컴퓨터(터미널 책상): SPACE로 보스 단계 선택창 — 같은 전면 접근 규칙. */
    public boolean canInteractWithMainComputer(Player player) {
        return canInteractWithFrontProp(player, mainComputer);
    }

    public boolean isNearMainComputer(Player player) {
        return isNearFrontProp(player, mainComputer);
    }

    /** 남쪽(앞)에서 접근하는 가구 공통 판정(자판기와 동일 규칙). */
    private boolean canInteractWithFrontProp(Player player, HeadquartersProp prop) {
        if (prop == null || player == null || player.isDead()
            || player.feetY() >= prop.anchorY()) return false;
        Vector2 target = new Vector2(prop.anchorX(), prop.anchorY() - 0.55f);
        if (distanceTo(player, target.x, target.y) > 1.6f) return false;
        return hasClearApproach(player, target, prop);
    }

    private boolean isNearFrontProp(Player player, HeadquartersProp prop) {
        return player != null && !player.isDead() && prop != null
            && distanceTo(player, prop.anchorX(), prop.anchorY() - 0.55f) <= PROMPT_RADIUS;
    }

    private static float distanceTo(Player player, float x, float y) {
        float dx = player.feetX() - x;
        float dy = player.feetY() + player.bounds().height * 0.5f - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private boolean hasClearApproach(Player player, Vector2 target, HeadquartersProp targetProp) {
        Vector2 start = new Vector2(player.feetX(), player.feetY() + player.bounds().height * 0.5f);
        int samples = Math.max(1, (int) (start.dst(target) / 0.1f));
        for (int i = 1; i < samples; i++) {
            float t = i / (float) samples;
            float x = start.x + (target.x - start.x) * t;
            float y = start.y + (target.y - start.y) * t;
            if (isSolid((int) x, (int) y)) return false;
            Rectangle point = new Rectangle(x - 0.01f, y - 0.01f, 0.02f, 0.02f);
            for (Rectangle obstacle : footprints) {
                if (obstacle.overlaps(point) && !isTargetFootprint(obstacle, targetProp)) return false;
            }
        }
        return true;
    }

    private boolean isTargetFootprint(Rectangle footprint, HeadquartersProp target) {
        Rectangle own = target.footprint();
        return own != null && footprint.equals(own);
    }

    public Rectangle vendingInteractionArea() {
        if (vending == null) return null;
        return new Rectangle(vending.anchorX() - 1.25f, vending.anchorY() - 1.75f,
            2.5f, 1.75f);
    }

    public Rectangle waterDispenserInteractionArea() {
        if (waterDispenser == null) return null;
        Vector2 point = interactionPoint("water_dispenser");
        return new Rectangle(point.x - WATER_INTERACTION_RADIUS,
            point.y - WATER_INTERACTION_RADIUS, WATER_INTERACTION_RADIUS * 2f,
            WATER_INTERACTION_RADIUS * 2f);
    }
}
