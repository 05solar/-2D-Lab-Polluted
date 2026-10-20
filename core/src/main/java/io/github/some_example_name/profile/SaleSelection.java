package io.github.some_example_name.profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 판매창 선택 상태(순수 Java): 아이템 종류별 보유 수량과 선택 수량을 관리한다.
 * 수량은 항상 0..보유 수량으로 제한되고, 합계는 long 정수 원으로 계산한다.
 * 실제 판매 검증·차감은 {@link HeadquartersEconomy}가 다시 수행한다(이 클래스는 표시용).
 */
public final class SaleSelection {

    public static final class Row {
        public final String itemId;
        public final int owned;
        public final boolean sellable;
        public final int unitPrice;
        public int selected;
        Row(String itemId, int owned, boolean sellable, int unitPrice) {
            this.itemId = itemId;
            this.owned = owned;
            this.sellable = sellable;
            this.unitPrice = unitPrice;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    /** 보관함 현황으로 목록을 다시 만든다. 가능한 기존 선택 수량은 범위 안에서 유지한다. */
    public void refresh(Map<String, Integer> heldCounts, HeadquartersEconomy economy) {
        Map<String, Integer> previous = new LinkedHashMap<>();
        for (Row row : rows) previous.put(row.itemId, row.selected);
        rows.clear();
        for (Map.Entry<String, Integer> entry : heldCounts.entrySet()) {
            Row row = new Row(entry.getKey(), entry.getValue(),
                economy.sellable(entry.getKey()), economy.unitPrice(entry.getKey()));
            Integer kept = previous.get(row.itemId);
            if (kept != null && row.sellable) row.selected = Math.min(kept, row.owned);
            rows.add(row);
        }
    }

    public List<Row> rows() { return rows; }

    public void adjust(int rowIndex, int delta) {
        if (rowIndex < 0 || rowIndex >= rows.size()) return;
        Row row = rows.get(rowIndex);
        if (!row.sellable) return;
        row.selected = Math.max(0, Math.min(row.owned, row.selected + delta));
    }

    /** 행 토글: 선택 0이면 전량 선택, 아니면 해제. */
    public void toggle(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) return;
        Row row = rows.get(rowIndex);
        if (!row.sellable) return;
        row.selected = row.selected == 0 ? row.owned : 0;
    }

    /** 전체 선택/해제: 하나라도 덜 선택돼 있으면 전부 선택, 아니면 전부 해제. */
    public void toggleAll() {
        boolean anyUnselected = false;
        for (Row row : rows) if (row.sellable && row.selected < row.owned) anyUnselected = true;
        for (Row row : rows) if (row.sellable) row.selected = anyUnselected ? row.owned : 0;
    }

    public int selectedCount() {
        int count = 0;
        for (Row row : rows) count += row.selected;
        return count;
    }

    /** 선택 합계(원). 오버플로 시 Long.MAX_VALUE 반환(실판매는 economy가 거부). */
    public long selectedTotalWon() {
        long total = 0L;
        for (Row row : rows) {
            try {
                total = Math.addExact(total,
                    Math.multiplyExact((long) row.unitPrice, (long) row.selected));
            } catch (ArithmeticException overflow) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    /** economy.sell에 넘길 선택 수량(0 제외). */
    public Map<String, Integer> selectedQuantities() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Row row : rows) if (row.selected > 0) out.put(row.itemId, row.selected);
        return out;
    }

    public void clearSelection() {
        for (Row row : rows) row.selected = 0;
    }
}
