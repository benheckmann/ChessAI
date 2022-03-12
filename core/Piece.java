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

    final static int typeMask = 0b00111;
    final static int blackMask = 0b10000;
    final static int whiteMask = 0b01000;
    final static int colourMask = whiteMask | blackMask;

    private static char[] whitePiecesUnicode = { ' ', '\u2654', '\u2659', '\u2658', '?', '\u2657', '\u2656', '\u2655' };
    private static char[] blackPiecesUnicode = { ' ', '\u265A', '\u265F', '\u265E', '?', '\u265D', '\u265C', '\u265B' };
    private static char[] whitePiecesAscii = { ' ', 'K', 'P', 'N', '?', 'B', 'R', 'Q' };
    private static char[] blackPiecesAscii = { ' ', 'k', 'p', 'n', '?', 'b', 'r', 'q' };

    public static boolean IsColour (int piece, int colour) {
        return (piece & colourMask) == colour;
    }

    public static int Colour (int piece) {
        return piece & colourMask;
    }

    public static int PieceType (int piece) {
        return piece & typeMask;
    }

    public static boolean IsRookOrQueen (int piece) {
        return (piece & 0b110) == 0b110;
    }

    public static boolean IsBishopOrQueen (int piece) {
        return (piece & 0b101) == 0b101;
    }

    public static boolean IsSlidingPiece (int piece) {
        return (piece & 0b100) != 0;
    }

    public static char pieceToUnicode(int piece) {
        if (IsColour(piece, White)) {
            return whitePiecesUnicode[PieceType(piece)];
        } else {
            return blackPiecesUnicode[PieceType(piece)];
        }
    }

    public static char pieceToAscii(int piece) {
        if (IsColour(piece, White)) {
            return whitePiecesAscii[PieceType(piece)];
        } else {
            return blackPiecesAscii[PieceType(piece)];
        }
    }
}