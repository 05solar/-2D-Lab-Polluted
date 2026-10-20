package io.github.some_example_name.profile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

/**
 * 파일 기반 프로필 저장(앱 전용 쓰기 가능 경로 — assets/클래스패스/소스 폴더 금지).
 *
 * 쓰기: 임시 파일에 완전한 JSON 작성 → flush + fsync → 기존 주 저장본을 백업으로 보관 →
 * 원자 교체(ATOMIC_MOVE, 미지원 시 REPLACE_EXISTING 폴백). 어느 시점에 실패해도
 * 주 저장/백업 중 적어도 하나의 정상본이 남는다.
 *
 * 읽기: 주 저장 손상 시 백업으로 복구한다. 둘 다 손상이면 손상본을 *.corrupt-<millis>로
 * 보존한 뒤 null을 돌려(명시적 복구 흐름) 호출 측이 새 프로필을 만든다 — 손상 파일을
 * 조용히 덮어쓰지 않는다.
 */
public final class FileProfileStore implements ProfileStore {

    private static final Logger LOG = Logger.getLogger(FileProfileStore.class.getName());
    private final File directory;
    private final File main, backup, temp;
    private boolean recoveredFromBackup;
    private boolean corruptionDetected;

    public FileProfileStore(File directory) {
        if (directory == null) throw new IllegalArgumentException("directory");
        this.directory = directory;
        main = new File(directory, "profile.json");
        backup = new File(directory, "profile.backup.json");
        temp = new File(directory, "profile.tmp.json");
    }

    @Override public synchronized PlayerProfile load() throws IOException {
        if (!main.exists() && !backup.exists()) return null; // 첫 실행
        PlayerProfile fromMain = tryRead(main);
        if (fromMain != null) return fromMain;
        PlayerProfile fromBackup = tryRead(backup);
        if (fromBackup != null) {
            recoveredFromBackup = true;
            LOG.warning("profile.json damaged - recovered from backup");
            return fromBackup;
        }
        // 둘 다 손상: 원본을 보존하고 명시적으로 새 프로필 흐름으로 넘긴다.
        corruptionDetected = true;
        long stamp = System.currentTimeMillis();
        if (main.exists()) quarantine(main, stamp);
        if (backup.exists()) quarantine(backup, stamp);
        LOG.severe("profile and backup both damaged - preserved as *.corrupt-" + stamp);
        return null;
    }

    @Override public synchronized void save(PlayerProfile profile) throws IOException {
        if (profile == null) throw new IllegalArgumentException("profile");
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("cannot create profile directory: " + directory);
        String json = ProfileCodec.toJson(profile);
        // 1) 임시 파일에 완전한 본문 작성 + fsync
        FileOutputStream out = new FileOutputStream(temp);
        try {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            writer.write(json);
            writer.flush();
            out.getFD().sync();
            writer.close();
        } finally {
            try { out.close(); } catch (IOException ignored) { }
        }
        // 2) 기존 주 저장본을 백업으로 보관(마지막 정상본 유지)
        if (main.exists())
            Files.copy(main.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        // 3) 원자 교체(미지원 플랫폼은 검증된 REPLACE_EXISTING 폴백 — 백업이 보호한다)
        try {
            Files.move(temp.toPath(), main.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temp.toPath(), main.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static PlayerProfile tryRead(File file) {
        if (!file.exists()) return null;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return ProfileCodec.fromJson(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception damaged) {
            return null;
        }
    }

    private static void quarantine(File file, long stamp) {
        File preserved = new File(file.getParentFile(), file.getName() + ".corrupt-" + stamp);
        if (!file.renameTo(preserved))
            LOG.severe("could not preserve damaged save: " + file);
    }

    public boolean recoveredFromBackup() { return recoveredFromBackup; }
    public boolean corruptionDetected() { return corruptionDetected; }
    public File directory() { return directory; }
}
