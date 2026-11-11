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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main timetable view panel.
 * Shows a weekly grid: days as columns, time-slots as rows.
 * Uses GridBagLayout directly in BorderLayout.CENTER (no JScrollPane) so that all slots
 * and days fit on ONE screen without horizontal or vertical scrollbars.
 * Dynamically detects whether Saturday has time slots in the database and auto-hides
 * the Saturday column if none exist.
 */
public class TimetableViewPanel extends JPanel implements MainFrame.Refreshable {

    private static final Logger LOG = Logger.getLogger(TimetableViewPanel.class.getName());

    private static final String ACADEMIC_YEAR = "2026-27";

    private final User        currentUser;
    private final AuthService authService;
    private MainFrame         mainFrame;

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
    private boolean           isUpdatingEntityCombo = false;

    // Grid panel (directly placed in CENTER of TimetableViewPanel)
    private JPanel gridPanel;

    // Current data
    private List<TimeSlot>        allSlots;    // includes breaks
    private List<TimetableEntry>  currentEntries;

    public TimetableViewPanel(User user, AuthService authService) {
        this(user, authService, null);
    }

    public TimetableViewPanel(User user, AuthService authService, MainFrame mainFrame) {
        this.currentUser      = user;
        this.authService      = authService;
        this.mainFrame        = mainFrame;
        this.timetableService = new TimetableService();
        this.classDivisionDAO = new ClassDivisionDAO();
        this.facultyDAO       = new FacultyDAO();
        this.roomDAO          = new RoomDAO();
        this.timeSlotDAO      = new TimeSlotDAO();
        this.entryDAO         = new TimetableEntryDAO();

        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    private void updateStatusBar(String message, boolean isError) {
        MainFrame mf = mainFrame;
        if (mf == null) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof MainFrame) {
                mf = (MainFrame) w;
            }
        }
        if (mf != null) {
            mf.setStatusMessage(message, isError);
        }
    }

    // =========================================================================
    private void buildUI() {
        // Compact single-row toolbar: 36px fixed height
        add(buildToolbar(), BorderLayout.NORTH);

        // Grid panel directly in CENTER (no JScrollPane wrapper)
        gridPanel = new JPanel(new BorderLayout());
        gridPanel.setBackground(Color.WHITE);
        add(gridPanel, BorderLayout.CENTER);

        // Load slots
        try {
            allSlots = timeSlotDAO.findAllIncBreaks();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to load time slots: " + ex.getMessage(), ex);
            allSlots = Collections.emptyList();
        }

        // Initial load
        SwingUtilities.invokeLater(this::loadEntityCombo);
    }

    // =========================================================================
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setPreferredSize(new Dimension(0, 36));
        bar.setMinimumSize(new Dimension(0, 36));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        bar.setBackground(UiTheme.COLOR_TOOLBAR_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.COLOR_GRID));

        // View type
        JLabel viewLbl = new JLabel("View:");
        viewLbl.setFont(UiTheme.FONT_BASE);
        bar.add(viewLbl);

        viewTypeCombo = new JComboBox<>(new String[]{"Class View", "Faculty View", "Room View"});
        viewTypeCombo.setFont(UiTheme.FONT_BASE);
        viewTypeCombo.setPreferredSize(new Dimension(115, 28));
        viewTypeCombo.addActionListener(e -> loadEntityCombo());
        bar.add(viewTypeCombo);

        // Entity selector
        JLabel selLbl = new JLabel("Select:");
        selLbl.setFont(UiTheme.FONT_BASE);
        bar.add(selLbl);

        entityCombo = new JComboBox<>();
        entityCombo.setFont(UiTheme.FONT_BASE);
        entityCombo.setPreferredSize(new Dimension(190, 28));
        entityCombo.addActionListener(e -> {
            if (!isUpdatingEntityCombo) refreshGrid();
        });
        bar.add(entityCombo);

        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(1, 20));
        bar.add(sep1);

        // Generate button (admin only)
        if (authService.isAdmin()) {
            JButton generateBtn = makeButton("\u26A1 Generate", UiTheme.COLOR_HEADER);
            generateBtn.addActionListener(e -> onGenerate());
            bar.add(generateBtn);

            JButton clearBtn = makeButton("\uD83D\uDDD1 Clear", UiTheme.COLOR_LOGOUT);
            clearBtn.addActionListener(e -> onClear());
            bar.add(clearBtn);

            JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
            sep2.setPreferredSize(new Dimension(1, 20));
            bar.add(sep2);
        }

        JButton exportBtn = makeButton("\uD83D\uDCC4 Export CSV", new Color(0x34, 0xA8, 0x53));
        exportBtn.addActionListener(e -> onExportCSV());
        bar.add(exportBtn);

        JButton printBtn = makeButton("\uD83D\uDDA8 Print", new Color(0x60, 0x60, 0x60));
        printBtn.addActionListener(e -> onPrint());
        bar.add(printBtn);

        JSeparator sep3 = new JSeparator(SwingConstants.VERTICAL);
        sep3.setPreferredSize(new Dimension(1, 20));
        bar.add(sep3);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(100, 18));
        bar.add(progressBar);

        progressLabel = new JLabel("");
        progressLabel.setFont(UiTheme.font(Font.ITALIC, 11f));
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
        btn.setFont(UiTheme.font(Font.BOLD, 12f));
        btn.setMargin(new Insets(2, 8, 2, 8));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 28));
        return btn;
    }

    // =========================================================================
    private void loadEntityCombo() {
        isUpdatingEntityCombo = true;
        try {
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
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to load entities: " + ex.getMessage(), ex);
            updateStatusBar("Error loading entities: " + ex.getMessage(), true);
        } finally {
            isUpdatingEntityCombo = false;
        }
        refreshGrid();
    }

    // =========================================================================
    @Override
    public void refresh() {
        try {
            allSlots = timeSlotDAO.findAllIncBreaks();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to load time slots: " + ex.getMessage(), ex);
            allSlots = Collections.emptyList();
        }
        loadEntityCombo();
    }

    private void refreshGrid() {
        ComboItem selected = (ComboItem) entityCombo.getSelectedItem();
        if (selected == null) {
            gridPanel.removeAll();
            gridPanel.revalidate();
            gridPanel.repaint();
            updateStatusBar("No selection.", false);
            return;
        }

        String view = (String) viewTypeCombo.getSelectedItem();
        int id = selected.getId();
        updateStatusBar("Loading " + view + "...", false);

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
                    updateStatusBar("Ready", false);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Error loading timetable: " + ex.getMessage(), ex);
                    buildGrid(Collections.emptyList());
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String errorMsg = "Error loading timetable: " + cause.getMessage();
                    updateStatusBar(errorMsg, true);
                }
            }
        };
        worker.execute();
    }

    // =========================================================================
    /** Build the weekly grid table directly inside gridPanel using GridBagLayout. */
    private void buildGrid(List<TimetableEntry> entries) {
        if (entries == null) {
            entries = Collections.emptyList();
        }
        gridPanel.removeAll();

        if (allSlots == null || allSlots.isEmpty()) {
            JLabel emptyLbl = new JLabel("No time slots configured. Please add slots in the Time Slots screen.", SwingConstants.CENTER);
            emptyLbl.setFont(UiTheme.FONT_BASE);
            gridPanel.add(emptyLbl, BorderLayout.CENTER);
            gridPanel.revalidate();
            gridPanel.repaint();
            return;
        }

        // Dynamically detect if Saturday has time slots in the database
        boolean hasSaturday = allSlots.stream()
                .anyMatch(ts -> "SATURDAY".equalsIgnoreCase(ts.getDay()));
        List<String> activeDays = new ArrayList<>();
        activeDays.add("MONDAY");
        activeDays.add("TUESDAY");
        activeDays.add("WEDNESDAY");
        activeDays.add("THURSDAY");
        activeDays.add("FRIDAY");
        if (hasSaturday) {
            activeDays.add("SATURDAY");
        }

        // Index entries by (day, slotNo)
        Map<String, List<TimetableEntry>> entryMap = new HashMap<>();
        for (TimetableEntry te : entries) {
            String key = te.getDay().toUpperCase() + "_" + te.getSlotNo();
            entryMap.computeIfAbsent(key, k -> new ArrayList<>()).add(te);
        }

        // Get unique slot numbers sorted by slotNo
        List<TimeSlot> uniqueSlots = allSlots.stream()
                .filter(ts -> "MONDAY".equalsIgnoreCase(ts.getDay()))
                .sorted(Comparator.comparingInt(TimeSlot::getSlotNo))
                .collect(Collectors.toList());
        if (uniqueSlots.isEmpty()) {
            Map<Integer, TimeSlot> slotMap = new TreeMap<>();
            for (TimeSlot ts : allSlots) {
                slotMap.putIfAbsent(ts.getSlotNo(), ts);
            }
            uniqueSlots = new ArrayList<>(slotMap.values());
        }

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(UiTheme.COLOR_GRID); // 1px borders between cells

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 1, 1);

        // Header row: 28px fixed height
        gbc.gridy = 0;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JPanel cornerHeader = makeHeaderCell("Time \\ Day");
        cornerHeader.setPreferredSize(new Dimension(80, 28));
        cornerHeader.setMinimumSize(new Dimension(80, 28));
        grid.add(cornerHeader, gbc);

        // Day columns share remaining width equally (weightx = 1.0)
        gbc.weightx = 1.0;
        for (int c = 0; c < activeDays.size(); c++) {
            gbc.gridx = c + 1;
            String day = activeDays.get(c);
            String label = day.substring(0, 1) + day.substring(1, 3).toLowerCase(); // Mon, Tue...
            JPanel dayHeader = makeHeaderCell(label);
            grid.add(dayHeader, gbc);
        }

        // Data rows: equal height for normal slots (weighty = 1.0), break rows at ~40% (weighty = 0.4)
        for (int r = 0; r < uniqueSlots.size(); r++) {
            TimeSlot slotDef = uniqueSlots.get(r);
            gbc.gridy = r + 1;

            boolean isBreakRow = slotDef.isBreak();
            gbc.weighty = isBreakRow ? 0.4 : 1.0;

            // Time column: fixed 80px width
            gbc.gridx = 0;
            gbc.weightx = 0.0;
            JPanel timeCell = makeTimeCell(slotDef);
            grid.add(timeCell, gbc);

            // Active day columns
            gbc.weightx = 1.0;
            for (int c = 0; c < activeDays.size(); c++) {
                String day = activeDays.get(c);
                gbc.gridx = c + 1;
                String key = day + "_" + slotDef.getSlotNo();
                List<TimetableEntry> dayEntries = entryMap.getOrDefault(key, Collections.emptyList());

                if (isBreakRow) {
                    JPanel bc = new JPanel(new BorderLayout());
                    bc.setBackground(UiTheme.COLOR_BREAK);
                    bc.setBorder(BorderFactory.createLineBorder(UiTheme.COLOR_GRID, 1));
                    JLabel bl = new JLabel(slotDef.getSlotNo() == 3 ? "Short Break" : "Lunch Break", SwingConstants.CENTER);
                    bl.setFont(UiTheme.italic(10f));
                    bl.setForeground(new Color(0x5F, 0x63, 0x68));
                    bc.add(bl, BorderLayout.CENTER);
                    grid.add(bc, gbc);
                } else if (dayEntries.isEmpty()) {
                    JPanel free = new JPanel(new BorderLayout());
                    free.setBackground(UiTheme.COLOR_FREE);
                    free.setBorder(BorderFactory.createLineBorder(UiTheme.COLOR_GRID, 1));
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
                    attachCellInteractions(free, UiTheme.COLOR_FREE, null, null);
                    grid.add(free, gbc);
                } else if (dayEntries.size() == 1) {
                    grid.add(makeSingleEntryCell(dayEntries.get(0), UiTheme.FONT_CELL_TITLE, UiTheme.FONT_CELL_SUB), gbc);
                } else if (dayEntries.size() == 2) {
                    // Split vertically into 2 rows, shrinking font by 1pt (10pt title, 9pt sub)
                    JPanel cellPanel = new JPanel(new GridLayout(2, 1, 0, 1));
                    cellPanel.setBackground(UiTheme.COLOR_GRID);
                    cellPanel.add(makeSingleEntryCell(dayEntries.get(0), UiTheme.FONT_CELL_TITLE_TINY, UiTheme.FONT_CELL_SUB_TINY));
                    cellPanel.add(makeSingleEntryCell(dayEntries.get(1), UiTheme.FONT_CELL_TITLE_TINY, UiTheme.FONT_CELL_SUB_TINY));
                    grid.add(cellPanel, gbc);
                } else {
                    // 3-4 parallel entries: split into 2x2 grid with 9pt/8pt font
                    int subCols = 2;
                    int subRows = (int) Math.ceil(dayEntries.size() / 2.0);
                    JPanel cellPanel = new JPanel(new GridLayout(subRows, subCols, 1, 1));
                    cellPanel.setBackground(UiTheme.COLOR_GRID);
                    for (TimetableEntry te : dayEntries) {
                        cellPanel.add(makeSingleEntryCell(te, UiTheme.FONT_CELL_TITLE_MICRO, UiTheme.FONT_CELL_SUB_MICRO));
                    }
                    grid.add(cellPanel, gbc);
                }
            }
        }

        gridPanel.add(grid, BorderLayout.CENTER);
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel makeHeaderCell(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UiTheme.COLOR_HEADER);
        p.setPreferredSize(new Dimension(0, 28));
        p.setMinimumSize(new Dimension(0, 28));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, UiTheme.COLOR_GRID));

        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(UiTheme.FONT_HEADER);
        l.setForeground(Color.WHITE);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private JPanel makeTimeCell(TimeSlot ts) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 1));
        p.setBackground(new Color(0xF0, 0xF4, 0xF9));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.COLOR_GRID, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        p.setPreferredSize(new Dimension(80, 0));
        p.setMinimumSize(new Dimension(80, 0));

        JLabel slot = new JLabel("S" + ts.getSlotNo(), SwingConstants.CENTER);
        slot.setFont(UiTheme.bold(10f));
        slot.setForeground(new Color(0x20, 0x21, 0x24));

        String timeStr = ts.getStartTime() != null && ts.getStartTime().length() >= 5
                ? ts.getStartTime().substring(0, 5) : "";
        JLabel time = new JLabel(timeStr, SwingConstants.CENTER);
        time.setFont(UiTheme.plain(9f));
        time.setForeground(new Color(0x5F, 0x63, 0x68));

        p.add(slot);
        p.add(time);
        return p;
    }

    private JPanel makeSingleEntryCell(TimetableEntry te, Font titleFont, Font subFont) {
        Color bg = switch (te.getSessionType()) {
            case "TH" -> UiTheme.COLOR_TH;
            case "PR" -> UiTheme.COLOR_PR;
            case "TU" -> UiTheme.COLOR_TU;
            default   -> UiTheme.COLOR_FREE;
        };

        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(bg);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.COLOR_GRID, 1),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));

        // Line 1: Subject short code (bold) + session type (e.g., "AI (TH)")
        String abbr = (te.getSubjectAbbr() != null && !te.getSubjectAbbr().trim().isEmpty())
                ? te.getSubjectAbbr().trim()
                : (te.getSubjectName() != null ? te.getSubjectName().trim() : "");
        String line1Text = abbr + " (" + te.getSessionType() + ")";

        // Line 2: Faculty initials and room on same line (e.g., "VV · 311", or with batch: "VV · [D] 311")
        String facAbbr = te.getFacultyAbbr() != null ? te.getFacultyAbbr().trim() : "";
        String roomName = te.getRoomName() != null ? te.getRoomName().trim() : "";
        String batchTag = (te.getBatchName() != null && !te.getBatchName().trim().isEmpty())
                ? "[" + te.getBatchName().trim() + "] " : "";
        String roomAndBatch = (batchTag + roomName).trim();

        String line2Text;
        if (!facAbbr.isEmpty() && !roomAndBatch.isEmpty()) {
            line2Text = facAbbr + " \u00B7 " + roomAndBatch;
        } else if (!facAbbr.isEmpty()) {
            line2Text = facAbbr;
        } else {
            line2Text = roomAndBatch;
        }

        JPanel textBlock = new JPanel(new GridLayout(2, 1, 0, 1));
        textBlock.setOpaque(false);

        JLabel l1 = new JLabel(line1Text, SwingConstants.CENTER);
        l1.setFont(titleFont);
        l1.setForeground(new Color(0x20, 0x21, 0x24));

        JLabel l2 = new JLabel(line2Text, SwingConstants.CENTER);
        l2.setFont(subFont);
        l2.setForeground(new Color(0x3C, 0x40, 0x43));

        textBlock.add(l1);
        textBlock.add(l2);
        cell.add(textBlock, BorderLayout.CENTER);

        // Standard 12pt HTML tooltip
        String tooltip = "<html><body style='font-family:sans-serif;font-size:12pt;padding:3px;'>"
                + "<b>" + (te.getSubjectName() != null ? te.getSubjectName() : "") + "</b><br>"
                + "<b>Faculty:</b> " + (te.getFacultyName() != null ? te.getFacultyName() : "-") + "<br>"
                + "<b>Room:</b> " + (te.getRoomName() != null ? te.getRoomName() : "-") + "<br>"
                + "<b>Type:</b> " + te.getSessionType()
                + (te.getBatchName() != null && !te.getBatchName().trim().isEmpty() ? "<br><b>Batch:</b> " + te.getBatchName() : "")
                + "</body></html>";

        // Right-click menu for edit/delete (admin only)
        JPopupMenu popup = null;
        if (authService.isAdmin()) {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            popup = new JPopupMenu();
            JMenuItem editItem = new JMenuItem("Edit Entry");
            JMenuItem delItem  = new JMenuItem("Delete Entry");
            editItem.setFont(UiTheme.FONT_BASE);
            delItem.setFont(UiTheme.FONT_BASE);
            editItem.addActionListener(ev -> onEditEntry(te));
            delItem.addActionListener(ev  -> onDeleteEntry(te));
            popup.add(editItem);
            popup.add(delItem);
        }

        attachCellInteractions(cell, bg, popup, tooltip);
        return cell;
    }

    private void attachCellInteractions(JPanel cell, Color normalBg, JPopupMenu popup, String tooltipText) {
        if (tooltipText != null) {
            cell.setToolTipText(tooltipText);
        }

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UiTheme.COLOR_HEADER, 1),
                        BorderFactory.createEmptyBorder(2, 4, 2, 4)
                ));
                cell.setBackground(UiTheme.COLOR_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cell);
                if (p.x < 0 || p.x >= cell.getWidth() || p.y < 0 || p.y >= cell.getHeight()) {
                    cell.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(UiTheme.COLOR_GRID, 1),
                            BorderFactory.createEmptyBorder(2, 4, 2, 4)
                    ));
                    cell.setBackground(normalBg);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (popup != null && e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (popup != null && e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };

        cell.addMouseListener(adapter);
        for (Component c : cell.getComponents()) {
            c.addMouseListener(adapter);
            if (c instanceof Container container) {
                for (Component sub : container.getComponents()) {
                    sub.addMouseListener(adapter);
                    if (tooltipText != null && sub instanceof JComponent jc) {
                        jc.setToolTipText(tooltipText);
                    }
                }
            }
        }
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
                    updateStatusBar(result.getPlaced().size() + " entries placed.", false);
                    refreshGrid();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    progressLabel.setText("Error: " + cause.getMessage());
                    updateStatusBar("Error during generation: " + cause.getMessage(), true);
                    JOptionPane.showMessageDialog(TimetableViewPanel.this,
                            "Error during generation: " + cause.getMessage(),
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
    // Package-private helpers for automated layout verification
    // =========================================================================
    void setTimeSlotsForTesting(List<TimeSlot> slots) {
        this.allSlots = slots;
    }

    void buildGridForTesting(List<TimetableEntry> entries) {
        buildGrid(entries);
    }

    JPanel getGridPanelForTesting() {
        return gridPanel;
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
