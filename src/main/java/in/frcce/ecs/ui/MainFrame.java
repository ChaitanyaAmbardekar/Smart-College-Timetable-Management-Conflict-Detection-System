package in.frcce.ecs.ui;

import in.frcce.ecs.model.User;
import in.frcce.ecs.service.AuthService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main application window with sidebar navigation and JTabbedPane content area.
 */
public class MainFrame extends JFrame {

    private final User        currentUser;
    private final AuthService authService;

    private JPanel contentArea;
    private CardLayout cardLayout;

    // Panel instances (lazy-loaded)
    private TimetableViewPanel  timetableViewPanel;
    private FacultyPanel        facultyPanel;
    private SubjectPanel        subjectPanel;
    private ClassPanel          classPanel;
    private RoomPanel           roomPanel;
    private TimeSlotPanel       timeSlotPanel;
    private AllocationPanel     allocationPanel;
    private UnavailabilityPanel unavailabilityPanel;
    private ConflictReportPanel conflictReportPanel;
    private WorkloadReportPanel workloadReportPanel;

    // Card names
    private static final String CARD_TIMETABLE   = "TIMETABLE";
    private static final String CARD_FACULTY      = "FACULTY";
    private static final String CARD_SUBJECT      = "SUBJECT";
    private static final String CARD_CLASS        = "CLASS";
    private static final String CARD_ROOM         = "ROOM";
    private static final String CARD_SLOT         = "SLOT";
    private static final String CARD_ALLOCATION   = "ALLOCATION";
    private static final String CARD_UNAVAIL      = "UNAVAIL";
    private static final String CARD_CONFLICT     = "CONFLICT";
    private static final String CARD_WORKLOAD     = "WORKLOAD";

    public MainFrame(User user, AuthService authService) {
        super("ECS Timetable Generator");
        this.currentUser = user;
        this.authService = authService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 720));
        buildUI();
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());

        // ---- Top header ----
        root.add(buildHeader(), BorderLayout.NORTH);

        // ---- Sidebar + Content ----
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), buildContent());
        split.setDividerLocation(220);
        split.setDividerSize(1);
        split.setEnabled(false);
        root.add(split, BorderLayout.CENTER);

        // ---- Status bar ----
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
        showCard(CARD_TIMETABLE);
    }

    // =========================================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x1A, 0x73, 0xE8));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        JLabel collegeLbl = new JLabel("Fr. Conceicao Rodrigues College of Engineering, Bandra, Mumbai");
        collegeLbl.setForeground(Color.WHITE);
        collegeLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel deptLbl = new JLabel("Electronics and Computer Science Department — Faculty Timetable Generator");
        deptLbl.setForeground(new Color(220, 230, 255));
        deptLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        left.add(collegeLbl);
        left.add(deptLbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel userLbl = new JLabel("\uD83D\uDC64 " + currentUser.getUsername()
                + " (" + currentUser.getRole() + ")");
        userLbl.setForeground(Color.WHITE);
        userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(new Color(0xEA, 0x43, 0x35));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?", "Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                authService.logout();
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        right.add(userLbl);
        right.add(logoutBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // =========================================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(0x2D, 0x2D, 0x2D));
        sidebar.setBorder(new EmptyBorder(10, 0, 10, 0));
        sidebar.setPreferredSize(new Dimension(220, 600));

        addSidebarSection(sidebar, "TIMETABLE");
        addSidebarItem(sidebar, "\uD83D\uDDD3  View Timetable", CARD_TIMETABLE, true);

        addSidebarSection(sidebar, "MASTER DATA");
        addSidebarItem(sidebar, "\uD83D\uDC68\u200D\uD83C\uDFEB  Faculty",        CARD_FACULTY,  authService.isAdmin());
        addSidebarItem(sidebar, "\uD83D\uDCDA  Subjects",      CARD_SUBJECT, authService.isAdmin());
        addSidebarItem(sidebar, "\uD83C\uDFEB  Classes",       CARD_CLASS,   authService.isAdmin());
        addSidebarItem(sidebar, "\uD83C\uDFE2  Rooms",         CARD_ROOM,    authService.isAdmin());
        addSidebarItem(sidebar, "\u23F0  Time Slots",          CARD_SLOT,    authService.isAdmin());

        addSidebarSection(sidebar, "ALLOCATION");
        addSidebarItem(sidebar, "\uD83D\uDD17  Faculty-Subject", CARD_ALLOCATION, authService.isAdmin());
        addSidebarItem(sidebar, "\uD83D\uDEAB  Unavailability",  CARD_UNAVAIL,    authService.isAdmin());

        addSidebarSection(sidebar, "REPORTS");
        addSidebarItem(sidebar, "\u26A0\uFE0F  Conflict Report", CARD_CONFLICT, true);
        addSidebarItem(sidebar, "\uD83D\uDCCA  Workload Report", CARD_WORKLOAD, true);

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private void addSidebarSection(JPanel sidebar, String title) {
        JLabel lbl = new JLabel("  " + title);
        lbl.setForeground(new Color(0x9A, 0xA0, 0xA6));
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setBorder(new EmptyBorder(14, 10, 4, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lbl);
    }

    private void addSidebarItem(JPanel sidebar, String label, String card, boolean enabled) {
        JButton btn = new JButton(label);
        btn.setMaximumSize(new Dimension(220, 38));
        btn.setPreferredSize(new Dimension(220, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(6, 16, 6, 8));
        btn.setBackground(new Color(0x2D, 0x2D, 0x2D));
        btn.setForeground(enabled ? new Color(0xE8, 0xEA, 0xED) : new Color(0x60, 0x60, 0x60));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        btn.setEnabled(enabled);
        btn.putClientProperty("card", card);

        btn.addActionListener(e -> {
            showCard(card);
            // Highlight selected
            for (Component c : sidebar.getComponents()) {
                if (c instanceof JButton b) {
                    b.setBackground(card.equals(b.getClientProperty("card"))
                            ? new Color(0x1A, 0x73, 0xE8)
                            : new Color(0x2D, 0x2D, 0x2D));
                }
            }
        });

        btn.addMouseListener(new MouseAdapter() {
            final Color normal  = btn.getBackground();
            final Color hover   = new Color(0x3D, 0x3D, 0x3D);
            @Override public void mouseEntered(MouseEvent e) { if (enabled) btn.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(normal); }
        });

        sidebar.add(btn);
    }

    // =========================================================================
    private JPanel buildContent() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(new Color(0xF8, 0xF9, 0xFA));

        // Lazy-load panels on first access
        timetableViewPanel  = new TimetableViewPanel(currentUser, authService);
        facultyPanel        = new FacultyPanel();
        subjectPanel        = new SubjectPanel();
        classPanel          = new ClassPanel();
        roomPanel           = new RoomPanel();
        timeSlotPanel       = new TimeSlotPanel();
        allocationPanel     = new AllocationPanel();
        unavailabilityPanel = new UnavailabilityPanel();
        conflictReportPanel = new ConflictReportPanel();
        workloadReportPanel = new WorkloadReportPanel();

        contentArea.add(timetableViewPanel,  CARD_TIMETABLE);
        contentArea.add(facultyPanel,        CARD_FACULTY);
        contentArea.add(subjectPanel,        CARD_SUBJECT);
        contentArea.add(classPanel,          CARD_CLASS);
        contentArea.add(roomPanel,           CARD_ROOM);
        contentArea.add(timeSlotPanel,       CARD_SLOT);
        contentArea.add(allocationPanel,     CARD_ALLOCATION);
        contentArea.add(unavailabilityPanel, CARD_UNAVAIL);
        contentArea.add(conflictReportPanel, CARD_CONFLICT);
        contentArea.add(workloadReportPanel, CARD_WORKLOAD);

        return contentArea;
    }

    private void showCard(String card) {
        cardLayout.show(contentArea, card);
        // Refresh data when switching to a panel
        Component shown = getVisibleCard();
        if (shown instanceof Refreshable r) r.refresh();
    }

    private Component getVisibleCard() {
        for (Component c : contentArea.getComponents()) {
            if (c.isVisible()) return c;
        }
        return null;
    }

    // =========================================================================
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bar.setBackground(new Color(0xF1, 0xF3, 0xF4));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xDA, 0xDC, 0xE0)));
        JLabel lbl = new JLabel("FrCRCE ECS Timetable Generator v1.0  |  Academic Year 2026-27");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Color.GRAY);
        bar.add(lbl);
        return bar;
    }

    /** Marker interface so panels can refresh their data when shown. */
    public interface Refreshable {
        void refresh();
    }
}

