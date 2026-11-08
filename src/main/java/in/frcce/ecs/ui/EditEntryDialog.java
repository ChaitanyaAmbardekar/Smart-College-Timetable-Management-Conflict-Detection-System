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
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Display current info (read-only)
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        form.add(new JLabel("<html><b>" + entry.getSubjectName() + " (" + entry.getSessionType() + ")</b></html>"), gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Faculty:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; form.add(new JLabel(entry.getFacultyName() != null ? entry.getFacultyName() : "-"), gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Batch:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; form.add(new JLabel(entry.getBatchName() != null ? entry.getBatchName() : "Whole Class"), gbc);

        // Editable: Time Slot
        gbc.gridx = 0; gbc.gridy = 3; form.add(new JLabel("New Time Slot:"), gbc);
        slotCombo = new JComboBox<>();
        TimeSlotDAO slotDAO = new TimeSlotDAO();
        for (TimeSlot ts : slotDAO.findAll()) {
            SlotItem item = new SlotItem(ts);
            slotCombo.addItem(item);
            if (ts.getSlotId() == entry.getSlotId()) slotCombo.setSelectedItem(item);
        }
        gbc.gridx = 1; gbc.gridy = 3; form.add(slotCombo, gbc);

        // Editable: Room
        gbc.gridx = 0; gbc.gridy = 4; form.add(new JLabel("New Room:"), gbc);
        roomCombo = new JComboBox<>();
        RoomDAO roomDAO = new RoomDAO();
        for (Room r : roomDAO.findAll()) {
            RoomItem item = new RoomItem(r);
            roomCombo.addItem(item);
            if (r.getRoomId() == entry.getRoomId()) roomCombo.setSelectedItem(item);
        }
        gbc.gridx = 1; gbc.gridy = 4; form.add(roomCombo, gbc);

        // Lock entry
        gbc.gridx = 0; gbc.gridy = 5; form.add(new JLabel("Lock Entry:"), gbc);
        lockCheck = new JCheckBox("Prevent moving during re-generation", entry.isLocked());
        gbc.gridx = 1; gbc.gridy = 5; form.add(lockCheck, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton saveBtn = new JButton("Save Changes");
        saveBtn.setBackground(new Color(0x1A, 0x73, 0xE8));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> onSave());

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 5, 0, 5);
        form.add(btnPanel, gbc);

        setContentPane(form);
    }

    private void onSave() {
        SlotItem selectedSlot = (SlotItem) slotCombo.getSelectedItem();
        RoomItem selectedRoom = (RoomItem) roomCombo.getSelectedItem();
        if (selectedSlot == null || selectedRoom == null) return;

        // Build a proposed copy to validate conflicts
        TimetableEntry proposed = new TimetableEntry();
        proposed.setEntryId(entry.getEntryId());
        proposed.setClassId(entry.getClassId());
        proposed.setFacultyId(entry.getFacultyId());
        proposed.setRoomId(selectedRoom.getRoom().getRoomId());
        proposed.setSlotId(selectedSlot.getSlot().getSlotId());
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

