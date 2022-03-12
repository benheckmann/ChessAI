package core;

import core.util.*;

public class BoardRepresentation {
    public static final String fileNames = "abcdefgh";
    public static final String rankNames = "12345678";

    public static final int a1 = 0;
    public static final int b1 = 1;
    public static final int c1 = 2;
    public static final int d1 = 3;
    public static final int e1 = 4;
    public static final int f1 = 5;
    public static final int g1 = 6;
    public static final int h1 = 7;

    public static final int a8 = 56;
    public static final int b8 = 57;
    public static final int c8 = 58;
    public static final int d8 = 59;
    public static final int e8 = 60;
    public static final int f8 = 61;
    public static final int g8 = 62;
    public static final int h8 = 63;

    // Rank (0 to 7) of square
    public static int RankIndex(int squareIndex) {
        return squareIndex >> 3;
    }

    // File (0 to 7) of square
    public static int FileIndex(int squareIndex) {
        return squareIndex & 0b000111;
    }

    // getter
    public static int IndexFromCoord(int fileIndex, int rankIndex) {
        return rankIndex * 8 + fileIndex;
    }

    public static int IndexFromCoord(Coord coord) {
        return IndexFromCoord(coord.fileIndex, coord.rankIndex);
    }

    public static int IndexFromSquareName(String squareName) {
        return IndexFromCoord(CoordFromSquareName(squareName));
    }

    public static Coord CoordFromIndex(int squareIndex) {
        return new Coord(FileIndex(squareIndex), RankIndex(squareIndex));
    }

    public static boolean LightSquare(int fileIndex, int rankIndex) {
        return (fileIndex + rankIndex) % 2 != 0;
    }

    public static String SquareNameFromCoordinate(int fileIndex, int rankIndex) {
        return "" + fileNames.charAt(fileIndex) + (rankIndex + 1);
    }

    public static Coord CoordFromSquareName(String squareName) {
        if (squareName.length() != 2 ||
                !fileNames.contains(squareName.substring(0, 1)) ||
                !rankNames.contains(squareName.substring(1))) {
            throw new IllegalArgumentException("Invalid square name: " + squareName);
        }
        int fileIndex = fileNames.indexOf(squareName.charAt(0));
        int rankIndex = Integer.parseInt(squareName.substring(1)) - 1;
        return new Coord(fileIndex, rankIndex);
    }

    public static String SquareNameFromIndex(int squareIndex) {
        return SquareNameFromCoordinate(CoordFromIndex(squareIndex));
    }

    public static String SquareNameFromCoordinate(Coord coord) {
        return SquareNameFromCoordinate(coord.fileIndex, coord.rankIndex);
    }
}
