package io.github.some_example_name.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.some_example_name.entity.monster.Monster;
import io.github.some_example_name.world.LaboratoryRoom;
import io.github.some_example_name.world.RoomPathfinder;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;

/** F-key route and compact fixed-screen minimap. */
public final class NavigationRenderer {
    private final ShapeRenderer shapes=new ShapeRenderer();
    private final OrthographicCamera hudCamera=new OrthographicCamera();
    private final Deque<float[]> footsteps=new ArrayDeque<>();
    private float stepAccumulator;
    public void updateFootsteps(float delta, boolean moving, float x, float y) {
        if (!moving) { stepAccumulator=0f; return; }
        stepAccumulator += delta;
        if (stepAccumulator >= .22f) {
            stepAccumulator %= .22f;
            footsteps.addLast(new float[]{x,y,2.2f});
            while(footsteps.size()>12) footsteps.removeFirst();
        }
        for(float[] mark:footsteps) mark[2]-=delta;
        while(!footsteps.isEmpty()&&footsteps.peekFirst()[2]<=0f) footsteps.removeFirst();
    }
    public void render(Camera worldCamera, LaboratoryRoom room, float px,float py,
                       float bodyWidth, float bodyHeight,
                       List<RoomPathfinder.Cell> path, List<Monster> monsters) {
        shapes.setProjectionMatrix(worldCamera.combined); shapes.begin(ShapeRenderer.ShapeType.Line);
        if(path!=null && path.size()>1) { shapes.setColor(Color.CYAN);
            for(int i=1;i<path.size();i++) shapes.line(path.get(i-1).x+.5f,path.get(i-1).y+.5f,path.get(i).x+.5f,path.get(i).y+.5f);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for(float[] mark:footsteps) {
            shapes.setColor(.7f,.75f,.7f,Math.min(.35f,mark[2]*.16f));
            shapes.circle(mark[0],mark[1],.055f,6);
        }
        shapes.end();
        int sw=Gdx.graphics.getWidth(), sh=Gdx.graphics.getHeight();
        hudCamera.setToOrtho(false,sw,sh); hudCamera.update(); shapes.setProjectionMatrix(hudCamera.combined);
        float scale=Math.min(4f,Math.min((sw*.22f)/room.widthInTiles(),(sh*.22f)/room.heightInTiles()));
        float x0=sw-12f-room.widthInTiles()*scale, y0=sh-12f-room.heightInTiles()*scale;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.015f,.02f,.03f,.78f); shapes.rect(x0-4,y0-4,room.widthInTiles()*scale+8,room.heightInTiles()*scale+8);
        for(int y=0;y<room.heightInTiles();y++) for(int x=0;x<room.widthInTiles();x++) {
            shapes.setColor(room.blocksActorAtCell(x,y,bodyWidth,bodyHeight)
                ?new Color(.55f,.25f,.22f,1f):new Color(.2f,.25f,.25f,1f));
            shapes.rect(x0+x*scale,y0+y*scale,scale,scale);
        }
        for(Monster m:monsters) if(m.isAlive()&&m.type()==io.github.some_example_name.combat.MonsterType.TEAM_LEADER) {
            shapes.setColor(Color.PURPLE); shapes.circle(x0+m.feetX()*scale,y0+m.feetY()*scale,Math.max(2f,scale*.65f),8);
        }
        shapes.setColor(Color.GREEN); shapes.circle(x0+px*scale,y0+py*scale,Math.max(2f,scale*.7f),8); shapes.end();
    }
    public void dispose(){shapes.dispose();}
}
