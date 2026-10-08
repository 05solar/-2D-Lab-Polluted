package io.github.some_example_name.world;

import io.github.some_example_name.entity.player.Player;
import io.github.some_example_name.inventory.Backpack;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared player and progression data, owned once by the game across map screens. */
public final class PlayerSessionState {
    private final Player player;
    private final Backpack backpack = new Backpack();
    private final Set<String> progressFlags = new HashSet<>();
    private long money;

    public PlayerSessionState(Player player) {
        if (player == null) throw new IllegalArgumentException("player");
        this.player = player;
    }

    public Player player() { return player; }
    public int hp() { return player.hp(); }
    public long money() { return money; }
    public void addMoney(long amount) {
        if (amount < 0 || Long.MAX_VALUE - money < amount) throw new IllegalArgumentException("money");
        money += amount;
    }
    public Backpack backpack() { return backpack; }
    /** 8칸 제한 하에 물품을 회수한다. 가득 차면 false(획득 거부). */
    public boolean collect(String id) { return backpack.collect(id); }
    public void addItem(String id) { backpack.collect(id); }
    public List<String> inventory() { return backpack.items(); }
    public void markProgress(String flag) {
        if (flag == null || flag.isEmpty()) throw new IllegalArgumentException("flag");
        progressFlags.add(flag);
    }
    public boolean hasProgress(String flag) { return progressFlags.contains(flag); }
}
