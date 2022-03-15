package core.ai;

import core.*;

// src: https://web.archive.org/web/20071031100051/http://www.brucemo.com/compchess/programming/hashing.htm
public class TranspositionTable {

    public static final int lookupFailed = Integer.MIN_VALUE;

    // The value for this position is the exact evaluation
    public static final int Exact = 0;
    // A move was found during the search that was too good, meaning the opponent
    // will play a different move earlier on, not allowing the position where this
    // move was available to be reached. Because the search cuts off at this point
    // (beta cut-off), an even better move may exist. This means that the evaluation
    // for the position could be even higher, making the stored value the lower
    // bound of the actual value.
    public static final int LowerBound = 1;
    // No move during the search resulted in a position that was better than the
    // current player could get from playing a different move in an earlier position
    // (i.e eval was <= alpha for all moves in the position). Due to the way
    // alpha-beta search works, the value we get here won't be the exact evaluation
    // of the position, but rather the upper bound of the evaluation. This means
    // that the evaluation is, at most, equal to this value.
    public static final int UpperBound = 2;

    public Entry[] entries;
    public final long size;
    public boolean enabled = true;
    Board board;

    public TranspositionTable(Board board, int size) {
        this.board = board;
        this.size = (long) size;

        entries = new Entry[size];
    }

    public void Clear() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry();
        }
    }

    public long Index() {
        long mod = board.ZobristKey % size;
        return mod < 0 ? mod + size : mod;
    }

    public Move GetStoredMove() {
        return entries[(int) Index()].move;
    }

    public int LookupEvaluation(int depth, int plyFromRoot, int alpha, int beta) {
        if (!enabled) {
            return lookupFailed;
        }
        Entry entry = entries[(int) Index()];

        if (entry.key == board.ZobristKey) {
            // Only use stored evaluation if it has been searched to at least the same depth
            // as would be searched now
            if (entry.depth >= depth) {
                int correctedScore = CorrectRetrievedMateScore(entry.value, plyFromRoot);
                // We have stored the exact evaluation for this position, so return it
                if (entry.nodeType == Exact) {
                    return correctedScore;
                }
                // We have stored the upper bound of the eval for this position. If it's less
                // than alpha then we don't need to
                // search the moves in this position as they won't interest us; otherwise we
                // will have to search to find the exact value
                if (entry.nodeType == UpperBound && correctedScore <= alpha) {
                    return correctedScore;
                }
                // We have stored the lower bound of the eval for this position. Only return if
                // it causes a beta cut-off.
                if (entry.nodeType == LowerBound && correctedScore >= beta) {
                    return correctedScore;
                }
            }
        }
        return lookupFailed;
    }

    public void StoreEvaluation(int depth, int numPlySearched, int eval, int evalType, Move move) {
        if (!enabled) {
            return;
        }
        Entry entry = new Entry(board.ZobristKey, CorrectMateScoreForStorage(eval, numPlySearched), (byte) depth,
                (byte) evalType, move);
        entries[(int) Index()] = entry;
    }

    int CorrectMateScoreForStorage(int score, int numPlySearched) {
        if (Search.IsMateScore(score)) {
            int sign = (int) Math.signum(score);
            return (score * sign + numPlySearched) * sign;
        }
        return score;
    }

    int CorrectRetrievedMateScore(int score, int numPlySearched) {
        if (Search.IsMateScore(score)) {
            int sign = (int) Math.signum(score);
            return (score * sign - numPlySearched) * sign;
        }
        return score;
    }

    public static class Entry {
        public final long key;
        public final int value;
        public final Move move;
        public final byte depth;
        public final byte nodeType;

        public Entry() {
            key = 0;
            value = 0;
            move = null;
            depth = 0;
            nodeType = 0;
        }

        public Entry(long key, int value, byte depth, byte nodeType, Move move) {
            this.key = key;
            this.value = value;
            this.depth = depth; // depth is how many ply were searched ahead from this position
            this.nodeType = nodeType;
            this.move = move;
        }
        
        public static int GetSize() {
            return 8 + 4 + 4 + 1 + 1;
        }
    }
}