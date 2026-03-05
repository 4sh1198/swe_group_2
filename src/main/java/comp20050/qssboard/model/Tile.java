package comp20050.qssboard.model;

public interface Tile {
    int getCol();
    int getRow();
    TileState getState();
    void setState(TileState s);
}
