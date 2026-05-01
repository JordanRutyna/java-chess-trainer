package openings;

import java.util.List;

public class OpeningLine {

    public String name;
    public String notes;
    public List<String> moves;

    public OpeningLine(String name, String notes, List<String> moves) {
        this.name = name;
        this.notes = notes;
        this.moves = moves;
    }

    @Override
    public String toString() {
        return name;
    }
}
