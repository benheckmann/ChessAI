package core;

import java.util.Stack;

import core.util.*;

public class Board {

    public static final int WHITE_INDEX = 0;
    public static final int BLACK_INDEX = 1;

    // Stores piece code for each square on the board.
    // Piece code is defined as piecetype | colour code
    public int[] Square;

    public boolean whiteToMove;
    public int colourToMove;
    public int opponentColour;
    public int colourToMoveIndex;

    // Bits 0-3 store white and black kingside/queenside castling legality
    // Bits 4-7 store file of ep square (starting at 1, so 0 = no ep square)
    // Bits 8-13 captured piece
    // Bits 14-... fifty mover counter
    Stack<Integer> gameStateHistory;
    public Integer currentGameState;

    public int plyCount; // Total plies played in game
    public int fiftyMoveCounter; // Num ply since last pawn move or capture

    public Long ZobristKey;
    public Stack<Long> RepetitionPositionHistory;

    public int[] KingSquare; // index of square of white and black king

    public PieceList[] rooks;
    public PieceList[] bishops;
    public PieceList[] queens;
    public PieceList[] knights;
    public PieceList[] pawns;

    PieceList[] allPieceLists;

    static final Integer WHITE_CASTLE_KINGSIDE_MASK = 0b1111111111111110;
    static final Integer WHITE_CASTLE_QUEENSIDE_MASK = 0b1111111111111101;
    static final Integer BLACK_CASTLE_KINGSIDE_MASK = 0b1111111111111011;
    static final Integer BLACK_CASTLE_QUEENSIDE_MASK = 0b1111111111110111;

    static final Integer WHITE_CASTLE_MASK = WHITE_CASTLE_KINGSIDE_MASK & WHITE_CASTLE_QUEENSIDE_MASK;
    static final Integer BLACK_CASTLE_MASK = BLACK_CASTLE_KINGSIDE_MASK & BLACK_CASTLE_QUEENSIDE_MASK;

    public Board() {
        Initialize();
    }

    PieceList GetPieceList(int pieceType, int colourIndex) {
        return allPieceLists[colourIndex * 8 + pieceType];
    }

    public void MakeMove(Move move) {
        MakeMove(move, false);
    }

    // inSearch parameter controls whether this move should be recorded in the
    // game history (for detecting three-fold repetition)
    public void MakeMove(Move move, boolean inSearch) {
        Integer oldEnPassantFile = (currentGameState >> 4) & 15;
        Integer originalCastleState = currentGameState & 15;
        Integer newCastleState = originalCastleState;
        currentGameState = 0;

        int opponentColourIndex = 1 - colourToMoveIndex;
        int moveFrom = move.getStartSquare();
        int moveTo = move.getTargetSquare();

        int capturedPieceType = Piece.getPieceType(Square[moveTo]);
        int movePiece = Square[moveFrom];
        int movePieceType = Piece.getPieceType(movePiece);

        int moveFlag = move.getMoveFlag();
        boolean isPromotion = move.isPromotion();
        boolean isEnPassant = moveFlag == Move.Flag.EnPassantCapture;

        // Handle captures
        currentGameState |= capturedPieceType << 8;
        if (capturedPieceType != 0 && !isEnPassant) {
            ZobristKey ^= ZobristHashing.piecesArray[capturedPieceType][opponentColourIndex][moveTo];
            GetPieceList(capturedPieceType, opponentColourIndex).removePieceAtSquare(moveTo);
        }

        // Move pieces in piece lists
        if (movePieceType == Piece.King) {
            KingSquare[colourToMoveIndex] = moveTo;
            newCastleState &= (whiteToMove) ? WHITE_CASTLE_MASK : BLACK_CASTLE_MASK;
        } else {
            GetPieceList(movePieceType, colourToMoveIndex).movePiece(moveFrom, moveTo);
        }

        int pieceOnTargetSquare = movePiece;

        // Handle promotion
        if (isPromotion) {
            int promoteType = 0;
            switch (moveFlag) {
                case Move.Flag.PromoteToQueen:
                    promoteType = Piece.Queen;
                    queens[colourToMoveIndex].addPieceAtSquare(moveTo);
                    break;
                case Move.Flag.PromoteToRook:
                    promoteType = Piece.Rook;
                    rooks[colourToMoveIndex].addPieceAtSquare(moveTo);
                    break;
                case Move.Flag.PromoteToBishop:
                    promoteType = Piece.Bishop;
                    bishops[colourToMoveIndex].addPieceAtSquare(moveTo);
                    break;
                case Move.Flag.PromoteToKnight:
                    promoteType = Piece.Knight;
                    knights[colourToMoveIndex].addPieceAtSquare(moveTo);
                    break;

            }
            pieceOnTargetSquare = promoteType | colourToMove;
            pawns[colourToMoveIndex].removePieceAtSquare(moveTo);
        } else {
            // Handle other special moves (en-passant, and castling)
            switch (moveFlag) {
                case Move.Flag.EnPassantCapture:
                    int epPawnSquare = moveTo + ((colourToMove == Piece.White) ? -8 : 8);
                    currentGameState |= Square[epPawnSquare] << 8; // add pawn as capture type
                    Square[epPawnSquare] = 0; // clear ep capture square
                    pawns[opponentColourIndex].removePieceAtSquare(epPawnSquare);
                    ZobristKey ^= ZobristHashing.piecesArray[Piece.Pawn][opponentColourIndex][epPawnSquare];
                    break;
                case Move.Flag.Castling:
                    boolean kingside = moveTo == BoardUtility.g1 || moveTo == BoardUtility.g8;
                    int castlingRookFromIndex = (kingside) ? moveTo + 1 : moveTo - 2;
                    int castlingRookToIndex = (kingside) ? moveTo - 1 : moveTo + 1;

                    Square[castlingRookFromIndex] = Piece.None;
                    Square[castlingRookToIndex] = Piece.Rook | colourToMove;

                    rooks[colourToMoveIndex].movePiece(castlingRookFromIndex, castlingRookToIndex);
                    ZobristKey ^= ZobristHashing.piecesArray[Piece.Rook][colourToMoveIndex][castlingRookFromIndex];
                    ZobristKey ^= ZobristHashing.piecesArray[Piece.Rook][colourToMoveIndex][castlingRookToIndex];
                    break;
            }
        }

        // Update the board representation:
        Square[moveTo] = pieceOnTargetSquare;
        Square[moveFrom] = 0;

        // Pawn has moved two forwards, mark file with en-passant flag
        if (moveFlag == Move.Flag.PawnTwoForward) {
            int file = BoardUtility.FileIndex(moveFrom) + 1;
            currentGameState |= file << 4;
            ZobristKey ^= ZobristHashing.enPassantFile[file];
        }

        // Piece moving to/from rook square removes castling right for that side
        if (originalCastleState != 0) {
            if (moveTo == BoardUtility.h1 || moveFrom == BoardUtility.h1) {
                newCastleState &= WHITE_CASTLE_KINGSIDE_MASK;
            } else if (moveTo == BoardUtility.a1 || moveFrom == BoardUtility.a1) {
                newCastleState &= WHITE_CASTLE_QUEENSIDE_MASK;
            }
            if (moveTo == BoardUtility.h8 || moveFrom == BoardUtility.h8) {
                newCastleState &= BLACK_CASTLE_KINGSIDE_MASK;
            } else if (moveTo == BoardUtility.a8 || moveFrom == BoardUtility.a8) {
                newCastleState &= BLACK_CASTLE_QUEENSIDE_MASK;
            }
        }

        // Update zobrist key with new piece position and side to move
        ZobristKey ^= ZobristHashing.sideToMove;
        ZobristKey ^= ZobristHashing.piecesArray[movePieceType][colourToMoveIndex][moveFrom];
        ZobristKey ^= ZobristHashing.piecesArray[Piece.getPieceType(pieceOnTargetSquare)][colourToMoveIndex][moveTo];

        if (oldEnPassantFile != 0)
            ZobristKey ^= ZobristHashing.enPassantFile[oldEnPassantFile];

        if (newCastleState != originalCastleState) {
            ZobristKey ^= ZobristHashing.castlingRights[originalCastleState]; // remove old castling rights state
            ZobristKey ^= ZobristHashing.castlingRights[newCastleState]; // add new castling rights state
        }
        currentGameState |= newCastleState;
        currentGameState |= (Integer) fiftyMoveCounter << 14;
        gameStateHistory.push(currentGameState);

        // Change side to move
        whiteToMove = !whiteToMove;
        colourToMove = (whiteToMove) ? Piece.White : Piece.Black;
        opponentColour = (whiteToMove) ? Piece.Black : Piece.White;
        colourToMoveIndex = 1 - colourToMoveIndex;
        plyCount++;
        fiftyMoveCounter++;

        if (!inSearch) {
            if (movePieceType == Piece.Pawn || capturedPieceType != Piece.None) {
                RepetitionPositionHistory.clear();
                fiftyMoveCounter = 0;
            } else {
                RepetitionPositionHistory.push(ZobristKey);
            }
        }
    }

    public void UnmakeMove(Move move) {
        UnmakeMove(move, false);
    }

    // Undo a move previously made on the board
    public void UnmakeMove (Move move, boolean inSearch) {

			//int opponentColour = ColourToMove;
			int opponentColourIndex = colourToMoveIndex;
			boolean undoingWhiteMove = opponentColour == Piece.White;
			colourToMove = opponentColour; // side who made the move we are undoing
			opponentColour = (undoingWhiteMove) ? Piece.Black : Piece.White;
			colourToMoveIndex = 1 - colourToMoveIndex;
			whiteToMove = !whiteToMove;

			int originalCastleState = currentGameState & 0b1111;

			int capturedPieceType = ((int) currentGameState >> 8) & 63;
			int capturedPiece = (capturedPieceType == 0) ? 0 : capturedPieceType | opponentColour;

			int movedFrom = move.getStartSquare();
			int movedTo = move.getTargetSquare();
			int moveFlags = move.getMoveFlag();
			boolean isEnPassant = moveFlags == Move.Flag.EnPassantCapture;
			boolean isPromotion = move.isPromotion();

			int toSquarePieceType = Piece.getPieceType (Square[movedTo]);
			int movedPieceType = (isPromotion) ? Piece.Pawn : toSquarePieceType;

			// Update zobrist key with new piece position and side to move
			ZobristKey ^= ZobristHashing.sideToMove;
			ZobristKey ^= ZobristHashing.piecesArray[movedPieceType][colourToMoveIndex][movedFrom]; // add piece back to square it moved from
			ZobristKey ^= ZobristHashing.piecesArray[toSquarePieceType][colourToMoveIndex][movedTo]; // remove piece from square it moved to

			int oldEnPassantFile = (currentGameState >> 4) & 15;
			if (oldEnPassantFile != 0)
				ZobristKey ^= ZobristHashing.enPassantFile[oldEnPassantFile];

			// ignore ep captures, handled later
			if (capturedPieceType != 0 && !isEnPassant) {
				ZobristKey ^= ZobristHashing.piecesArray[capturedPieceType][opponentColourIndex][movedTo];
				GetPieceList (capturedPieceType, opponentColourIndex).addPieceAtSquare(movedTo);
			}

			// Update king index
			if (movedPieceType == Piece.King) {
				KingSquare[colourToMoveIndex] = movedFrom;
			} else if (!isPromotion) {
				GetPieceList (movedPieceType, colourToMoveIndex).movePiece(movedTo, movedFrom);
			}

			// put back moved piece
			Square[movedFrom] = movedPieceType | colourToMove; // note that if move was a pawn promotion, this will put the promoted piece back instead of the pawn. Handled in special move switch
			Square[movedTo] = capturedPiece; // will be 0 if no piece was captured

			if (isPromotion) {
				pawns[colourToMoveIndex].addPieceAtSquare (movedFrom);
				switch (moveFlags) {
					case Move.Flag.PromoteToQueen:
						queens[colourToMoveIndex].removePieceAtSquare (movedTo);
						break;
					case Move.Flag.PromoteToKnight:
						knights[colourToMoveIndex].removePieceAtSquare (movedTo);
						break;
					case Move.Flag.PromoteToRook:
						rooks[colourToMoveIndex].removePieceAtSquare (movedTo);
						break;
					case Move.Flag.PromoteToBishop:
						bishops[colourToMoveIndex].removePieceAtSquare (movedTo);
						break;
				}
			} else if (isEnPassant) { // ep cature: put captured pawn back on right square
				int epIndex = movedTo + ((colourToMove == Piece.White) ? -8 : 8);
				Square[movedTo] = 0;
				Square[epIndex] = (int) capturedPiece;
				pawns[opponentColourIndex].addPieceAtSquare (epIndex);
				ZobristKey ^= ZobristHashing.piecesArray[Piece.Pawn][opponentColourIndex][epIndex];
			} else if (moveFlags == Move.Flag.Castling) { // castles: move rook back to starting square

				boolean kingside = movedTo == 6 || movedTo == 62;
				int castlingRookFromIndex = (kingside) ? movedTo + 1 : movedTo - 2;
				int castlingRookToIndex = (kingside) ? movedTo - 1 : movedTo + 1;

				Square[castlingRookToIndex] = 0;
				Square[castlingRookFromIndex] = Piece.Rook | colourToMove;

				rooks[colourToMoveIndex].movePiece(castlingRookToIndex, castlingRookFromIndex);
				ZobristKey ^= ZobristHashing.piecesArray[Piece.Rook][colourToMoveIndex][castlingRookFromIndex];
				ZobristKey ^= ZobristHashing.piecesArray[Piece.Rook][colourToMoveIndex][castlingRookToIndex];

			}

			gameStateHistory.pop(); // removes current state from history
			currentGameState = gameStateHistory.peek(); // sets current state to previous state in history

			fiftyMoveCounter = (int) (currentGameState & 4294950912l) >> 14;
			int newEnPassantFile = (int) (currentGameState >> 4) & 15;
			if (newEnPassantFile != 0)
				ZobristKey ^= ZobristHashing.enPassantFile[newEnPassantFile];

			int newCastleState = currentGameState & 0b1111;
			if (newCastleState != originalCastleState) {
				ZobristKey ^= ZobristHashing.castlingRights[originalCastleState]; // remove old castling rights state
				ZobristKey ^= ZobristHashing.castlingRights[newCastleState]; // add new castling rights state
			}

			plyCount--;

			if (!inSearch && RepetitionPositionHistory.size() > 0) {
				RepetitionPositionHistory.pop();
			}

		}

    // Load the starting position
    public void LoadStartPosition() {
        LoadPosition(FenUtility.startFen);
    }

    // Load custom position from fen String
    public void LoadPosition(String fen) {
        Initialize();
        var loadedPosition = FenUtility.PositionFromFen(fen);

        // Load pieces into board array and piece lists
        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            int piece = loadedPosition.squares[squareIndex];
            Square[squareIndex] = piece;

            if (piece != Piece.None) {
                int pieceType = Piece.getPieceType(piece);
                int pieceColourIndex = (Piece.isColour(piece, Piece.White)) ? WHITE_INDEX : BLACK_INDEX;
                if (Piece.isSlidingPiece(piece)) {
                    if (pieceType == Piece.Queen) {
                        queens[pieceColourIndex].addPieceAtSquare(squareIndex);
                    } else if (pieceType == Piece.Rook) {
                        rooks[pieceColourIndex].addPieceAtSquare(squareIndex);
                    } else if (pieceType == Piece.Bishop) {
                        bishops[pieceColourIndex].addPieceAtSquare(squareIndex);
                    }
                } else if (pieceType == Piece.Knight) {
                    knights[pieceColourIndex].addPieceAtSquare(squareIndex);
                } else if (pieceType == Piece.Pawn) {
                    pawns[pieceColourIndex].addPieceAtSquare(squareIndex);
                } else if (pieceType == Piece.King) {
                    KingSquare[pieceColourIndex] = squareIndex;
                }
            }
        }

        // Side to move
        whiteToMove = loadedPosition.whiteToMove;
        colourToMove = (whiteToMove) ? Piece.White : Piece.Black;
        opponentColour = (whiteToMove) ? Piece.Black : Piece.White;
        colourToMoveIndex = (whiteToMove) ? 0 : 1;

        // Create gamestate
        int whiteCastle = ((loadedPosition.whiteCastleKingside) ? 1 << 0 : 0)
                | ((loadedPosition.whiteCastleQueenside) ? 1 << 1 : 0);
        int blackCastle = ((loadedPosition.blackCastleKingside) ? 1 << 2 : 0)
                | ((loadedPosition.blackCastleQueenside) ? 1 << 3 : 0);
        int epState = loadedPosition.epFile << 4;
        short initialGameState = (short) (whiteCastle | blackCastle | epState);
        gameStateHistory.push(Integer.valueOf(initialGameState));
        currentGameState = (int) initialGameState;
        plyCount = loadedPosition.plyCount;

        // Initialize zobrist key
        ZobristKey = ZobristHashing.calculateZobristKey(this);
    }

    void Initialize() {
        Square = new int[64];
        KingSquare = new int[2];

        gameStateHistory = new Stack<Integer>();
        ZobristKey = 0l;
        RepetitionPositionHistory = new Stack<Long>();
        plyCount = 0;
        fiftyMoveCounter = 0;

        knights = new PieceList[] { new PieceList(10), new PieceList(10) };
        pawns = new PieceList[] { new PieceList(8), new PieceList(8) };
        rooks = new PieceList[] { new PieceList(10), new PieceList(10) };
        bishops = new PieceList[] { new PieceList(10), new PieceList(10) };
        queens = new PieceList[] { new PieceList(9), new PieceList(9) };
        PieceList emptyList = new PieceList(0);
        allPieceLists = new PieceList[] {
                emptyList,
                emptyList,
                pawns[WHITE_INDEX],
                knights[WHITE_INDEX],
                emptyList,
                bishops[WHITE_INDEX],
                rooks[WHITE_INDEX],
                queens[WHITE_INDEX],
                emptyList,
                emptyList,
                pawns[BLACK_INDEX],
                knights[BLACK_INDEX],
                emptyList,
                bishops[BLACK_INDEX],
                rooks[BLACK_INDEX],
                queens[BLACK_INDEX],
        };
    }
}