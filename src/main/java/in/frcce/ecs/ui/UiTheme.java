package in.frcce.ecs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralized typography, color palette, and component styling for the ECS Timetable Generator.
 * Unifies fonts across the application and manages HiDPI scaling and table styling.
 */
public final class UiTheme {

    private UiTheme() {
        // utility class
    }

    // =========================================================================
    // FONT RESOLUTION
    // =========================================================================

    private static final String[] CANDIDATE_FAMILIES = {
            "Segoe UI",
            "SF Pro Text",
            "SF Pro Display",
            "Inter"
    };

    private static final String RESOLVED_FONT_FAMILY;

    static {
        String chosen = Font.SANS_SERIF;
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Set<String> available = new HashSet<>(Arrays.asList(ge.getAvailableFontFamilyNames()));
            for (String candidate : CANDIDATE_FAMILIES) {
                if (available.contains(candidate)) {
                    chosen = candidate;
                    break;
                }
            }
        } catch (Throwable t) {
            chosen = Font.SANS_SERIF;
        }
        RESOLVED_FONT_FAMILY = chosen;

        // Initialize HiDPI property and tooltip font
        init();
    }

    /**
     * Initializes global UI properties such as flatlaf.uiScale and ToolTip font.
     */
    public static void init() {
        try {
            System.setProperty("flatlaf.uiScale", "1.0");
        } catch (Throwable ignored) {
        }
        try {
            UIManager.put("ToolTip.font", font(Font.PLAIN, 12f));
            UIManager.put("ToolTip.background", Color.WHITE);
            UIManager.put("ToolTip.foreground", new Color(0x20, 0x21, 0x24));
            UIManager.put("ToolTip.border", BorderFactory.createLineBorder(COLOR_GRID));
        } catch (Throwable ignored) {
        }
    }

    public static String getFontFamily() {
        return RESOLVED_FONT_FAMILY;
    }

    public static Font font(int style, float size) {
        return new Font(RESOLVED_FONT_FAMILY, style, Math.round(size));
    }

    public static Font bold(float size) {
        return font(Font.BOLD, size);
    }

    public static Font plain(float size) {
        return font(Font.PLAIN, size);
    }

    public static Font italic(float size) {
        return font(Font.ITALIC, size);
    }

    // =========================================================================
    // STANDARD FONTS
    // =========================================================================

    public static final Font FONT_BASE             = font(Font.PLAIN, 12f);
    public static final Font FONT_HEADER           = font(Font.BOLD,  12f);
    public static final Font FONT_CELL_TITLE       = font(Font.BOLD,  11f);
    public static final Font FONT_CELL_SUB         = font(Font.PLAIN, 10f);
    public static final Font FONT_CELL_TINY        = font(Font.PLAIN,  9f);
    public static final Font FONT_CELL_TITLE_TINY  = font(Font.BOLD,  10f);
    public static final Font FONT_CELL_SUB_TINY    = font(Font.PLAIN,  9f);
    public static final Font FONT_CELL_TITLE_MICRO = font(Font.BOLD,   9f);
    public static final Font FONT_CELL_SUB_MICRO   = font(Font.PLAIN,  8f);
    public static final Font FONT_STATUS           = font(Font.PLAIN, 11f);
    public static final Font FONT_SECTION          = font(Font.BOLD,  11f);

    // =========================================================================
    // STANDARD COLORS
    // =========================================================================

    public static final Color COLOR_HEADER           = new Color(0x2F, 0x6F, 0xDE); // #2F6FDE soft blue
    public static final Color COLOR_GRID             = new Color(0xD9, 0xDE, 0xE7); // #D9DEE7 light grey
    public static final Color COLOR_HOVER            = new Color(0xBD, 0xD7, 0xEE); // #BDD7EE hover highlight
    public static final Color COLOR_HOVER_SUBTLE     = new Color(0xE8, 0xF0, 0xFE); // #E8F0FE subtle highlight
    public static final Color COLOR_TH               = new Color(0xD6, 0xE4, 0xFF); // #D6E4FF soft light blue
    public static final Color COLOR_PR               = new Color(0xD3, 0xF4, 0xD8); // #D3F4D8 soft light green
    public static final Color COLOR_TU               = new Color(0xFF, 0xE8, 0xCC); // #FFE8CC soft light orange
    public static final Color COLOR_FREE             = new Color(0xF8, 0xF9, 0xFA); // #F8F9FA clean light background
    public static final Color COLOR_BREAK            = new Color(0xF0, 0xF2, 0xF5); // #F0F2F5 subtle break grey
    public static final Color COLOR_STATUS_ERROR     = new Color(0xD9, 0x30, 0x25); // #D93025 error red
    public static final Color COLOR_STATUS_NORMAL    = new Color(0x3C, 0x40, 0x43); // #3C4043 normal dark grey
    public static final Color COLOR_TOOLBAR_BG       = new Color(0xF1, 0xF3, 0xF4); // #F1F3F4 toolbar grey
    public static final Color COLOR_STATUS_BG        = new Color(0xF1, 0xF3, 0xF4); // #F1F3F4 status bar grey
    public static final Color COLOR_SIDEBAR_BG       = new Color(0x2D, 0x2D, 0x2D); // #2D2D2D dark sidebar
    public static final Color COLOR_SIDEBAR_SECTION  = new Color(0x9A, 0xA0, 0xA6); // section title grey
    public static final Color COLOR_SIDEBAR_TEXT     = new Color(0xE8, 0xEA, 0xED); // item text
    public static final Color COLOR_SIDEBAR_DISABLED = new Color(0x60, 0x60, 0x60); // disabled item
    public static final Color COLOR_SIDEBAR_HOVER    = new Color(0x3D, 0x3D, 0x3D); // sidebar button hover
    public static final Color COLOR_SIDEBAR_SELECTED = new Color(0x2F, 0x6F, 0xDE); // selected nav item
    public static final Color COLOR_LOGOUT           = new Color(0xEA, 0x43, 0x35); // #EA4335 logout button

    // =========================================================================
    // TABLE STYLING UTILITIES
    // =========================================================================

    /**
     * Applies standard compact table styling across the application.
     * Row height: 24px, table font: 12pt plain, header height: 28px, header font: 12pt bold,
     * grid color: #D9DEE7.
     */
    public static void applyTableStyle(JTable table) {
        if (table == null) return;
        table.setRowHeight(24);
        table.setFont(FONT_BASE);
        table.setGridColor(COLOR_GRID);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        applyHeaderStyle(table.getTableHeader());
    }

    /**
     * Applies standard 28px blue header styling to a JTableHeader.
     */
    public static void applyHeaderStyle(JTableHeader header) {
        if (header == null) return;
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 28));
        header.setFont(FONT_HEADER);
        header.setBackground(COLOR_HEADER);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);

        // Ensure header renderer retains background and foreground colors across all Look & Feels
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(COLOR_HEADER);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(FONT_HEADER);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_GRID),
                        BorderFactory.createEmptyBorder(4, 6, 4, 6)
                ));
                lbl.setOpaque(true);
                return lbl;
            }
        });
    }

    /**
     * Styles a standard toolbar button with 28px height, 12pt font, and pointer cursor.
     */
    public static void styleButton(JButton btn) {
        if (btn == null) return;
        btn.setFont(FONT_BASE);
        Dimension pref = btn.getPreferredSize();
        btn.setPreferredSize(new Dimension(Math.max(pref.width, 70), 28));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Styles a primary/action button with 28px height, bold 12pt font, and custom background.
     */
    public static void styleActionButton(JButton btn, Color bg, Color fg) {
        if (btn == null) return;
        styleButton(btn);
        btn.setFont(bold(12f));
        if (bg != null) btn.setBackground(bg);
        if (fg != null) btn.setForeground(fg);
    }

    /**
     * Styles a panel toolbar with standard background and compact padding.
     */
    public static void styleToolbar(JPanel toolbar) {
        if (toolbar == null) return;
        toolbar.setBackground(COLOR_TOOLBAR_BG);
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }
}
