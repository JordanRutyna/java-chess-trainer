package openings;

import core.MoveEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpeningTrainer {

    private final OpeningBook book;
    private final List<String> currentMoves = new ArrayList<>();

    public OpeningTrainer(OpeningBook book) {
        this.book = book;
    }

    // Called by GameController each time a move is made
    public void recordMove(int move) {
        currentMoves.add(MoveEncoder.toAlgebraic(move));
    }

    // Called on undo
    public void undoLastMove() {
        if (!currentMoves.isEmpty()) {
            currentMoves.remove(currentMoves.size() - 1);
        }
    }

    public void reset() {
        currentMoves.clear();
    }

    // Save a snapshot of the current position with a name and notes
    public void saveCurrentLine(String name, String notes) {
        OpeningLine line = new OpeningLine(name, notes, new ArrayList<>(currentMoves));
        book.addLine(line);
    }

    public List<String> getCurrentMoves() {
        return Collections.unmodifiableList(currentMoves);
    }

    public OpeningBook getBook() {
        return book;
    }
}
