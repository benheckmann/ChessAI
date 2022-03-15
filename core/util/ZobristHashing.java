package core.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import core.*;

public class ZobristHashing {
    static final int SEED = 2361912;
    static final String randomNumbersFileName = "RandomNumbers.txt";

    /// piece type, colour, square index
    public static final long[][][] piecesArray = new long[8][2][64];
    public static final long[] castlingRights = new long[16];
    /// ep file (0 = no ep).
    public static final long[] enPassantFile = new long[9]; // no need for rank info as side to move is included in key
    public static final long sideToMove;

    static Random prng = new Random(SEED);

    static void writeRandomNumbers() throws FileNotFoundException {
        prng = new Random(SEED);
        String randomNumberString = "";
        int numRandomNumbers = 64 * 8 * 2 + castlingRights.length + 9 + 1;

        for (int i = 0; i < numRandomNumbers; i++) {
            randomNumberString += getRandomUnsigned64BitNumber();
            if (i != numRandomNumbers - 1) {
                randomNumberString += ',';
            }
        }
        var writer = new PrintWriter(getRandomNumbersPath());
        writer.write(randomNumberString);
        writer.close();
    }

    static Queue<Long> readRandomNumbers () throws IOException {
        Path path = Paths.get(getRandomNumbersPath());
        if (!java.nio.file.Files.exists(path)) {
            writeRandomNumbers();
        }

        Queue<Long> randomNumbers = new LinkedList<Long>();

        var reader = new BufferedReader(new FileReader(getRandomNumbersPath()));
        String numbersString = reader.readLine();
        reader.close ();

        String[] numberStrings = numbersString.split(",");
        for (int i = 0; i < numberStrings.length; i++) {
            long number = Long.parseLong(numberStrings[i]);
            randomNumbers.add(number);
        }
        return randomNumbers;
    }

    static {
        var randomNumbers = (Queue<Long>) new LinkedList<Long>();
        try {
            randomNumbers = readRandomNumbers();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            for (int pieceIndex = 0; pieceIndex < 8; pieceIndex++) {
                piecesArray[pieceIndex][Board.WHITE_INDEX][squareIndex] = randomNumbers.poll();
                piecesArray[pieceIndex][Board.BLACK_INDEX][squareIndex] = randomNumbers.poll();
            }
        }

        for (int i = 0; i < 16; i++) {
            castlingRights[i] = randomNumbers.poll();
        }

        for (int i = 0; i < enPassantFile.length; i++) {
            enPassantFile[i] = randomNumbers.poll();
        }

        sideToMove = randomNumbers.poll();
    }

    /**
     * Calculate zobrist key from current board position.
     */
    public static long calculateZobristKey (Board board) {
        long zobristKey = 0;

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            if (board.Square[squareIndex] != 0) {
                int pieceType = Piece.getPieceType (board.Square[squareIndex]);
                int pieceColour = Piece.getColour (board.Square[squareIndex]);

                zobristKey ^= piecesArray[pieceType][(pieceColour == Piece.White) ? Board.WHITE_INDEX : Board.BLACK_INDEX][squareIndex];
            }
        }

        int epIndex = (int) (board.currentGameState >> 4) & 15;
        if (epIndex != -1) {
            zobristKey ^= enPassantFile[epIndex];
        }

        if (board.colourToMove == Piece.Black) {
            zobristKey ^= sideToMove;
        }

        zobristKey ^= castlingRights[board.currentGameState & 0b1111];

        return zobristKey;
    }

    static String getRandomNumbersPath() {
        return Paths.get(randomNumbersFileName).toString();
    }

    static long getRandomUnsigned64BitNumber() {
        byte[] buffer = new byte[8];
        prng.nextBytes(buffer);
        return toInt64(buffer);
    }

    private static long toInt64(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) bytes[i] & 0xffL) << (i * 8);
        }
        return value;
    }
}