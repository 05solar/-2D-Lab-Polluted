package io.github.some_example_name.screen;

import com.badlogic.gdx.ScreenAdapter;
import io.github.some_example_name.LaboratoryGame;
import io.github.some_example_name.asset.GameAssets;

/**
 * 모든 게임 화면의 공통 베이스. 게임 루트와 공용 자산 참조를 보관한다.
 * 화면은 생성/전환/입력 연결/업데이트·렌더 호출 순서만 관리하고, 게임 규칙은 시스템/도메인에 둔다.
 */
public abstract class BaseScreen extends ScreenAdapter {

    protected final LaboratoryGame game;
    protected final GameAssets assets;

    protected BaseScreen(LaboratoryGame game, GameAssets assets) {
        this.game = game;
        this.assets = assets;
    }
}
