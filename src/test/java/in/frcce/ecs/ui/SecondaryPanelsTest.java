package in.frcce.ecs.ui;

import in.frcce.ecs.model.User;
import in.frcce.ecs.service.AuthService;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class SecondaryPanelsTest {

    public static void setup() {
        System.setProperty("java.awt.headless", "true");
        UiTheme.init();
    }

    private void assertTableStyle(JTable table, String panelName) {
        if (table == null) throw new AssertionError(panelName + " table should not be null");
        if (table.getRowHeight() != 24)
            throw new AssertionError(panelName + " table row height should be 24px but was " + table.getRowHeight());
        if (!UiTheme.FONT_BASE.equals(table.getFont()))
            throw new AssertionError(panelName + " table font should be FONT_BASE but was " + table.getFont());
        if (!UiTheme.COLOR_GRID.equals(table.getGridColor()))
            throw new AssertionError(panelName + " table grid color should be COLOR_GRID but was " + table.getGridColor());

        JTableHeader header = table.getTableHeader();
        if (header == null) throw new AssertionError(panelName + " header should not be null");
        if (header.getPreferredSize().height != 28)
            throw new AssertionError(panelName + " header height should be 28px but was " + header.getPreferredSize().height);
        if (!UiTheme.FONT_HEADER.equals(header.getFont()))
            throw new AssertionError(panelName + " header font should be FONT_HEADER but was " + header.getFont());
    }

    private JTable findTable(Component component) {
        if (component instanceof JTable t) return t;
        if (component instanceof JScrollPane sp && sp.getViewport().getView() instanceof JTable t) return t;
        if (component instanceof JSplitPane sp) {
            JTable t = findTable(sp.getLeftComponent());
            if (t != null) return t;
            t = findTable(sp.getRightComponent());
            if (t != null) return t;
        }
        if (component instanceof Container container) {
            for (Component c : container.getComponents()) {
                JTable t = findTable(c);
                if (t != null) return t;
            }
        }
        return null;
    }

    public void testFacultyPanelStyling() {
        FacultyPanel panel = new FacultyPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "FacultyPanel");
    }

    public void testSubjectPanelStyling() {
        SubjectPanel panel = new SubjectPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "SubjectPanel");
    }

    public void testClassPanelStyling() {
        ClassPanel panel = new ClassPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "ClassPanel");
    }

    public void testRoomPanelStyling() {
        RoomPanel panel = new RoomPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "RoomPanel");
    }

    public void testTimeSlotPanelStyling() {
        TimeSlotPanel panel = new TimeSlotPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "TimeSlotPanel");
    }

    public void testAllocationPanelStyling() {
        AllocationPanel panel = new AllocationPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "AllocationPanel");
    }

    public void testUnavailabilityPanelStyling() {
        UnavailabilityPanel panel = new UnavailabilityPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "UnavailabilityPanel");
    }

    public void testConflictReportPanelStyling() {
        ConflictReportPanel panel = new ConflictReportPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "ConflictReportPanel");
    }

    public void testWorkloadReportPanelStyling() {
        WorkloadReportPanel panel = new WorkloadReportPanel();
        JTable table = findTable(panel);
        assertTableStyle(table, "WorkloadReportPanel");
    }

    public static void main(String[] args) {
        setup();
        SecondaryPanelsTest test = new SecondaryPanelsTest();
        int pass = 0;
        System.out.println("=============================================================");
        System.out.println("Running Part 4 Secondary Panels Style Verification...");
        System.out.println("=============================================================");

        try { test.testFacultyPanelStyling(); System.out.println("  [PASS] FacultyPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] FacultyPanel: " + t.getMessage()); }
        try { test.testSubjectPanelStyling(); System.out.println("  [PASS] SubjectPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] SubjectPanel: " + t.getMessage()); }
        try { test.testClassPanelStyling(); System.out.println("  [PASS] ClassPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] ClassPanel: " + t.getMessage()); }
        try { test.testRoomPanelStyling(); System.out.println("  [PASS] RoomPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] RoomPanel: " + t.getMessage()); }
        try { test.testTimeSlotPanelStyling(); System.out.println("  [PASS] TimeSlotPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] TimeSlotPanel: " + t.getMessage()); }
        try { test.testAllocationPanelStyling(); System.out.println("  [PASS] AllocationPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] AllocationPanel: " + t.getMessage()); }
        try { test.testUnavailabilityPanelStyling(); System.out.println("  [PASS] UnavailabilityPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] UnavailabilityPanel: " + t.getMessage()); }
        try { test.testConflictReportPanelStyling(); System.out.println("  [PASS] ConflictReportPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] ConflictReportPanel: " + t.getMessage()); }
        try { test.testWorkloadReportPanelStyling(); System.out.println("  [PASS] WorkloadReportPanel compact styling"); pass++; } catch (Throwable t) { System.err.println("  [FAIL] WorkloadReportPanel: " + t.getMessage()); }

        System.out.println("=============================================================");
        System.out.println("Secondary Panels Verification: " + pass + " passed, " + (9 - pass) + " failed");
        System.out.println("=============================================================");
        if (pass < 9) System.exit(1);
    }
}
