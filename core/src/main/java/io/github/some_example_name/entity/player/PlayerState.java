package io.github.some_example_name.entity.player;

/** Life state is separate from movement and facing so death is a one-way transition. */
public enum PlayerState {
    ALIVE,
    DEAD
}
