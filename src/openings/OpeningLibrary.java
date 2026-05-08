package openings;

import java.io.*;
import java.util.*;

public class OpeningLibrary {

    private final String dataDir;
    private final Map<String, OpeningBook> books = new LinkedHashMap<>();

    public OpeningLibrary(String dataDir) {
        this.dataDir = dataDir;
        loadAll();
    }

    // Returns all book names plus "All"
    public List<String> getBookNames() {
        List<String> names = new ArrayList<>();
        names.add("All");
        names.addAll(books.keySet());
        return names;
    }

    public OpeningBook getBook(String name) {
        return books.get(name);
    }

    // Create a new book (new JSON file) and add it to the library
    public OpeningBook createBook(String name) {
        if (books.containsKey(name)) {
            return books.get(name);
        }
        String path = dataDir + File.separator + name + ".json";
        OpeningBook book = new OpeningBook(path);
        books.put(name, book);
        return book;
    }

    // Get candidate response moves across all books or a specific book
    // Returns list of coordinate move strings
    public List<String> getCandidateMoves(List<String> sanPath,
            String activeBook) {
        List<String> candidates = new ArrayList<>();
        Collection<OpeningBook> toSearch = activeBook.equals("All")
                ? books.values()
                : Collections.singletonList(books.get(activeBook));

        if (toSearch == null) {
            return candidates;
        }

        Set<String> seen = new HashSet<>();
        for (OpeningBook book : toSearch) {
            if (book == null) {
                continue;
            }
            for (String[] entry : book.getChildrenAsCoordinates(sanPath)) {
                if (seen.add(entry[0])) { // deduplicate by coordinate move
                    candidates.add(entry[0]);
                }
            }
        }
        return candidates;
    }

    // Find a node across all books, returning the first match
    public OpeningNode findNode(List<String> sanPath, String activeBook) {
        Collection<OpeningBook> toSearch = activeBook.equals("All")
                ? books.values()
                : Collections.singletonList(books.get(activeBook));

        for (OpeningBook book : toSearch) {
            if (book == null) {
                continue;
            }
            OpeningNode node = book.findNode(sanPath);
            if (node != null && (!node.name.isEmpty() || !node.notes.isEmpty())) {
                return node;
            }
        }
        return null;
    }

    // Find nearest ancestor title across books
    public String findParentTitle(List<String> sanPath, String activeBook) {
        Collection<OpeningBook> toSearch = activeBook.equals("All")
                ? books.values()
                : Collections.singletonList(books.get(activeBook));

        String lastTitle = "";
        for (OpeningBook book : toSearch) {
            if (book == null) {
                continue;
            }
            OpeningNode node = book.getRoot();
            for (String san : sanPath) {
                node = node == null ? null : node.findChildBySan(san);
                if (node != null && !node.name.isEmpty()) {
                    lastTitle = node.name;
                }
            }
        }
        return lastTitle;
    }

    private void loadAll() {
        File dir = new File(dataDir);
        dir.mkdirs();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) {
            return;
        }
        Arrays.sort(files); // consistent ordering
        for (File f : files) {
            OpeningBook book = new OpeningBook(f.getAbsolutePath());
            books.put(book.getName(), book);
        }
    }
}
