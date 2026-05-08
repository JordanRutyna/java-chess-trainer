package ui;

import app.GameController;
import app.HumanPlayer;
import core.GameState;
import core.MoveEncoder;
import core.MoveGenerator;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import openings.OpeningBook;
import openings.OpeningLibrary;
import openings.OpeningNode;
import openings.OpeningTrainer;

public class MainWindow extends JFrame implements GameController.GameListener {

    private final BoardPanel boardPanel;
    private final InfoPanel infoPanel;
    private final PieceRenderer renderer;
    private GameController controller;
    private HumanPlayer whitePlayer;
    private HumanPlayer blackPlayer;
    private OpeningBook openingBook;
    private OpeningTrainer openingTrainer;
    private OpeningLibrary openingLibrary;

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
        // Preserve book selection across resets
        String selectedBook = infoPanel.getSelectedBook();

        if (controller != null) controller.stopGame();

        whitePlayer = new HumanPlayer();
        blackPlayer = new HumanPlayer();

        controller = new GameController(whitePlayer, blackPlayer);
        controller.setListener(this);

        infoPanel.clearSaveFields();
        infoPanel.setOnSave(this::saveCurrentLine);
        infoPanel.setOnReset(this::resetBoard);

        boardPanel.setPlayers(whitePlayer, blackPlayer);
        boardPanel.updateState(controller.getCurrentState());
        boardPanel.clearCheck();
        boardPanel.clearLastMove();
        // boardPanel.setFlipped(!infoPanel.isPracticingAsWhite());
        infoPanel.reset();

        openingLibrary = new OpeningLibrary(getDataDir());
        openingTrainer = new OpeningTrainer(openingLibrary);
        infoPanel.setBooks(openingLibrary.getBookNames());
        // Restore book selection
        if (selectedBook != null && !selectedBook.isEmpty()) {
            infoPanel.setSelectedBook(selectedBook);
            openingTrainer.setActiveBook(selectedBook);
        }
        infoPanel.setOnBookChanged(()
                -> openingTrainer.setActiveBook(infoPanel.getSelectedBook()));

        openingTrainer.setUserColor(infoPanel.isPracticingAsWhite());
        openingTrainer.setMatchListener(new OpeningTrainer.MatchListener() {
            @Override
            public void onLineMatched(OpeningNode node) {
                SwingUtilities.invokeLater(()
                        -> infoPanel.setSaveFields(node.name, node.notes));
            }

            @Override
            public void onLineUnmatched(String parentTitle) {
                SwingUtilities.invokeLater(() -> {
                    if (parentTitle != null && !parentTitle.isEmpty()) {
                        infoPanel.setInheritedTitle(parentTitle);
                    } else {
                        infoPanel.clearSaveFields();
                    }
                });
            }
        });

        openingTrainer.setResponseListener(moveAlgebraic
                -> SwingUtilities.invokeLater(() -> playResponseMove(moveAlgebraic)));

        controller.setOpeningTrainer(openingTrainer);

        controller.startGame();

        if (!infoPanel.isPracticingAsWhite()) {
            new javax.swing.Timer(400, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                List<String> candidates = openingLibrary.getCandidateMoves(
                        new ArrayList<>(), infoPanel.getSelectedBook());
                if (!candidates.isEmpty()) {
                    String chosen = candidates.get(new java.util.Random().nextInt(candidates.size()));
                    playResponseMove(chosen);
                }
            }).start();
        }
    }

    private void saveCurrentLine() {
        if (!infoPanel.hasTitleOwned()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a title for this line.",
                    "Title required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String bookName = infoPanel.getSaveBook();
        if (bookName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select or create a book to save to.\n"
                    + "Click the + button next to Book to create one.",
                    "No book selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        openingTrainer.saveCurrentLine(
                infoPanel.getSaveTitle(),
                infoPanel.getSaveNotes(),
                bookName);
        infoPanel.setBooks(openingLibrary.getBookNames());
        JOptionPane.showMessageDialog(this,
                "Line saved to " + bookName + ".", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetBoard() {
        startNewGame();
    }

    private void playResponseMove(String algebraic) {
        new javax.swing.Timer(400, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            GameState gs = controller.getCurrentState();
            List<Integer> legal = MoveGenerator.generateLegalMoves(gs);
            for (int move : legal) {
                if (MoveEncoder.toAlgebraic(move).equals(algebraic)) {
                    if (gs.isWhiteToMove()) {
                        whitePlayer.submitMove(move);
                    } else {
                        blackPlayer.submitMove(move);
                    }
                    return;
                }
            }
        }).start();
    }

    private String getDataPath(String filename) {
        // Resolve relative to the project root (one level up from src or out)
        String workingDir = System.getProperty("user.dir");
        java.io.File dataDir = new java.io.File(workingDir, "data");
        dataDir.mkdirs();
        return new java.io.File(dataDir, filename).getAbsolutePath();
    }

    private String getDataDir() {
        String workingDir = System.getProperty("user.dir");
        File dataDir = new File(workingDir, "data");
        dataDir.mkdirs();
        return dataDir.getAbsolutePath();
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
