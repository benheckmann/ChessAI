package core.util;

public class BitBoardUtility {
    public static boolean ContainsSquare (long bitboard, int square) {
        return ((bitboard >> square) & 1) != 0;
    }
}