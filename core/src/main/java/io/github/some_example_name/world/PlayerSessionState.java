package io.github.some_example_name.world;

import io.github.some_example_name.entity.player.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared player and progression data, owned once by the game across map screens. */
public final class PlayerSessionState {
    private final Player player;
    private final List<String> inventory = new ArrayList<>();
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
    public void addItem(String id) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("item");
        inventory.add(id);
    }
    public List<String> inventory() { return Collections.unmodifiableList(inventory); }
    public void markProgress(String flag) {
        if (flag == null || flag.isEmpty()) throw new IllegalArgumentException("flag");
        progressFlags.add(flag);
    }
    public boolean hasProgress(String flag) { return progressFlags.contains(flag); }
}
