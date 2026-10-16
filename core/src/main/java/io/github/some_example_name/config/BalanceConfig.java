package io.github.some_example_name.config;

/**
 * 조정 가능한 게임 수치를 모아 둔다. 숫자를 코드 여러 곳에 직접 쓰지 않는다.
 * 전역 singleton이 아니라 주입 가능한 값 객체다. (테스트는 이 객체를 생성해 주입한다)
 * 수치가 잦게 바뀌면 JSON으로 이동한다.
 *
 * [확정] 표시 외의 값은 기획 확정 전 임시값이다. (docs/GAME_RULES.md 참고)
 */
public class BalanceConfig {

    // --- 플레이어 ---
    public int playerMaxHp = 100;                 // [확정]
    public int playerAttackDamage = 15;           // [확정]
    public float playerWalkSpeed = 3.5f;          // [임시값] 월드 단위(타일)/초
    public float playerRunSpeed = 6.5f;           // [임시값] 월드 단위(타일)/초
    public float playerAttackCooldownSeconds = 0.45f;
    public float playerInvulnerabilitySeconds = 0.6f;
    public float playerAttackRange = 0.9f;
    public float playerAttackWidth = 0.8f;
    public float playerBoundsWidth = 0.6f;        // [임시값] 충돌 박스 폭(월드 단위)
    public float playerBoundsHeight = 0.5f;       // [임시값] 충돌 박스 높이(발밑 기준)
    /** 몬스터 처치 1마리당 HP 회복량(최대 HP 제한, 사망자 부활 없음). [확정] */
    public int killHealHp = 20;
    /** 사망 후 본부 재소환까지 대기(쓰러짐 연출 0.8s 포함). [임시값] */
    public float deathRespawnDelaySeconds = 2.0f;

    // --- 돌진(F) ---
    public float dashDistanceTiles = 3f;          // [확정] 초기 돌진 거리(타일)
    public float dashDurationSeconds = 0.18f;     // [확정] 돌진 지속 시간
    public float dashCooldownSeconds = 3f;        // [확정] 발동 순간부터의 쿨다운
    /** 돌진 타격 판정의 몸체 확장(월드 단위). 스치는 몬스터도 닿은 것으로 본다. */
    public float dashHitPadding = 0.12f;
    /** 돌진 하위 이동 단계의 최대 길이(월드 단위) — 벽/가구 관통·중간 몬스터 건너뜀 방지. */
    public float dashSubstepLength = 0.3f;

    // --- 효과음·음악 ---
    public float sfxMasterVolume = 1f;            // 전체 효과음 음량(0..1)
    public float musicVolume = 0.45f;             // 배경음악 음량(0..1, 효과음을 가리지 않게 낮게)

    // --- 몬스터(확정 HP / 임시 그 외) : 2단계 이후 사용, 확정값 중앙 관리용 ---
    public int slimeMaxHp = 20;                   // [확정]
    public int guardMaxHp = 60;                   // [확정]
    public int researcherMaxHp = 40;              // [확정]
    public int teamLeaderMaxHp = 70;              // [확정]
    public int teamLeaderAttackDamage = 35;       // [확정]
    public int teamLeaderRunTriggerCount = 2;     // [확정]
    public int slimeAttackDamage = 10;
    public int researcherAttackDamage = 15;
    public int guardAttackDamage = 20;
    // 2026-10-16: 전체적으로 느리게 하향(기존 1.1/1.8/2.2/2.0).
    public float slimeMoveSpeed = 0.8f;
    public float researcherMoveSpeed = 1.3f;
    public float guardMoveSpeed = 1.6f;
    public float teamLeaderMoveSpeed = 1.5f;
    public float slimeDetectionRadius = 4.0f;
    public float researcherDetectionRadius = 5.0f;
    public float guardDetectionRadius = 7.0f;
    public float teamLeaderHearingRadius = 4.0f;
    public float monsterAttackRange = 0.75f;
    public float teamLeaderAttackRange = 1.2f;
    public float monsterAttackCooldownSeconds = 0.75f;
    // --- 몬스터 탐지·추적 해제·배회 (2026-10-16 개편) ---
    // 발견 거리는 종류별 *DetectionRadius(슬라임 4 / 연구원 5 / 경비원 7 ≈ 기준 5타일)를 쓴다.
    // 해제 거리를 발견 거리보다 크게 둬 경계에서 추적 상태가 반복 전환되지 않는다(히스테리시스).
    /** 추적 해제 거리(타일). 이보다 멀어지면 추적을 멈춘다. */
    public float monsterChaseReleaseRadius = 8f;
    /** 시야를 잃은 뒤에도 마지막 목격 지점으로 추적을 유지하는 시간(초). */
    public float monsterLostSightGraceSeconds = 2f;
    /** 마지막 목격 지점 주변 탐색(SEARCH) 시간 범위(초). */
    public float monsterSearchMinSeconds = 2f;
    public float monsterSearchMaxSeconds = 4f;
    /** 활동 구역(스폰 지점)에서 벗어날 수 있는 최대 거리(타일). 넘으면 복귀한다. */
    public float monsterHomeLeashRadius = 12f;
    /** 감지·경로 재평가 주기(초). 개체 id로 분산해 매 프레임 전체 몬스터가 검사하지 않는다. */
    public float monsterSenseIntervalSeconds = 0.15f;
    // 종류별 배회(이동 거리 범위 / 휴식 시간 범위, 초) — 서로 리듬이 다르게.
    public float slimeWanderStepMin = 1f, slimeWanderStepMax = 2f;      // 짧게 이동, 자주 멈춤
    public float slimeRestMin = 0.8f, slimeRestMax = 2f;
    public float guardPatrolRadius = 4f;                                 // 홈 주변 순찰 지점 반경
    public float guardRestMin = 0.5f, guardRestMax = 1f;                 // 순찰 지점 사이 짧은 대기
    public float researcherWanderRadius = 2f;                            // 장비(홈) 주변 짧은 배회
    public float researcherRestMin = 1.5f, researcherRestMax = 3f;       // 상대적으로 긴 대기
    public float knockbackSeconds = 0.12f;
    public float playerKnockbackDistance = 0.28f;
    public float monsterKnockbackDistance = 0.2f;
    // EASY 종류별 "기준" 생성 수(변경 전 원본값 — 배수 계산의 단일 근거).
    // applyDifficulty(EASY)는 항상 기준값×easyMonsterMultiplier로 다시 계산하므로
    // 입장/재생성을 반복해도 이미 늘린 수에 다시 곱해지지 않는다.
    public int easySlimeBaseCount = 3;            // [확정]
    public int easyResearcherBaseCount = 2;       // [확정]
    public int easyGuardBaseCount = 1;            // [확정]
    public int easyTeamLeaderBaseCount = 1;       // [확정]
    /** EASY 전용 몬스터 생성 배수. NORMAL/HARD에는 적용하지 않는다. [확정] */
    public int easyMonsterMultiplier = 8;
    public int slimeSpawnCount = easySlimeBaseCount * easyMonsterMultiplier;
    public int researcherSpawnCount = easyResearcherBaseCount * easyMonsterMultiplier;
    public int guardSpawnCount = easyGuardBaseCount * easyMonsterMultiplier;
    public int teamLeaderSpawnCount = easyTeamLeaderBaseCount * easyMonsterMultiplier;
    public int monsterSpawnAttempts = 5000;
    public long monsterSpawnSeed = System.nanoTime();
    public float monsterSpawnExclusionRadius = 4f;
    public float monsterBoundsWidth = 0.62f;
    public float monsterBoundsHeight = 0.48f;
    public float monsterSpacing = 1.5f;

    // --- 루트 컨테이너(상자/캐비넷) ---
    /** 개봉에 필요한 SPACE 입력 횟수(키를 뗐다 다시 눌러야 1회). [확정] */
    public int woodenChestPresses = 1;
    public int epicCabinetPresses = 5;
    public int legendaryCabinetPresses = 8;
    /** 컨테이너 상호작용 거리(월드 단위). */
    public float containerInteractRadius = 1.45f;
    /** 캐비넷(일반/고급) 표시 배율. 닫힘·진행·열림 전 프레임 동일 기준점(하단 중앙). */
    public float cabinetRenderScale = 1.4f;
    /** easy 맵 컨테이너 수(등급 비율 wood>epic>legendary 유지, 대부분의 방에 ≥1 + 도서관/표본실 복수). */
    public int woodenChestCount = 14;
    public int epicCabinetCount = 7;
    public int legendaryCabinetCount = 2;

    /** 컨테이너 id → 개봉 필요 입력 횟수. */
    public int requiredPresses(String containerId) {
        if ("wooden_chest".equals(containerId)) return woodenChestPresses;
        if ("epic_cabinet".equals(containerId)) return epicCabinetPresses;
        if ("legendary_cabinet".equals(containerId)) return legendaryCabinetPresses;
        throw new IllegalArgumentException("unknown container: " + containerId);
    }

    // --- 시간 ---
    public float maxFrameDeltaSeconds = 1f / 30f; // delta 상한(저프레임 시 터널링/점프 방지)
    public float mapFadeSeconds = 0.35f;           // [임시값] 각 페이드 구간

    public void applyDifficulty(io.github.some_example_name.world.GameDifficulty difficulty) {
        switch (difficulty) {
            // EASY만 기준값×배수(3/2/1/1 × 8 = 24/16/8/8). 항상 기준값에서 다시 계산한다.
            case EASY:
                slimeSpawnCount = easySlimeBaseCount * easyMonsterMultiplier;
                researcherSpawnCount = easyResearcherBaseCount * easyMonsterMultiplier;
                guardSpawnCount = easyGuardBaseCount * easyMonsterMultiplier;
                teamLeaderSpawnCount = easyTeamLeaderBaseCount * easyMonsterMultiplier;
                break;
            // NORMAL/HARD 생성 수·주기는 이번 변경에서 바꾸지 않는다.
            case NORMAL: slimeSpawnCount=5; researcherSpawnCount=3; guardSpawnCount=2; teamLeaderSpawnCount=1; break;
            case HARD: slimeSpawnCount=7; researcherSpawnCount=5; guardSpawnCount=3; teamLeaderSpawnCount=1; break;
            default: throw new IllegalArgumentException("difficulty");
        }
    }
}
