package in.frcce.ecs.ui;

import in.frcce.ecs.dao.FacultyDAO;
import in.frcce.ecs.dao.FacultyUnavailabilityDAO;
import in.frcce.ecs.dao.TimeSlotDAO;
import in.frcce.ecs.dao.TimetableEntryDAO;
import in.frcce.ecs.model.Faculty;
import in.frcce.ecs.model.FacultyUnavailability;
import in.frcce.ecs.model.TimeSlot;
import in.frcce.ecs.model.TimetableEntry;
import in.frcce.ecs.util.ExportUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Faculty Workload Report and Free-Slot Finder.
 */
public class WorkloadReportPanel extends JPanel implements MainFrame.Refreshable {

    private final FacultyDAO              facultyDAO = new FacultyDAO();
    private final TimetableEntryDAO       entryDAO   = new TimetableEntryDAO();
    private final TimeSlotDAO             slotDAO    = new TimeSlotDAO();
    private final FacultyUnavailabilityDAO unavailDAO = new FacultyUnavailabilityDAO();

    private JTable            workloadTable;
    private DefaultTableModel workloadTableModel;

    private JTable            freeSlotTable;
    private DefaultTableModel freeSlotTableModel;
    private JLabel            freeSlotHeaderLabel;

    private JComboBox<String> yearCombo;

    private static final String[] WORKLOAD_COLS = {
        "Faculty ID", "Name", "Designation", "Theory (h)", "Practical (h)", "Tutorial (h)", "Total (h)", "Max (h)", "Remaining (h)"
    };

    private static final String[] FREE_SLOT_COLS = {
        "Day", "Slot #", "Start Time", "End Time"
    };

    public WorkloadReportPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        UiTheme.styleToolbar(toolbar);

        JLabel yrLbl = new JLabel("Academic Year:");
        yrLbl.setFont(UiTheme.bold(11f));
        toolbar.add(yrLbl);

        yearCombo = new JComboBox<>(new String[]{"2026-27", "2025-26"});
        yearCombo.setFont(UiTheme.FONT_BASE);
        yearCombo.setPreferredSize(new Dimension(100, 28));
        yearCombo.addActionListener(e -> refresh());
        toolbar.add(yearCombo);

        JButton refreshBtn = new JButton("↻ Refresh");
        UiTheme.styleButton(refreshBtn);
        refreshBtn.addActionListener(e -> refresh());
        toolbar.add(refreshBtn);

        JButton exportBtn = new JButton("📄 Export Workload CSV");
        UiTheme.styleActionButton(exportBtn, new Color(0x34, 0xA8, 0x53), Color.WHITE);
        exportBtn.addActionListener(e -> ExportUtil.exportTableToCSV(workloadTable, this));
        toolbar.add(exportBtn);

        add(toolbar, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildWorkloadSection(), buildFreeSlotSection());
        split.setResizeWeight(0.65);
        add(split, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildWorkloadSection() {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder tb = new TitledBorder("Faculty Workload Summary");
        tb.setTitleFont(UiTheme.FONT_SECTION);
        panel.setBorder(tb);

        workloadTableModel = new DefaultTableModel(WORKLOAD_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        workloadTable = new JTable(workloadTableModel);
        UiTheme.applyTableStyle(workloadTable);
        workloadTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        workloadTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = workloadTable.getSelectedRow();
                if (row >= 0) {
                    int facultyId = (int) workloadTableModel.getValueAt(row, 0);
                    String name = (String) workloadTableModel.getValueAt(row, 1);
                    freeSlotHeaderLabel.setText("Available Free Slots for: " + name);
                    loadFreeSlots(facultyId);
                } else {
                    freeSlotHeaderLabel.setText("Select a faculty above to view free slots");
                    freeSlotTableModel.setRowCount(0);
                }
            }
        });

        panel.add(new JScrollPane(workloadTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFreeSlotSection() {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder tb = new TitledBorder("Free Slots / Availability");
        tb.setTitleFont(UiTheme.FONT_SECTION);
        panel.setBorder(tb);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        freeSlotHeaderLabel = new JLabel("Select a faculty above to view free slots");
        freeSlotHeaderLabel.setFont(UiTheme.bold(12f));
        top.add(freeSlotHeaderLabel);
        panel.add(top, BorderLayout.NORTH);

        freeSlotTableModel = new DefaultTableModel(FREE_SLOT_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        freeSlotTable = new JTable(freeSlotTableModel);
        UiTheme.applyTableStyle(freeSlotTable);

        panel.add(new JScrollPane(freeSlotTable), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refresh() {
        workloadTableModel.setRowCount(0);
        String ay = (String) yearCombo.getSelectedItem();
        if (ay == null) ay = "2026-27";

        List<Faculty> faculties = facultyDAO.findAll();
        for (Faculty f : faculties) {
            List<TimetableEntry> entries = entryDAO.findByFaculty(f.getFacultyId(), ay);
            int th = 0;
            int pr = 0;
            int tu = 0;
            for (TimetableEntry te : entries) {
                switch (te.getSessionType()) {
                    case "TH" -> th++;
                    case "PR" -> pr++;
                    case "TU" -> tu++;
                }
            }
            int total = th + pr + tu;
            int max = f.getMaxHoursPerWeek();
            int remaining = max - total;

            workloadTableModel.addRow(new Object[]{
                f.getFacultyId(), f.getName(), f.getDesignation(),
                th, pr, tu, total, max, remaining
            });
        }
    }

    private void loadFreeSlots(int facultyId) {
        freeSlotTableModel.setRowCount(0);
        String ay = (String) yearCombo.getSelectedItem();
        if (ay == null) ay = "2026-27";

        List<TimetableEntry> busyEntries = entryDAO.findByFaculty(facultyId, ay);
        Set<Integer> busySlotIds = new HashSet<>();
        for (TimetableEntry te : busyEntries) busySlotIds.add(te.getSlotId());

        List<FacultyUnavailability> unavails = unavailDAO.findByFaculty(facultyId);
        for (FacultyUnavailability fu : unavails) busySlotIds.add(fu.getSlotId());

        List<TimeSlot> allSlots = slotDAO.findAll(); // non-break slots
        for (TimeSlot ts : allSlots) {
            if (!busySlotIds.contains(ts.getSlotId())) {
                freeSlotTableModel.addRow(new Object[]{
                    ts.getDay(), ts.getSlotNo(), ts.getStartTime(), ts.getEndTime()
                });
            }
        }
    }
}

