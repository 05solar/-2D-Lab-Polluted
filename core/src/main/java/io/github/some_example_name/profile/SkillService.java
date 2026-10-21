package io.github.some_example_name.profile;

/**
 * 공통 스킬 실행 서비스(순수 Java): 입력 → 장착/해금 → 생존·입력 허용 → 쿨다운 →
 * 효과별 검증 → 성공 시 효과 적용·쿨다운 시작. 성공하지 않은 시도는 HP·쿨다운·이펙트를
 * 바꾸지 않고 실패 이유를 결과로 돌려준다(표시는 UI 몫).
 *
 * 효과 수치는 Context(공용 파생 모델)에서 읽고 렌더러에 두지 않는다. 돌진의 이동·충돌은
 * 기존 규칙 그대로 호출 측이 수행한다(여기서는 발동 판정·쿨다운만 — 발동되면 소비).
 */
public final class SkillService {

    public enum Result {
        OK, LOCKED_SLOT, EMPTY_SLOT, NOT_UNLOCKED, ON_COOLDOWN, BLOCKED, DEAD, FULL_HP, UNKNOWN_SKILL
    }

    /** 실행 환경·효과 적용 접점(화면이 구현). 모델이 UI/GL에 의존하지 않게 한다. */
    public interface Context {
        boolean worldSkillInputAllowed(); // 패널/가방/일시정지/전환/등록 대기 중 false
        boolean alive();
        int currentHp();
        int currentMaxHp();
        boolean unlocked(String skillId);
        float cooldownSeconds(String skillId); // 공용 설정/카탈로그에서
        int healAmount();                      // 기본 + 강화 보너스
        /** 효과 적용: true를 돌려주면 성공으로 간주(돌진 시작 등). */
        boolean applyEffect(String skillId, int effectValue);
    }

    private SkillService() { }

    public static Result tryExecute(int slot, SkillLoadout loadout, int activeSlotCapacity,
                                    SkillRuntime runtime, Context ctx) {
        if (!ctx.worldSkillInputAllowed()) return Result.BLOCKED;
        if (!ctx.alive() || ctx.currentHp() <= 0) return Result.DEAD;
        if (slot < 0 || slot >= SkillLoadout.SLOT_COUNT) return Result.EMPTY_SLOT;
        if (slot >= activeSlotCapacity) return Result.LOCKED_SLOT;
        String skillId = loadout.skillAt(slot);
        if (skillId == null) return Result.EMPTY_SLOT;
        if (!ctx.unlocked(skillId)) return Result.NOT_UNLOCKED;
        if (!runtime.ready(skillId)) return Result.ON_COOLDOWN;
        // 효과별 실행 가능 검증(실패 시 아무것도 바꾸지 않는다).
        int effectValue = 0;
        if (SkillLoadout.HEAL.equals(skillId)) {
            if (ctx.currentHp() >= ctx.currentMaxHp()) return Result.FULL_HP;
            // 실제 회복량 = min(계산 회복량, maxHp - currentHp) — 적용은 Context가 한 번에.
            effectValue = Math.min(ctx.healAmount(), ctx.currentMaxHp() - ctx.currentHp());
        } else if (!SkillLoadout.KNOWN_SKILLS.contains(skillId)) {
            return Result.UNKNOWN_SKILL;
        }
        // 돌진/충격파/전기 투척/보호막: 시작 가능 여부는 Context.applyEffect가 최종 판정
        // (예: 돌진 중 재시작, 보호막 활성 중 재시전) — 거부 시 쿨다운을 시작하지 않는다.
        if (!ctx.applyEffect(skillId, effectValue)) return Result.BLOCKED;
        runtime.startCooldown(skillId, ctx.cooldownSeconds(skillId));
        return Result.OK;
    }
}
