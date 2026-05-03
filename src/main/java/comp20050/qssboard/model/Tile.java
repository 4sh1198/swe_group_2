package comp20050.qssboard.model;

public interface Tile {
    TileState getState();

    void setState(TileState s);
}
