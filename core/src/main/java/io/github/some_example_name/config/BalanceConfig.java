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
    public float slimeMoveSpeed = 1.1f;
    public float researcherMoveSpeed = 1.8f;
    public float guardMoveSpeed = 2.2f;
    public float teamLeaderMoveSpeed = 2.0f;
    public float slimeDetectionRadius = 4.0f;
    public float researcherDetectionRadius = 5.0f;
    public float guardDetectionRadius = 7.0f;
    public float teamLeaderHearingRadius = 4.0f;
    public float monsterAttackRange = 0.75f;
    public float teamLeaderAttackRange = 1.2f;
    public float monsterAttackCooldownSeconds = 0.75f;
    public float monsterWanderSeconds = 1.8f;
    public float knockbackSeconds = 0.12f;
    public float playerKnockbackDistance = 0.28f;
    public float monsterKnockbackDistance = 0.2f;
    public int slimeSpawnCount = 3;
    public int researcherSpawnCount = 2;
    public int guardSpawnCount = 1;
    public int teamLeaderSpawnCount = 1;
    public int monsterSpawnAttempts = 500;
    public long monsterSpawnSeed = System.nanoTime();
    public float monsterSpawnExclusionRadius = 4f;
    public float monsterBoundsWidth = 0.62f;
    public float monsterBoundsHeight = 0.48f;
    public float monsterSpacing = 1.5f;

    // --- 시간 ---
    public float maxFrameDeltaSeconds = 1f / 30f; // delta 상한(저프레임 시 터널링/점프 방지)
    public float mapFadeSeconds = 0.35f;           // [임시값] 각 페이드 구간
}
