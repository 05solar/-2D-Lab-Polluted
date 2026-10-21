package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.config.AssetPaths;
import io.github.some_example_name.entity.Direction;
import io.github.some_example_name.render.AnimationSet;
import io.github.some_example_name.world.LaboratoryTileCatalogV2;
import io.github.some_example_name.world.LaboratoryTileSetV2;
import io.github.some_example_name.combat.MonsterType;
import io.github.some_example_name.render.MonsterAnimationSet;

import java.util.HashMap;
import java.util.Map;

/**
 * 게임 자산의 로딩과 해제를 한곳에서 책임진다. 게임 루트에서 한 번 생성하고 종료 시 한 번 해제한다.
 * 텍스처 소유권은 여기 한 곳에만 둔다(엔티티/레이아웃/렌더러가 Texture를 새로 만들지 않는다).
 *
 * Laboratory Tileset V2: laboratory_tiles_v2.json을 읽어 카탈로그를 만들고, 4개 아틀라스 텍스처를
 * 로드해 {@link LaboratoryTileSetV2}(ID→TextureRegion)를 구성한다. 구버전 단일 타일셋은 더 이상
 * 로드하지 않는다(fallback로 옛 타일이 섞이지 않도록).
 */
public class GameAssets implements Disposable {

    private final Map<String, Texture> atlasTextures = new HashMap<>();
    private final LaboratoryTileSetV2 tileSet;
    private final Texture sideDoorTexture;
    private final HeadquartersAssets headquartersAssets;
    private final RoomKitAssets roomKitAssets;
    private final LaboratoryFurnitureAssets laboratoryFurnitureAssets;
    private final LootAssets lootAssets;
    private final DifficultyModalAssets difficultyModalAssets;
    private final PlayerHudAssets hudAssets;
    private final ExteriorWallAssets exteriorWalls;
    private final LaboratoryStructureV4Assets structureV4;
    private final CombatSoundAssets combatSounds;
    private final CombatUiAssets combatUi;
    private final HqSalesAssets hqSales;
    private final UpgradeAssets upgrade;
    private final SkillAssets skills;
    private final ThreeSkillsAssets threeSkills;
    private final BossAssets boss;
    private final BossNineAssets bossNine;
    // 배경음악: Sound와 달리 스트리밍되는 Music 인스턴스. 여기서 소유·해제하고
    // 재생 시작(무한 반복)은 게임 루트가 한 번만 호출한다.
    private final com.badlogic.gdx.audio.Music backgroundMusic;
    // Mona 픽셀 폰트(OFL)로 생성한 BitmapFont. 화면 UI용과 월드 좌표용(연타 카운트 —
    // 자체 scale 상태를 가져 분리)을 따로 둔다. 렌더러는 빌려 쓰기만 하고 해제하지 않는다.
    private final com.badlogic.gdx.graphics.g2d.BitmapFont uiFont, worldFont;
    private final Texture minimapFrameTex, footprintsTex, returnButtonTex;
    private final TextureRegion minimapFrame, footprints, returnButton;
    private final Texture sfxToggleOnTex, sfxToggleOffTex;
    private final TextureRegion sfxToggleOn, sfxToggleOff;
    private final Texture fadePixel;

    private final Texture playerTexture;
    private final Texture playerDeathTexture;
    private final AnimationSet playerAnimations;
    private final Map<MonsterType, Texture> monsterTextures = new HashMap<>();
    private final Map<MonsterType, MonsterAnimationSet> monsterAnimations = new HashMap<>();

    public GameAssets() {
        LaboratoryTileCatalogV2 catalog = LaboratoryTileCatalogV2.fromJson(
            Gdx.files.internal(AssetPaths.TILES_V2_JSON).readString("UTF-8"));
        for (LaboratoryTileCatalogV2.AtlasDef atlas : catalog.atlases()) {
            Texture tex = loadTile(AssetPaths.TILES_V2_DIR + atlas.file);
            atlasTextures.put(atlas.name, tex);
            Gdx.app.log("GameAssets", "atlas=" + atlas.name + " file=" + atlas.file
                + " size=" + tex.getWidth() + "x" + tex.getHeight()
                + " grid=" + atlas.columns + "x" + atlas.rows + " tiles=" + atlas.ids.size());
        }
        tileSet = new LaboratoryTileSetV2(catalog, atlasTextures);
        sideDoorTexture = loadTile(AssetPaths.LAB_SIDE_DOORS);
        TextureRegion[][] sideDoors = TextureRegion.split(sideDoorTexture, 64, 64);
        // Sheet row 0: left frame is closed, right frame is open. Keep logical door IDs.
        tileSet.replaceRegion("vertical_door_closed", sideDoors[0][0]);
        tileSet.replaceRegion("vertical_door_open", sideDoors[0][1]);
        Gdx.app.log("GameAssets", "tileSet regions=" + tileSet.regionCount());

        headquartersAssets = new HeadquartersAssets();
        roomKitAssets = new RoomKitAssets();
        laboratoryFurnitureAssets = new LaboratoryFurnitureAssets();
        lootAssets = new LootAssets(AssetPaths.LOOT_DIR,
            Gdx.files.internal(AssetPaths.LOOT_TABLES).readString("UTF-8"));
        difficultyModalAssets = new DifficultyModalAssets();
        hudAssets = new PlayerHudAssets();
        exteriorWalls = new ExteriorWallAssets();
        structureV4 = new LaboratoryStructureV4Assets();
        combatSounds = new CombatSoundAssets();
        combatUi = new CombatUiAssets();
        hqSales = new HqSalesAssets();
        upgrade = new UpgradeAssets();
        skills = new SkillAssets();
        threeSkills = new ThreeSkillsAssets();
        boss = new BossAssets();
        bossNine = new BossNineAssets();
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(AssetPaths.MUSIC_BACKGROUND));
        // Mona12 Text KR(픽셀 폰트, 12px 설계)을 2배수 24px로 생성해 선명하게 쓴다.
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator fontGenerator =
            new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(
                Gdx.files.internal(AssetPaths.FONT_MONA_KR_BOLD));
        com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter fontParameter =
            new com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParameter.size = 24;
        fontParameter.minFilter = Texture.TextureFilter.Nearest;
        fontParameter.magFilter = Texture.TextureFilter.Nearest;
        uiFont = fontGenerator.generateFont(fontParameter);
        uiFont.setUseIntegerPositions(false);
        worldFont = fontGenerator.generateFont(fontParameter);
        worldFont.setUseIntegerPositions(false);
        fontGenerator.dispose();
        minimapFrameTex = loadTile(AssetPaths.NAV_MINIMAP_FRAME);
        footprintsTex = loadTile(AssetPaths.NAV_FOOTPRINTS);
        returnButtonTex = loadTile(AssetPaths.NAV_RETURN_BUTTON);
        minimapFrame = new TextureRegion(minimapFrameTex);
        footprints = new TextureRegion(footprintsTex);
        returnButton = new TextureRegion(returnButtonTex);
        sfxToggleOnTex = loadTile(AssetPaths.SFX_TOGGLE_ON);
        sfxToggleOffTex = loadTile(AssetPaths.SFX_TOGGLE_OFF);
        sfxToggleOn = new TextureRegion(sfxToggleOnTex);
        sfxToggleOff = new TextureRegion(sfxToggleOffTex);
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(1f, 1f, 1f, 1f);
        pixel.fill();
        fadePixel = new Texture(pixel);
        pixel.dispose();

        playerTexture = loadTile(AssetPaths.PLAYER_SHEET);
        playerAnimations = loadPlayerAnimations(playerTexture);
        playerDeathTexture = loadTile(AssetPaths.PLAYER_DEATH_SHEET);
        loadPlayerDeathAnimations(playerDeathTexture, playerAnimations);
        loadMonsterAssets();
    }

    /** Nearest 필터 + ClampToEdge(타일 경계 번짐/보라선 방지). */
    static Texture loadTile(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return texture;
    }

    private static AnimationSet loadPlayerAnimations(Texture sheet) {
        JsonValue root = new JsonReader().parse(Gdx.files.internal(AssetPaths.PLAYER_ANIMATION_DATA));
        int frameW = root.getInt("frameWidth");
        int frameH = root.getInt("frameHeight");
        TextureRegion[][] frames = TextureRegion.split(sheet, frameW, frameH);

        AnimationSet set = new AnimationSet();
        for (JsonValue a = root.get("animations").child; a != null; a = a.next) {
            String name = a.name();
            int row = a.getInt("row");
            int[] frameIndices = a.get("frames").asIntArray();
            float frameDuration = a.getFloat("frameDuration");
            boolean loop = a.getBoolean("loop", false);

            Array<TextureRegion> regions = new Array<>();
            for (int fi : frameIndices) regions.add(frames[row][fi]);
            Animation<TextureRegion> animation = new Animation<>(frameDuration,
                regions.toArray(TextureRegion.class));
            animation.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
            set.putAnimation(name, animation);

            if (a.has("hitFrames")) set.putHitFrames(name, a.get("hitFrames").asIntArray());
        }

        JsonValue idle = root.get("idleFrames");
        if (idle != null) {
            for (JsonValue d = idle.child; d != null; d = d.next) {
                Direction direction = Direction.valueOf(d.name().toUpperCase());
                set.putIdle(direction, frames[d.getInt("row")][d.getInt("column")]);
            }
        }
        return set;
    }

    private static void loadPlayerDeathAnimations(Texture sheet, AnimationSet set) {
        JsonValue root = new JsonReader().parse(
            Gdx.files.internal(AssetPaths.PLAYER_DEATH_ANIMATION_DATA));
        int frameW = root.getInt("frameWidth"), frameH = root.getInt("frameHeight");
        if (frameW != 64 || frameH != 64 || sheet.getWidth() != 512 || sheet.getHeight() != 256)
            throw new IllegalStateException("unexpected player death sheet dimensions");
        TextureRegion[][] grid = TextureRegion.split(sheet, frameW, frameH);
        for (JsonValue a = root.get("animations").child; a != null; a = a.next) {
            int row = a.getInt("row");
            int[] indices = a.get("frames").asIntArray();
            Array<TextureRegion> frames = new Array<>();
            for (int index : indices) frames.add(grid[row][index]);
            Animation<TextureRegion> animation = new Animation<>(a.getFloat("frameDuration"),
                frames.toArray(TextureRegion.class));
            animation.setPlayMode(a.getBoolean("loop", false)
                ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
            set.putAnimation(a.name(), animation);
        }
    }

    private void loadMonsterAssets() {
        loadMonster(MonsterType.SLIME, "slime");
        loadMonster(MonsterType.RESEARCHER, "researcher");
        loadMonster(MonsterType.GUARD, "guard");
        loadMonster(MonsterType.TEAM_LEADER, "team_leader");
    }

    private void loadMonster(MonsterType type, String key) {
        String base = AssetPaths.MONSTER_DIR + key;
        com.badlogic.gdx.utils.JsonValue root = new JsonReader().parse(
            Gdx.files.internal(base + "_animations.json"));
        Texture texture = loadTile(AssetPaths.MONSTER_DIR + root.getString("image"));
        monsterTextures.put(type, texture);
        int frameW = root.getInt("frameWidth"), frameH = root.getInt("frameHeight");
        TextureRegion[][] grid = TextureRegion.split(texture, frameW, frameH);
        MonsterAnimationSet set = new MonsterAnimationSet();
        for (com.badlogic.gdx.utils.JsonValue a = root.get("animations").child;
             a != null; a = a.next) {
            int row = a.getInt("row");
            int[] indices = a.get("frames").asIntArray();
            float duration = a.getFloat("frameDuration");
            com.badlogic.gdx.utils.Array<TextureRegion> frames = new com.badlogic.gdx.utils.Array<>();
            for (int index : indices) frames.add(grid[row][index]);
            boolean loop = a.getBoolean("loop", false);
            Animation<TextureRegion> animation = new Animation<>(duration,
                frames.toArray(TextureRegion.class));
            animation.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
            int[] hit = a.has("hitFrames") ? a.get("hitFrames").asIntArray() : null;
            set.put(a.name(), animation, hit);
        }
        monsterAnimations.put(type, set);
    }

    public LaboratoryTileSetV2 tileSet() { return tileSet; }
    public HeadquartersAssets headquarters() { return headquartersAssets; }
    public RoomKitAssets roomKits() { return roomKitAssets; }
    public LaboratoryFurnitureAssets laboratoryFurniture() { return laboratoryFurnitureAssets; }
    public LootAssets loot() { return lootAssets; }
    public DifficultyModalAssets difficultyModal() { return difficultyModalAssets; }
    public PlayerHudAssets hud() { return hudAssets; }
    public ExteriorWallAssets exteriorWalls() { return exteriorWalls; }
    public LaboratoryStructureV4Assets structureV4() { return structureV4; }
    public CombatSoundAssets combatSounds() { return combatSounds; }
    public CombatUiAssets combatUi() { return combatUi; }
    public HqSalesAssets hqSales() { return hqSales; }
    public UpgradeAssets upgrade() { return upgrade; }
    public SkillAssets skills() { return skills; }
    public ThreeSkillsAssets threeSkills() { return threeSkills; }
    public BossAssets boss() { return boss; }
    public BossNineAssets bossNine() { return bossNine; }
    public com.badlogic.gdx.audio.Music backgroundMusic() { return backgroundMusic; }
    /** 화면 UI 텍스트(HP 숫자·쿨다운·키 라벨 등)용 Mona BitmapFont(공유 — 해제 금지). */
    public com.badlogic.gdx.graphics.g2d.BitmapFont uiFont() { return uiFont; }
    /** 월드 좌표 텍스트(연타 카운트)용 Mona BitmapFont — 자체 scale을 유지하므로 분리. */
    public com.badlogic.gdx.graphics.g2d.BitmapFont worldFont() { return worldFont; }

    /**
     * 오디오 토글(버튼 1개로 효과음+배경음악 세트 전환).
     * 끄면 재생 중 효과음 정지·이후 재생 차단 + 배경음악 일시정지,
     * 켜면 효과음 재생 허용 + 배경음악을 멈춘 지점부터 재개한다(무한 반복 유지).
     *
     * @return 전환 후 켜짐 여부
     */
    public boolean toggleAudio() {
        boolean enabled = combatSounds.toggleSfx();
        // 개발용 무음 실행(LAB_MUTE_MUSIC=1)에서는 음악을 건드리지 않는다.
        if (!"1".equals(System.getenv("LAB_MUTE_MUSIC"))) {
            if (enabled) {
                if (!backgroundMusic.isPlaying()) backgroundMusic.play();
            } else {
                backgroundMusic.pause();
            }
        }
        return enabled;
    }
    /** 현재 효과음 설정에 맞는 토글 버튼 이미지(켜짐=청록 음파, 꺼짐=주황 빗금). */
    public TextureRegion sfxToggle(boolean enabled) { return enabled ? sfxToggleOn : sfxToggleOff; }
    public TextureRegion minimapFrame() { return minimapFrame; }
    public TextureRegion footprints() { return footprints; }
    public TextureRegion returnRouteButton() { return returnButton; }
    public Texture fadePixel() { return fadePixel; }

    public AnimationSet playerAnimations() { return playerAnimations; }
    public MonsterAnimationSet monsterAnimations(MonsterType type) { return monsterAnimations.get(type); }
    public Map<MonsterType, MonsterAnimationSet> monsterAnimations() { return monsterAnimations; }

    @Override
    public void dispose() {
        for (Texture tex : atlasTextures.values()) tex.dispose();
        sideDoorTexture.dispose();
        headquartersAssets.dispose();
        roomKitAssets.dispose();
        laboratoryFurnitureAssets.dispose();
        lootAssets.dispose();
        difficultyModalAssets.dispose();
        hudAssets.dispose();
        exteriorWalls.dispose();
        structureV4.dispose();
        combatSounds.dispose();
        combatUi.dispose();
        hqSales.dispose();
        upgrade.dispose();
        skills.dispose();
        threeSkills.dispose();
        boss.dispose();
        bossNine.dispose();
        backgroundMusic.dispose();
        uiFont.dispose();
        worldFont.dispose();
        minimapFrameTex.dispose();
        footprintsTex.dispose();
        returnButtonTex.dispose();
        sfxToggleOnTex.dispose();
        sfxToggleOffTex.dispose();
        fadePixel.dispose();
        playerTexture.dispose();
        playerDeathTexture.dispose();
        for (Texture texture : monsterTextures.values()) texture.dispose();
    }
}
