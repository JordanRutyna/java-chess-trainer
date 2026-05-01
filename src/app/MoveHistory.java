package app;

import core.GameState;
import java.util.ArrayDeque;
import java.util.Deque;

public class MoveHistory {

    private final Deque<GameState> history = new ArrayDeque<>();

    public void push(GameState gs) {
        history.push(gs.copy());
    }

    public GameState pop() {
        return history.isEmpty() ? null : history.pop();
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public void clear() {
        history.clear();
    }

    public int size() {
        return history.size();
    }
}
