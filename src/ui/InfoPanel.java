package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class InfoPanel extends JPanel {

    private final DefaultListModel<String> moveListModel = new DefaultListModel<>();
    private final JList<String> moveList = new JList<>(moveListModel);
    private final JLabel statusLabel = new JLabel("White to move");
    private int fullMoveNumber = 1;
    private boolean whiteNext = true;

    private final JTextField titleField = new JTextField();
    private final JTextArea notesArea = new JTextArea(3, 1);
    private final JButton saveButton = new JButton("Save to Repertoire");
    private Runnable onSave;

    public InfoPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 0));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(statusLabel, BorderLayout.NORTH);

        moveList.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(moveList);
        scroll.setBorder(BorderFactory.createTitledBorder("Moves"));
        add(scroll, BorderLayout.CENTER);

        add(buildSavePanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSavePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Opening Repertoire"));

        JPanel fields = new JPanel(new GridLayout(4, 1, 2, 2));
        fields.add(new JLabel("Title:"));
        fields.add(titleField);
        fields.add(new JLabel("Notes:"));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        fields.add(notesScroll);

        saveButton.addActionListener(e -> {
            if (onSave != null) {
                onSave.run();
        
            }});

        panel.add(fields, BorderLayout.CENTER);
        panel.add(saveButton, BorderLayout.SOUTH);
        return panel;
    }

    public void setOnSave(Runnable callback) {
        this.onSave = callback;
    }

    public String getSaveTitle() {
        return titleField.getText().trim();
    }

    public String getSaveNotes() {
        return notesArea.getText().trim();
    }

    public void clearSaveFields() {
        titleField.setText("");
        notesArea.setText("");
    }

    public void addMove(String algebraic) {
        if (whiteNext) {
            moveListModel.addElement(fullMoveNumber + ". " + algebraic);
        } else {
            int last = moveListModel.size() - 1;
            String existing = moveListModel.get(last);
            moveListModel.set(last, existing + "  " + algebraic);
            fullMoveNumber++;
        }
        whiteNext = !whiteNext;
        moveList.ensureIndexIsVisible(moveListModel.size() - 1);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void reset() {
        moveListModel.clear();
        fullMoveNumber = 1;
        whiteNext = true;
        statusLabel.setText("White to move");
    }
}
