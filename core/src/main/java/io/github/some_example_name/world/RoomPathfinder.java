package io.github.some_example_name.world;

import com.badlogic.gdx.math.Rectangle;
import io.github.some_example_name.collision.ObstacleGrid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

/** Shortest four-direction tile path over currently passable room cells. */
public final class RoomPathfinder {
    public static final class Cell {
        public final int x, y;
        Cell(int x, int y) { this.x=x; this.y=y; }
    }
    public List<Cell> shortestPath(LaboratoryRoom room, int sx, int sy, int tx, int ty) {
        return shortestPath(room, sx, sy, tx, ty, .6f, .5f);
    }
    public List<Cell> shortestPath(LaboratoryRoom room, int sx, int sy, int tx, int ty,
                                   float bodyWidth, float bodyHeight) {
        return shortestPath((ObstacleGrid) room, sx, sy, tx, ty, bodyWidth, bodyHeight);
    }
    /**
     * 시작/목표 셀이 정확히 비어 있지 않아도 주변(자신+8방향)에서 가장 가까운 통행 가능
     * 셀로 보정해 경로를 찾는다. 플레이어가 가구·상자에 붙어 서 있거나(시작 셀 중심이
     * footprint와 겹침) 대문 앞 셀이 일시적으로 점유돼도 귀환 발자국이 끊기지 않게 한다.
     */
    public List<Cell> shortestPathNear(ObstacleGrid grid, int sx, int sy, int tx, int ty,
                                       float bodyWidth, float bodyHeight) {
        int w = grid.widthInTiles(), h = grid.heightInTiles();
        List<Rectangle> obstacles = new ArrayList<>();
        for (Rectangle obstacle : grid.obstacles()) obstacles.add(obstacle);
        // 가까운 순서(자기 자신 → 반경 1 → 반경 2)로 통행 가능한 보정 셀을 찾는다.
        // 반경 2까지 보는 이유: 가구 사이 좁은 포켓에 서 있으면 셀 중심 기준 스윕 판정이
        // 인접 셀까지 모두 막힌 것으로 볼 수 있어, 한 칸 보정만으로는 부족할 수 있다.
        List<int[]> offsets = new ArrayList<>();
        for (int r = 0; r <= 2; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++)
                    if (Math.max(Math.abs(dx), Math.abs(dy)) == r) offsets.add(new int[]{dx, dy});
        int startX = sx, startY = sy, targetX = tx, targetY = ty;
        for (int[] o : offsets) {
            int x = sx + o[0], y = sy + o[1];
            if (inside(x, y, w, h) && !blockedAtCell(grid, obstacles, x, y, bodyWidth, bodyHeight)) {
                startX = x; startY = y; break;
            }
        }
        for (int[] o : offsets) {
            int x = tx + o[0], y = ty + o[1];
            if (inside(x, y, w, h) && !blockedAtCell(grid, obstacles, x, y, bodyWidth, bodyHeight)) {
                targetX = x; targetY = y; break;
            }
        }
        return shortestPath(grid, startX, startY, targetX, targetY, bodyWidth, bodyHeight);
    }

    public List<Cell> shortestPath(ObstacleGrid grid, int sx, int sy, int tx, int ty,
                                   float bodyWidth, float bodyHeight) {
        int w=grid.widthInTiles(), h=grid.heightInTiles();
        List<Rectangle> obstacles=new ArrayList<>();
        for(Rectangle obstacle:grid.obstacles()) obstacles.add(obstacle);
        if (!inside(sx,sy,w,h) || !inside(tx,ty,w,h)
                || blockedAtCell(grid,obstacles,sx,sy,bodyWidth,bodyHeight)
                || blockedAtCell(grid,obstacles,tx,ty,bodyWidth,bodyHeight)) return Collections.emptyList();
        int[][] px=new int[h][w], py=new int[h][w]; boolean[][] seen=new boolean[h][w];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) px[y][x]=-1;
        Deque<int[]> q=new ArrayDeque<>(); q.add(new int[]{sx,sy}); seen[sy][sx]=true;
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
        while(!q.isEmpty() && !seen[ty][tx]) {
            int[] c=q.removeFirst();
            for(int[] d:dirs) { int x=c[0]+d[0], y=c[1]+d[1];
                if(x>=0&&y>=0&&x<w&&y<h&&!seen[y][x]
                        &&!blockedAtCell(grid,obstacles,x,y,bodyWidth,bodyHeight)
                        &&!blockedAlongPath(grid,obstacles,c[0]+.5f,c[1]+.5f,x+.5f,y+.5f,bodyWidth,bodyHeight)) {
                    seen[y][x]=true; px[y][x]=c[0]; py[y][x]=c[1]; q.addLast(new int[]{x,y});
                }
            }
        }
        if(!seen[ty][tx]) return Collections.emptyList();
        List<Cell> path=new ArrayList<>(); int x=tx,y=ty;
        while(x!=sx||y!=sy) { path.add(new Cell(x,y)); int nx=px[y][x],ny=py[y][x]; x=nx;y=ny; }
        path.add(new Cell(sx,sy)); Collections.reverse(path); return path;
    }
    private static boolean blockedAtCell(ObstacleGrid grid,List<Rectangle> obstacles,int tx,int ty,float bw,float bh) {
        Rectangle body=new Rectangle(tx+.5f-bw*.5f,ty+.5f,bw,bh);
        int minX=(int)Math.floor(body.x+.0001f),minY=(int)Math.floor(body.y+.0001f);
        int maxX=(int)Math.floor(body.x+body.width-.0001f),maxY=(int)Math.floor(body.y+body.height-.0001f);
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++)if(grid.isSolid(x,y))return true;
        for(Rectangle obstacle:obstacles)if(body.overlaps(obstacle))return true;
        return false;
    }
    private static boolean blockedAlongPath(ObstacleGrid grid,List<Rectangle> obstacles,float fromX,float fromY,float toX,float toY,
                                             float bw,float bh) {
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(toX-fromX),Math.abs(toY-fromY))/.08f));
        for(int i=1;i<=steps;i++) {
            float t=i/(float)steps;
            if(blockedAt(grid,obstacles,fromX+(toX-fromX)*t,fromY+(toY-fromY)*t,bw,bh))return true;
        }
        return false;
    }
    private static boolean blockedAt(ObstacleGrid grid,List<Rectangle> obstacles,float feetX,float feetY,float bw,float bh) {
        Rectangle body=new Rectangle(feetX-bw*.5f,feetY,bw,bh);
        int minX=(int)Math.floor(body.x+.0001f),minY=(int)Math.floor(body.y+.0001f);
        int maxX=(int)Math.floor(body.x+body.width-.0001f),maxY=(int)Math.floor(body.y+body.height-.0001f);
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++)if(grid.isSolid(x,y))return true;
        for(Rectangle obstacle:obstacles)if(body.overlaps(obstacle))return true;
        return false;
    }
    private static boolean inside(int x,int y,int w,int h){return x>=0&&y>=0&&x<w&&y<h;}
}
