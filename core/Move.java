package core;

import java.util.List;

public class Move {

    public static class Flag {
        public static final int None = 0;
        public static final int EnPassantCapture = 1;
        public static final int Castling = 2;
        public static final int PromoteToQueen = 3;
        public static final int PromoteToKnight = 4;
        public static final int PromoteToRook = 5;
        public static final int PromoteToBishop = 6;
        public static final int PawnTwoForward = 7;
    }

    // Bits 0-5 store the index of the start square
    // Bits 6-11 store the index of the target square
    // Bits 12-15 store the flag
    static final short startSquareMask = (short) 0b0000000000111111;
    static final short targetSquareMask = (short) 0b0000111111000000;
    static final short flagMask = (short) 0b0111000000000000;

    public final short moveValue;

    public Move(short moveValue) {
        this.moveValue = moveValue;
    }

    public Move(int startSquare, int targetSquare) {
        moveValue = (short) (startSquare | targetSquare << 6);
    }

    public Move(int startSquare, int targetSquare, int flag) {
        moveValue = (short) (startSquare | targetSquare << 6 | flag << 12);
    }

    /**
     * Tries to generate a move from an input string in long algebraic notation and
     * the current board. (e.g. e2e4, e7e5, e1g1 (white short castling), e7e8q (for
     * promotion))
     */
    public Move(Board board, String lan) {
        lan = lan.toLowerCase();
        short lanMoveValue = parseMove(board, lan);
        moveValue = lanMoveValue;
    }

    /**
     * Tries to generate a move from an input string in long algebraic notation and
     * a board. Will throw an exception if the move is not legal.
     */
    public static short parseMove(Board board, String lan) {
        lan = lan.toLowerCase();
        if (!isValidLan(lan)) {
            throw new IllegalArgumentException("Invalid move: " + lan);
        }
        int startSquare = BoardRepresentation.IndexFromSquareName(lan.substring(0, 2));
        int targetSquare = BoardRepresentation.IndexFromSquareName(lan.substring(2, 4));
        int wantedPromotionFlag = lan.length() == 5 ? "qnrb".indexOf(lan.substring(4)) + 3 : 0;
        boolean moveIsLegal = false;
        Move chosenMove = null;
        MoveGenerator moveGenerator = new MoveGenerator();
        List<Move> legalMoves = moveGenerator.GenerateMoves(board);
        for (Move legalMove : legalMoves) {
            if (legalMove.StartSquare() == startSquare && legalMove.TargetSquare() == targetSquare) {
                if (legalMove.IsPromotion()){
                    if(legalMove.MoveFlag() == Move.Flag.PromoteToQueen && (wantedPromotionFlag == Move.Flag.PromoteToKnight)){
                        break;
                    }
                    if(legalMove.MoveFlag() != Move.Flag.PromoteToQueen && (wantedPromotionFlag != Move.Flag.PromoteToKnight)){
                        continue;
                    }
                }
                moveIsLegal = true;
                chosenMove = legalMove;
                break;
            }
        }
        
        if (moveIsLegal) {
            return chosenMove.moveValue;
        } else {
            throw new IllegalArgumentException("Invalid move: " + lan);
        }
    }

    // getter
    public int StartSquare() {
        return moveValue & startSquareMask;
    }

    public int TargetSquare() {
        return (moveValue & targetSquareMask) >> 6;
    }

    public boolean IsPromotion() {
        int flag = MoveFlag();
        return flag == Flag.PromoteToQueen || flag == Flag.PromoteToRook || flag == Flag.PromoteToKnight
                || flag == Flag.PromoteToBishop;
    }

    public int MoveFlag() {
        return moveValue >> 12;
    }

    public int PromotionPieceType() {
        switch (MoveFlag()) {
            case Flag.PromoteToRook:
                return Piece.Rook;
            case Flag.PromoteToKnight:
                return Piece.Knight;
            case Flag.PromoteToBishop:
                return Piece.Bishop;
            case Flag.PromoteToQueen:
                return Piece.Queen;
            default:
                return Piece.None;
        }

    }

    public static Move InvalidMove() {
        return new Move((short) 0);

    }

    public static boolean SameMove(Move a, Move b) {
        return a.moveValue == b.moveValue;
    }

    public short Value() {
        return moveValue;

    }

    public boolean IsInvalid() {
        return moveValue == 0;

    }

    public String Name() {
        return BoardRepresentation.SquareNameFromIndex(StartSquare()) + "-"
                + BoardRepresentation.SquareNameFromIndex(TargetSquare());
    }

    private static boolean isValidLan(String lan) {
        lan = lan.toLowerCase();
        return lan.matches("^[a-h][1-8][a-h][1-8][qrbn]?$");
    }
}