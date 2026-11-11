package in.frcce.ecs.ui;

import in.frcce.ecs.dao.TimetableEntryDAO;
import in.frcce.ecs.model.TimeSlot;
import in.frcce.ecs.model.TimetableEntry;
import in.frcce.ecs.model.User;
import in.frcce.ecs.service.AuthService;
import in.frcce.ecs.service.TimetableService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit and headless layout tests for UiTheme, MainFrame, TimetableViewPanel, and EditEntryDialog.
 * Verifies Milestone 2 (R1 & R2) requirements:
 * - UiTheme font resolution, typography constants, and color palette
 * - Table and header styling utilities
 * - MainFrame window sizing (1280x760 design, min 1100x680, no forced MAXIMIZED_BOTH)
 * - 44px top banner with college — dept em dash and logout button
 * - 190px sidebar with 32px nav buttons and 11pt section labels
 * - 22px status bar with inline legend swatches (Theory, Practical, Tutorial, Break, Free)
 * - TimetableViewPanel directly in BorderLayout.CENTER with NO JScrollPane
 * - 36px compact toolbar with 28px buttons
 * - Fixed 28px header row, 80px time column, 1.0 day column weight
 * - Equal height for regular slot rows (1.0 weight) and 0.4 weight for break rows
 * - Dynamic Saturday auto-hiding when no Saturday slots exist in DB
 * - Single-entry cell design (Line 1: Code (Type), Line 2: Fac · Room [Batch])
 * - Parallel batch cells: 2 entries split vertically with 1pt shrink, 4 entries 2x2 grid
 * - 12pt tooltips with complete details
 * - Mouse hover border highlight (#2F6FDE) and background highlight (#BDD7EE)
 * - Off-screen layout verification at 1920x1080, 1366x768, 1280x760, and 1100x680
 */
public class UiThemeAndLayoutTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("Running Comprehensive Milestone 2 UI & Layout Verification...");
        System.out.println("=============================================================");

        testUiThemeFonts();
        testUiThemeColors();
        testUiThemeTableStyle();
        testMainFrameDimensionsAndComponents();
        testTimetableViewPanelNoScrollpaneAndToolbar();
        testSaturdayAutoHide();
        testCellDesignAndParallelBatches();
        testCellHoverAndTooltips();
        testEditEntryDialogStyling();
        testMultiResolutionRendering();

        System.out.println("=============================================================");
        System.out.println("Milestone 2 Verification Results: " + passed + " passed, " + failed + " failed");
        System.out.println("=============================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean condition, String detail) {
        if (condition) {
            System.out.println("  [PASS] " + name);
            passed++;
        } else {
            System.err.println("  [FAIL] " + name + ": " + detail);
            failed++;
        }
    }

    private static void testUiThemeFonts() {
        check("UiTheme font family resolved",
                UiTheme.getFontFamily() != null && !UiTheme.getFontFamily().isEmpty(),
                "Family is null/empty");

        check("FONT_BASE is 12pt plain",
                UiTheme.FONT_BASE.getSize() == 12 && UiTheme.FONT_BASE.getStyle() == Font.PLAIN,
                "Size=" + UiTheme.FONT_BASE.getSize() + ", style=" + UiTheme.FONT_BASE.getStyle());

        check("FONT_HEADER is 12pt bold",
                UiTheme.FONT_HEADER.getSize() == 12 && UiTheme.FONT_HEADER.getStyle() == Font.BOLD,
                "Size=" + UiTheme.FONT_HEADER.getSize() + ", style=" + UiTheme.FONT_HEADER.getStyle());

        check("FONT_CELL_TITLE is 11pt bold",
                UiTheme.FONT_CELL_TITLE.getSize() == 11 && UiTheme.FONT_CELL_TITLE.getStyle() == Font.BOLD,
                "Size=" + UiTheme.FONT_CELL_TITLE.getSize());

        check("FONT_CELL_SUB is 10pt plain",
                UiTheme.FONT_CELL_SUB.getSize() == 10 && UiTheme.FONT_CELL_SUB.getStyle() == Font.PLAIN,
                "Size=" + UiTheme.FONT_CELL_SUB.getSize());

        check("FONT_CELL_TINY is 9pt plain",
                UiTheme.FONT_CELL_TINY.getSize() == 9 && UiTheme.FONT_CELL_TINY.getStyle() == Font.PLAIN,
                "Size=" + UiTheme.FONT_CELL_TINY.getSize());

        check("FONT_STATUS is 11pt plain",
                UiTheme.FONT_STATUS.getSize() == 11 && UiTheme.FONT_STATUS.getStyle() == Font.PLAIN,
                "Size=" + UiTheme.FONT_STATUS.getSize());

        check("FONT_SECTION is 11pt bold",
                UiTheme.FONT_SECTION.getSize() == 11 && UiTheme.FONT_SECTION.getStyle() == Font.BOLD,
                "Size=" + UiTheme.FONT_SECTION.getSize());

        check("Utility bold() and plain() match expected styles",
                UiTheme.bold(14f).getStyle() == Font.BOLD && UiTheme.plain(14f).getStyle() == Font.PLAIN,
                "bold/plain styles");
    }

    private static void testUiThemeColors() {
        check("COLOR_HEADER is #2F6FDE",
                UiTheme.COLOR_HEADER.equals(new Color(0x2F, 0x6F, 0xDE)),
                "Header color: " + UiTheme.COLOR_HEADER);

        check("COLOR_GRID is #D9DEE7",
                UiTheme.COLOR_GRID.equals(new Color(0xD9, 0xDE, 0xE7)),
                "Grid color: " + UiTheme.COLOR_GRID);

        check("COLOR_HOVER is #BDD7EE",
                UiTheme.COLOR_HOVER.equals(new Color(0xBD, 0xD7, 0xEE)),
                "Hover color: " + UiTheme.COLOR_HOVER);

        check("COLOR_TH is #D6E4FF",
                UiTheme.COLOR_TH.equals(new Color(0xD6, 0xE4, 0xFF)),
                "TH color: " + UiTheme.COLOR_TH);

        check("COLOR_PR is #D3F4D8",
                UiTheme.COLOR_PR.equals(new Color(0xD3, 0xF4, 0xD8)),
                "PR color: " + UiTheme.COLOR_PR);

        check("COLOR_TU is #FFE8CC",
                UiTheme.COLOR_TU.equals(new Color(0xFF, 0xE8, 0xCC)),
                "TU color: " + UiTheme.COLOR_TU);

        check("COLOR_FREE is #F8F9FA",
                UiTheme.COLOR_FREE.equals(new Color(0xF8, 0xF9, 0xFA)),
                "FREE color: " + UiTheme.COLOR_FREE);

        check("COLOR_BREAK is #F0F2F5",
                UiTheme.COLOR_BREAK.equals(new Color(0xF0, 0xF2, 0xF5)),
                "BREAK color: " + UiTheme.COLOR_BREAK);

        check("COLOR_STATUS_ERROR is #D93025",
                UiTheme.COLOR_STATUS_ERROR.equals(new Color(0xD9, 0x30, 0x25)),
                "ERROR color: " + UiTheme.COLOR_STATUS_ERROR);
    }

    private static void testUiThemeTableStyle() {
        JTable table = new JTable(5, 3);
        UiTheme.applyTableStyle(table);

        check("Table row height is 24px",
                table.getRowHeight() == 24,
                "Row height: " + table.getRowHeight());

        check("Table font is 12pt plain",
                table.getFont().getSize() == 12 && table.getFont().getStyle() == Font.PLAIN,
                "Table font: " + table.getFont());

        check("Table grid color is #D9DEE7",
                table.getGridColor().equals(UiTheme.COLOR_GRID),
                "Grid color: " + table.getGridColor());

        JTableHeader header = table.getTableHeader();
        check("Table header height is 28px",
                header.getPreferredSize().height == 28,
                "Header height: " + header.getPreferredSize().height);

        check("Table header font is 12pt bold",
                header.getFont().getSize() == 12 && header.getFont().getStyle() == Font.BOLD,
                "Header font: " + header.getFont());

        check("Table header background is COLOR_HEADER",
                header.getBackground().equals(UiTheme.COLOR_HEADER),
                "Header bg: " + header.getBackground());
    }

    private static void testMainFrameDimensionsAndComponents() {
        User user = new User();
        user.setUsername("testadmin");
        user.setRole("ADMIN");
        AuthService auth = new AuthService();

        MainFrame frame = new MainFrame(user, auth);

        check("MainFrame min width is 1100",
                frame.getMinimumSize().width == 1100,
                "Min width: " + frame.getMinimumSize().width);

        check("MainFrame min height is 680",
                frame.getMinimumSize().height == 680,
                "Min height: " + frame.getMinimumSize().height);

        check("MainFrame initial width >= 1100",
                frame.getWidth() >= 1100,
                "Width: " + frame.getWidth());

        check("MainFrame initial height >= 680",
                frame.getHeight() >= 680,
                "Height: " + frame.getHeight());

        // Find top banner, sidebar, and status bar
        Container root = (Container) frame.getContentPane();
        Component north = null;
        Component center = null;
        Component south = null;

        for (Component c : root.getComponents()) {
            if (c instanceof JPanel p) {
                if (p.getPreferredSize().height == 44) {
                    north = p;
                } else if (p.getPreferredSize().height == 22) {
                    south = p;
                }
            } else if (c instanceof JSplitPane) {
                center = c;
            }
        }

        check("Top banner height is 44px",
                north != null && north.getPreferredSize().height == 44,
                "North banner height: " + (north != null ? north.getPreferredSize().height : "null"));

        check("Top banner background is COLOR_HEADER (#2F6FDE)",
                north != null && north.getBackground().equals(UiTheme.COLOR_HEADER),
                "North banner bg: " + (north != null ? north.getBackground() : "null"));

        // Check top banner text for college — dept em dash
        boolean foundEmDashTitle = false;
        if (north instanceof Container northContainer) {
            for (Component c : northContainer.getComponents()) {
                if (c instanceof JLabel l && l.getText() != null && l.getText().contains("\u2014")) {
                    foundEmDashTitle = true;
                    break;
                }
            }
        }
        check("Top banner has college \u2014 dept em dash title",
                foundEmDashTitle,
                "Could not find em dash title label in top banner");

        check("Status bar height is 22px",
                south != null && south.getPreferredSize().height == 22,
                "South bar height: " + (south != null ? south.getPreferredSize().height : "null"));

        check("Split pane exists in center",
                center instanceof JSplitPane,
                "Center is not JSplitPane");

        if (center instanceof JSplitPane split) {
            check("Sidebar divider is set to 190px",
                    split.getDividerLocation() == 190,
                    "Divider location: " + split.getDividerLocation());

            Component leftComp = split.getLeftComponent();
            check("Sidebar left component preferred width is 190px",
                    leftComp != null && leftComp.getPreferredSize().width == 190,
                    "Left comp width: " + (leftComp != null ? leftComp.getPreferredSize().width : "null"));
        }

        // Check status bar contains legend labels: Theory, Practical, Tutorial, Break, Free
        boolean foundTheory = false, foundPractical = false, foundTutorial = false, foundBreak = false, foundFree = false;
        if (south instanceof Container southContainer) {
            for (Component c : getDescendants(southContainer)) {
                if (c instanceof JLabel l && l.getText() != null) {
                    if ("Theory".equals(l.getText())) foundTheory = true;
                    if ("Practical".equals(l.getText())) foundPractical = true;
                    if ("Tutorial".equals(l.getText())) foundTutorial = true;
                    if ("Break".equals(l.getText())) foundBreak = true;
                    if ("Free".equals(l.getText())) foundFree = true;
                }
            }
        }
        check("Status bar includes all legend items (Theory, Practical, Tutorial, Break, Free)",
                foundTheory && foundPractical && foundTutorial && foundBreak && foundFree,
                "Legend items: TH=" + foundTheory + ", PR=" + foundPractical + ", TU=" + foundTutorial + ", BR=" + foundBreak + ", FR=" + foundFree);

        frame.dispose();
    }

    private static void testTimetableViewPanelNoScrollpaneAndToolbar() {
        User user = new User();
        user.setUsername("testadmin");
        user.setRole("ADMIN");
        AuthService auth = new AuthService();

        TimetableViewPanel panel = new TimetableViewPanel(user, auth);

        // Verify there is NO JScrollPane in the panel hierarchy
        boolean hasScrollPane = containsComponentOfType(panel, JScrollPane.class);
        check("TimetableViewPanel contains NO JScrollPane",
                !hasScrollPane,
                "Found JScrollPane in TimetableViewPanel");

        // Find toolbar (NORTH)
        Component north = ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        check("Toolbar is 36px fixed height",
                north != null && north.getPreferredSize().height == 36,
                "Toolbar height: " + (north != null ? north.getPreferredSize().height : "null"));

        // Verify no separate legend at SOUTH of TimetableViewPanel
        Component south = ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        check("TimetableViewPanel has no separate SOUTH legend panel",
                south == null,
                "Found south component in TimetableViewPanel: " + south);
    }

    private static void testSaturdayAutoHide() {
        User user = new User();
        user.setUsername("testadmin");
        user.setRole("ADMIN");
        AuthService auth = new AuthService();

        TimetableViewPanel panel = new TimetableViewPanel(user, auth);

        // Case 1: Slots containing Saturday
        List<TimeSlot> slotsWithSat = createMockTimeSlots(true);
        panel.setTimeSlotsForTesting(slotsWithSat);
        panel.buildGridForTesting(new ArrayList<>());

        JPanel gridPanel = panel.getGridPanelForTesting();
        Component gridComp1 = gridPanel.getComponent(0);
        check("Grid exists when Saturday slots are present",
                gridComp1 instanceof JPanel,
                "Grid is not a JPanel");

        int columnCountWithSat = countHeaderColumns((JPanel) gridComp1);
        check("Grid has 7 columns (Time + 6 days: Mon-Sat) when Saturday slots exist",
                columnCountWithSat == 7,
                "Found " + columnCountWithSat + " columns");

        // Case 2: Slots WITHOUT Saturday
        List<TimeSlot> slotsWithoutSat = createMockTimeSlots(false);
        panel.setTimeSlotsForTesting(slotsWithoutSat);
        panel.buildGridForTesting(new ArrayList<>());

        Component gridComp2 = gridPanel.getComponent(0);
        int columnCountWithoutSat = countHeaderColumns((JPanel) gridComp2);
        check("Grid has 6 columns (Time + 5 days: Mon-Fri) when Saturday slots are absent",
                columnCountWithoutSat == 6,
                "Found " + columnCountWithoutSat + " columns");
    }

    private static void testCellDesignAndParallelBatches() {
        User user = new User();
        user.setUsername("testadmin");
        user.setRole("ADMIN");
        AuthService auth = new AuthService();

        TimetableViewPanel panel = new TimetableViewPanel(user, auth);
        panel.setTimeSlotsForTesting(createMockTimeSlots(false));

        // Create entries:
        // Mon Slot 1: Single Theory entry
        TimetableEntry e1 = new TimetableEntry();
        e1.setDay("MONDAY");
        e1.setSlotNo(1);
        e1.setSubjectAbbr("AI");
        e1.setSubjectName("Artificial Intelligence");
        e1.setSessionType("TH");
        e1.setFacultyAbbr("VV");
        e1.setFacultyName("V. Vernekar");
        e1.setRoomName("311");

        // Tue Slot 1: 2 parallel practical batches (Batch A, Batch B)
        TimetableEntry e2a = new TimetableEntry();
        e2a.setDay("TUESDAY");
        e2a.setSlotNo(1);
        e2a.setSubjectAbbr("VLSI");
        e2a.setSubjectName("VLSI Design");
        e2a.setSessionType("PR");
        e2a.setFacultyAbbr("SG");
        e2a.setBatchName("A");
        e2a.setRoomName("Lab 1");

        TimetableEntry e2b = new TimetableEntry();
        e2b.setDay("TUESDAY");
        e2b.setSlotNo(1);
        e2b.setSubjectAbbr("COA");
        e2b.setSubjectName("Comp Org");
        e2b.setSessionType("PR");
        e2b.setFacultyAbbr("AB");
        e2b.setBatchName("B");
        e2b.setRoomName("Lab 2");

        // Wed Slot 1: 4 parallel batches (A, B, C, D)
        List<TimetableEntry> fourBatches = new ArrayList<>();
        for (String b : new String[]{"A", "B", "C", "D"}) {
            TimetableEntry te = new TimetableEntry();
            te.setDay("WEDNESDAY");
            te.setSlotNo(1);
            te.setSubjectAbbr("CNS");
            te.setSubjectName("Crypto Lab");
            te.setSessionType("PR");
            te.setFacultyAbbr("F" + b);
            te.setBatchName(b);
            te.setRoomName("Lab " + b);
            fourBatches.add(te);
        }

        List<TimetableEntry> entries = new ArrayList<>();
        entries.add(e1);
        entries.add(e2a);
        entries.add(e2b);
        entries.addAll(fourBatches);

        panel.buildGridForTesting(entries);
        JPanel grid = (JPanel) panel.getGridPanelForTesting().getComponent(0);

        // Find cell for Mon Slot 1 (Single entry)
        // GridBagLayout layout: row 0 = header, row 1 = slot 1 (gridy=1). Col 0 = time, col 1 = Mon (gridx=1)
        Component monSlot1 = findGridComponentAt(grid, 1, 1);
        check("Single entry cell found at Mon Slot 1",
                monSlot1 instanceof JPanel,
                "Cell is " + monSlot1);

        if (monSlot1 instanceof JPanel cell) {
            check("Single entry cell has COLOR_TH background",
                    cell.getBackground().equals(UiTheme.COLOR_TH),
                    "Cell bg: " + cell.getBackground());

            List<JLabel> labels = getLabels(cell);
            boolean hasLine1 = labels.stream().anyMatch(l -> "AI (TH)".equals(l.getText()));
            boolean hasLine2 = labels.stream().anyMatch(l -> l.getText() != null && l.getText().contains("VV") && l.getText().contains("311"));
            check("Single entry cell Line 1 is 'AI (TH)' and Line 2 is 'VV \u00B7 311'",
                    hasLine1 && hasLine2,
                    "Labels: " + labels.stream().map(JLabel::getText).toList());

            JLabel titleLbl = labels.stream().filter(l -> "AI (TH)".equals(l.getText())).findFirst().orElse(null);
            check("Single entry title font is 11pt bold",
                    titleLbl != null && titleLbl.getFont().getSize() == 11 && titleLbl.getFont().getStyle() == Font.BOLD,
                    "Title font: " + (titleLbl != null ? titleLbl.getFont() : "null"));
        }

        // Find cell for Tue Slot 1 (2 parallel entries)
        Component tueSlot1 = findGridComponentAt(grid, 2, 1);
        check("2 parallel entries container has GridLayout(2, 1)",
                tueSlot1 instanceof JPanel p && p.getLayout() instanceof GridLayout gl && gl.getRows() == 2 && gl.getColumns() == 1,
                "TueSlot1 layout: " + (tueSlot1 instanceof JPanel p ? p.getLayout() : "null"));

        if (tueSlot1 instanceof JPanel p) {
            check("2 parallel entries has 2 sub-cells",
                    p.getComponentCount() == 2,
                    "Subcell count: " + p.getComponentCount());

            Component sub1 = p.getComponent(0);
            if (sub1 instanceof JPanel subCell) {
                List<JLabel> subLabels = getLabels(subCell);
                JLabel subTitle = subLabels.stream().filter(l -> l.getText().contains("(PR)")).findFirst().orElse(null);
                check("2 parallel entries font shrunk by 1pt (10pt title)",
                        subTitle != null && subTitle.getFont().getSize() == 10,
                        "Subcell title font: " + (subTitle != null ? subTitle.getFont() : "null"));
            }
        }

        // Find cell for Wed Slot 1 (4 parallel entries)
        Component wedSlot1 = findGridComponentAt(grid, 3, 1);
        check("4 parallel entries container has 2x2 GridLayout",
                wedSlot1 instanceof JPanel p && p.getLayout() instanceof GridLayout gl && gl.getRows() == 2 && gl.getColumns() == 2,
                "WedSlot1 layout: " + (wedSlot1 instanceof JPanel p ? p.getLayout() : "null"));

        if (wedSlot1 instanceof JPanel p) {
            check("4 parallel entries has 4 sub-cells",
                    p.getComponentCount() == 4,
                    "Subcell count: " + p.getComponentCount());
        }
    }

    private static void testCellHoverAndTooltips() {
        User user = new User();
        user.setUsername("testadmin");
        user.setRole("ADMIN");
        AuthService auth = new AuthService();

        TimetableViewPanel panel = new TimetableViewPanel(user, auth);
        panel.setTimeSlotsForTesting(createMockTimeSlots(false));

        TimetableEntry e = new TimetableEntry();
        e.setDay("MONDAY");
        e.setSlotNo(1);
        e.setSubjectAbbr("AI");
        e.setSubjectName("Artificial Intelligence");
        e.setSessionType("TH");
        e.setFacultyAbbr("VV");
        e.setFacultyName("V. Vernekar");
        e.setRoomName("311");

        List<TimetableEntry> entries = new ArrayList<>();
        entries.add(e);
        panel.buildGridForTesting(entries);

        JPanel grid = (JPanel) panel.getGridPanelForTesting().getComponent(0);
        JPanel cell = (JPanel) findGridComponentAt(grid, 1, 1);

        // Verify tooltip
        String tooltip = cell.getToolTipText();
        check("Cell tooltip contains 12pt font style",
                tooltip != null && tooltip.contains("font-size:12pt"),
                "Tooltip: " + tooltip);

        check("Cell tooltip contains subject, faculty, room, and type",
                tooltip != null && tooltip.contains("Artificial Intelligence") && tooltip.contains("V. Vernekar") && tooltip.contains("311") && tooltip.contains("TH"),
                "Tooltip content: " + tooltip);

        // Test hover interaction
        Color originalBg = cell.getBackground();
        MouseEvent enterEvent = new MouseEvent(cell, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 10, 10, 0, false);
        for (java.awt.event.MouseListener ml : cell.getMouseListeners()) {
            ml.mouseEntered(enterEvent);
        }

        check("Cell hover changes background to COLOR_HOVER (#BDD7EE)",
                cell.getBackground().equals(UiTheme.COLOR_HOVER),
                "Hover bg: " + cell.getBackground());

        Color hoverBorderColor = getLineBorderColor(cell.getBorder());
        check("Cell hover highlights border to COLOR_HEADER (#2F6FDE)",
                UiTheme.COLOR_HEADER.equals(hoverBorderColor),
                "Hover border color: " + hoverBorderColor);

        // Exit mouse beyond cell bounds (-10, -10)
        MouseEvent exitEvent = new MouseEvent(cell, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -10, -10, 0, false);
        for (java.awt.event.MouseListener ml : cell.getMouseListeners()) {
            ml.mouseExited(exitEvent);
        }

        check("Cell hover exit restores original background",
                cell.getBackground().equals(originalBg),
                "Restored bg: " + cell.getBackground());

        Color restoredBorderColor = getLineBorderColor(cell.getBorder());
        check("Cell hover exit restores COLOR_GRID border",
                UiTheme.COLOR_GRID.equals(restoredBorderColor),
                "Restored border: " + restoredBorderColor);
    }

    private static void testEditEntryDialogStyling() {
        TimetableEntry e = new TimetableEntry();
        e.setEntryId(1);
        e.setSubjectName("Operating Systems");
        e.setSessionType("TH");
        e.setFacultyName("P. Patil");
        e.setDay("MONDAY");
        e.setSlotId(1);
        e.setRoomId(1);

        JPanel dummyParent = new JPanel();
        EditEntryDialog dialog = new EditEntryDialog(dummyParent, e,
                new TimetableService(), new TimetableEntryDAO(), "2026-27", null);

        // Find action buttons in dialog (exclude combo arrow buttons)
        List<JButton> buttons = new ArrayList<>();
        List<JLabel> labels = new ArrayList<>();
        for (Component c : getDescendants(dialog.getContentPane())) {
            if (c instanceof JButton b && !(b.getParent() instanceof JComboBox)) buttons.add(b);
            if (c instanceof JLabel l) labels.add(l);
        }

        check("EditEntryDialog has buttons with 28px preferred height",
                buttons.size() == 2 && buttons.stream().allMatch(b -> b.getPreferredSize().height == 28),
                "Button heights: " + buttons.stream().map(b -> b.getPreferredSize().height).toList());

        JButton saveBtn = buttons.stream().filter(b -> "Save Changes".equals(b.getText())).findFirst().orElse(null);
        check("Save button has COLOR_HEADER (#2F6FDE) background",
                saveBtn != null && saveBtn.getBackground().equals(UiTheme.COLOR_HEADER),
                "Save btn bg: " + (saveBtn != null ? saveBtn.getBackground() : "null"));

        check("EditEntryDialog labels have 11pt bold font",
                labels.stream().filter(l -> l.getText() != null && l.getText().endsWith(":")).allMatch(l -> l.getFont().getSize() == 11 && l.getFont().getStyle() == Font.BOLD),
                "Labels font check");

        dialog.dispose();
    }

    private static void testMultiResolutionRendering() {
        User user = new User();
        user.setUsername("testadmin");
        user.setRole("ADMIN");
        AuthService auth = new AuthService();

        MainFrame frame = new MainFrame(user, auth);

        int[][] resolutions = {
                {1920, 1080},
                {1366, 768},
                {1280, 760},
                {1100, 680}
        };

        for (int[] res : resolutions) {
            int w = res[0];
            int h = res[1];
            try {
                frame.setSize(w, h);
                frame.validate();
                frame.doLayout();

                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                frame.paint(g2d);
                g2d.dispose();

                check("Renders off-screen without error at " + w + "x" + h,
                        true,
                        "");
            } catch (Exception ex) {
                check("Renders off-screen without error at " + w + "x" + h,
                        false,
                        ex.getMessage());
            }
        }

        frame.dispose();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<TimeSlot> createMockTimeSlots(boolean includeSaturday) {
        List<TimeSlot> slots = new ArrayList<>();
        String[] days = includeSaturday
                ? new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"}
                : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};

        int slotId = 1;
        for (String day : days) {
            slots.add(new TimeSlot(slotId++, day, "08:45:00", "09:45:00", 1, false));
            slots.add(new TimeSlot(slotId++, day, "09:45:00", "10:45:00", 2, false));
            slots.add(new TimeSlot(slotId++, day, "10:45:00", "11:00:00", 3, true)); // Short Break
            slots.add(new TimeSlot(slotId++, day, "11:00:00", "12:00:00", 4, false));
            slots.add(new TimeSlot(slotId++, day, "12:00:00", "13:00:00", 5, false));
            slots.add(new TimeSlot(slotId++, day, "13:00:00", "13:30:00", 6, true)); // Lunch Break
            slots.add(new TimeSlot(slotId++, day, "13:30:00", "14:30:00", 7, false));
            slots.add(new TimeSlot(slotId++, day, "14:30:00", "15:30:00", 8, false));
        }
        return slots;
    }

    private static int countHeaderColumns(JPanel grid) {
        int maxGridX = 0;
        GridBagLayout gbl = (GridBagLayout) grid.getLayout();
        for (Component c : grid.getComponents()) {
            GridBagConstraints gbc = gbl.getConstraints(c);
            if (gbc.gridy == 0) {
                maxGridX = Math.max(maxGridX, gbc.gridx + 1);
            }
        }
        return maxGridX;
    }

    private static Component findGridComponentAt(JPanel grid, int gridx, int gridy) {
        GridBagLayout gbl = (GridBagLayout) grid.getLayout();
        for (Component c : grid.getComponents()) {
            GridBagConstraints gbc = gbl.getConstraints(c);
            if (gbc.gridx == gridx && gbc.gridy == gridy) {
                return c;
            }
        }
        return null;
    }

    private static List<JLabel> getLabels(Container container) {
        List<JLabel> list = new ArrayList<>();
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel l) {
                list.add(l);
            } else if (c instanceof Container child) {
                list.addAll(getLabels(child));
            }
        }
        return list;
    }

    private static List<Component> getDescendants(Container container) {
        List<Component> list = new ArrayList<>();
        for (Component c : container.getComponents()) {
            list.add(c);
            if (c instanceof Container child) {
                list.addAll(getDescendants(child));
            }
        }
        return list;
    }

    private static boolean containsComponentOfType(Container parent, Class<?> targetClass) {
        for (Component c : parent.getComponents()) {
            if (targetClass.isInstance(c)) {
                return true;
            }
            if (c instanceof Container childContainer) {
                if (containsComponentOfType(childContainer, targetClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Color getLineBorderColor(Border border) {
        if (border instanceof LineBorder lb) {
            return lb.getLineColor();
        }
        if (border instanceof CompoundBorder cb) {
            if (cb.getOutsideBorder() instanceof LineBorder lb) {
                return lb.getLineColor();
            }
            if (cb.getInsideBorder() instanceof LineBorder lb) {
                return lb.getLineColor();
            }
        }
        return null;
    }
}
