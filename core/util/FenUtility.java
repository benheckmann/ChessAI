package core.util;

import java.util.Dictionary;
import java.util.Hashtable;

import core.*;

public class FenUtility {

    static Dictionary<Character, Integer> pieceTypeFromSymbol = new Hashtable<Character, Integer>() {
        {
            put('k', Piece.King);
            put('p', Piece.Pawn);
            put('n', Piece.Knight);
            put('b', Piece.Bishop);
            put('r', Piece.Rook);
            put('q', Piece.Queen);
        }
    };

    public static final String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // Load position from fen String
    public static LoadedPositionInfo PositionFromFen (String fen) {

        LoadedPositionInfo loadedPositionInfo = new LoadedPositionInfo();
        String[] sections = fen.split(" ");

        int file = 0;
        int rank = 7;

        for (Character symbol : sections[0].toCharArray()) {
            if (symbol == '/') {
                file = 0;
                rank--;
            } else {
                if (Character.isDigit(symbol)) {
                    file += (int) Character.getNumericValue(symbol);
                } else {
                    int pieceColour = (Character.isUpperCase(symbol)) ? Piece.White : Piece.Black;
                    int pieceType = pieceTypeFromSymbol.get(Character.toLowerCase(symbol));
                    loadedPositionInfo.squares[rank * 8 + file] = pieceType | pieceColour;
                    file++;
                }
            }
        }

        loadedPositionInfo.whiteToMove = (sections[1] == "w");

        String castlingRights = (sections.length > 2) ? sections[2] : "KQkq";
        loadedPositionInfo.whiteCastleKingside = castlingRights.contains("K");
        loadedPositionInfo.whiteCastleQueenside = castlingRights.contains("Q");
        loadedPositionInfo.blackCastleKingside = castlingRights.contains("k");
        loadedPositionInfo.blackCastleQueenside = castlingRights.contains("q");

        if (sections.length > 3) {
            String enPassantFileName = String.valueOf(sections[3].charAt(0));
            if (BoardRepresentation.fileNames.contains(enPassantFileName)) {
                loadedPositionInfo.epFile = BoardRepresentation.fileNames.indexOf(enPassantFileName) + 1;
            }
        }

        // Half-move clock
        if (sections.length > 4) {
            try {
                loadedPositionInfo.plyCount = Integer.parseInt(sections[4]);
            } catch (NumberFormatException e) {
                loadedPositionInfo.plyCount = 0;
            }
        }
        return loadedPositionInfo;
    }

    // Get the fen String of the current position
    public static String CurrentFen(Board board) {
        String fen = "";
        for (int rank = 7; rank >= 0; rank--) {
            int numEmptyFiles = 0;
            for (int file = 0; file < 8; file++) {
                int i = rank * 8 + file;
                int piece = board.Square[i];
                if (piece != 0) {
                    if (numEmptyFiles != 0) {
                        fen += numEmptyFiles;
                        numEmptyFiles = 0;
                    }
                    boolean isBlack = Piece.IsColour(piece, Piece.Black);
                    int pieceType = Piece.PieceType(piece);
                    Character pieceChar = ' ';
                    switch (pieceType) {
                        case Piece.Rook:
                            pieceChar = 'R';
                            break;
                        case Piece.Knight:
                            pieceChar = 'N';
                            break;
                        case Piece.Bishop:
                            pieceChar = 'B';
                            break;
                        case Piece.Queen:
                            pieceChar = 'Q';
                            break;
                        case Piece.King:
                            pieceChar = 'K';
                            break;
                        case Piece.Pawn:
                            pieceChar = 'P';
                            break;
                    }
                    fen += (isBlack) ? pieceChar.toString().toLowerCase() : pieceChar.toString();
                } else {
                    numEmptyFiles++;
                }

            }
            if (numEmptyFiles != 0) {
                fen += numEmptyFiles;
            }
            if (rank != 0) {
                fen += '/';
            }
        }

        // Side to move
        fen += ' ';
        fen += (board.WhiteToMove) ? 'w' : 'b';

        // Castling
        boolean whiteKingside = (board.currentGameState & 1) == 1;
        boolean whiteQueenside = (board.currentGameState >> 1 & 1) == 1;
        boolean blackKingside = (board.currentGameState >> 2 & 1) == 1;
        boolean blackQueenside = (board.currentGameState >> 3 & 1) == 1;
        fen += ' ';
        fen += (whiteKingside) ? "K" : "";
        fen += (whiteQueenside) ? "Q" : "";
        fen += (blackKingside) ? "k" : "";
        fen += (blackQueenside) ? "q" : "";
        fen += ((board.currentGameState & 15) == 0) ? "-" : "";

        // En-passant
        fen += ' ';
        int epFile = (int) (board.currentGameState >> 4) & 15;
        if (epFile == 0) {
            fen += '-';
        } else {
            String fileName = String.valueOf(BoardRepresentation.fileNames.charAt(epFile - 1));
            int epRank = (board.WhiteToMove) ? 6 : 3;
            fen += fileName + epRank;
        }

        // 50 move counter
        fen += ' ';
        fen += board.fiftyMoveCounter;

        // Full-move count (should be one at start, and increase after each move by
        // black)
        fen += ' ';
        fen += (board.plyCount / 2) + 1;

        return fen;
    }

    public static class LoadedPositionInfo {
        public int[] squares;
        public boolean whiteCastleKingside;
        public boolean whiteCastleQueenside;
        public boolean blackCastleKingside;
        public boolean blackCastleQueenside;
        public int epFile;
        public boolean whiteToMove;
        public int plyCount;

        public LoadedPositionInfo() {
            squares = new int[64];
        }
    }
}