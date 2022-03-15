package core.ai;

import core.*;

public class AIPlayer extends Player {

    Search search;
    boolean moveFound;
    Move move;
    Board board;

    public AIPlayer(GameManager gm, Board board) {
        super(gm, board);
        search = new Search(board, this);
    }

    @Override
    public void update() {
        if (moveFound) {
            moveFound = false;
            System.out.println("AI chose move: " + move);
            choseMove(move);
        }
    }

    @Override
    public void notifyTurnToMove() {
        search.StartSearch();
        moveFound = true;
    }

    void OnSearchComplete(Move move) {
        try {
            Thread.sleep(Search.DELAY_PER_MOVE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        moveFound = true;
        this.move = move;
        update();
    }
}
