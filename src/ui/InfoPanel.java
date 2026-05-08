package ui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class InfoPanel extends JPanel {

    // PGN move list (right side)
    private final DefaultListModel<String> moveListModel = new DefaultListModel<>();
    private final JList<String> moveList = new JList<>(moveListModel);
    private int fullMoveNumber = 1;
    private boolean whiteNext = true;

    // Status
    private final JLabel statusLabel = new JLabel("White to move");

    // Opening repertoire fields (left side)
    private final JTextField titleField = new JTextField();
    private final JTextArea notesArea = new JTextArea(3, 1);
    private final JButton saveButton = new JButton("Save");
    private final JButton resetButton = new JButton("Reset Board");
    private final JComboBox<String> colorPicker
            = new JComboBox<>(new String[]{"Practice as White", "Practice as Black"});

    private final JComboBox<String> bookPicker = new JComboBox<>();
    private final JButton addBookButton = new JButton("+");
    private Runnable onBookChanged;

    // Callbacks
    private Runnable onSave;
    private Runnable onReset;
    private Runnable onCategoryChanged;

    public InfoPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(480, 0));

        // Status bar at top
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
        add(statusLabel, BorderLayout.NORTH);

        // Main split: opening panel left, pgn right
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildOpeningPanel(), buildPgnPanel());
        split.setDividerLocation(260);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildOpeningPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Opening Repertoire"));

        // Color picker at top
        panel.add(colorPicker, BorderLayout.NORTH);

        // Fields in center
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.insets = new Insets(2, 2, 2, 2);

        // Title label + field
        c.gridy = 0;
        fields.add(new JLabel("Title:"), c);
        c.gridy = 1;

        // Use a left-aligned document so long titles show start not end
        titleField.setHorizontalAlignment(JTextField.LEFT);
        // Switch from gray to normal as soon as the user types
        titleField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (titleField.getForeground().equals(Color.GRAY)) {
                    titleField.setText("");
                    titleField.setForeground(UIManager.getColor("TextField.foreground"));
                }
            }
        });
        fields.add(titleField, c);

        // Notes label + area
        c.gridy = 2;
        fields.add(new JLabel("Notes:"), c);
        c.gridy = 3;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        fields.add(new JScrollPane(notesArea), c);
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 4; fields.add(new JLabel("Book:"), c);
        c.gridy = 5;
        JPanel bookRow = new JPanel(new BorderLayout(2, 0));
        bookRow.add(bookPicker, BorderLayout.CENTER);
        bookRow.add(addBookButton, BorderLayout.EAST);
        addBookButton.setMargin(new Insets(0, 4, 0, 4));
        addBookButton.setToolTipText("Create new book");
        addBookButton.addActionListener(e -> addBook());
        bookPicker.addActionListener(e -> {
            if (onBookChanged != null) onBookChanged.run();
        });
        fields.add(bookRow, c);

        panel.add(fields, BorderLayout.CENTER);

        // Buttons at bottom
        JPanel buttons = new JPanel(new GridLayout(2, 1, 2, 4));
        saveButton.addActionListener(e -> {
            if (onSave != null) {
                onSave.run();
        
            }});
        resetButton.addActionListener(e -> {
            if (onReset != null) {
                onReset.run();
        
            }});
        buttons.add(saveButton);
        buttons.add(resetButton);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildPgnPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Moves"));
        moveList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(moveList), BorderLayout.CENTER);
        return panel;
    }

    // Setters for callbacks
    public void setOnSave(Runnable r) {
        this.onSave = r;
    }

    public void setOnReset(Runnable r) {
        this.onReset = r;
    }

    public void setOnBookChanged(Runnable r) { 
        this.onBookChanged = r; 
    }

    public void setSelectedBook(String name) {
        bookPicker.setSelectedItem(name);
    }

    // Color and category accessors
    public boolean isPracticingAsWhite() {
        return colorPicker.getSelectedIndex() == 0;
    }

    private void addBook() {
        String name = JOptionPane.showInputDialog(this,
                "New book name:", "Create Book", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            name = name.trim();
            for (int i = 0; i < bookPicker.getItemCount(); i++) {
                if (bookPicker.getItemAt(i).equals(name)) {
                    bookPicker.setSelectedIndex(i);
                    return;
                }
            }
            bookPicker.addItem(name);
            bookPicker.setSelectedItem(name);
        }
    }

    public String getSaveBook() {
        String selected = getSelectedBook();
        return selected.equals("All") ? "" : selected;
    }

    // Opening fields
    public String getSaveTitle() {
        return titleField.getText().trim();
    }

    public String getSaveNotes() {
        return notesArea.getText().trim();
    }

    public String getSelectedBook() {
        Object sel = bookPicker.getSelectedItem();
        return sel != null ? sel.toString() : "All";
    }

    public void setBooks(List<String> bookNames) {
        bookPicker.removeAllItems();
        for (String name : bookNames) bookPicker.addItem(name);
    }

    public void setSaveFields(String title, String notes) {
        titleField.setText(title);
        notesArea.setText(notes);
    }

    // Show inherited parent title grayed out
    public void setInheritedTitle(String parentTitle) {
        if (parentTitle != null && !parentTitle.isEmpty()) {
            titleField.setText(parentTitle);
            titleField.setForeground(Color.GRAY);
        } else {
            titleField.setText("");
            titleField.setForeground(UIManager.getColor("TextField.foreground"));
        }
    }

    public void clearSaveFields() {
        titleField.setText("");
        titleField.setForeground(UIManager.getColor("TextField.foreground"));
        notesArea.setText("");
    }

    // PGN move list
    public void addMove(String san) {
        if (whiteNext) {
            moveListModel.addElement(fullMoveNumber + ". " + san);
        } else {
            int last = moveListModel.size() - 1;
            moveListModel.set(last, moveListModel.get(last) + "  " + san);
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
        clearSaveFields();
    }

    public boolean hasTitleOwned() {
        return !titleField.getText().isEmpty()
                && !titleField.getForeground().equals(Color.GRAY);
    }
}
