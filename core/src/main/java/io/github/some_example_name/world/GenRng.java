package io.github.some_example_name.world;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 연구소 생성 전용 결정적 난수(순수 Java). 팩 참조 규칙(FNV-1a32(seed십진문자열+"|"+scope)
 * → XorShift32(13,17,5))을 그대로 이식했고 golden vector로 파이썬 참조와 일치를 검증한다.
 *
 * scope 문자열로 topology/theme/furniture/loot/monster 하위 스트림을 분리한다 — 한 스트림의
 * 소비량이 변해도(예: 바닥 장식 추가) 다른 스트림의 결과가 밀리지 않는다. 전역 Random·시스템
 * 시간·컬렉션 순회 순서에 의존하지 않으며, 후보는 호출 측이 정렬한 뒤 넘긴다.
 */
public final class GenRng {

    private int state; // unsigned 32비트로 취급

    public GenRng(String seedDecimal, String scope) {
        int h = 0x811C9DC5; // 2166136261
        for (byte b : (seedDecimal + "|" + scope).getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xFF);
            h *= 16777619; // 32비트 오버플로 = 파이썬의 &0xffffffff와 동일
        }
        state = h == 0 ? 0x6D2B79F5 : h;
    }

    /** 다음 unsigned 32비트 값(0..2^32-1)을 long으로. */
    public long next() {
        int x = state;
        x ^= x << 13;
        x ^= x >>> 17; // unsigned shift — 파이썬 x>>17과 동일
        x ^= x << 5;
        state = x;
        return x & 0xFFFFFFFFL;
    }

    /** [0, bound) — 참조와 같은 unsigned modulo. bound ≥ 1. */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound");
        return (int) (next() % bound);
    }

    public <T> T pick(List<T> items) { return items.get(nextInt(items.size())); }

    /** 참조와 동일한 Fisher-Yates(뒤→앞, j = next()%(i+1)). 제자리 섞기. */
    public <T> void shuffle(List<T> items) {
        for (int i = items.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T tmp = items.get(i);
            items.set(i, items.get(j));
            items.set(j, tmp);
        }
    }

    /** 가중 추첨(참조와 동일: n = next()%sum 후 순서대로 차감). weights[i] ≥ 1. */
    public int weightedIndex(int[] weights) {
        long sum = 0;
        for (int w : weights) sum += w;
        long n = next() % sum;
        for (int i = 0; i < weights.length; i++) {
            if (n < weights[i]) return i;
            n -= weights[i];
        }
        return weights.length - 1;
    }
}
