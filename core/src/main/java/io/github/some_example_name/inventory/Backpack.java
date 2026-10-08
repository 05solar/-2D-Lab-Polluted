package io.github.some_example_name.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 연구소 1회 진입 중 회수품을 담는 8칸 가방(순수 도메인, 화면/텍스처 없음 → 화면 없이 테스트 가능).
 * 물품 하나가 한 칸을 차지하며 최대 8칸. 가득 차면 추가 회수를 거부한다(물품은 월드에 남긴다).
 *
 * 본부 인계 흐름이 생기면 {@link #handOver()}로 비워 다음 출입의 8칸을 재사용한다.
 */
public final class Backpack {

    public static final int MAX_SLOTS = 8;

    private final List<String> items = new ArrayList<>();

    /** 물품을 한 칸에 담는다. 가득 차 있으면 담지 않고 false(획득 거부)를 반환한다. */
    public boolean collect(String itemId) {
        if (itemId == null || itemId.isEmpty()) throw new IllegalArgumentException("itemId");
        if (isFull()) return false;
        items.add(itemId);
        return true;
    }

    public boolean isFull() { return items.size() >= MAX_SLOTS; }
    public int count() { return items.size(); }
    public int capacity() { return MAX_SLOTS; }

    /** 슬롯 인덱스(0..7)의 물품 ID. 비어 있으면 null. */
    public String slot(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    public List<String> items() { return Collections.unmodifiableList(items); }

    /** 본부 보관함 인계용: 현재 물품을 모두 돌려주고 가방을 비운다. */
    public List<String> handOver() {
        List<String> out = new ArrayList<>(items);
        items.clear();
        return out;
    }
}
