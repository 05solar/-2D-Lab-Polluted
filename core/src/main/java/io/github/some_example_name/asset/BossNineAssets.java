package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.some_example_name.config.AssetPaths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 9단계 보스 텍스처(boss_nine_stages_v1). Nearest, GameAssets 소유·해제.
 * 보스 시트 1024×1024 = 4×4 of 256(발 앵커 128,224 → 발 비율 32/256), 프레임별 trim 없음.
 * 세그먼트는 idle 0-3 / 예고 4-5 / 발사 6-7 / 회복 8-9 / 이동 10-11 / 사망 12-15(마지막 유지)
 * 로만 재생한다. VFX 스트립 640×160 = 4프레임(160×160). 초상화는 카탈로그 경로로 지연 로드.
 */
public final class BossNineAssets implements Disposable {

    /** 신규 엔진 보스 시트(animationSet 이름 = 파일 베이스). legacy_1_1은 BossAssets 사용. */
    private static final String[] BOSS_SETS = {
        "coolant_colossus", "culture_colony", "security_suppressor", "neural_specimen",
        "containment_fusion", "cryogenic_multipod", "mutant_botanical", "mobile_culture_tank"};
    private static final String[] VFX_SETS = {
        "cold_cone", "ice_zone", "ice_shard", "ice_impact", "acid_splash", "spore_zone",
        "red_laser", "psychic_wave", "psychic_blast", "enrage_aura", "root_eruption",
        "toxic_leak"};

    private final List<Texture> owned = new ArrayList<>();
    private final Map<String, TextureRegion[]> bossFrames = new LinkedHashMap<>();
    private final Map<String, TextureRegion[]> vfxFrames = new LinkedHashMap<>();
    private final Map<String, TextureRegion> portraits = new LinkedHashMap<>();
    private final TextureRegion cardNormal, cardSelected, cardLocked, cardCleared;
    private final TextureRegion lockBadge, clearBadge;

    public BossNineAssets() {
        for (String set : BOSS_SETS) {
            Texture sheet = load(AssetPaths.BOSS9_DIR + "runtime/boss/" + set + "_1024x1024.png");
            TextureRegion[][] grid = TextureRegion.split(sheet, 256, 256);
            TextureRegion[] frames = new TextureRegion[16];
            for (int i = 0; i < 16; i++) frames[i] = grid[i / 4][i % 4];
            bossFrames.put(set, frames);
        }
        for (String set : VFX_SETS) {
            Texture strip = load(AssetPaths.BOSS9_DIR + "runtime/vfx/" + set + "_640x160.png");
            TextureRegion[] frames = new TextureRegion[4];
            for (int i = 0; i < 4; i++) frames[i] = new TextureRegion(strip, i * 160, 0, 160, 160);
            vfxFrames.put(set, frames);
        }
        cardNormal = region(load(AssetPaths.BOSS9_DIR + "runtime/ui/stage_card_normal_144.png"));
        cardSelected = region(load(AssetPaths.BOSS9_DIR + "runtime/ui/stage_card_selected_144.png"));
        cardLocked = region(load(AssetPaths.BOSS9_DIR + "runtime/ui/stage_card_locked_144.png"));
        cardCleared = region(load(AssetPaths.BOSS9_DIR + "runtime/ui/stage_card_cleared_144.png"));
        lockBadge = region(load(AssetPaths.BOSS9_DIR + "runtime/ui/lock_badge_24.png"));
        clearBadge = region(load(AssetPaths.BOSS9_DIR + "runtime/ui/clear_badge_24.png"));
    }

    private Texture load(String path) {
        Texture texture = GameAssets.loadTile(path);
        owned.add(texture);
        return texture;
    }

    private static TextureRegion region(Texture texture) { return new TextureRegion(texture); }

    public boolean hasBossSheet(String animationSet) { return bossFrames.containsKey(animationSet); }

    public TextureRegion bossFrame(String animationSet, int index) {
        TextureRegion[] frames = bossFrames.get(animationSet);
        if (frames == null) throw new IllegalArgumentException("unknown boss sheet " + animationSet);
        return frames[Math.max(0, Math.min(15, index))];
    }

    public TextureRegion vfxFrame(String art, int index) {
        TextureRegion[] frames = vfxFrames.get(art);
        if (frames == null) frames = vfxFrames.get("acid_splash"); // 미지 아트 폴백
        return frames[Math.max(0, Math.min(3, index))];
    }

    public boolean hasVfx(String art) { return vfxFrames.containsKey(art); }

    /** 카탈로그 상대 경로("runtime/portraits/...png")로 지연 로드(실루엣/컬러 공용). */
    public TextureRegion portrait(String relativePath) {
        TextureRegion cached = portraits.get(relativePath);
        if (cached != null) return cached;
        TextureRegion loaded = region(load(AssetPaths.BOSS9_DIR + relativePath));
        portraits.put(relativePath, loaded);
        return loaded;
    }

    public TextureRegion card(boolean locked, boolean cleared) {
        return locked ? cardLocked : cleared ? cardCleared : cardNormal;
    }

    public TextureRegion cardSelected() { return cardSelected; }
    public TextureRegion lockBadge() { return lockBadge; }
    public TextureRegion clearBadge() { return clearBadge; }

    @Override public void dispose() {
        for (Texture texture : owned) texture.dispose();
        owned.clear();
    }
}
