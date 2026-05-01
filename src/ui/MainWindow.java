package ui;

import app.GameController;
import app.HumanPlayer;
import core.GameState;
import core.MoveEncoder;
import java.awt.*;
import javax.swing.*;
import openings.OpeningBook;
import openings.OpeningLine;
import openings.OpeningTrainer;

public class MainWindow extends JFrame implements GameController.GameListener {

    private final BoardPanel boardPanel;
    private final InfoPanel infoPanel;
    private final PieceRenderer renderer;
    private GameController controller;
    
    private OpeningTrainer openingTrainer;
    private OpeningBook openingBook;

    public MainWindow() {
        super("Chess");
        renderer = new PieceRenderer();
        boardPanel = new BoardPanel(renderer);
        infoPanel = new InfoPanel();

        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        startNewGame();
    }

    private void startNewGame() {
        if (controller != null) {
            controller.stopGame();
        }

        HumanPlayer white = new HumanPlayer();
        HumanPlayer black = new HumanPlayer();

        controller = new GameController(white, black);
        controller.setListener(this);
        infoPanel.setOnSave(this::saveCurrentLine);

        boardPanel.setPlayers(white, black);
        boardPanel.updateState(controller.getCurrentState());
        boardPanel.clearCheck();
        infoPanel.reset();

        openingBook = new OpeningBook(getDataPath("openings.json"));
        openingTrainer = new OpeningTrainer(openingBook);
        openingTrainer.setMatchListener(new OpeningTrainer.MatchListener() {
            @Override
            public void onLineMatched(OpeningLine line) {
                SwingUtilities.invokeLater(() -> {
                    infoPanel.setSaveFields(line.name, line.notes);
                });
            }

            @Override
            public void onLineUnmatched() {
                SwingUtilities.invokeLater(() -> {
                    infoPanel.clearSaveFields();
                });
            }
        });
        controller.setOpeningTrainer(openingTrainer);

        controller.startGame();
    }

    private void saveCurrentLine() {
        String title = infoPanel.getSaveTitle();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a title for this line.",
                    "Title required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String notes = infoPanel.getSaveNotes();
        openingTrainer.saveCurrentLine(title, notes);
        JOptionPane.showMessageDialog(this,
                "Line saved to repertoire.", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String getDataPath(String filename) {
        // Resolve relative to the project root (one level up from src or out)
        String workingDir = System.getProperty("user.dir");
        java.io.File dataDir = new java.io.File(workingDir, "data");
        dataDir.mkdirs();
        return new java.io.File(dataDir, filename).getAbsolutePath();
    }

    // GameListener callbacks (called from background thread, must dispatch to EDT)
    @Override
    public void onMoveMade(GameState gs, int move, String san) {
        SwingUtilities.invokeLater(() -> {
            boardPanel.clearCheck();
            if (move != -1) {
                boardPanel.setLastMove(MoveEncoder.getFrom(move), MoveEncoder.getTo(move));
                infoPanel.addMove(san);
            } else {
                boardPanel.clearLastMove();
            }
            boardPanel.updateState(gs);
            infoPanel.setStatus(gs.isWhiteToMove() ? "White to move" : "Black to move");
        });
    }

    @Override
    public void onCheckmate(int winningSide) {
        SwingUtilities.invokeLater(() -> {
            String winner = winningSide == GameState.WHITE ? "White" : "Black";
            infoPanel.setStatus("Checkmate! " + winner + " wins.");
            JOptionPane.showMessageDialog(this,
                    winner + " wins by checkmate!", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void onStalemate() {
        SwingUtilities.invokeLater(() -> {
            infoPanel.setStatus("Stalemate! Draw.");
            JOptionPane.showMessageDialog(this,
                    "Stalemate! The game is a draw.", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void onCheck(int sideInCheck) {
        SwingUtilities.invokeLater(() -> {
            GameState gs = controller.getCurrentState();
            long kingBoard = sideInCheck == GameState.WHITE
                    ? gs.board.whiteKing : gs.board.blackKing;
            int kingSquare = Long.numberOfTrailingZeros(kingBoard);
            boardPanel.setCheckedKing(kingSquare);
            infoPanel.setStatus((sideInCheck == GameState.WHITE ? "White" : "Black") + " is in check!");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }
}
