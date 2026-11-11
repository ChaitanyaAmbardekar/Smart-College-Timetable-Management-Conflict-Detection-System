package in.frcce.ecs.ui;

import in.frcce.ecs.dao.RoomDAO;
import in.frcce.ecs.dao.TimeSlotDAO;
import in.frcce.ecs.dao.TimetableEntryDAO;
import in.frcce.ecs.model.Room;
import in.frcce.ecs.model.TimeSlot;
import in.frcce.ecs.model.TimetableEntry;
import in.frcce.ecs.service.TimetableService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Dialog to manually move or edit a timetable entry.
 * Validates conflicts before saving and gives clear feedback.
 * Uses compact padding, UiTheme typography, and 28px buttons.
 */
public class EditEntryDialog extends JDialog {

    private final TimetableEntry     entry;
    private final TimetableService   timetableService;
    private final TimetableEntryDAO  entryDAO;
    private final String             academicYear;
    private final Runnable           onSaveCallback;

    private JComboBox<SlotItem> slotCombo;
    private JComboBox<RoomItem> roomCombo;
    private JCheckBox           lockCheck;

    public EditEntryDialog(Component parent, TimetableEntry entry,
                           TimetableService timetableService,
                           TimetableEntryDAO entryDAO,
                           String academicYear,
                           Runnable onSaveCallback) {
        super(SwingUtilities.getWindowAncestor(parent), "Edit Timetable Entry", ModalityType.APPLICATION_MODAL);
        this.entry            = entry;
        this.timetableService = timetableService;
        this.entryDAO         = entryDAO;
        this.academicYear     = academicYear;
        this.onSaveCallback   = onSaveCallback;

        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 14, 10, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 4, 3, 4);

        // Display current info (read-only)
        gbc.gridx = 0; gbc.gridy = 0; form.add(makeFieldLabel("Subject:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        JLabel subLbl = new JLabel("<html><b>" + entry.getSubjectName() + " (" + entry.getSessionType() + ")</b></html>");
        subLbl.setFont(UiTheme.FONT_BASE);
        form.add(subLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(makeFieldLabel("Faculty:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        JLabel facLbl = new JLabel(entry.getFacultyName() != null ? entry.getFacultyName() : "-");
        facLbl.setFont(UiTheme.FONT_BASE);
        form.add(facLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(makeFieldLabel("Batch:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        JLabel batchLbl = new JLabel(entry.getBatchName() != null ? entry.getBatchName() : "Whole Class");
        batchLbl.setFont(UiTheme.FONT_BASE);
        form.add(batchLbl, gbc);

        // Editable: Time Slot
        gbc.gridx = 0; gbc.gridy = 3; form.add(makeFieldLabel("New Time Slot:"), gbc);
        slotCombo = new JComboBox<>();
        slotCombo.setFont(UiTheme.FONT_BASE);
        slotCombo.setPreferredSize(new Dimension(280, 28));
        TimeSlotDAO slotDAO = new TimeSlotDAO();
        for (TimeSlot ts : slotDAO.findAll()) {
            SlotItem item = new SlotItem(ts);
            slotCombo.addItem(item);
            if (ts.getSlotId() == entry.getSlotId()) slotCombo.setSelectedItem(item);
        }
        gbc.gridx = 1; gbc.gridy = 3; form.add(slotCombo, gbc);

        // Editable: Room
        gbc.gridx = 0; gbc.gridy = 4; form.add(makeFieldLabel("New Room:"), gbc);
        roomCombo = new JComboBox<>();
        roomCombo.setFont(UiTheme.FONT_BASE);
        roomCombo.setPreferredSize(new Dimension(280, 28));
        RoomDAO roomDAO = new RoomDAO();
        for (Room r : roomDAO.findAll()) {
            RoomItem item = new RoomItem(r);
            roomCombo.addItem(item);
            if (r.getRoomId() == entry.getRoomId()) roomCombo.setSelectedItem(item);
        }
        gbc.gridx = 1; gbc.gridy = 4; form.add(roomCombo, gbc);

        // Lock entry
        gbc.gridx = 0; gbc.gridy = 5; form.add(makeFieldLabel("Lock Entry:"), gbc);
        lockCheck = new JCheckBox("Prevent moving during re-generation", entry.isLocked());
        lockCheck.setFont(UiTheme.FONT_BASE);
        gbc.gridx = 1; gbc.gridy = 5; form.add(lockCheck, gbc);

        // Buttons (28px height)
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UiTheme.plain(12f));
        cancelBtn.setPreferredSize(new Dimension(80, 28));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Save Changes");
        saveBtn.setFont(UiTheme.bold(12f));
        saveBtn.setBackground(UiTheme.COLOR_HEADER);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(115, 28));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> onSave());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 4, 2, 4);
        form.add(btnPanel, gbc);

        setContentPane(form);
    }

    private JLabel makeFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UiTheme.font(Font.BOLD, 11f));
        lbl.setForeground(new Color(0x3C, 0x40, 0x43));
        return lbl;
    }

    private void onSave() {
        SlotItem selectedSlot = (SlotItem) slotCombo.getSelectedItem();
        RoomItem selectedRoom = (RoomItem) roomCombo.getSelectedItem();
        if (selectedSlot == null || selectedRoom == null) return;

        // Build a proposed copy to validate conflicts
        TimetableEntry proposed = new TimetableEntry();
        proposed.setEntryId(entry.getEntryId());
        proposed.setClassId(entry.getClassId());
        proposed.setBatchId(entry.getBatchId());
        proposed.setBatchName(entry.getBatchName());
        proposed.setSubjectId(entry.getSubjectId());
        proposed.setFacultyId(entry.getFacultyId());
        proposed.setRoomId(selectedRoom.getRoom().getRoomId());
        proposed.setSlotId(selectedSlot.getSlot().getSlotId());
        proposed.setDay(selectedSlot.getSlot().getDay());
        proposed.setSessionType(entry.getSessionType());

        // Validate conflicts (ignore conflict with itself)
        boolean slotChanged = selectedSlot.getSlot().getSlotId() != entry.getSlotId();
        boolean roomChanged = selectedRoom.getRoom().getRoomId() != entry.getRoomId();

        if (slotChanged || roomChanged) {
            List<String> conflicts = timetableService.checkConflicts(proposed, academicYear);
            if (!conflicts.isEmpty()) {
                StringBuilder msg = new StringBuilder("<html><b>Cannot move entry — clashes detected:</b><ul>");
                for (String c : conflicts) msg.append("<li>").append(c).append("</li>");
                msg.append("</ul></html>");
                JOptionPane.showMessageDialog(this, msg.toString(), "Conflict Detected",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Apply changes
        entry.setSlotId(selectedSlot.getSlot().getSlotId());
        entry.setRoomId(selectedRoom.getRoom().getRoomId());
        entry.setLocked(lockCheck.isSelected());

        entryDAO.update(entry);

        JOptionPane.showMessageDialog(this, "Entry updated successfully.", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
        dispose();
        if (onSaveCallback != null) onSaveCallback.run();
    }

    // Package-private helpers for automated verification
    JComboBox<?> getSlotComboForTesting() { return slotCombo; }
    JComboBox<?> getRoomComboForTesting() { return roomCombo; }
    JCheckBox getLockCheckForTesting() { return lockCheck; }

    private static class SlotItem {
        private final TimeSlot slot;
        SlotItem(TimeSlot slot) { this.slot = slot; }
        TimeSlot getSlot() { return slot; }
        @Override public String toString() {
            return slot.getDay() + " S" + slot.getSlotNo() + " (" + slot.getDisplayTime() + ")";
        }
    }

    private static class RoomItem {
        private final Room room;
        RoomItem(Room room) { this.room = room; }
        Room getRoom() { return room; }
        @Override public String toString() {
            return room.getName() + " (" + room.getType() + ", cap " + room.getCapacity() + ")";
        }
    }
}
