package openings;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class OpeningBook {

    private final String filePath;
    private final List<OpeningLine> lines = new ArrayList<>();

    public OpeningBook(String filePath) {
        this.filePath = filePath;
        load();
    }

    public List<OpeningLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void addLine(OpeningLine line) {
        lines.add(line);
        save();
    }

    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            save();
        }
    }

    public void updateLine(int index, OpeningLine line) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, line);
            save();
        }
    }

    private void load() {
        File f = new File(filePath);
        if (!f.exists()) {
            return;
        }
        try {
            String raw = new String(Files.readAllBytes(f.toPath())).trim();
            if (raw.isEmpty() || raw.equals("[]")) {
                return;
            }
            // Strip outer array brackets
            raw = raw.substring(1, raw.lastIndexOf(']')).trim();
            // Split on top-level object boundaries
            for (String obj : splitObjects(raw)) {
                OpeningLine line = parseObject(obj.trim());
                if (line != null) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load openings: " + e.getMessage());
        }
    }

    // Split a JSON array body into individual {...} objects
    private List<String> splitObjects(String body) {
        List<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{') {
                if (depth++ == 0) {
                    start = i;
            
                }} else if (c == '}') {
                if (--depth == 0) {
                    objects.add(body.substring(start, i + 1));
            
                }}
        }
        return objects;
    }

    private OpeningLine parseObject(String obj) {
        String name = extractString(obj, "name");
        String notes = extractString(obj, "notes");
        List<String> moves = extractArray(obj, "moves");
        if (name == null) {
            return null;
        }
        return new OpeningLine(name, notes != null ? notes : "", moves);
    }

    private String extractString(String obj, String key) {
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx == -1) {
            return null;
        }
        int colon = obj.indexOf(':', idx + search.length());
        int q1 = obj.indexOf('"', colon + 1);
        int q2 = obj.indexOf('"', q1 + 1);
        if (q1 == -1 || q2 == -1) {
            return null;
        }
        return obj.substring(q1 + 1, q2);
    }

    private List<String> extractArray(String obj, String key) {
        List<String> result = new ArrayList<>();
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx == -1) {
            return result;
        }
        int open = obj.indexOf('[', idx);
        int close = obj.indexOf(']', open);
        if (open == -1 || close == -1) {
            return result;
        }
        String arrayBody = obj.substring(open + 1, close).trim();
        if (arrayBody.isEmpty()) {
            return result;
        }
        for (String token : arrayBody.split(",")) {
            String t = token.trim().replace("\"", "");
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    private void save() {
        try {
            File f = new File(filePath);
            f.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < lines.size(); i++) {
                OpeningLine line = lines.get(i);
                sb.append("  {\n");
                sb.append("    \"name\": \"").append(escape(line.name)).append("\",\n");
                sb.append("    \"notes\": \"").append(escape(line.notes)).append("\",\n");
                sb.append("    \"moves\": [");
                for (int m = 0; m < line.moves.size(); m++) {
                    sb.append("\"").append(line.moves.get(m)).append("\"");
                    if (m < line.moves.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]\n  }");
                if (i < lines.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("]");
            Files.write(f.toPath(), sb.toString().getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save openings: " + e.getMessage());
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
