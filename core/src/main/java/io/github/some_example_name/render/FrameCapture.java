package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * 개발용 프레임버퍼 → PNG 저장(실제 GL 렌더 결과 검증). 합성 미리보기가 아니라 실행 중 화면을 그대로 저장한다.
 * 렌더 스레드에서 현재 백버퍼 픽셀을 읽어 상하 반전 후 PNG로 쓰고 절대 경로를 로그로 출력한다.
 *
 * 저장 위치: 작업 디렉터리(assets)의 상위(프로젝트 루트)/docs/images/frames. 파일명에 호출 카운터를 붙여 덮어쓰지 않는다.
 */
public final class FrameCapture {

    private static int counter = 0;

    private FrameCapture() { }

    /** 현재 프레임을 label 이름으로 저장하고 저장 경로를 반환(실패 시 null). */
    public static String capture(String label) {
        int w = Gdx.graphics.getBackBufferWidth();
        int h = Gdx.graphics.getBackBufferHeight();
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true); // 상하 반전 포함
        // 알파를 255로 강제(일부 블렌딩/폰트가 알파를 낮춰 PNG가 투명해지는 것 방지).
        for (int i = 3; i < pixels.length; i += 4) pixels[i] = (byte) 0xFF;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        ByteBuffer buffer = pixmap.getPixels();
        buffer.clear();
        BufferUtils.copy(pixels, 0, buffer, pixels.length);
        try {
            File dir = resolveOutputDir();
            dir.mkdirs();
            File out = new File(dir, String.format("frame_%03d_%s.png", ++counter, sanitize(label)));
            PixmapIO.writePNG(new FileHandle(out), pixmap);
            String path = out.getAbsolutePath();
            Gdx.app.log("FrameCapture", "saved " + path + " (" + w + "x" + h + ")");
            return path;
        } catch (Exception e) {
            Gdx.app.error("FrameCapture", "failed to save frame", e);
            return null;
        } finally {
            pixmap.dispose();
        }
    }

    private static File resolveOutputDir() {
        // 작업 디렉터리는 rootProject/assets. 그 상위가 프로젝트 루트.
        File working = new File(System.getProperty("user.dir"));
        File root = working.getName().equals("assets") && working.getParentFile() != null
            ? working.getParentFile() : working;
        return new File(new File(new File(root, "docs"), "images"), "frames");
    }

    private static String sanitize(String label) {
        return label == null ? "frame" : label.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
