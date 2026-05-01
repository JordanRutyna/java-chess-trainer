package ui;

import app.GameController;
import app.HumanPlayer;
import core.GameState;
import core.MoveEncoder;
import core.MoveGenerator;

import openings.OpeningBook;
import openings.OpeningTrainer;

import javax.swing.*;
import java.awt.*;

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

        openingBook = new OpeningBook("../data/openings.json");
        openingTrainer = new OpeningTrainer(openingBook);
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
        infoPanel.clearSaveFields();
        JOptionPane.showMessageDialog(this,
                "Line saved to repertoire.", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // GameListener callbacks (called from background thread, must dispatch to EDT)
    @Override
    public void onMoveMade(GameState gs, int move) {
        SwingUtilities.invokeLater(() -> {
            boardPanel.clearCheck();
            boardPanel.updateState(gs);
            if (move != -1) {
                infoPanel.addMove(MoveEncoder.toAlgebraic(move));
            }
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
