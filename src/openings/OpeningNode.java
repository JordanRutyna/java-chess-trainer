package openings;

import java.util.ArrayList;
import java.util.List;

public class OpeningNode {

    public String move;
    public String name;
    public String notes;
    public List<OpeningNode> children;

    public OpeningNode(String move, String name, String notes) {
        this.move = move != null ? move : "";
        this.name = name != null ? name : "";
        this.notes = notes != null ? notes : "";
        this.children = new ArrayList<>();
    }

    public boolean isRoot() {
        return move == null || move.isEmpty();
    }

    public OpeningNode findChildBySan(String san) {
        for (OpeningNode child : children) {
            if (child.move.equals(san)) {
                return child;
            }
        }
        return null;
    }

    public OpeningNode addChild(String san) {
        OpeningNode existing = findChildBySan(san);
        if (existing != null) {
            return existing;
        }
        OpeningNode child = new OpeningNode(san, "", "");
        children.add(child);
        return child;
    }
}
