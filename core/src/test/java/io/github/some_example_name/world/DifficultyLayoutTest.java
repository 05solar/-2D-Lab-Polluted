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
        assertEquals(26,easy.roomAreas().size()); // 기존 16 + 보조실 10(+62%)
        assertEquals(28,normal.room().widthInTiles());
        assertEquals(38,hard.room().widthInTiles());
        assertFalse(normal.room().isSolid(3,0)); assertFalse(hard.room().isSolid(3,0));
        assertFalse(easy.room().isSolid(easy.room().southExitCenterX(), 0));
        assertAllOpenReachable(easy.room()); assertAllOpenReachable(normal.room()); assertAllOpenReachable(hard.room());
        // EASY만 기준값×8(3/2/1/1 → 24/16/8/8). NORMAL/HARD는 이번 변경에서 그대로다.
        BalanceConfig c=new BalanceConfig();
        c.applyDifficulty(GameDifficulty.EASY);
        assertEquals(c.easySlimeBaseCount*c.easyMonsterMultiplier,c.slimeSpawnCount);
        assertEquals(c.easyResearcherBaseCount*c.easyMonsterMultiplier,c.researcherSpawnCount);
        assertEquals(c.easyGuardBaseCount*c.easyMonsterMultiplier,c.guardSpawnCount);
        assertEquals(c.easyTeamLeaderBaseCount*c.easyMonsterMultiplier,c.teamLeaderSpawnCount);
        assertEquals(24,c.slimeSpawnCount); assertEquals(16,c.researcherSpawnCount);
        assertEquals(8,c.guardSpawnCount); assertEquals(8,c.teamLeaderSpawnCount);
        // 재적용해도 이미 늘린 수에 다시 8이 곱해지지 않는다(기준값에서 재계산).
        c.applyDifficulty(GameDifficulty.EASY);
        assertEquals(24,c.slimeSpawnCount);
        c.applyDifficulty(GameDifficulty.NORMAL);
        assertEquals(5,c.slimeSpawnCount); assertEquals(3,c.researcherSpawnCount);
        assertEquals(2,c.guardSpawnCount); assertEquals(1,c.teamLeaderSpawnCount);
        c.applyDifficulty(GameDifficulty.HARD);
        assertEquals(7,c.slimeSpawnCount); assertEquals(5,c.researcherSpawnCount);
        assertEquals(3,c.guardSpawnCount); assertEquals(1,c.teamLeaderSpawnCount);
    }

    @Test public void spawnedMonsterCountsMatchConfigExactlyPerDifficulty() {
        // EASY: 실제 스폰 수가 종류별 기준×8과 정확히 일치(조용한 감소 금지).
        BalanceConfig easy=new BalanceConfig(); easy.applyDifficulty(GameDifficulty.EASY);
        assertSpawnCounts(LaboratoryLayout.forDifficulty(GameDifficulty.EASY,easy).room(),easy,24,16,8,8);
        BalanceConfig normal=new BalanceConfig(); normal.applyDifficulty(GameDifficulty.NORMAL);
        assertSpawnCounts(LaboratoryLayout.forDifficulty(GameDifficulty.NORMAL,normal).room(),normal,5,3,2,1);
        BalanceConfig hard=new BalanceConfig(); hard.applyDifficulty(GameDifficulty.HARD);
        assertSpawnCounts(LaboratoryLayout.forDifficulty(GameDifficulty.HARD,hard).room(),hard,7,5,3,1);
    }

    private static void assertSpawnCounts(LaboratoryRoom room,BalanceConfig config,
            int slimes,int researchers,int guards,int leaders) {
        java.util.List<io.github.some_example_name.entity.monster.Monster> monsters=
            new MonsterSpawnSystem(config).spawn(room,20261015L);
        int s=0,r=0,g=0,l=0;
        for(io.github.some_example_name.entity.monster.Monster m:monsters) {
            switch(m.type()) {
                case SLIME: s++; break;
                case RESEARCHER: r++; break;
                case GUARD: g++; break;
                default: l++; break;
            }
            // 문/개구부 위·옆 생성 금지, 좌표 중첩 금지(최소 간격).
            assertFalse("spawned off doors",nearDoor(room,m.feetX(),m.feetY()));
        }
        assertEquals("slime count",slimes,s);
        assertEquals("researcher count",researchers,r);
        assertEquals("guard count",guards,g);
        assertEquals("team leader count",leaders,l);
        for(int i=0;i<monsters.size();i++)for(int j=i+1;j<monsters.size();j++) {
            float dx=monsters.get(i).feetX()-monsters.get(j).feetX();
            float dy=monsters.get(i).feetY()-monsters.get(j).feetY();
            assertTrue("monsters are not stacked",dx*dx+dy*dy>=config.monsterSpacing*config.monsterSpacing-.001f);
        }
    }

    private static boolean nearDoor(LaboratoryRoom room,float x,float y) {
        int tx=(int)x,ty=(int)y;
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
            TileType t=room.rawTileAt(tx+dx,ty+dy);
            if(t==TileType.DOORWAY_OPEN||t==TileType.DOOR_CLOSED)return true;
        }
        return false;
    }

    @Test public void teamLeaderUsesSafeOuterCornerCandidate() {
        LaboratoryRoom room=LaboratoryLayout.easyRoom().room();
        BalanceConfig config=new BalanceConfig(); config.applyDifficulty(GameDifficulty.EASY);
        java.util.List<io.github.some_example_name.entity.monster.Monster> monsters=
            new MonsterSpawnSystem(config).spawn(room,12345L);
        java.util.List<io.github.some_example_name.entity.monster.Monster> leaders=new java.util.ArrayList<>();
        for(io.github.some_example_name.entity.monster.Monster m:monsters)
            if(m.type()==MonsterType.TEAM_LEADER) leaders.add(m);
        assertEquals("EASY 8배에서도 팀장 수는 기준×8",8,leaders.size());
        int minX=room.widthInTiles(),minY=room.heightInTiles(),maxX=0,maxY=0;
        for(int y=0;y<room.heightInTiles();y++){int open=0;for(int x=0;x<room.widthInTiles();x++)if(!room.isSolid(x,y))open++;if(open>8){minY=Math.min(minY,y);maxY=Math.max(maxY,y);}}
        for(int x=0;x<room.widthInTiles();x++){int open=0;for(int y=0;y<room.heightInTiles();y++)if(!room.isSolid(x,y))open++;if(open>8){minX=Math.min(minX,x);maxX=Math.max(maxX,x);}}
        float left=minX+1.5f,bottom=minY+1.5f,right=maxX-1.5f,top=maxY-1.5f;
        // 압축 캠퍼스(.55 간격)에선 통행 가능 bbox 모서리가 비플레이 검은 영역에 걸려,
        // 3x3 안전 스폰이 가능한 가장 가까운 방 셀이 모서리에서 ~12타일 안쪽이다.
        // 8배에서도 팀장 후보는 모서리 거리순이므로 모두 구석 방 영역에 머문다(중앙까지는 ~45).
        for(io.github.some_example_name.entity.monster.Monster leader:leaders) {
            float nearest=Math.min(Math.min(distance(leader.feetX(),leader.feetY(),left,bottom),
                distance(leader.feetX(),leader.feetY(),right,bottom)),
                Math.min(distance(leader.feetX(),leader.feetY(),left,top),
                    distance(leader.feetX(),leader.feetY(),right,top)));
            assertTrue("leader near a room corner (was "+nearest+")",nearest<16f);
        }
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
