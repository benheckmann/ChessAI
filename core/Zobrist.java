package core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Zobrist {
    static final int seed = 2361912;
    static final String randomNumbersFileName = "RandomNumbers.txt";

    /// piece type, colour, square index
    public static final long[][][] piecesArray = new long[8][2][64];
    public static final long[] castlingRights = new long[16];
    /// ep file (0 = no ep).
    public static final long[] enPassantFile = new long[9]; // no need for rank info as side to move is included in key
    public static final long sideToMove;

    static Random prng = new Random(seed);

    static void WriteRandomNumbers() throws FileNotFoundException {
        prng = new Random(seed);
        String randomNumberString = "";
        int numRandomNumbers = 64 * 8 * 2 + castlingRights.length + 9 + 1;

        for (int i = 0; i < numRandomNumbers; i++) {
            randomNumberString += RandomUnsigned64BitNumber();
            if (i != numRandomNumbers - 1) {
                randomNumberString += ',';
            }
        }
        var writer = new PrintWriter(randomNumbersPath());
        writer.write(randomNumberString);
        writer.close();
    }

    static Queue<Long> ReadRandomNumbers () throws IOException {
        Path path = Paths.get(randomNumbersPath());
        if (!java.nio.file.Files.exists(path)) {
            WriteRandomNumbers();
        }

        Queue<Long> randomNumbers = new LinkedList<Long>();

        var reader = new BufferedReader(new FileReader(randomNumbersPath()));
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
            randomNumbers = ReadRandomNumbers();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            for (int pieceIndex = 0; pieceIndex < 8; pieceIndex++) {
                piecesArray[pieceIndex][Board.WhiteIndex][squareIndex] = randomNumbers.poll();
                piecesArray[pieceIndex][Board.BlackIndex][squareIndex] = randomNumbers.poll();
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

    /// Calculate zobrist key from current board position. This should only be used
    /// after setting board from fen; during search the key should be updated
    /// incrementally.
    public static long CalculateZobristKey (Board board) {
        long zobristKey = 0;

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            if (board.Square[squareIndex] != 0) {
                int pieceType = Piece.PieceType (board.Square[squareIndex]);
                int pieceColour = Piece.Colour (board.Square[squareIndex]);

                zobristKey ^= piecesArray[pieceType][(pieceColour == Piece.White) ? Board.WhiteIndex : Board.BlackIndex][squareIndex];
            }
        }

        int epIndex = (int) (board.currentGameState >> 4) & 15;
        if (epIndex != -1) {
            zobristKey ^= enPassantFile[epIndex];
        }

        if (board.ColourToMove == Piece.Black) {
            zobristKey ^= sideToMove;
        }

        zobristKey ^= castlingRights[board.currentGameState & 0b1111];

        return zobristKey;
    }

    // getter
    static String randomNumbersPath() {
        return Paths.get(randomNumbersFileName).toString();
    }

    static long RandomUnsigned64BitNumber() {
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