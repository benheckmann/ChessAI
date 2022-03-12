package core.util;

public class Coord implements Comparable<Coord> {
    public final int fileIndex;
    public final int rankIndex;

    public Coord (int fileIndex, int rankIndex) {
        this.fileIndex = fileIndex;
        this.rankIndex = rankIndex;
    }

    // getter
    public boolean IsLightSquare () {
        return (fileIndex + rankIndex) % 2 != 0;
    }

    @Override
    public int compareTo (Coord other) {
        return (fileIndex == other.fileIndex && rankIndex == other.rankIndex) ? 0 : 1;
    }
}