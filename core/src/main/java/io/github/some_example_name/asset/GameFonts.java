package io.github.some_example_name.asset;

import com.badlogic.gdx.Gdx;
import io.github.some_example_name.config.AssetPaths;
import java.awt.Font;
import java.io.InputStream;

/**
 * AWT 래스터 라벨(한글 토스트·모달·본부 라벨)용 Mona 픽셀 폰트 로더(OFL 1.1, Monad ABXY).
 * 한 번만 로드해 파생(deriveFont)으로 재사용한다. 로드 실패나 글리프 누락 시 기존
 * 시스템 한글 폰트 후보로 폴백하므로 어떤 환경에서도 라벨이 깨지지 않는다.
 *
 * LibGDX 쪽 숫자/영문 UI는 GameAssets가 같은 TTF로 FreeType BitmapFont를 생성한다.
 */
public final class GameFonts {

    private static Font monaBold;
    private static boolean loadAttempted;

    private GameFonts() { }

    /** text 전체를 그릴 수 있으면 Mona(Bold)를, 아니면 시스템 한글 폰트를 돌려준다. */
    public static synchronized Font awtBold(String text, int size) {
        ensureLoaded();
        if (monaBold != null && monaBold.canDisplayUpTo(text) == -1)
            return monaBold.deriveFont(Font.BOLD, (float) size);
        String[] candidates = {"Malgun Gothic", "맑은 고딕", "Gulim", "Dialog"};
        for (String family : candidates) {
            Font font = new Font(family, Font.BOLD, size);
            if (font.canDisplayUpTo(text) == -1) return font;
        }
        return new Font(Font.SANS_SERIF, Font.BOLD, size);
    }

    private static void ensureLoaded() {
        if (loadAttempted) return;
        loadAttempted = true;
        try {
            if (Gdx.files == null) return; // 헤드리스 테스트 등 — 폴백 사용
            InputStream in = Gdx.files.internal(AssetPaths.FONT_MONA_KR_BOLD).read();
            try {
                monaBold = Font.createFont(Font.TRUETYPE_FONT, in);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            if (Gdx.app != null) Gdx.app.log("GameFonts", "Mona font load failed: " + e);
            monaBold = null;
        }
    }
}
