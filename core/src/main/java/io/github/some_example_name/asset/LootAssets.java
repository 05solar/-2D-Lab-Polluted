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
    private static final class Clip { Texture texture; TextureRegion[] frames; float duration; int rewardFrame; }
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
            Texture texture=load(root+"containers/"+id+"/"+anim.getString("image"));
            TextureRegion[][] grid=TextureRegion.split(texture,anim.getInt("frameWidth"),anim.getInt("frameHeight"));
            JsonValue settings=anim.get("animation"); int[] indices=settings.get("frames").asIntArray();
            Clip clip=new Clip(); clip.texture=texture; clip.duration=settings.getFloat("frameDuration"); clip.frames=new TextureRegion[indices.length];
            clip.rewardFrame=settings.getInt("rewardFrame",indices.length-1);
            for(int i=0;i<indices.length;i++) clip.frames[i]=grid[indices[i]/anim.getInt("columns")][indices[i]%anim.getInt("columns")];
            containers.put(id,clip);
            String atlasPath=root+c.getString("itemAtlas");
            String rarity=c.getString("rarity");
            if(!items.containsKey(rarity)) {
                Texture atlas=load(atlasPath); items.put(rarity,TextureRegion.split(atlas,64,64));
            }
        }
    }
    private Texture load(String path) { Texture t=new Texture(com.badlogic.gdx.Gdx.files.internal(path)); t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest); t.setWrap(Texture.TextureWrap.ClampToEdge,Texture.TextureWrap.ClampToEdge); textures.put(path,t); return t; }
    public LootTableCatalog catalog(){return catalog;}
    public Map<String,Integer> prices(){return java.util.Collections.unmodifiableMap(prices);}
    public TextureRegion chest(String id,int frame){Clip c=containers.get(id); return c.frames[Math.max(0,Math.min(frame,c.frames.length-1))];}
    public float frameDuration(String id){return containers.get(id).duration;}
    /** Base, unframed art for a loose floor item. */
    public TextureRegion itemBase(LootItem item){return items.get(item.rarity)[0][item.atlasColumn];}
    /** Item icon with the rarity-colored frame for inventory slots. */
    public TextureRegion itemFramed(LootItem item){return items.get(item.rarity)[1][item.atlasColumn];}
    /** Backwards-compatible framed item accessor. */
    public TextureRegion item(LootItem item){return itemFramed(item);}
    public LootContainer newContainer(String id,float x,float y) {
        Clip clip=containers.get(id); int[] sequence=new int[clip.frames.length];
        for(int i=0;i<sequence.length;i++) sequence[i]=i;
        return new LootContainer(id,x,y,clip.duration,sequence,clip.rewardFrame);
    }
    @Override public void dispose(){for(Texture t:textures.values())t.dispose();}
}
