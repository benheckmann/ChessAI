package core.ai;

import core.*;

public class AISettings {
    // public event System.Action requestAbortSearch;

    public int depth;
    public boolean useIterativeDeepening;
    public boolean useTranspositionTable;

    public boolean useThreading;
    public boolean useFixedDepthSearch;
    public int searchTimeMillis = 1000;
    public boolean endlessSearchMode;
    public boolean clearTTEachMove;

    public boolean useBook;
    // public TextAsset book;
    public int maxBookPly = 10;
    
    public MoveGenerator.PromotionMode promotionsToSearch;

    public Search.SearchDiagnostics diagnostics;

    public void RequestAbortSearch () {
        // requestAbortSearch?.Invoke ();
    }
}
