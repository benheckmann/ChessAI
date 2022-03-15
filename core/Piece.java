package core;

public class Piece {

    public static final int None = 0;
    public static final int King = 1;
    public static final int Pawn = 2;
    public static final int Knight = 3;
    public static final int Bishop = 5;
    public static final int Rook = 6;
    public static final int Queen = 7;

    public static final int White = 8;
    public static final int Black = 16;

    final static int TYPE_MASK = 0b00111;
    final static int BLACK_MASK = 0b10000;
    final static int WHITE_MASK = 0b01000;
    final static int COLOUR_MASK = WHITE_MASK | BLACK_MASK;

    private static char[] whitePiecesUnicode = { ' ', '\u2654', '\u2659', '\u2658', '?', '\u2657', '\u2656', '\u2655' };
    private static char[] blackPiecesUnicode = { ' ', '\u265A', '\u265F', '\u265E', '?', '\u265D', '\u265C', '\u265B' };
    private static char[] whitePiecesAscii = { ' ', 'K', 'P', 'N', '?', 'B', 'R', 'Q' };
    private static char[] blackPiecesAscii = { ' ', 'k', 'p', 'n', '?', 'b', 'r', 'q' };

    public static boolean isColour (int piece, int colour) {
        return (piece & COLOUR_MASK) == colour;
    }

    public static int getColour(int piece) {
        return piece & COLOUR_MASK;
    }

    public static int getPieceType(int piece) {
        return piece & TYPE_MASK;
    }

    public static boolean isRookOrQueen(int piece) {
        return (piece & 0b110) == 0b110;
    }

    public static boolean isBishopOrQueen(int piece) {
        return (piece & 0b101) == 0b101;
    }

    public static boolean isSlidingPiece(int piece) {
        return (piece & 0b100) != 0;
    }

    public static char pieceToUnicode(int piece) {
        if (isColour(piece, White)) {
            return whitePiecesUnicode[getPieceType(piece)];
        } else {
            return blackPiecesUnicode[getPieceType(piece)];
        }
    }

    public static char pieceToAscii(int piece) {
        if (isColour(piece, White)) {
            return whitePiecesAscii[getPieceType(piece)];
        } else {
            return blackPiecesAscii[getPieceType(piece)];
        }
    }
}