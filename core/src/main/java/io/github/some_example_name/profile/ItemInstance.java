package io.github.some_example_name.profile;

/** 본부 보관함의 개별 아이템 인스턴스. 같은 itemId라도 instanceId로 소유권을 구분한다. */
public final class ItemInstance {
    public final long instanceId;
    public final String itemId;

    public ItemInstance(long instanceId, String itemId) {
        if (instanceId < 0 || itemId == null || itemId.isEmpty())
            throw new IllegalArgumentException("item instance");
        this.instanceId = instanceId;
        this.itemId = itemId;
    }
}
