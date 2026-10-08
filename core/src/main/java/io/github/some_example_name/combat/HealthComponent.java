package io.github.some_example_name.combat;

/** Small GL independent health model shared by player combat and monsters. */
public final class HealthComponent {
    private final int maxHp;
    private int hp;

    public HealthComponent(int maxHp) {
        if (maxHp <= 0) throw new IllegalArgumentException("maxHp");
        this.maxHp = maxHp;
        hp = maxHp;
    }

    public int maxHp() { return maxHp; }
    public int hp() { return hp; }
    public boolean isAlive() { return hp > 0; }

    public boolean damage(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount");
        int before = hp;
        hp = Math.max(0, hp - amount);
        return before > 0 && hp == 0;
    }

    public void heal(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount");
        hp = Math.min(maxHp, hp + amount);
    }

    public float ratio() { return Math.max(0f, Math.min(1f, hp / (float) maxHp)); }
}
