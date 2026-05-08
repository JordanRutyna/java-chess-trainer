package openings;

import java.util.*;

public class OpeningTrainer {

    public interface MatchListener {
        void onLineMatched(OpeningNode node);
        void onLineUnmatched(String parentTitle);
    }

    public interface ResponseListener {
        void onResponseAvailable(String moveAlgebraic);
    }

    private final OpeningLibrary library;
    private final List<String> currentSans = new ArrayList<>();
    private String activeBook    = "All";
    private boolean userIsWhite  = true;
    private MatchListener matchListener;
    private ResponseListener responseListener;
    private final Random random  = new Random();

    public OpeningTrainer(OpeningLibrary library) {
        this.library = library;
    }

    public void setMatchListener(MatchListener l)       { this.matchListener    = l; }
    public void setResponseListener(ResponseListener l) { this.responseListener = l; }
    public void setUserColor(boolean userIsWhite)       { this.userIsWhite       = userIsWhite; }
    public void setActiveBook(String bookName)          { this.activeBook        = bookName; }

    public void recordMove(int move, String san) {
        currentSans.add(san);
        notifyMatch();
        checkForResponse();
    }

    public void undoLastMove() {
        if (!currentSans.isEmpty()) {
            currentSans.remove(currentSans.size() - 1);
            notifyMatch();
        }
    }

    public void reset() {
        currentSans.clear();
        if (matchListener != null) matchListener.onLineUnmatched(null);
    }

    public void saveCurrentLine(String name, String notes, String bookName) {
        OpeningBook book = library.getBook(bookName);
        if (book == null) book = library.createBook(bookName);
        book.saveLine(new ArrayList<>(currentSans), name, notes);
        notifyMatch();
    }

    public List<String> getCurrentSans() {
        return Collections.unmodifiableList(currentSans);
    }

    public OpeningLibrary getLibrary() { return library; }

    private void notifyMatch() {
        if (matchListener == null) return;
        OpeningNode node = library.findNode(currentSans, activeBook);
        if (node != null) {
            matchListener.onLineMatched(node);
        } else {
            matchListener.onLineUnmatched(
                library.findParentTitle(currentSans, activeBook));
        }
    }

    private void checkForResponse() {
        if (responseListener == null) return;

        boolean blackToMove = currentSans.size() % 2 != 0;
        if (userIsWhite  && !blackToMove) return;
        if (!userIsWhite && blackToMove)  return;

        List<String> candidates = library.getCandidateMoves(currentSans, activeBook);
        if (candidates.isEmpty()) return;

        String chosen = candidates.get(random.nextInt(candidates.size()));
        responseListener.onResponseAvailable(chosen);
    }
}