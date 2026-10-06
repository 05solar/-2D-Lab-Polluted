package io.github.some_example_name;

import com.badlogic.gdx.Game;
import io.github.some_example_name.asset.GameAssets;
import io.github.some_example_name.screen.LaboratoryScreen;

/**
 * 게임 루트. 화면 전환과 전역 생명주기 자원(GameAssets)의 소유를 담당한다.
 * 게임 규칙(전투, AI, 보물 등)은 이 클래스에 두지 않는다.
 */
public class LaboratoryGame extends Game {

    private GameAssets assets;

    @Override
    public void create() {
        assets = new GameAssets();
        // 1단계: 바로 연구소 탐사 화면으로 진입한다.
        // (LoadingScreen / HeadquartersScreen 은 이후 단계에서 도입)
        setScreen(new LaboratoryScreen(this, assets));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
    }
}
