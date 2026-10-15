package io.github.some_example_name.world;

import io.github.some_example_name.inventory.LootItem;
import io.github.some_example_name.inventory.LootTableCatalog;
import java.util.Random;

/**
 * One-shot chest state driven by the animation metadata supplied with the loot pack.
 *
 * 개봉은 "서로 다른 SPACE 입력" {@code requiredPresses}회가 모여야 완료된다(키를 뗐다 다시
 * 눌러야 1회 — 엣지 판정은 입력 계층이 보장). 진행도는 이 컨테이너에 저장되어 잠시 멀어져도
 * 유지되며, 필요 횟수 도달 시 한 번만 열림 애니메이션과 아이템 추첨을 실행한다.
 */
public final class LootContainer {
    /** 유효한 개봉 입력 한 번의 결과. */
    public enum PressResult { PROGRESS, OPENED, ALREADY_OPEN }

    private final String id;
    private final float x,y,frameDuration;
    private final int[] frames;
    private final int rewardFrame;
    private final int requiredPresses;
    private int presses;
    private float elapsed;
    private boolean opened, rewardEmitted;
    private LootItem reward;
    public LootContainer(String id,float x,float y,float frameDuration,int[] frames) {
        this(id,x,y,frameDuration,frames,frames.length-1);
    }
    public LootContainer(String id,float x,float y,float frameDuration,int[] frames,int rewardFrame) {
        this(id,x,y,frameDuration,frames,rewardFrame,1);
    }
    public LootContainer(String id,float x,float y,float frameDuration,int[] frames,int rewardFrame,
                         int requiredPresses) {
        if(id==null||frames==null||frames.length==0||frameDuration<=0f) throw new IllegalArgumentException();
        if(rewardFrame<0||rewardFrame>=frames.length)throw new IllegalArgumentException("rewardFrame");
        if(requiredPresses<1)throw new IllegalArgumentException("requiredPresses");
        this.id=id;this.x=x;this.y=y;this.frameDuration=frameDuration;this.frames=frames.clone();this.rewardFrame=rewardFrame;
        this.requiredPresses=requiredPresses;
    }

    /**
     * 유효한 개봉 입력 한 번. 필요 횟수에 도달하는 입력에서만 추첨·개봉하며(1회 보장),
     * 이미 열린 컨테이너는 재추첨하지 않는다.
     */
    public PressResult press(LootTableCatalog tables, Random random) {
        if (opened) return PressResult.ALREADY_OPEN;
        presses++;
        if (presses < requiredPresses) return PressResult.PROGRESS;
        open(tables, random);
        return PressResult.OPENED;
    }

    public boolean open(LootTableCatalog tables,Random random) {
        if(opened) return false; opened=true; presses=requiredPresses; reward=tables.roll(id,random); return true;
    }
    public LootItem update(float delta) {
        if(!opened) return null;
        elapsed+=Math.max(0f,delta);
        if(!rewardEmitted&&elapsed>=rewardFrame*frameDuration) { rewardEmitted=true; return reward; }
        return null;
    }
    public int frame() { return frames[Math.min(frames.length-1,(int)(elapsed/frameDuration))]; }
    public boolean opened(){return opened;} public boolean rewardEmitted(){return rewardEmitted;}
    public int presses(){return presses;} public int requiredPresses(){return requiredPresses;}
    /** 0..1 개봉 진행 비율(발광 강도 등 연출용). */
    public float progressRatio(){return Math.min(1f,presses/(float)requiredPresses);}
    public String id(){return id;} public float x(){return x;} public float y(){return y;}
}
