package io.github.some_example_name.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.some_example_name.inventory.LootItem;
import io.github.some_example_name.inventory.LootTableCatalog;
import io.github.some_example_name.world.LootContainer;
import java.util.HashMap;
import java.util.Map;

/** Owns the supplied chest animation sheets and rarity item atlases. */
public final class LootAssets implements Disposable {
    private static final class Clip { Texture texture; TextureRegion[] frames; float duration; int rewardFrame;
        float opaqueTopRatio; }
    private final Map<String,Clip> containers=new HashMap<>();
    private final Map<String,TextureRegion[][]> items=new HashMap<>();
    private final Map<String,Texture> textures=new HashMap<>();
    private final LootTableCatalog catalog;
    private final Map<String,Integer> prices=new HashMap<>();
    public LootAssets(String root,String tableJson) {
        catalog=new LootTableCatalog(tableJson);
        for(JsonValue container:new JsonReader().parse(tableJson).get("containers"))
            for(JsonValue item:container.get("items")) prices.put(item.getString("id"),item.getInt("price"));
        JsonValue tables=new JsonReader().parse(tableJson);
        for(JsonValue c=tables.get("containers").child;c!=null;c=c.next) {
            String id=c.getString("id");
            JsonValue anim=new JsonReader().parse(com.badlogic.gdx.Gdx.files.internal(root+c.getString("animation")).readString("UTF-8"));
            String sheetPath=root+"containers/"+id+"/"+anim.getString("image");
            // SPACE 안내를 "실제 불투명 그림 상단" 기준으로 띄우기 위해 로드 시 한 번만
            // 닫힘 프레임(첫 프레임)의 알파 상단을 측정한다(프레임마다 다시 읽지 않는다).
            com.badlogic.gdx.graphics.Pixmap sheet=new com.badlogic.gdx.graphics.Pixmap(
                com.badlogic.gdx.Gdx.files.internal(sheetPath));
            Texture texture=fromPixmap(sheetPath,sheet);
            TextureRegion[][] grid=TextureRegion.split(texture,anim.getInt("frameWidth"),anim.getInt("frameHeight"));
            JsonValue settings=anim.get("animation"); int[] indices=settings.get("frames").asIntArray();
            Clip clip=new Clip(); clip.texture=texture; clip.duration=settings.getFloat("frameDuration"); clip.frames=new TextureRegion[indices.length];
            clip.rewardFrame=settings.getInt("rewardFrame",indices.length-1);
            for(int i=0;i<indices.length;i++) clip.frames[i]=grid[indices[i]/anim.getInt("columns")][indices[i]%anim.getInt("columns")];
            clip.opaqueTopRatio=measureOpaqueTopRatio(sheet,anim.getInt("frameWidth"),
                anim.getInt("frameHeight"),anim.getInt("columns"),indices[0]);
            sheet.dispose();
            containers.put(id,clip);
            String atlasPath=root+c.getString("itemAtlas");
            String rarity=c.getString("rarity");
            if(!items.containsKey(rarity)) {
                Texture atlas=load(atlasPath); items.put(rarity,TextureRegion.split(atlas,64,64));
            }
        }
    }

    /** 닫힘 프레임의 불투명(알파>8) 최상단까지의 높이 비율(하단 앵커 기준 0..1). */
    private static float measureOpaqueTopRatio(com.badlogic.gdx.graphics.Pixmap sheet,
            int frameW,int frameH,int columns,int frameIndex) {
        int x0=frameIndex%columns*frameW, y0=frameIndex/columns*frameH; // pixmap y=0은 위
        for(int y=0;y<frameH;y++)
            for(int x=0;x<frameW;x++)
                if((sheet.getPixel(x0+x,y0+y)&0xff)>8)
                    return (frameH-y)/(float)frameH;
        return 1f; // 완전 투명 프레임이면 전체 캔버스를 기준으로 둔다
    }

    private Texture fromPixmap(String path,com.badlogic.gdx.graphics.Pixmap pixmap) {
        Texture t=new Texture(pixmap);
        t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        t.setWrap(Texture.TextureWrap.ClampToEdge,Texture.TextureWrap.ClampToEdge);
        textures.put(path,t);
        return t;
    }
    private Texture load(String path) { Texture t=new Texture(com.badlogic.gdx.Gdx.files.internal(path)); t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest); t.setWrap(Texture.TextureWrap.ClampToEdge,Texture.TextureWrap.ClampToEdge); textures.put(path,t); return t; }
    public LootTableCatalog catalog(){return catalog;}
    public Map<String,Integer> prices(){return java.util.Collections.unmodifiableMap(prices);}
    public TextureRegion chest(String id,int frame){Clip c=containers.get(id); return c.frames[Math.max(0,Math.min(frame,c.frames.length-1))];}
    public float frameDuration(String id){return containers.get(id).duration;}
    /** 닫힘 프레임의 실제 불투명 상단 비율(하단 앵커 기준 0..1) — SPACE 안내 기준점. */
    public float opaqueTopRatio(String id){return containers.get(id).opaqueTopRatio;}
    /** Base, unframed art for a loose floor item. */
    public TextureRegion itemBase(LootItem item){return items.get(item.rarity)[0][item.atlasColumn];}
    /** Item icon with the rarity-colored frame for inventory slots. */
    public TextureRegion itemFramed(LootItem item){return items.get(item.rarity)[1][item.atlasColumn];}
    /** Backwards-compatible framed item accessor. */
    public TextureRegion item(LootItem item){return itemFramed(item);}
    public LootContainer newContainer(String id,float x,float y) {
        return newContainer(id,x,y,1);
    }
    /** requiredPresses: 개봉에 필요한 SPACE 입력 횟수(BalanceConfig.requiredPresses). */
    public LootContainer newContainer(String id,float x,float y,int requiredPresses) {
        Clip clip=containers.get(id); int[] sequence=new int[clip.frames.length];
        for(int i=0;i<sequence.length;i++) sequence[i]=i;
        return new LootContainer(id,x,y,clip.duration,sequence,clip.rewardFrame,requiredPresses);
    }
    @Override public void dispose(){for(Texture t:textures.values())t.dispose();}
}
