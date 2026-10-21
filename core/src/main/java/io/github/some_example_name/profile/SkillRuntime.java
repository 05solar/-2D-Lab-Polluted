package io.github.some_example_name.profile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * skillId별 남은 쿨다운(초, 게임 delta 기준 — 절대시각 불사용). 게임 루트가 한 개를 소유해
 * 슬롯 교체·키 변경·가방/패널·맵 전환·화면 재생성으로 초기화되지 않는다. 일시정지 중에는
 * tick이 호출되지 않아 함께 멈춘다. 프로필 저장 시 스냅샷을 싣고 로드 시 복원한다
 * (오프라인 경과 미적용 정책).
 */
public final class SkillRuntime {

    private final Map<String, Float> remaining = new LinkedHashMap<>();

    public void tick(float delta) {
        if (delta <= 0f) return;
        for (Map.Entry<String, Float> entry : remaining.entrySet())
            entry.setValue(Math.max(0f, entry.getValue() - delta));
    }

    public void startCooldown(String skillId, float seconds) {
        if (skillId == null || seconds < 0f) return;
        remaining.put(skillId, seconds);
    }

    public float remainingSeconds(String skillId) {
        Float value = remaining.get(skillId);
        return value == null ? 0f : value;
    }

    public boolean ready(String skillId) { return remainingSeconds(skillId) <= 0f; }

    /** 프로필 저장용 스냅샷(0 초과만). */
    public Map<String, Float> snapshot() {
        Map<String, Float> out = new LinkedHashMap<>();
        for (Map.Entry<String, Float> entry : remaining.entrySet())
            if (entry.getValue() > 0f) out.put(entry.getKey(), entry.getValue());
        return out;
    }

    public void restore(Map<String, Float> snapshot) {
        remaining.clear();
        if (snapshot != null) for (Map.Entry<String, Float> entry : snapshot.entrySet())
            if (entry.getValue() != null && entry.getValue() > 0f)
                remaining.put(entry.getKey(), entry.getValue());
    }
}
