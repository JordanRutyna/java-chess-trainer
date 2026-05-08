package ui;

import app.HumanPlayer;
import core.BitBoard;
import core.GameState;
import core.MoveEncoder;
import core.MoveGenerator;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class BoardPanel extends JPanel {

    private static final int TILE_SIZE = 80;

    private static final Color LIGHT = new Color(240, 217, 181);
    private static final Color DARK = new Color(181, 136, 99);
    private static final Color HIGHLIGHT = new Color(106, 135, 75, 180);
    private static final Color LEGAL_DOT = new Color(0, 0, 0, 60);
    private static final Color LEGAL_CAP = new Color(106, 135, 75, 120);
    private static final Color CHECK_RED = new Color(220, 50, 50, 160);
    private static final Color LAST_MOVE = new Color(205, 210, 106, 160);

    private GameState gameState;
    private final PieceRenderer renderer;
    private HumanPlayer whitePlayer;
    private HumanPlayer blackPlayer;

    // Selection and drag state
    private int selectedSquare = -1;
    private int dragSquare = -1;   // square being dragged from
    private int dragX = -1;
    private int dragY = -1;
    private boolean isDragging = false;
    private List<Integer> legalMovesForSelected = new ArrayList<>();

    // Highlight state
    private int checkedKingSquare = -1;
    private int lastMoveFrom = -1;
    private int lastMoveTo = -1;

    private boolean flipped = false;

    public BoardPanel(PieceRenderer renderer) {
        this.renderer = renderer;
        setPreferredSize(new Dimension(TILE_SIZE * 8, TILE_SIZE * 8));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePress(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleDrag(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleRelease(e.getX(), e.getY());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Handled by press/release, nothing needed here
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setPlayers(HumanPlayer white, HumanPlayer black) {
        this.whitePlayer = white;
        this.blackPlayer = black;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public void updateState(GameState gs) {
        this.gameState = gs;
        repaint();
    }

    public void setCheckedKing(int square) {
        this.checkedKingSquare = square;
        repaint();
    }

    public void clearCheck() {
        this.checkedKingSquare = -1;
    }

    public void setLastMove(int from, int to) {
        this.lastMoveFrom = from;
        this.lastMoveTo = to;
    }

    public void clearLastMove() {
        this.lastMoveFrom = -1;
        this.lastMoveTo = -1;
    }

    private int pixelToSquare(int x, int y) {
        int file = x / TILE_SIZE;
        int rank = 7 - (y / TILE_SIZE);
        if (flipped) {
            file = 7 - file;
            rank = 7 - rank;
        }
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            return -1;
        }
        return rank * 8 + file;
    }

    private Point squareToPixel(int square) {
        int file = BitBoard.fileOf(square);
        int rank = BitBoard.rankOf(square);
        if (flipped) {
            file = 7 - file;
            rank = 7 - rank;
        }
        return new Point(file * TILE_SIZE, (7 - rank) * TILE_SIZE);
    }

    private void handlePress(int x, int y) {
        if (gameState == null) {
            return;
        }
        int square = pixelToSquare(x, y);
        if (square == -1) {
            return;
        }

        long squareBit = 1L << square;
        boolean isOwn = gameState.isWhiteToMove()
                ? (gameState.board.whitePieces & squareBit) != 0
                : (gameState.board.blackPieces & squareBit) != 0;

        // If clicking a legal move destination while something is selected, move there
        if (selectedSquare != -1 && !isOwn) {
            for (int move : legalMovesForSelected) {
                if (MoveEncoder.getTo(move) == square) {
                    submitMove(move);
                    selectedSquare = -1;
                    legalMovesForSelected.clear();
                    isDragging = false;
                    repaint();
                    return;
                }
            }
        }

        if (isOwn) {
            selectedSquare = square;
            dragSquare = square;
            dragX = x;
            dragY = y;
            isDragging = false; // not dragging yet, just pressed

            List<Integer> allLegal = MoveGenerator.generateLegalMoves(gameState);
            legalMovesForSelected = new ArrayList<>();
            for (int move : allLegal) {
                if (MoveEncoder.getFrom(move) == square) {
                    legalMovesForSelected.add(move);
                }
            }
            repaint();
        } else {
            selectedSquare = -1;
            legalMovesForSelected.clear();
            isDragging = false;
            repaint();
        }
    }

    private void handleDrag(int x, int y) {
        if (dragSquare == -1) {
            return;
        }
        isDragging = true;
        dragX = x;
        dragY = y;
        repaint();
    }

    private void handleRelease(int x, int y) {
        if (gameState == null || dragSquare == -1) {
            return;
        }

        if (isDragging) {
            int targetSquare = pixelToSquare(x, y);
            boolean moved = false;
            if (targetSquare != -1 && targetSquare != dragSquare) {
                for (int move : legalMovesForSelected) {
                    if (MoveEncoder.getTo(move) == targetSquare) {
                        submitMove(move);
                        selectedSquare = -1;
                        legalMovesForSelected.clear();
                        moved = true;
                        break;
                    }
                }
            }
            // If dropped on an invalid square, piece snaps back but selection stays
            isDragging = false;
            dragSquare = -1;
            if (!moved) {
                // Keep selection and highlights, piece snapped back
                dragSquare = -1;
            }
        }
        // If it was just a click (not a drag), selection was already handled in press
        repaint();
    }

    private void submitMove(int move) {
        if (MoveEncoder.isPromotion(move)) {
            move = handlePromotion(move);
        }
        if (gameState.isWhiteToMove()) {
            whitePlayer.submitMove(move);
        } else {
            blackPlayer.submitMove(move);
        }
    }

    private int handlePromotion(int move) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
                this, "Promote to:", "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (choice < 0) {
            choice = 0;
        }
        int[] promoPieces = {
            MoveEncoder.PROMO_QUEEN, MoveEncoder.PROMO_ROOK,
            MoveEncoder.PROMO_BISHOP, MoveEncoder.PROMO_KNIGHT
        };
        int flag = MoveEncoder.getFlag(move) == MoveEncoder.PROMO_CAPTURE
                ? MoveEncoder.PROMO_CAPTURE : MoveEncoder.PROMOTION;
        return MoveEncoder.encodePromotion(
                MoveEncoder.getFrom(move), MoveEncoder.getTo(move),
                flag, promoPieces[choice]);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gameState == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSquares(g2);
        drawLastMoveHighlight(g2);
        drawSelectionHighlight(g2);
        drawCheckHighlight(g2);
        drawLegalMoveIndicators(g2);
        drawPieces(g2);          // draws all pieces except the one being dragged
        drawDraggedPiece(g2);    // drawn last so it floats on top
    }

    private void drawSquares(Graphics2D g) {
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                boolean isLight = (rank + file) % 2 != 0;
                g.setColor(isLight ? LIGHT : DARK);
                int drawFile = flipped ? 7 - file : file;
                int drawRank = flipped ? rank : 7 - rank;
                g.fillRect(drawFile * TILE_SIZE, drawRank * TILE_SIZE,
                        TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private void drawLastMoveHighlight(Graphics2D g) {
        g.setColor(LAST_MOVE);
        if (lastMoveFrom != -1) {
            g.fillRect(drawX(lastMoveFrom), drawY(lastMoveFrom), TILE_SIZE, TILE_SIZE);
        }
        if (lastMoveTo != -1) {
            g.fillRect(drawX(lastMoveTo), drawY(lastMoveTo), TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawSelectionHighlight(Graphics2D g) {
        if (selectedSquare != -1) {
            g.setColor(HIGHLIGHT);
            g.fillRect(drawX(selectedSquare), drawY(selectedSquare), TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawCheckHighlight(Graphics2D g) {
        if (checkedKingSquare != -1) {
            g.setColor(CHECK_RED);
            g.fillRect(drawX(checkedKingSquare), drawY(checkedKingSquare), TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawLegalMoveIndicators(Graphics2D g) {
        for (int move : legalMovesForSelected) {
            int to = MoveEncoder.getTo(move);
            int x = drawX(to);
            int y = drawY(to);
            boolean isCapture = (gameState.board.allPieces & (1L << to)) != 0;
            if (isCapture) {
                g.setColor(LEGAL_CAP);
                g.setStroke(new BasicStroke(6));
                g.drawRect(x + 3, y + 3, TILE_SIZE - 6, TILE_SIZE - 6);
            } else {
                g.setColor(LEGAL_DOT);
                int dotSize = TILE_SIZE / 3;
                g.fillOval(x + (TILE_SIZE - dotSize) / 2,
                        y + (TILE_SIZE - dotSize) / 2,
                        dotSize, dotSize);
            }
        }
    }

    private int drawX(int square) {
        int file = BitBoard.fileOf(square);
        return (flipped ? 7 - file : file) * TILE_SIZE;
    }

    private int drawY(int square) {
        int rank = BitBoard.rankOf(square);
        return (flipped ? rank : 7 - rank) * TILE_SIZE;
    }

    private void drawPieces(Graphics2D g) {
        BitBoard b = gameState.board;
        drawPieceType(g, b.whitePawns, "w", "p");
        drawPieceType(g, b.whiteKnights, "w", "n");
        drawPieceType(g, b.whiteBishops, "w", "b");
        drawPieceType(g, b.whiteRooks, "w", "r");
        drawPieceType(g, b.whiteQueens, "w", "q");
        drawPieceType(g, b.whiteKing, "w", "k");
        drawPieceType(g, b.blackPawns, "b", "p");
        drawPieceType(g, b.blackKnights, "b", "n");
        drawPieceType(g, b.blackBishops, "b", "b");
        drawPieceType(g, b.blackRooks, "b", "r");
        drawPieceType(g, b.blackQueens, "b", "q");
        drawPieceType(g, b.blackKing, "b", "k");
    }

    private void drawPieceType(Graphics2D g, long board, String color, String piece) {
        while (board != 0) {
            int sq = Long.numberOfTrailingZeros(board);
            board &= board - 1;
            if (isDragging && sq == dragSquare) {
                continue;
            }
            var img = renderer.getImage(color, piece);
            if (img != null) {
                g.drawImage(img, drawX(sq), drawY(sq), TILE_SIZE, TILE_SIZE, null);
            }
        }
    }

    private void drawDraggedPiece(Graphics2D g) {
        if (!isDragging || dragSquare == -1 || gameState == null) {
            return;
        }

        String[] colors = {"w", "b"};
        String[] types = {"p", "n", "b", "r", "q", "k"};
        long[][] boards = {
            {gameState.board.whitePawns, gameState.board.whiteKnights,
                gameState.board.whiteBishops, gameState.board.whiteRooks,
                gameState.board.whiteQueens, gameState.board.whiteKing},
            {gameState.board.blackPawns, gameState.board.blackKnights,
                gameState.board.blackBishops, gameState.board.blackRooks,
                gameState.board.blackQueens, gameState.board.blackKing}
        };

        long dragBit = 1L << dragSquare;
        for (int c = 0; c < 2; c++) {
            for (int t = 0; t < 6; t++) {
                if ((boards[c][t] & dragBit) != 0) {
                    BufferedImage img = renderer.getImage(colors[c], types[t]);
                    if (img != null) {
                        // Center the piece on the cursor
                        g.drawImage(img,
                                dragX - TILE_SIZE / 2,
                                dragY - TILE_SIZE / 2,
                                TILE_SIZE, TILE_SIZE, null);
                    }
                    return;
                }
            }
        }
    }
}
