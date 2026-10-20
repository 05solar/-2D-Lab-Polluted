package io.github.some_example_name.profile;

import java.io.IOException;

/** 프로필 영속화 추상화. 모델/화면은 파일 경로를 모른 채 이 인터페이스만 쓴다. */
public interface ProfileStore {
    /** 저장된 프로필, 없으면 null(첫 실행 — 호출 측이 기본 프로필을 만든다). */
    PlayerProfile load() throws IOException;

    /** 전체 프로필을 원자적으로 저장한다. 실패 시 기존 저장본이 보존되어야 한다. */
    void save(PlayerProfile profile) throws IOException;
}
