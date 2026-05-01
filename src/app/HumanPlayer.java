package app;

import core.GameState;
import java.util.concurrent.LinkedBlockingQueue;

public class HumanPlayer implements Player {

    private final LinkedBlockingQueue<Integer> moveQueue = new LinkedBlockingQueue<>();

    // Called by the UI when the user clicks a valid move
    public void submitMove(int move) {
        moveQueue.offer(move);
    }

    @Override
    public int getMove(GameState gs) {
        try {
            return moveQueue.take(); // blocks until UI submits a move
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
