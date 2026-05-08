package openings;

import app.PgnUtil;
import core.GameState;
import core.MoveEncoder;
import core.MoveGenerator;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class OpeningBook {

    private final String filePath;
    private final String name;
    private final OpeningNode root;

    public OpeningBook(String filePath) {
        this.filePath = filePath;
        this.name     = extractName(filePath);
        this.root     = new OpeningNode("", "", "");
        load();
    }

    public String getName() { return name; }
    public OpeningNode getRoot() { return root; }

    public OpeningNode findNode(List<String> sanMoves) {
        OpeningNode current = root;
        for (String san : sanMoves) {
            current = current.findChildBySan(san);
            if (current == null) return null;
        }
        return current;
    }

    public void saveLine(List<String> sanMoves, String name, String notes) {
        OpeningNode current = root;
        for (String san : sanMoves) {
            current = current.addChild(san);
        }
        current.name  = name;
        current.notes = notes;
        save();
    }

    public List<String[]> getChildrenAsCoordinates(List<String> sanPath) {
        OpeningNode node = findNode(sanPath);
        if (node == null) return Collections.emptyList();

        GameState gs = GameState.newGame();
        for (String san : sanPath) {
            List<Integer> legal = MoveGenerator.generateLegalMoves(gs);
            for (int move : legal) {
                if (PgnUtil.toSan(move, gs).equals(san)) {
                    MoveGenerator.applyMove(gs, move);
                    break;
                }
            }
        }

        List<String[]> result = new ArrayList<>();
        for (OpeningNode child : node.children) {
            List<Integer> legal = MoveGenerator.generateLegalMoves(gs);
            for (int move : legal) {
                if (PgnUtil.toSan(move, gs).equals(child.move)) {
                    result.add(new String[]{
                        MoveEncoder.toAlgebraic(move),
                        child.move
                    });
                    break;
                }
            }
        }
        return result;
    }

    private static String extractName(String filePath) {
        String fileName = new File(filePath).getName();
        int dot = fileName.lastIndexOf('.');
        return dot != -1 ? fileName.substring(0, dot) : fileName;
    }

    private void load() {
        File f = new File(filePath);
        if (!f.exists()) return;
        try {
            String raw = new String(Files.readAllBytes(f.toPath())).trim();
            if (raw.isEmpty()) return;
            raw = raw.substring(1, raw.lastIndexOf(']')).trim();
            if (raw.isEmpty()) return;
            for (String obj : splitObjects(raw)) {
                OpeningNode child = parseNode(obj.trim());
                if (child != null) root.children.add(child);
            }
        } catch (IOException e) {
            System.err.println("Failed to load openings: " + e.getMessage());
        }
    }

    private void save() {
        try {
            File f = new File(filePath);
            f.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < root.children.size(); i++) {
                appendNode(sb, root.children.get(i), 1);
                if (i < root.children.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.write(f.toPath(), sb.toString().getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save openings: " + e.getMessage());
        }
    }

    private void appendNode(StringBuilder sb, OpeningNode node, int depth) {
        String indent = "  ".repeat(depth);
        sb.append(indent).append("{\n");
        sb.append(indent).append("  \"move\": \"").append(escape(node.move)).append("\",\n");
        sb.append(indent).append("  \"name\": \"").append(escape(node.name)).append("\",\n");
        sb.append(indent).append("  \"notes\": \"").append(escape(node.notes)).append("\",\n");
        sb.append(indent).append("  \"children\": [");
        if (node.children.isEmpty()) {
            sb.append("]");
        } else {
            sb.append("\n");
            for (int i = 0; i < node.children.size(); i++) {
                appendNode(sb, node.children.get(i), depth + 1);
                if (i < node.children.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(indent).append("  ]");
        }
        sb.append("\n").append(indent).append("}");
    }

    private OpeningNode parseNode(String obj) {
        String move  = extractString(obj, "move");
        String name  = extractString(obj, "name");
        String notes = extractString(obj, "notes");
        if (move == null) return null;
        OpeningNode node = new OpeningNode(move,
            name  != null ? name  : "",
            notes != null ? notes : "");

        int childrenStart = obj.indexOf("\"children\"");
        if (childrenStart != -1) {
            int open  = obj.indexOf('[', childrenStart);
            int close = findMatchingBracket(obj, open);
            if (open != -1 && close != -1) {
                String body = obj.substring(open + 1, close).trim();
                if (!body.isEmpty()) {
                    for (String childObj : splitObjects(body)) {
                        OpeningNode child = parseNode(childObj.trim());
                        if (child != null) node.children.add(child);
                    }
                }
            }
        }
        return node;
    }

    private int findMatchingBracket(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            if      (s.charAt(i) == '[') depth++;
            else if (s.charAt(i) == ']') { if (--depth == 0) return i; }
        }
        return -1;
    }

    private List<String> splitObjects(String body) {
        List<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if      (c == '{') { if (depth++ == 0) start = i; }
            else if (c == '}') { if (--depth == 0) objects.add(body.substring(start, i + 1)); }
        }
        return objects;
    }

    private String extractString(String obj, String key) {
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx == -1) return null;
        int colon = obj.indexOf(':', idx + search.length());
        int q1    = obj.indexOf('"', colon + 1);
        int q2    = obj.indexOf('"', q1 + 1);
        if (q1 == -1 || q2 == -1) return null;
        return obj.substring(q1 + 1, q2);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}