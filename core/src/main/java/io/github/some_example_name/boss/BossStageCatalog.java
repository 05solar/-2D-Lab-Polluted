package io.github.some_example_name.boss;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 보스 단계 목록(data/stages.json — 2026-10-20 동봉 초안 채택, 순수 Java).
 * progressUnlocked(진행 해금)·implemented(이번 빌드 구현)·cleared(토벌 기록)를 구분한다 —
 * 해금됐지만 미구현인 단계는 "해금됨 · 준비 중"으로 보이고 입장은 차단된다.
 */
public final class BossStageCatalog {

    public static final class Stage {
        public final String stageId;
        public final boolean implemented;
        public final boolean initialProgressUnlocked;
        public final String nextStage; // null이면 마지막
        public final String bossId;    // null이면 보스 데이터 없음
        // ----- 9단계 확장(stage_catalog.runtime.json — 구 스키마에서는 기본값) -----
        public final String displayName;
        /** true면 기존 1-1 레거시 엔진/밸런스/아트 보존(저장 키·보스 ID 불변). */
        public final boolean legacyEngine;
        public final String animationSet;
        public final int draftMaxHp;
        public final float drawWidthTiles, drawHeightTiles;
        public final float hurtboxW, hurtboxH, hurtboxCenterOffsetY;
        public final float footprintW, footprintH;
        public final String[] patternIds;
        public final String arenaId;
        public final String portraitSilhouette, portraitColor;

        Stage(JsonValue node) {
            stageId = node.getString("stageId", node.getString("id", null));
            // 구 스키마=implemented, 신 스키마=integrationReady.
            implemented = node.getBoolean("implemented",
                node.getBoolean("integrationReady", false));
            initialProgressUnlocked = node.getBoolean("initialProgressUnlocked", false);
            nextStage = node.getString("nextStage", null);
            bossId = node.getString("bossId", null);
            displayName = node.getString("displayName", stageId);
            legacyEngine = node.getBoolean("legacyEngine",
                node.getBoolean("preserveExistingBalance", false));
            animationSet = node.getString("animationSet", null);
            draftMaxHp = node.getInt("draftMaxHp", 600);
            JsonValue render = node.get("render");
            drawWidthTiles = render == null ? 4f : render.getFloat("drawWidthTiles", 8f);
            drawHeightTiles = render == null ? 4f : render.getFloat("drawHeightTiles", 8f);
            float[] hurt = node.get("draftHurtboxTiles") == null ? new float[]{2.2f, 1.5f}
                : node.get("draftHurtboxTiles").asFloatArray();
            hurtboxW = hurt[0];
            hurtboxH = hurt[1];
            float[] hurtOffset = node.get("draftHurtboxCenterOffsetFromFeetTiles") == null
                ? new float[]{0f, 1.6f}
                : node.get("draftHurtboxCenterOffsetFromFeetTiles").asFloatArray();
            hurtboxCenterOffsetY = hurtOffset[1];
            float[] foot = node.get("draftMovementFootprintTiles") == null
                ? new float[]{1.8f, 1.2f}
                : node.get("draftMovementFootprintTiles").asFloatArray();
            footprintW = foot[0];
            footprintH = foot[1];
            java.util.List<String> ids = new java.util.ArrayList<>();
            JsonValue patterns = node.get("patterns");
            if (patterns != null) for (JsonValue p = patterns.child; p != null; p = p.next)
                ids.add(p.asString());
            patternIds = ids.toArray(new String[0]);
            arenaId = node.getString("arenaId", null);
            JsonValue portrait = node.get("portrait");
            portraitSilhouette = portrait == null ? null : portrait.getString("silhouette", null);
            portraitColor = portrait == null ? null : portrait.getString("color", null);
        }
    }

    private final List<Stage> stages = new ArrayList<>();

    public BossStageCatalog(String json) {
        JsonValue root = new JsonReader().parse(json);
        for (JsonValue s = root.get("stages").child; s != null; s = s.next)
            stages.add(new Stage(s));
        if (stages.isEmpty()) throw new IllegalArgumentException("no boss stages");
    }

    public List<Stage> stages() { return Collections.unmodifiableList(stages); }

    public Stage stage(String stageId) {
        for (Stage stage : stages) if (stage.stageId.equals(stageId)) return stage;
        return null;
    }

    /** 진행 해금 여부: 카탈로그 기본 해금 또는 저장된 해금 기록. */
    public boolean progressUnlocked(String stageId, Set<String> savedUnlocked) {
        Stage stage = stage(stageId);
        if (stage == null) return false;
        return stage.initialProgressUnlocked || savedUnlocked.contains(stageId);
    }

    /** 입장 가능 = 진행 해금 + 이번 빌드 구현 + 보스 데이터 존재. */
    public boolean enterable(String stageId, Set<String> savedUnlocked) {
        Stage stage = stage(stageId);
        return stage != null && stage.implemented && stage.bossId != null
            && progressUnlocked(stageId, savedUnlocked);
    }
}
