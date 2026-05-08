package app;

import core.GameState;
import core.MoveGenerator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import openings.OpeningTrainer;

public class GameController {

    // UI registers a listener to receive game events
    public interface GameListener {
        void onMoveMade(GameState gs, int move, String san);
        void onCheckmate(int winningSide);
        void onStalemate();
        void onCheck(int sideInCheck);
    }

    private GameState currentState;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final MoveHistory history;
    private GameListener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;

    private OpeningTrainer openingTrainer;

    public GameController(Player whitePlayer, Player blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.history = new MoveHistory();
        this.currentState = GameState.newGame();
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    public void startGame() {
        running = true;
        executor.submit(this::gameLoop);
    }

    public void stopGame() {
        running = false;
        executor.shutdownNow();
    }

    private void gameLoop() {
        while (running) {
            List<Integer> legalMoves = MoveGenerator.generateLegalMoves(currentState);

            if (legalMoves.isEmpty()) {
                boolean inCheck = MoveGenerator.isKingInCheck(currentState, currentState.sideToMove);
                if (inCheck) {
                    // The side to move has no moves and is in check: checkmate
                    // The winner is the other side
                    int winner = currentState.sideToMove == GameState.WHITE
                            ? GameState.BLACK : GameState.WHITE;
                    if (listener != null) {
                        listener.onCheckmate(winner);
                    }
                } else {
                    if (listener != null) {
                        listener.onStalemate();
                    }
                }
                running = false;
                return;
            }

            Player current = currentState.isWhiteToMove() ? whitePlayer : blackPlayer;

            // Ask the current player for a move (blocks for human, computes for AI)
            int move = current.getMove(currentState);

            if (move == -1 || !running) {
                return; // interrupted
            }
            // Validate the move is actually legal before applying
            if (!legalMoves.contains(move)) {
                continue;
            }

            // Save state for undo before applying
            history.push(currentState);
            
            // Before applyMove:
            GameState beforeMove = currentState.copy();

            MoveGenerator.applyMove(currentState, move);

            String san = PgnUtil.toSan(move, beforeMove);
            if (openingTrainer != null) openingTrainer.recordMove(move, san);

            if (listener != null) listener.onMoveMade(currentState, move, san);

            // Check if the move put the opponent in check
            List<Integer> opponentMoves = MoveGenerator.generateLegalMoves(currentState);
            boolean opponentInCheck = MoveGenerator.isKingInCheck(currentState, currentState.sideToMove);
            if (opponentInCheck && !opponentMoves.isEmpty()) {
                if (listener != null) {
                    listener.onCheck(currentState.sideToMove);
                }
            }
        }
    }

    // Undo the last move (only meaningful in human vs human or opening trainer)
    public void undoLastMove() {
        GameState previous = history.pop();
        if (previous != null) {
            currentState = previous;
            if (openingTrainer != null) {
                openingTrainer.undoLastMove();
            }
            if (listener != null) {
                listener.onMoveMade(currentState, -1, null);
            }
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public MoveHistory getHistory() {
        return history;
    }

    public void setOpeningTrainer(OpeningTrainer trainer) {
        this.openingTrainer = trainer;
    }
}
