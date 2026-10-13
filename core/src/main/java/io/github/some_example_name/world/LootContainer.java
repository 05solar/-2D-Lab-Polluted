package io.github.some_example_name.world;

import io.github.some_example_name.inventory.LootItem;
import io.github.some_example_name.inventory.LootTableCatalog;
import java.util.Random;

/** One-shot chest state driven by the animation metadata supplied with the loot pack. */
public final class LootContainer {
    private final String id;
    private final float x,y,frameDuration;
    private final int[] frames;
    private final int rewardFrame;
    private float elapsed;
    private boolean opened, rewardEmitted;
    private LootItem reward;
    public LootContainer(String id,float x,float y,float frameDuration,int[] frames) {
        this(id,x,y,frameDuration,frames,frames.length-1);
    }
    public LootContainer(String id,float x,float y,float frameDuration,int[] frames,int rewardFrame) {
        if(id==null||frames==null||frames.length==0||frameDuration<=0f) throw new IllegalArgumentException();
        if(rewardFrame<0||rewardFrame>=frames.length)throw new IllegalArgumentException("rewardFrame");
        this.id=id;this.x=x;this.y=y;this.frameDuration=frameDuration;this.frames=frames.clone();this.rewardFrame=rewardFrame;
    }
    public boolean open(LootTableCatalog tables,Random random) {
        if(opened) return false; opened=true; reward=tables.roll(id,random); return true;
    }
    public LootItem update(float delta) {
        if(!opened) return null;
        elapsed+=Math.max(0f,delta);
        if(!rewardEmitted&&elapsed>=rewardFrame*frameDuration) { rewardEmitted=true; return reward; }
        return null;
    }
    public int frame() { return frames[Math.min(frames.length-1,(int)(elapsed/frameDuration))]; }
    public boolean opened(){return opened;} public boolean rewardEmitted(){return rewardEmitted;}
    public String id(){return id;} public float x(){return x;} public float y(){return y;}
}
