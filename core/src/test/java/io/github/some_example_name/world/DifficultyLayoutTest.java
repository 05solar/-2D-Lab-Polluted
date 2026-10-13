package io.github.some_example_name.world;

import io.github.some_example_name.config.BalanceConfig;
import io.github.some_example_name.combat.MonsterSpawnSystem;
import io.github.some_example_name.combat.MonsterType;
import org.junit.Test;
import static org.junit.Assert.*;

public class DifficultyLayoutTest {
    @Test public void difficultyMapsHaveDifferentLayoutsAndSpawnCounts() {
        LaboratoryLayout easy=LaboratoryLayout.forDifficulty(GameDifficulty.EASY);
        LaboratoryLayout normal=LaboratoryLayout.forDifficulty(GameDifficulty.NORMAL);
        LaboratoryLayout hard=LaboratoryLayout.forDifficulty(GameDifficulty.HARD);
        assertEquals(128,easy.room().widthInTiles());
        assertEquals(96,easy.room().heightInTiles());
        assertEquals(16,easy.roomAreas().size());
        assertEquals(28,normal.room().widthInTiles());
        assertEquals(38,hard.room().widthInTiles());
        assertFalse(normal.room().isSolid(3,0)); assertFalse(hard.room().isSolid(3,0));
        assertFalse(easy.room().isSolid(easy.room().southExitCenterX(), 0));
        assertAllOpenReachable(easy.room()); assertAllOpenReachable(normal.room()); assertAllOpenReachable(hard.room());
        BalanceConfig c=new BalanceConfig(); c.applyDifficulty(GameDifficulty.EASY); int e=c.slimeSpawnCount;
        c.applyDifficulty(GameDifficulty.NORMAL); int n=c.slimeSpawnCount;
        c.applyDifficulty(GameDifficulty.HARD); int h=c.slimeSpawnCount;
        assertTrue(e<n&&n<h);
    }

    @Test public void teamLeaderUsesSafeOuterCornerCandidate() {
        LaboratoryRoom room=LaboratoryLayout.easyRoom().room();
        java.util.List<io.github.some_example_name.entity.monster.Monster> monsters=
            new MonsterSpawnSystem(new BalanceConfig()).spawn(room,12345L);
        io.github.some_example_name.entity.monster.Monster leader=null;
        for(io.github.some_example_name.entity.monster.Monster m:monsters)
            if(m.type()==MonsterType.TEAM_LEADER) leader=m;
        assertNotNull(leader);
        int minX=room.widthInTiles(),minY=room.heightInTiles(),maxX=0,maxY=0;
        for(int y=0;y<room.heightInTiles();y++){int open=0;for(int x=0;x<room.widthInTiles();x++)if(!room.isSolid(x,y))open++;if(open>8){minY=Math.min(minY,y);maxY=Math.max(maxY,y);}}
        for(int x=0;x<room.widthInTiles();x++){int open=0;for(int y=0;y<room.heightInTiles();y++)if(!room.isSolid(x,y))open++;if(open>8){minX=Math.min(minX,x);maxX=Math.max(maxX,x);}}
        float left=minX+1.5f,bottom=minY+1.5f,right=maxX-1.5f,top=maxY-1.5f;
        float nearest=Math.min(Math.min(distance(leader.feetX(),leader.feetY(),left,bottom),
            distance(leader.feetX(),leader.feetY(),right,bottom)),
            Math.min(distance(leader.feetX(),leader.feetY(),left,top),
                distance(leader.feetX(),leader.feetY(),right,top)));
        assertTrue("leader near a room corner",nearest<5.5f);
    }

    private static float distance(float x,float y,float a,float b){float dx=x-a,dy=y-b;return (float)Math.sqrt(dx*dx+dy*dy);}

    private static void assertAllOpenReachable(LaboratoryRoom room) {
        boolean[][] seen=new boolean[room.heightInTiles()][room.widthInTiles()];
        java.util.ArrayDeque<int[]> q=new java.util.ArrayDeque<>();
        int sx=(int)room.spawnPoint().x,sy=(int)room.spawnPoint().y; seen[sy][sx]=true;q.add(new int[]{sx,sy});
        int[][] d={{1,0},{-1,0},{0,1},{0,-1}};
        while(!q.isEmpty()){int[] c=q.remove();for(int[] v:d){int x=c[0]+v[0],y=c[1]+v[1];
            if(x>=0&&y>=0&&x<room.widthInTiles()&&y<room.heightInTiles()&&!seen[y][x]&&!room.isSolid(x,y)){seen[y][x]=true;q.add(new int[]{x,y});}
        }}
        for(int y=0;y<room.heightInTiles();y++)for(int x=0;x<room.widthInTiles();x++)
            if(!room.isSolid(x,y))assertTrue("unreachable open tile "+x+","+y+" in "+room.widthInTiles(),seen[y][x]);
    }
}
