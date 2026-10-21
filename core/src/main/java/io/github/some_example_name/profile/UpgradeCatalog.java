package io.github.some_example_name.profile;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 영구 강화 트리 카탈로그(순수 Java). 가격·효과 수치의 유일한 출처는
 * assets/ui/hq_upgrade_tree_v1/data/upgrade_tree.json이며 코드에 복사하지 않는다.
 *
 * 로딩 단계에서 중복 ID·없는 선행 노드·순환 의존·음수 가격·알 수 없는 효과를 검출해
 * IllegalArgumentException으로 거부한다. 모든 선행 조건은 AND다. 각 노드는 1회 구매다.
 */
public final class UpgradeCatalog {

    public enum EffectType {
        MAX_HP_ADD, BASE_ATTACK_ADD, SKILL_UNLOCK, SKILL_HEAL_ADD, ACTIVE_SLOT_ADD,
        /** 특정 스킬(skillId)의 기본 피해 가산 — 기본 공격 강화와 중복 가산하지 않는다. */
        SKILL_DAMAGE_ADD,
        /** 보호막 흡수량 가산. */
        SHIELD_ABSORB_ADD
    }

    public static final class Node {
        public final String nodeId, branch, displayName, description, iconId, skillId;
        public final long costWon;
        public final List<String> prerequisites;
        public final EffectType effectType;
        public final int effectValue;
        public final float uiX, uiY;
        Node(String nodeId, String branch, String displayName, String description, long costWon,
             List<String> prerequisites, EffectType effectType, int effectValue,
             String skillId, String iconId, float uiX, float uiY) {
            this.nodeId = nodeId;
            this.branch = branch;
            this.displayName = displayName;
            this.description = description;
            this.costWon = costWon;
            this.prerequisites = prerequisites;
            this.effectType = effectType;
            this.effectValue = effectValue;
            this.skillId = skillId;
            this.iconId = iconId;
            this.uiX = uiX;
            this.uiY = uiY;
        }
    }

    public final int catalogVersion;
    public final int healBaseAmount;
    public final float healCooldownSeconds;
    private final Map<String, Node> nodesById = new LinkedHashMap<>();

    public UpgradeCatalog(String json) {
        JsonValue root = new JsonReader().parse(json);
        catalogVersion = root.getInt("catalogVersion", 1);
        JsonValue heal = root.get("skillDefaults") == null ? null
            : root.get("skillDefaults").get("heal");
        healBaseAmount = heal == null ? 30 : heal.getInt("healAmount", 30);
        healCooldownSeconds = heal == null ? 10f : heal.getFloat("cooldownSeconds", 10f);
        for (JsonValue n = root.get("nodes").child; n != null; n = n.next) {
            String nodeId = n.getString("nodeId");
            if (nodesById.containsKey(nodeId))
                throw new IllegalArgumentException("duplicate nodeId: " + nodeId);
            long cost = n.getLong("costWon");
            if (cost < 0) throw new IllegalArgumentException("negative cost: " + nodeId);
            EffectType effect;
            try {
                effect = EffectType.valueOf(n.getString("effectType"));
            } catch (IllegalArgumentException unknown) {
                throw new IllegalArgumentException("unknown effectType in " + nodeId
                    + ": " + n.getString("effectType"));
            }
            List<String> prereqs = new ArrayList<>();
            JsonValue pre = n.get("prerequisites");
            if (pre != null) for (JsonValue p = pre.child; p != null; p = p.next)
                prereqs.add(p.asString());
            float[] position = n.get("uiPosition") == null ? new float[]{0f, 0f}
                : n.get("uiPosition").asFloatArray();
            nodesById.put(nodeId, new Node(nodeId, n.getString("branch", ""),
                n.getString("displayName", nodeId), n.getString("description", ""),
                cost, prereqs, effect, n.getInt("effectValue", 0),
                n.getString("skillId", null), n.getString("iconId", "skill"),
                position[0], position[1]));
        }
        validateGraph();
    }

    private void validateGraph() {
        for (Node node : nodesById.values())
            for (String prereq : node.prerequisites)
                if (!nodesById.containsKey(prereq))
                    throw new IllegalArgumentException(
                        "missing prerequisite " + prereq + " of " + node.nodeId);
        // 순환 의존 검출(DFS 3색).
        Map<String, Integer> state = new HashMap<>();
        for (Node node : nodesById.values()) checkCycle(node.nodeId, state);
    }

    private void checkCycle(String nodeId, Map<String, Integer> state) {
        Integer current = state.get(nodeId);
        if (current != null && current == 1)
            throw new IllegalArgumentException("prerequisite cycle at " + nodeId);
        if (current != null && current == 2) return;
        state.put(nodeId, 1);
        for (String prereq : nodesById.get(nodeId).prerequisites) checkCycle(prereq, state);
        state.put(nodeId, 2);
    }

    public Node node(String nodeId) { return nodesById.get(nodeId); }
    public List<Node> nodes() { return new ArrayList<>(nodesById.values()); }

    /** 선행 조건(AND) 전부 구매되었는지. */
    public boolean prerequisitesMet(String nodeId, Set<String> purchased) {
        Node node = nodesById.get(nodeId);
        if (node == null) return false;
        for (String prereq : node.prerequisites)
            if (!purchased.contains(prereq)) return false;
        return true;
    }
}
