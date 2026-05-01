package openings;

import core.MoveEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpeningTrainer {

    public interface MatchListener {
        void onLineMatched(OpeningLine line);
        void onLineUnmatched();
    }

    private final OpeningBook book;
    private final List<String> currentMoves = new ArrayList<>();
    private MatchListener matchListener;

    public OpeningTrainer(OpeningBook book) {
        this.book = book;
    }

    public void setMatchListener(MatchListener listener) {
        this.matchListener = listener;
    }

    public void recordMove(int move) {
        currentMoves.add(MoveEncoder.toAlgebraic(move));
        checkForMatch();
    }

    public void undoLastMove() {
        if (!currentMoves.isEmpty()) {
            currentMoves.remove(currentMoves.size() - 1);
            checkForMatch();
        }
    }

    public void reset() {
        currentMoves.clear();
        if (matchListener != null) {
            matchListener.onLineUnmatched();
        }
    }

    public void saveCurrentLine(String name, String notes) {
        List<OpeningLine> lines = new ArrayList<>(book.getLines());
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).moves.equals(currentMoves)) {
                book.updateLine(i, new OpeningLine(name, notes, new ArrayList<>(currentMoves)));
                return;
            }
        }
        // No existing line with these moves, add a new one
        book.addLine(new OpeningLine(name, notes, new ArrayList<>(currentMoves)));
    }

    public List<String> getCurrentMoves() {
        return Collections.unmodifiableList(currentMoves);
    }

    public OpeningBook getBook() {
        return book;
    }

    // Check if current move sequence exactly matches any saved line
    private void checkForMatch() {
        if (matchListener == null) {
            return;
        }
        for (OpeningLine line : book.getLines()) {
            if (line.moves.equals(currentMoves)) {
                matchListener.onLineMatched(line);
                return;
            }
        }
        matchListener.onLineUnmatched();
    }
}
