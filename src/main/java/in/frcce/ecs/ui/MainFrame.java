package in.frcce.ecs.ui;

import in.frcce.ecs.model.User;
import in.frcce.ecs.service.AuthService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main application window with compact sidebar navigation, status bar with legend,
 * and CardLayout content area. Sized to fit screen down to 1100x680 without scrollbars.
 */
public class MainFrame extends JFrame {

    private final User        currentUser;
    private final AuthService authService;

    private JPanel contentArea;
    private CardLayout cardLayout;
    private JLabel statusLabel;
    private JPanel sidebarPanel;
    private String currentCard = CARD_TIMETABLE;

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
    private static final String CARD_FACULTY     = "FACULTY";
    private static final String CARD_SUBJECT     = "SUBJECT";
    private static final String CARD_CLASS       = "CLASS";
    private static final String CARD_ROOM        = "ROOM";
    private static final String CARD_SLOT        = "SLOT";
    private static final String CARD_ALLOCATION  = "ALLOCATION";
    private static final String CARD_UNAVAIL     = "UNAVAIL";
    private static final String CARD_CONFLICT    = "CONFLICT";
    private static final String CARD_WORKLOAD    = "WORKLOAD";

    public MainFrame(User user, AuthService authService) {
        super("ECS Timetable Generator");
        this.currentUser = user;
        this.authService = authService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Minimum window size: 1100x680, target design size: 1280x760
        setMinimumSize(new Dimension(1100, 680));
        buildUI();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(1280, screen.width - 40);
        int h = Math.min(760, screen.height - 60);
        setSize(Math.max(1100, w), Math.max(680, h));
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());

        // ---- Top header: 44px fixed height ----
        root.add(buildHeader(), BorderLayout.NORTH);

        // ---- Sidebar (190px) + Content ----
        sidebarPanel = buildSidebar();
        JScrollPane sidebarScroll = new JScrollPane(sidebarPanel);
        sidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setBorder(null);
        sidebarScroll.setPreferredSize(new Dimension(190, 0));
        sidebarScroll.setMinimumSize(new Dimension(190, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                sidebarScroll, buildContent());
        split.setDividerLocation(190);
        split.setDividerSize(1);
        split.setEnabled(false);
        root.add(split, BorderLayout.CENTER);

        // ---- Status bar: 22px fixed height with legend on right ----
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
        showCard(CARD_TIMETABLE);
    }

    // =========================================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.COLOR_HEADER);
        header.setPreferredSize(new Dimension(0, 44));
        header.setMinimumSize(new Dimension(0, 44));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        header.setBorder(new EmptyBorder(0, 16, 0, 16));

        // Left: College name and department separated by an em dash
        JLabel collegeDeptLbl = new JLabel("Fr. Conceicao Rodrigues College of Engineering \u2014 Electronics & Computer Science Department");
        collegeDeptLbl.setForeground(Color.WHITE);
        collegeDeptLbl.setFont(UiTheme.font(Font.BOLD, 12f));

        // Right: User info and Logout button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.setOpaque(false);
        JLabel userLbl = new JLabel("\uD83D\uDC64 " + currentUser.getUsername()
                + " (" + currentUser.getRole() + ")");
        userLbl.setForeground(Color.WHITE);
        userLbl.setFont(UiTheme.font(Font.PLAIN, 12f));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(UiTheme.COLOR_LOGOUT);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFont(UiTheme.font(Font.BOLD, 11f));
        logoutBtn.setPreferredSize(new Dimension(72, 26));
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

        header.add(collegeDeptLbl, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // =========================================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UiTheme.COLOR_SIDEBAR_BG);
        sidebar.setBorder(new EmptyBorder(6, 0, 6, 0));
        sidebar.setPreferredSize(new Dimension(190, 420));
        sidebar.setMinimumSize(new Dimension(190, 0));

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
        lbl.setForeground(UiTheme.COLOR_SIDEBAR_SECTION);
        lbl.setFont(UiTheme.FONT_SECTION);
        lbl.setBorder(new EmptyBorder(8, 10, 2, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lbl);
    }

    private void addSidebarItem(JPanel sidebar, String label, String card, boolean enabled) {
        JButton btn = new JButton(label);
        btn.setMaximumSize(new Dimension(190, 32));
        btn.setPreferredSize(new Dimension(190, 32));
        btn.setMinimumSize(new Dimension(190, 32));
        btn.setFont(UiTheme.font(Font.PLAIN, 12f));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(4, 12, 4, 8));
        btn.setBackground(UiTheme.COLOR_SIDEBAR_BG);
        btn.setForeground(enabled ? UiTheme.COLOR_SIDEBAR_TEXT : UiTheme.COLOR_SIDEBAR_DISABLED);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        btn.setEnabled(enabled);
        btn.putClientProperty("card", card);

        btn.addActionListener(e -> showCard(card));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (enabled && !card.equals(currentCard)) {
                    btn.setBackground(UiTheme.COLOR_SIDEBAR_HOVER);
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(card.equals(currentCard)
                        ? UiTheme.COLOR_SIDEBAR_SELECTED
                        : UiTheme.COLOR_SIDEBAR_BG);
            }
        });

        sidebar.add(btn);
    }

    private void updateSidebarHighlight() {
        if (sidebarPanel == null) return;
        for (Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton b) {
                Object cardProp = b.getClientProperty("card");
                if (cardProp != null) {
                    boolean isSelected = currentCard.equals(cardProp.toString());
                    b.setBackground(isSelected ? UiTheme.COLOR_SIDEBAR_SELECTED : UiTheme.COLOR_SIDEBAR_BG);
                }
            }
        }
    }

    // =========================================================================
    private JPanel buildContent() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(new Color(0xF8, 0xF9, 0xFA));

        // Lazy-load panels on first access
        timetableViewPanel  = new TimetableViewPanel(currentUser, authService, this);
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
        this.currentCard = card;
        cardLayout.show(contentArea, card);
        updateSidebarHighlight();
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
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 22));
        bar.setMinimumSize(new Dimension(0, 22));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        bar.setBackground(UiTheme.COLOR_STATUS_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.COLOR_GRID));

        statusLabel = new JLabel("FrCRCE ECS Timetable Generator v1.0  |  Academic Year 2026-27");
        statusLabel.setFont(UiTheme.FONT_STATUS);
        statusLabel.setForeground(UiTheme.COLOR_STATUS_NORMAL);
        statusLabel.setBorder(new EmptyBorder(1, 8, 1, 8));

        // Legend moved directly into the status bar on the right using 12x12px color squares
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        legendPanel.setOpaque(false);
        addLegendItem(legendPanel, "Theory",     UiTheme.COLOR_TH);
        addLegendItem(legendPanel, "Practical",  UiTheme.COLOR_PR);
        addLegendItem(legendPanel, "Tutorial",   UiTheme.COLOR_TU);
        addLegendItem(legendPanel, "Break",      UiTheme.COLOR_BREAK);
        addLegendItem(legendPanel, "Free",       UiTheme.COLOR_FREE);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(legendPanel, BorderLayout.EAST);
        return bar;
    }

    private void addLegendItem(JPanel panel, String label, Color color) {
        JPanel swatch = new JPanel();
        swatch.setBackground(color);
        swatch.setBorder(BorderFactory.createLineBorder(new Color(0xB0, 0xB5, 0xBD)));
        swatch.setPreferredSize(new Dimension(12, 12));
        panel.add(swatch);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UiTheme.FONT_STATUS);
        lbl.setForeground(UiTheme.COLOR_STATUS_NORMAL);
        panel.add(lbl);
    }

    /**
     * Displays a non-blocking status or error message in the bottom status bar.
     *
     * @param message the text to display
     * @param isError if true, displayed in red (#D93025); if false, displayed in dark grey (#3C4043)
     */
    public void setStatusMessage(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setForeground(isError ? UiTheme.COLOR_STATUS_ERROR : UiTheme.COLOR_STATUS_NORMAL);
        }
    }

    /** Marker interface so panels can refresh their data when shown. */
    public interface Refreshable {
        void refresh();
    }
}
