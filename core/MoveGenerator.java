package core;

import core.data.*;
import core.util.*;

import java.util.LinkedList;
import java.util.List;

public class MoveGenerator {

    public enum PromotionMode {
        All, QueenOnly, QueenAndKnight
    }

    public PromotionMode promotionsToGenerate = PromotionMode.All;

    List<Move> moves;
    boolean isWhiteToMove;
    int friendlyColour;
    int opponentColour;
    int friendlyKingSquare;
    int friendlyColourIndex;
    int opponentColourIndex;

    boolean inCheck;
    boolean inDoubleCheck;
    boolean pinsExistInPosition;
    long checkRayBitmask;
    long pinRayBitmask;
    long opponentKnightAttacks;
    long opponentAttackMapNoPawns;
    public long opponentAttackMap;
    public long opponentPawnAttackMap;
    long opponentSlidingAttackMap;

    boolean genQuiets;
    Board board;

    public List<Move> generateMoves(Board board) {
        return generateMoves(board, true);
    }

    /**
     * Generates list of legal moves in current position. Non capturing moves can
     * optionally be excluded.
     */
    public List<Move> generateMoves(Board board, boolean includeQuietMoves) {
        this.board = board;
        genQuiets = includeQuietMoves;
        init();

        CalculateAttackData();
        generateKingMoves();
        
        // only king moves are valid in a double check position
        if (inDoubleCheck) {
            return moves;
        }

        generateSlidingMoves();
        generateKnightMoves();
        generatePawnMoves();

        return moves;
    }

    public boolean isInCheck() {
        // assumes GenerateMoves() has been called
        return inCheck;
    }

    void init() {
        moves = new LinkedList<Move>();
        inCheck = false;
        inDoubleCheck = false;
        pinsExistInPosition = false;
        checkRayBitmask = 0;
        pinRayBitmask = 0;

        isWhiteToMove = board.colourToMove == Piece.White;
        friendlyColour = board.colourToMove;
        opponentColour = board.opponentColour;
        friendlyKingSquare = board.KingSquare[board.colourToMoveIndex];
        friendlyColourIndex = (board.whiteToMove) ? Board.WHITE_INDEX : Board.BLACK_INDEX;
        opponentColourIndex = 1 - friendlyColourIndex;
    }

    void generateKingMoves() {
        for (int i = 0; i < PrecomputedMoveData.kingMoves[friendlyKingSquare].length; i++) {
            int targetSquare = PrecomputedMoveData.kingMoves[friendlyKingSquare][i];
            int pieceOnTargetSquare = board.Square[targetSquare];

            if (Piece.isColour(pieceOnTargetSquare, friendlyColour)) {
                continue;
            }

            boolean isCapture = Piece.isColour(pieceOnTargetSquare, opponentColour);
            if (!isCapture) {
                if (!genQuiets || SquareIsInCheckRay(targetSquare)) {
                    continue;
                }
            }

            if (!SquareIsAttacked(targetSquare)) {
                moves.add(new Move(friendlyKingSquare, targetSquare));
                // castling
                if (!inCheck && !isCapture) {
                    // kingside
                    if ((targetSquare == BoardUtility.f1 || targetSquare == BoardUtility.f8)
                            && HasKingsideCastleRight()) {
                        int castleKingsideSquare = targetSquare + 1;
                        if (board.Square[castleKingsideSquare] == Piece.None) {
                            if (!SquareIsAttacked(castleKingsideSquare)) {
                                moves.add(new Move(friendlyKingSquare, castleKingsideSquare, Move.Flag.Castling));
                            }
                        }
                    }
                    // queenside
                    else if ((targetSquare == BoardUtility.d1 || targetSquare == BoardUtility.d8)
                            && HasQueensideCastleRight()) {
                        int castleQueensideSquare = targetSquare - 1;
                        if (board.Square[castleQueensideSquare] == Piece.None
                                && board.Square[castleQueensideSquare - 1] == Piece.None) {
                            if (!SquareIsAttacked(castleQueensideSquare)) {
                                moves.add(new Move(friendlyKingSquare, castleQueensideSquare, Move.Flag.Castling));
                            }
                        }
                    }
                }
            }
        }
    }

    void generateSlidingMoves() {
        PieceList rooks = board.rooks[friendlyColourIndex];
        for (int i = 0; i < rooks.size(); i++) {
            generateSlidingPieceMoves(rooks.get(i), 0, 4);
        }

        PieceList bishops = board.bishops[friendlyColourIndex];
        for (int i = 0; i < bishops.size(); i++) {
            generateSlidingPieceMoves(bishops.get(i), 4, 8);
        }

        PieceList queens = board.queens[friendlyColourIndex];
        for (int i = 0; i < queens.size(); i++) {
            generateSlidingPieceMoves(queens.get(i), 0, 8);
        }

    }

    void generateSlidingPieceMoves(int startSquare, int startDirIndex, int endDirIndex) {
        boolean isPinned = IsPinned(startSquare);
        if (inCheck && isPinned) {
            return;
        }

        for (int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            int currentDirOffset = PrecomputedMoveData.directionOffsets[directionIndex];
            if (isPinned && !IsMovingAlongRay(currentDirOffset, friendlyKingSquare, startSquare)) {
                continue;
            }

            for (int n = 0; n < PrecomputedMoveData.numSquaresToEdge[startSquare][directionIndex]; n++) {
                int targetSquare = startSquare + currentDirOffset * (n + 1);
                int targetSquarePiece = board.Square[targetSquare];
                if (Piece.isColour(targetSquarePiece, friendlyColour)) {
                    break;
                }
                boolean isCapture = targetSquarePiece != Piece.None;

                boolean movePreventsCheck = SquareIsInCheckRay(targetSquare);
                if (movePreventsCheck || !inCheck) {
                    if (genQuiets || isCapture) {
                        moves.add(new Move(startSquare, targetSquare));
                    }
                }
                if (isCapture || movePreventsCheck) {
                    break;
                }
            }
        }
    }

    void generateKnightMoves() {
        PieceList myKnights = board.knights[friendlyColourIndex];

        for (int i = 0; i < myKnights.size(); i++) {
            int startSquare = myKnights.get(i);

            if (IsPinned(startSquare)) {
                continue;
            }

            for (int knightMoveIndex = 0; knightMoveIndex < PrecomputedMoveData.knightMoves[startSquare].length; knightMoveIndex++) {
                int targetSquare = PrecomputedMoveData.knightMoves[startSquare][knightMoveIndex];
                int targetSquarePiece = board.Square[targetSquare];
                boolean isCapture = Piece.isColour(targetSquarePiece, opponentColour);
                if (genQuiets || isCapture) {
                    if (Piece.isColour(targetSquarePiece, friendlyColour)
                            || (inCheck && !SquareIsInCheckRay(targetSquare))) {
                        continue;
                    }
                    moves.add(new Move(startSquare, targetSquare));
                }
            }
        }
    }

    void generatePawnMoves() {
        PieceList myPawns = board.pawns[friendlyColourIndex];
        int pawnOffset = (friendlyColour == Piece.White) ? 8 : -8;
        int startRank = (board.whiteToMove) ? 1 : 6;
        int finalRankBeforePromotion = (board.whiteToMove) ? 6 : 1;

        int enPassantFile = ((int) (board.currentGameState >> 4) & 15) - 1;
        int enPassantSquare = -1;
        if (enPassantFile != -1) {
            enPassantSquare = 8 * ((board.whiteToMove) ? 5 : 2) + enPassantFile;
        }

        for (int i = 0; i < myPawns.size(); i++) {
            int startSquare = myPawns.get(i);
            int rank = BoardUtility.RankIndex(startSquare);
            boolean oneStepFromPromotion = rank == finalRankBeforePromotion;

            if (genQuiets) {

                int squareOneForward = startSquare + pawnOffset;

                if (board.Square[squareOneForward] == Piece.None) {
                    if (!IsPinned(startSquare) || IsMovingAlongRay(pawnOffset, startSquare, friendlyKingSquare)) {
                        if (!inCheck || SquareIsInCheckRay(squareOneForward)) {
                            if (oneStepFromPromotion) {
                                MakePromotionMoves(startSquare, squareOneForward);
                            } else {
                                moves.add(new Move(startSquare, squareOneForward));
                            }
                        }

                        if (rank == startRank) {
                            int squareTwoForward = squareOneForward + pawnOffset;
                            if (board.Square[squareTwoForward] == Piece.None) {
                                if (!inCheck || SquareIsInCheckRay(squareTwoForward)) {
                                    moves.add(new Move(startSquare, squareTwoForward, Move.Flag.PawnTwoForward));
                                }
                            }
                        }
                    }
                }
            }

            for (int j = 0; j < 2; j++) {
                if (PrecomputedMoveData.numSquaresToEdge[startSquare][PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][j]] > 0) {
                    int pawnCaptureDir = PrecomputedMoveData.directionOffsets[PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][j]];
                    int targetSquare = startSquare + pawnCaptureDir;
                    int targetPiece = board.Square[targetSquare];
                    if (IsPinned(startSquare) && !IsMovingAlongRay(pawnCaptureDir, friendlyKingSquare, startSquare)) {
                        continue;
                    }
                    if (Piece.isColour(targetPiece, opponentColour)) {
                        if (inCheck && !SquareIsInCheckRay(targetSquare)) {
                            continue;
                        }
                        if (oneStepFromPromotion) {
                            MakePromotionMoves(startSquare, targetSquare);
                        } else {
                            moves.add(new Move(startSquare, targetSquare));
                        }
                    }
                    if (targetSquare == enPassantSquare) {
                        int epCapturedPawnSquare = targetSquare + ((board.whiteToMove) ? -8 : 8);
                        if (!InCheckAfterEnPassant(startSquare, targetSquare, epCapturedPawnSquare)) {
                            moves.add(new Move(startSquare, targetSquare, Move.Flag.EnPassantCapture));
                        }
                    }
                }
            }
        }
    }

    void MakePromotionMoves(int fromSquare, int toSquare) {
        moves.add(new Move(fromSquare, toSquare, Move.Flag.PromoteToQueen));
        if (promotionsToGenerate == PromotionMode.All) {
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PromoteToKnight));
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PromoteToRook));
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PromoteToBishop));
        } else if (promotionsToGenerate == PromotionMode.QueenAndKnight) {
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PromoteToKnight));
        }

    }

    boolean IsMovingAlongRay(int rayDir, int startSquare, int targetSquare) {
        int moveDir = PrecomputedMoveData.directionLookup[targetSquare - startSquare + 63];
        return (rayDir == moveDir || -rayDir == moveDir);
    }

    boolean IsPinned(int square) {
        return pinsExistInPosition && ((pinRayBitmask >> square) & 1) != 0;
    }

    boolean SquareIsInCheckRay(int square) {
        return inCheck && ((checkRayBitmask >> square) & 1) != 0;
    }

    boolean HasKingsideCastleRight() {
        int mask = (board.whiteToMove) ? 1 : 4;
        return (board.currentGameState & mask) != 0;

    }

    boolean HasQueensideCastleRight() {
        int mask = (board.whiteToMove) ? 2 : 8;
        return (board.currentGameState & mask) != 0;

    }

    void GenSlidingAttackMap() {
        opponentSlidingAttackMap = 0;

        PieceList enemyRooks = board.rooks[opponentColourIndex];
        for (int i = 0; i < enemyRooks.size(); i++) {
            UpdateSlidingAttackPiece(enemyRooks.get(i), 0, 4);
        }

        PieceList enemyQueens = board.queens[opponentColourIndex];
        for (int i = 0; i < enemyQueens.size(); i++) {
            UpdateSlidingAttackPiece(enemyQueens.get(i), 0, 8);
        }

        PieceList enemyBishops = board.bishops[opponentColourIndex];
        for (int i = 0; i < enemyBishops.size(); i++) {
            UpdateSlidingAttackPiece(enemyBishops.get(i), 4, 8);
        }
    }

    void UpdateSlidingAttackPiece(int startSquare, int startDirIndex, int endDirIndex) {
        for (int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            int currentDirOffset = PrecomputedMoveData.directionOffsets[directionIndex];
            for (int n = 0; n < PrecomputedMoveData.numSquaresToEdge[startSquare][directionIndex]; n++) {
                int targetSquare = startSquare + currentDirOffset * (n + 1);
                int targetSquarePiece = board.Square[targetSquare];
                opponentSlidingAttackMap |= 1l << targetSquare;
                if (targetSquare != friendlyKingSquare) {
                    if (targetSquarePiece != Piece.None) {
                        break;
                    }
                }
            }
        }
    }

    void CalculateAttackData() {
        GenSlidingAttackMap();
        int startDirIndex = 0;
        int endDirIndex = 8;

        if (board.queens[opponentColourIndex].size() == 0) {
            startDirIndex = (board.rooks[opponentColourIndex].size() > 0) ? 0 : 4;
            endDirIndex = (board.bishops[opponentColourIndex].size() > 0) ? 8 : 4;
        }

        for (int dir = startDirIndex; dir < endDirIndex; dir++) {
            boolean isDiagonal = dir > 3;

            int n = PrecomputedMoveData.numSquaresToEdge[friendlyKingSquare][dir];
            int directionOffset = PrecomputedMoveData.directionOffsets[dir];
            boolean isFriendlyPieceAlongRay = false;
            long rayMask = 0;

            for (int i = 0; i < n; i++) {
                int squareIndex = friendlyKingSquare + directionOffset * (i + 1);
                rayMask |= 1l << squareIndex;
                int piece = board.Square[squareIndex];
                if (piece != Piece.None) {
                    if (Piece.isColour(piece, friendlyColour)) {
                        if (!isFriendlyPieceAlongRay) {
                            isFriendlyPieceAlongRay = true;
                        }
                        else {
                            break;
                        }
                    }
                    else {
                        int pieceType = Piece.getPieceType(piece);
                        if (isDiagonal && Piece.isBishopOrQueen(pieceType)
                                || !isDiagonal && Piece.isRookOrQueen(pieceType)) {
                            if (isFriendlyPieceAlongRay) {
                                pinsExistInPosition = true;
                                pinRayBitmask |= rayMask;
                            }
                            else {
                                checkRayBitmask |= rayMask;
                                inDoubleCheck = inCheck; // if already in check, then this is double check
                                inCheck = true;
                            }
                            break;
                        } else {
                            break;
                        }
                    }
                }
            }
            if (inDoubleCheck) {
                break;
            }

        }
        PieceList opponentKnights = board.knights[opponentColourIndex];
        opponentKnightAttacks = 0;
        boolean isKnightCheck = false;

        for (int knightIndex = 0; knightIndex < opponentKnights.size(); knightIndex++) {
            int startSquare = opponentKnights.get(knightIndex);
            opponentKnightAttacks |= PrecomputedMoveData.knightAttackBitboards[startSquare];

            if (!isKnightCheck && BitBoardUtility.ContainsSquare(opponentKnightAttacks, friendlyKingSquare)) {
                isKnightCheck = true;
                inDoubleCheck = inCheck; // if already in check, then this is double check
                inCheck = true;
                checkRayBitmask |= 1l << startSquare;
            }
        }
        PieceList opponentPawns = board.pawns[opponentColourIndex];
        opponentPawnAttackMap = 0;
        boolean isPawnCheck = false;

        for (int pawnIndex = 0; pawnIndex < opponentPawns.size(); pawnIndex++) {
            int pawnSquare = opponentPawns.get(pawnIndex);
            long pawnAttacks = PrecomputedMoveData.pawnAttackBitboards[pawnSquare][opponentColourIndex];
            opponentPawnAttackMap |= pawnAttacks;

            if (!isPawnCheck && BitBoardUtility.ContainsSquare(pawnAttacks, friendlyKingSquare)) {
                isPawnCheck = true;
                inDoubleCheck = inCheck; // if already in check, then this is double check
                inCheck = true;
                checkRayBitmask |= 1l << pawnSquare;
            }
        }

        int enemyKingSquare = board.KingSquare[opponentColourIndex];

        opponentAttackMapNoPawns = opponentSlidingAttackMap | opponentKnightAttacks
                | PrecomputedMoveData.kingAttackBitboards[enemyKingSquare];
        opponentAttackMap = opponentAttackMapNoPawns | opponentPawnAttackMap;
    }

    boolean SquareIsAttacked(int square) {
        return BitBoardUtility.ContainsSquare(opponentAttackMap, square);
    }

    boolean InCheckAfterEnPassant(int startSquare, int targetSquare, int epCapturedPawnSquare) {
        board.Square[targetSquare] = board.Square[startSquare];
        board.Square[startSquare] = Piece.None;
        board.Square[epCapturedPawnSquare] = Piece.None;

        boolean inCheckAfterEpCapture = false;
        if (SquareAttackedAfterEPCapture(epCapturedPawnSquare, startSquare)) {
            inCheckAfterEpCapture = true;
        }
        board.Square[targetSquare] = Piece.None;
        board.Square[startSquare] = Piece.Pawn | friendlyColour;
        board.Square[epCapturedPawnSquare] = Piece.Pawn | opponentColour;
        return inCheckAfterEpCapture;
    }

    boolean SquareAttackedAfterEPCapture(int epCaptureSquare, int capturingPawnStartSquare) {
        if (BitBoardUtility.ContainsSquare(opponentAttackMapNoPawns, friendlyKingSquare)) {
            return true;
        }
        int dirIndex = (epCaptureSquare < friendlyKingSquare) ? 2 : 3;
        for (int i = 0; i < PrecomputedMoveData.numSquaresToEdge[friendlyKingSquare][dirIndex]; i++) {
            int squareIndex = friendlyKingSquare + PrecomputedMoveData.directionOffsets[dirIndex] * (i + 1);
            int piece = board.Square[squareIndex];
            if (piece != Piece.None) {
                if (Piece.isColour(piece, friendlyColour)) {
                    break;
                }
                else {
                    if (Piece.isRookOrQueen(piece)) {
                        return true;
                    } else {
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < 2; i++) {
            if (PrecomputedMoveData.numSquaresToEdge[friendlyKingSquare][PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][i]] > 0) {
                int piece = board.Square[friendlyKingSquare
                        + PrecomputedMoveData.directionOffsets[PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][i]]];
                if (piece == (Piece.Pawn | opponentColour)) // is enemy pawn
                {
                    return true;
                }
            }
        }

        return false;
    }
}
