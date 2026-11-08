package in.frcce.ecs.ui;

import in.frcce.ecs.dao.*;
import in.frcce.ecs.model.*;
import in.frcce.ecs.service.AuthService;
import in.frcce.ecs.service.TimetableService;
import in.frcce.ecs.util.ExportUtil;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main timetable view panel.
 * Shows a weekly grid: days as columns, time-slots as rows.
 * Supports Class-wise, Faculty-wise, and Room-wise views.
 * Includes Generate, Export CSV, Print, and manual Edit actions.
 */
public class TimetableViewPanel extends JPanel implements MainFrame.Refreshable {

    private static final String ACADEMIC_YEAR = "2026-27";

    // Colors by session type
    private static final Color COLOR_TH   = new Color(0xBB, 0xDE, 0xFB); // light blue
    private static final Color COLOR_PR   = new Color(0xCC, 0xFF, 0xCC); // light green
    private static final Color COLOR_TU   = new Color(0xFF, 0xE0, 0xB2); // light orange
    private static final Color COLOR_FREE = new Color(0xF8, 0xF9, 0xFA); // near-white
    private static final Color COLOR_BREAK = new Color(0xE8, 0xEA, 0xED); // grey

    private static final String[] DAYS = {"MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"};

    private final User        currentUser;
    private final AuthService authService;

    private final TimetableService    timetableService;
    private final ClassDivisionDAO    classDivisionDAO;
    private final FacultyDAO          facultyDAO;
    private final RoomDAO             roomDAO;
    private final TimeSlotDAO         timeSlotDAO;
    private final TimetableEntryDAO   entryDAO;

    // Toolbar controls
    private JComboBox<String> viewTypeCombo;
    private JComboBox<Object> entityCombo;
    private JLabel            progressLabel;
    private JProgressBar      progressBar;

    // Grid panel
    private JPanel gridPanel;

    // Current data
    private List<TimeSlot>        allSlots;    // includes breaks
    private List<TimetableEntry>  currentEntries;

    // For manual edit
    private List<TimetableEntry> lastConflictPlaced = new ArrayList<>();

    public TimetableViewPanel(User user, AuthService authService) {
        this.currentUser     = user;
        this.authService     = authService;
        this.timetableService = new TimetableService();
        this.classDivisionDAO = new ClassDivisionDAO();
        this.facultyDAO       = new FacultyDAO();
        this.roomDAO          = new RoomDAO();
        this.timeSlotDAO      = new TimeSlotDAO();
        this.entryDAO         = new TimetableEntryDAO();

        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    // =========================================================================
    private void buildUI() {
        add(buildToolbar(), BorderLayout.NORTH);

        // Scrollable grid
        gridPanel = new JPanel(new BorderLayout());
        gridPanel.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Legend
        add(buildLegend(), BorderLayout.SOUTH);

        // Load slots
        allSlots = timeSlotDAO.findAllIncBreaks();

        // Initial load
        SwingUtilities.invokeLater(this::loadEntityCombo);
    }

    // =========================================================================
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(new Color(0xF1, 0xF3, 0xF4));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xDA, 0xDC, 0xE0)));

        // View type
        bar.add(new JLabel("View:"));
        viewTypeCombo = new JComboBox<>(new String[]{"Class View", "Faculty View", "Room View"});
        viewTypeCombo.addActionListener(e -> loadEntityCombo());
        bar.add(viewTypeCombo);

        // Entity selector
        bar.add(new JLabel("Select:"));
        entityCombo = new JComboBox<>();
        entityCombo.setPreferredSize(new Dimension(220, 28));
        entityCombo.addActionListener(e -> refreshGrid());
        bar.add(entityCombo);

        bar.add(new JSeparator(SwingConstants.VERTICAL));

        // Generate button (admin only)
        if (authService.isAdmin()) {
            JButton generateBtn = makeButton("⚡ Generate", new Color(0x1A, 0x73, 0xE8));
            generateBtn.addActionListener(e -> onGenerate());
            bar.add(generateBtn);

            JButton clearBtn = makeButton("🗑 Clear", new Color(0xEA, 0x43, 0x35));
            clearBtn.addActionListener(e -> onClear());
            bar.add(clearBtn);

            bar.add(new JSeparator(SwingConstants.VERTICAL));
        }

        JButton exportBtn = makeButton("📄 Export CSV", new Color(0x34, 0xA8, 0x53));
        exportBtn.addActionListener(e -> onExportCSV());
        bar.add(exportBtn);

        JButton printBtn = makeButton("🖨 Print", new Color(0x60, 0x60, 0x60));
        printBtn.addActionListener(e -> onPrint());
        bar.add(printBtn);

        bar.add(new JSeparator(SwingConstants.VERTICAL));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(120, 20));
        bar.add(progressBar);

        progressLabel = new JLabel("");
        progressLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        progressLabel.setForeground(Color.GRAY);
        bar.add(progressLabel);

        return bar;
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return btn;
    }

    // =========================================================================
    private JPanel buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        legend.setBackground(new Color(0xF8, 0xF9, 0xFA));
        legend.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xDA, 0xDC, 0xE0)));
        legend.add(new JLabel("Legend:"));
        addLegendItem(legend, "Theory (TH)",     COLOR_TH);
        addLegendItem(legend, "Practical (PR)",  COLOR_PR);
        addLegendItem(legend, "Tutorial (TU)",   COLOR_TU);
        addLegendItem(legend, "Break",           COLOR_BREAK);
        addLegendItem(legend, "Free",            COLOR_FREE);
        return legend;
    }

    private void addLegendItem(JPanel panel, String label, Color color) {
        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        swatch.setPreferredSize(new Dimension(16, 16));
        panel.add(swatch);
        panel.add(new JLabel(label));
    }

    // =========================================================================
    private void loadEntityCombo() {
        entityCombo.removeAllItems();
        String view = (String) viewTypeCombo.getSelectedItem();
        if ("Class View".equals(view)) {
            for (ClassDivision cd : classDivisionDAO.findAll()) {
                entityCombo.addItem(new ComboItem(cd.getClassId(), cd.getLabel()));
            }
        } else if ("Faculty View".equals(view)) {
            for (Faculty f : facultyDAO.findAll()) {
                entityCombo.addItem(new ComboItem(f.getFacultyId(), f.getName()));
            }
        } else {
            for (Room r : roomDAO.findAll()) {
                entityCombo.addItem(new ComboItem(r.getRoomId(), r.getName() + " (" + r.getType() + ")"));
            }
        }
        refreshGrid();
    }

    // =========================================================================
    @Override
    public void refresh() {
        allSlots = timeSlotDAO.findAllIncBreaks();
        loadEntityCombo();
    }

    private void refreshGrid() {
        ComboItem selected = (ComboItem) entityCombo.getSelectedItem();
        if (selected == null) {
            gridPanel.removeAll();
            gridPanel.revalidate();
            gridPanel.repaint();
            return;
        }

        String view = (String) viewTypeCombo.getSelectedItem();
        int id = selected.getId();

        SwingWorker<List<TimetableEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TimetableEntry> doInBackground() {
                if ("Class View".equals(view))   return entryDAO.findByClass(id, ACADEMIC_YEAR);
                if ("Faculty View".equals(view)) return entryDAO.findByFaculty(id, ACADEMIC_YEAR);
                return entryDAO.findByRoom(id, ACADEMIC_YEAR);
            }

            @Override
            protected void done() {
                try {
                    currentEntries = get();
                    buildGrid(currentEntries);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TimetableViewPanel.this,
                            "Error loading timetable: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // =========================================================================
    /** Build the weekly grid table inside gridPanel. */
    private void buildGrid(List<TimetableEntry> entries) {
        gridPanel.removeAll();

        if (allSlots == null || allSlots.isEmpty()) {
            gridPanel.add(new JLabel("No time slots configured. Please add slots in the Time Slots screen."),
                    BorderLayout.CENTER);
            gridPanel.revalidate();
            gridPanel.repaint();
            return;
        }

        // Index entries by (day, slotNo)
        Map<String, List<TimetableEntry>> entryMap = new HashMap<>();
        for (TimetableEntry te : entries) {
            String key = te.getDay() + "_" + te.getSlotNo();
            entryMap.computeIfAbsent(key, k -> new ArrayList<>()).add(te);
        }

        // Build grid: rows = slots, cols = days
        // Get unique slot numbers
        List<TimeSlot> uniqueSlots = allSlots.stream()
                .filter(ts -> "MONDAY".equals(ts.getDay()))
                .sorted(Comparator.comparingInt(TimeSlot::getSlotNo))
                .collect(Collectors.toList());

        int rows = uniqueSlots.size() + 1; // +1 for header
        int cols = DAYS.length + 1;        // +1 for time column

        JPanel grid = new JPanel(new GridLayout(rows, cols, 1, 1));
        grid.setBackground(new Color(0xDA, 0xDC, 0xE0)); // grid line color

        // Header row
        grid.add(makeHeaderCell("Time \\ Day"));
        for (String day : DAYS) {
            grid.add(makeHeaderCell(day.substring(0, 3))); // Mon, Tue...
        }

        // Data rows
        for (TimeSlot slotDef : uniqueSlots) {
            // Time column
            JPanel timeCell = makeTimeCell(slotDef);
            grid.add(timeCell);

            for (String day : DAYS) {
                String key = day + "_" + slotDef.getSlotNo();
                List<TimetableEntry> dayEntries = entryMap.getOrDefault(key, Collections.emptyList());

                if (slotDef.isBreak()) {
                    JPanel bc = new JPanel(new BorderLayout());
                    bc.setBackground(COLOR_BREAK);
                    JLabel bl = new JLabel(slotDef.getSlotNo() == 3 ? "Short Break" : "Lunch Break",
                            SwingConstants.CENTER);
                    bl.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                    bl.setForeground(Color.GRAY);
                    bc.add(bl, BorderLayout.CENTER);
                    grid.add(bc);
                } else if (dayEntries.isEmpty()) {
                    JPanel free = new JPanel(new BorderLayout());
                    free.setBackground(COLOR_FREE);
                    // Double-click to manually add (admin only)
                    if (authService.isAdmin()) {
                        free.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        free.setToolTipText("Double-click to manually add a session");
                        final String d = day;
                        final int sno = slotDef.getSlotNo();
                        free.addMouseListener(new MouseAdapter() {
                            @Override public void mouseClicked(MouseEvent e) {
                                if (e.getClickCount() == 2) onManualAdd(d, sno);
                            }
                        });
                    }
                    grid.add(free);
                } else {
                    // Could be multiple entries (batch parallel sessions)
                    JPanel cellPanel = new JPanel(new GridLayout(dayEntries.size(), 1, 0, 1));
                    cellPanel.setBackground(Color.LIGHT_GRAY);
                    for (TimetableEntry te : dayEntries) {
                        cellPanel.add(makeEntryCell(te));
                    }
                    grid.add(cellPanel);
                }
            }
        }

        JScrollPane innerScroll = new JScrollPane(grid);
        innerScroll.setBorder(null);
        gridPanel.add(innerScroll, BorderLayout.CENTER);
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel makeHeaderCell(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0x1A, 0x73, 0xE8));
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Color.WHITE);
        l.setBorder(new EmptyBorder(6, 4, 6, 4));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private JPanel makeTimeCell(TimeSlot ts) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setBackground(new Color(0xE8, 0xF0, 0xFE));
        JLabel slot = new JLabel("S" + ts.getSlotNo(), SwingConstants.CENTER);
        slot.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JLabel time = new JLabel(ts.getStartTime().substring(0, 5), SwingConstants.CENTER);
        time.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        time.setForeground(Color.GRAY);
        p.add(slot);
        p.add(time);
        return p;
    }

    private JPanel makeEntryCell(TimetableEntry te) {
        Color bg = switch (te.getSessionType()) {
            case "TH" -> COLOR_TH;
            case "PR" -> COLOR_PR;
            case "TU" -> COLOR_TU;
            default   -> COLOR_FREE;
        };

        JPanel cell = new JPanel(new GridLayout(3, 1, 0, 1));
        cell.setBackground(bg);
        cell.setBorder(new EmptyBorder(2, 4, 2, 4));

        String abbr = te.getSubjectAbbr() != null ? te.getSubjectAbbr() : te.getSubjectName();
        JLabel subj = new JLabel(abbr + " (" + te.getSessionType() + ")", SwingConstants.CENTER);
        subj.setFont(new Font("Segoe UI", Font.BOLD, 10));

        String facAbbr = te.getFacultyAbbr() != null ? te.getFacultyAbbr() : "";
        JLabel fac = new JLabel(facAbbr, SwingConstants.CENTER);
        fac.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        fac.setForeground(new Color(0x20, 0x20, 0x60));

        String roomBatch = te.getRoomName() != null ? te.getRoomName() : "";
        if (te.getBatchName() != null && !te.getBatchName().isEmpty()) {
            roomBatch = "[" + te.getBatchName() + "] " + roomBatch;
        }
        JLabel room = new JLabel(roomBatch, SwingConstants.CENTER);
        room.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        room.setForeground(Color.GRAY);

        cell.add(subj);
        cell.add(fac);
        cell.add(room);

        // Tooltip with full info
        cell.setToolTipText("<html><b>" + te.getSubjectName() + "</b><br>"
                + "Faculty: " + (te.getFacultyName() != null ? te.getFacultyName() : "") + "<br>"
                + "Room: " + (te.getRoomName() != null ? te.getRoomName() : "") + "<br>"
                + "Type: " + te.getSessionType()
                + (te.getBatchName() != null ? "<br>Batch: " + te.getBatchName() : "")
                + "</html>");

        // Right-click menu for edit/delete (admin only)
        if (authService.isAdmin()) {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JPopupMenu popup = new JPopupMenu();
            JMenuItem editItem = new JMenuItem("Edit Entry");
            JMenuItem delItem  = new JMenuItem("Delete Entry");
            editItem.addActionListener(ev -> onEditEntry(te));
            delItem.addActionListener(ev  -> onDeleteEntry(te));
            popup.add(editItem);
            popup.add(delItem);
            cell.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)  { if (e.isPopupTrigger()) popup.show(cell, e.getX(), e.getY()); }
                @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) popup.show(cell, e.getX(), e.getY()); }
            });
        }

        return cell;
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    private void onGenerate() {
        ComboItem selected = (ComboItem) entityCombo.getSelectedItem();
        if (!"Class View".equals(viewTypeCombo.getSelectedItem()) || selected == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select 'Class View' and choose a class before generating.",
                    "Select Class", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "<html>Generate timetable for <b>" + selected.getLabel() + "</b>?<br>" +
                "This will clear all existing (non-locked) entries for this class.</html>",
                "Generate Timetable", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressLabel.setText("Generating...");

        int classId = selected.getId();
        timetableService.setProgressListener(msg -> SwingUtilities.invokeLater(
                () -> progressLabel.setText(msg)));

        SwingWorker<TimetableService.GenerationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected TimetableService.GenerationResult doInBackground() {
                return timetableService.generate(classId, ACADEMIC_YEAR);
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                try {
                    TimetableService.GenerationResult result = get();
                    ConflictReportPanel.setLastConflicts(result.getConflicts());

                    String msg = "<html><b>Generation complete!</b><br>"
                            + "✓ Placed: " + result.getPlaced().size() + " sessions<br>"
                            + (result.isFullyPlaced() ? "✓ All sessions placed successfully." :
                               "⚠ " + result.getConflicts().size() + " sessions could not be placed.<br>"
                             + "Check the Conflict Report tab for details.")
                            + "</html>";
                    JOptionPane.showMessageDialog(TimetableViewPanel.this, msg,
                            "Generation Result",
                            result.isFullyPlaced() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

                    progressLabel.setText(result.getPlaced().size() + " entries placed.");
                    refreshGrid();
                } catch (Exception ex) {
                    progressLabel.setText("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(TimetableViewPanel.this,
                            "Error during generation: " + ex.getMessage(),
                            "Generation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void onClear() {
        ComboItem selected = (ComboItem) entityCombo.getSelectedItem();
        if (!"Class View".equals(viewTypeCombo.getSelectedItem()) || selected == null) {
            JOptionPane.showMessageDialog(this, "Please select 'Class View' and a class.",
                    "Select Class", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "<html>Clear ALL timetable entries for <b>" + selected.getLabel() + "</b>?<br>" +
                "<font color='red'>This cannot be undone.</font></html>",
                "Clear Timetable", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        int deleted = entryDAO.deleteByClass(selected.getId(), ACADEMIC_YEAR);
        JOptionPane.showMessageDialog(this, deleted + " entries cleared.", "Done",
                JOptionPane.INFORMATION_MESSAGE);
        refreshGrid();
    }

    private void onExportCSV() {
        if (currentEntries == null || currentEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No timetable data to export.",
                    "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Build a temporary JTable from the entries
        String[] cols = {"Day","Slot#","Time","Subject","Faculty","Room","Batch","Type"};
        Object[][] data = currentEntries.stream()
                .sorted(Comparator.comparing(TimetableEntry::getDay)
                        .thenComparingInt(TimetableEntry::getSlotNo))
                .map(te -> new Object[]{
                        te.getDay(), te.getSlotNo(),
                        (te.getStartTime() != null ? te.getStartTime().substring(0,5) : "")
                      + "-" + (te.getEndTime() != null ? te.getEndTime().substring(0,5) : ""),
                        te.getSubjectName(), te.getFacultyName(), te.getRoomName(),
                        te.getBatchName() != null ? te.getBatchName() : "",
                        te.getSessionType()
                }).toArray(Object[][]::new);
        JTable exportTable = new JTable(data, cols);
        ExportUtil.exportTableToCSV(exportTable, this);
    }

    private void onPrint() {
        if (currentEntries == null || currentEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No timetable data to print.", "Print",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] cols = {"Day","Slot","Time","Subject","Faculty","Room","Batch","Type"};
        Object[][] data = currentEntries.stream()
                .sorted(Comparator.comparing(TimetableEntry::getDay)
                        .thenComparingInt(TimetableEntry::getSlotNo))
                .map(te -> new Object[]{
                        te.getDay(), te.getSlotNo(),
                        (te.getStartTime() != null ? te.getStartTime().substring(0,5) : "")
                      + "-" + (te.getEndTime() != null ? te.getEndTime().substring(0,5) : ""),
                        te.getSubjectName(), te.getFacultyName(), te.getRoomName(),
                        te.getBatchName() != null ? te.getBatchName() : "",
                        te.getSessionType()
                }).toArray(Object[][]::new);
        JTable printTable = new JTable(data, cols);
        String title = "ECS Timetable — " + (entityCombo.getSelectedItem() != null ?
                entityCombo.getSelectedItem().toString() : "");
        ExportUtil.printTable(printTable, title, this);
    }

    private void onEditEntry(TimetableEntry te) {
        new EditEntryDialog(this, te, timetableService, entryDAO, ACADEMIC_YEAR, this::refreshGrid)
                .setVisible(true);
    }

    private void onDeleteEntry(TimetableEntry te) {
        int ch = JOptionPane.showConfirmDialog(this,
                "<html>Delete this entry?<br><b>" + te.getSubjectName()
                + "</b> on " + te.getDay() + " Slot " + te.getSlotNo() + "</html>",
                "Delete Entry", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            entryDAO.delete(te.getEntryId());
            refreshGrid();
        }
    }

    private void onManualAdd(String day, int slotNo) {
        JOptionPane.showMessageDialog(this,
                "Manual add: Day=" + day + ", Slot=" + slotNo + "\n"
                + "(Use the Faculty-Subject allocation and re-generate, or right-click existing entries to edit.)",
                "Manual Add", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    // Helper: combo item with id + label
    private static class ComboItem {
        private final int    id;
        private final String label;
        ComboItem(int id, String label) { this.id = id; this.label = label; }
        int    getId()    { return id; }
        String getLabel() { return label; }
        @Override public String toString() { return label; }
    }
}

