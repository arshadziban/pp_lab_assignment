import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * GUI Client for the Distributed Attendance Management System.
 *
 * Uses Java Swing. All RMI calls are dispatched on a background SwingWorker
 * thread so the UI never freezes. Multiple instances of this class can run
 * simultaneously against the same server, demonstrating concurrent access.
 */
public class AttendanceClient extends JFrame {

    // ── RMI ──────────────────────────────────────────────────────────────────
    private AttendanceService service;
    private final String serverHost;

    // ── Connection panel ─────────────────────────────────────────────────────
    private JTextField hostField;
    private JButton connectBtn;
    private JLabel statusLabel;

    // ── Mark attendance panel ─────────────────────────────────────────────────
    private JTextField studentIdField;
    private JTextField studentNameField;
    private JRadioButton presentRb, absentRb;

    // ── Query panel ───────────────────────────────────────────────────────────
    private JTextField queryIdField;

    // ── Output ────────────────────────────────────────────────────────────────
    private JTextArea outputArea;
    private JTable recordTable;
    private DefaultTableModel tableModel;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color PRIMARY   = new Color(0x1565C0);
    private static final Color ACCENT    = new Color(0x0288D1);
    private static final Color SUCCESS   = new Color(0x2E7D32);
    private static final Color DANGER    = new Color(0xC62828);
    private static final Color BG        = new Color(0xF5F5F5);
    private static final Color PANEL_BG  = Color.WHITE;

    // ── Constructor ───────────────────────────────────────────────────────────
    public AttendanceClient(String serverHost) {
        this.serverHost = serverHost;
        buildUI();
        setTitle("Attendance Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 720);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI BUILD
    // ═════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(8, 8));

        add(buildHeader(),         BorderLayout.NORTH);
        add(buildCenterPanel(),    BorderLayout.CENTER);
        add(buildStatusBar(),      BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PRIMARY);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Distributed Attendance Management System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JPanel text = new JPanel(new GridLayout(1, 1));
        text.setOpaque(false);
        text.add(title);

        // Connection row
        JPanel conn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        conn.setOpaque(false);
        JLabel hostLbl = lbl("Server:", Color.WHITE);
        hostField = new JTextField(serverHost, 12);
        hostField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        connectBtn = styledBtn("Connect", ACCENT);
        connectBtn.addActionListener(e -> connectToServer());

        conn.add(hostLbl);
        conn.add(hostField);
        conn.add(connectBtn);

        p.add(text, BorderLayout.WEST);
        p.add(conn, BorderLayout.EAST);
        return p;
    }

    // ── Center ────────────────────────────────────────────────────────────────
    private JSplitPane buildCenterPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        split.setDividerLocation(340);
        split.setBorder(new EmptyBorder(8, 8, 8, 8));
        split.setResizeWeight(0.38);
        return split;
    }

    // ── Left panel: controls ──────────────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);

        p.add(buildMarkPanel());
        p.add(Box.createVerticalStrut(10));
        p.add(buildQueryPanel());
        p.add(Box.createVerticalStrut(10));
        p.add(buildBulkPanel());
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel buildMarkPanel() {
        JPanel card = card("Mark Attendance");

        studentIdField   = inputField("e.g. S001");
        studentNameField = inputField("e.g. Alice Rahman");

        ButtonGroup bg = new ButtonGroup();
        presentRb = new JRadioButton("Present", true);
        absentRb  = new JRadioButton("Absent");
        styleRb(presentRb, SUCCESS);
        styleRb(absentRb,  DANGER);
        bg.add(presentRb);
        bg.add(absentRb);

        JPanel rbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rbRow.setOpaque(false);
        rbRow.add(presentRb);
        rbRow.add(absentRb);

        JButton markBtn = styledBtn("Mark Attendance", PRIMARY);
        markBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        markBtn.addActionListener(e -> doMarkAttendance());

        addRow(card, "Student ID *",   studentIdField);
        addRow(card, "Student Name",   studentNameField);
        addRow(card, "Status *",       rbRow);
        card.add(Box.createVerticalStrut(8));
        card.add(markBtn);
        return card;
    }

    private JPanel buildQueryPanel() {
        JPanel card = card("Query Student");

        queryIdField = inputField("Enter Student ID");

        JButton historyBtn  = styledBtn("View History",    ACCENT);
        JButton percentBtn  = styledBtn("Get Percentage",  new Color(0x6A1B9A));
        historyBtn.addActionListener(e -> doGetHistory());
        percentBtn.addActionListener(e -> doGetPercentage());

        addRow(card, "Student ID *", queryIdField);
        card.add(Box.createVerticalStrut(8));
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 6, 0));
        btnRow.setOpaque(false);
        btnRow.add(historyBtn);
        btnRow.add(percentBtn);
        card.add(btnRow);
        return card;
    }

    private JPanel buildBulkPanel() {
        JPanel card = card("Bulk Operations");

        JButton allBtn   = styledBtn("Load All Records",  PRIMARY);
        JButton clearBtn = styledBtn("Clear All Records", DANGER);
        allBtn.addActionListener(e -> doGetAllRecords());
        clearBtn.addActionListener(e -> doClearAll());

        JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
        row.setOpaque(false);
        row.add(allBtn);
        row.add(clearBtn);
        card.add(row);
        return card;
    }

    // ── Right panel: output ───────────────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG);

        // Table
        String[] cols = {"Student ID", "Name", "Classes", "Present", "Attendance %"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        recordTable = new JTable(tableModel);
        recordTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        recordTable.setRowHeight(26);
        recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        recordTable.setSelectionBackground(new Color(0xBBDEFB));
        recordTable.setGridColor(new Color(0xDDDDDD));
        recordTable.setShowGrid(true);

        // Custom header renderer to ensure column names display correctly
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setBackground(PRIMARY);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setHorizontalAlignment(CENTER);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 1, new Color(0x90CAF9)));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            recordTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        recordTable.getTableHeader().setPreferredSize(new Dimension(0, 32));
        recordTable.getTableHeader().setReorderingAllowed(false);
        styleTableColumns();

        JScrollPane tableScroll = new JScrollPane(recordTable);
        tableScroll.setBorder(titled("All Records"));
        tableScroll.setPreferredSize(new Dimension(540, 250));

        // Text output
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(0x1E1E2E));
        outputArea.setForeground(new Color(0xCDD6F4));
        outputArea.setCaretColor(Color.WHITE);
        outputArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        outputArea.setText("  Console output will appear here.\n  Connect to server to begin.\n");

        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.setBorder(titled("Console / Response"));

        JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, outScroll);
        vSplit.setResizeWeight(0.5);
        vSplit.setDividerLocation(250);

        p.add(vSplit, BorderLayout.CENTER);

        // Clear console button
        JButton clrConsole = new JButton("Clear Console");
        clrConsole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clrConsole.addActionListener(e -> outputArea.setText(""));
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(clrConsole);
        p.add(btnRow, BorderLayout.SOUTH);

        return p;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setBackground(new Color(0xE3F2FD));
        p.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0xBBDEFB)));

        statusLabel = new JLabel("Not connected  |  Start the server then click Connect.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(0x555555));
        p.add(statusLabel);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RMI ACTIONS  (each runs on a background thread via SwingWorker)
    // ═════════════════════════════════════════════════════════════════════════

    private void connectToServer() {
        String host = hostField.getText().trim();
        setStatus("Connecting to " + host + "...", Color.ORANGE);
        new SwingWorker<AttendanceService, Void>() {
            @Override protected AttendanceService doInBackground() throws Exception {
                Registry reg = LocateRegistry.getRegistry(host, 1099);
                return (AttendanceService) reg.lookup("AttendanceService");
            }
            @Override protected void done() {
                try {
                    service = get();
                    setStatus("Connected to " + host + ":1099", SUCCESS);
                    connectBtn.setText("Reconnect");
                    log("Connected to RMI server at " + host + ":1099");
                    log("Ready. Use the controls on the left to manage attendance.");
                    // auto-load records after successful connect
                    loadAllRecordsSilent();
                } catch (Exception ex) {
                    setStatus("Connection failed: " + ex.getMessage(), DANGER);
                    log("ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void doMarkAttendance() {
        if (!ensureConnected()) return;
        String id     = studentIdField.getText().trim();
        String name   = studentNameField.getText().trim();
        String status = presentRb.isSelected() ? "Present" : "Absent";

        if (id.isEmpty()) { warn("Student ID is required."); return; }

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return service.markAttendance(id, name, status);
            }
            @Override protected void done() {
                try {
                    String resp = get();
                    log(resp);
                    doGetAllRecords();
                } catch (Exception ex) { log("ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doGetHistory() {
        if (!ensureConnected()) return;
        String id = queryIdField.getText().trim();
        if (id.isEmpty()) { warn("Student ID is required."); return; }

        new SwingWorker<AttendanceRecord, Void>() {
            @Override protected AttendanceRecord doInBackground() throws Exception {
                return service.getAttendanceRecord(id);
            }
            @Override protected void done() {
                try {
                    AttendanceRecord rec = get();
                    if (rec == null) { log("No record found for ID: " + id); return; }
                    StringBuilder sb = new StringBuilder();
                    sb.append("─── Attendance History: ").append(rec.getStudentId())
                      .append(" (").append(rec.getStudentName()).append(") ───\n");
                    List<String> hist = rec.getAttendanceHistory();
                    for (int i = 0; i < hist.size(); i++) {
                        sb.append(String.format("  Class %2d : %s%n", i + 1, hist.get(i)));
                    }
                    sb.append(String.format("  Total: %d classes | Present: %d | %.1f%%\n",
                            rec.getTotalClasses(), rec.getPresentCount(), rec.getAttendancePercentage()));
                    log(sb.toString());
                } catch (Exception ex) { log("ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doGetPercentage() {
        if (!ensureConnected()) return;
        String id = queryIdField.getText().trim();
        if (id.isEmpty()) { warn("Student ID is required."); return; }

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return service.getAttendancePercentage(id);
            }
            @Override protected void done() {
                try { log(get()); } catch (Exception ex) { log("ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void loadAllRecordsSilent() {
        if (service == null) return;
        new SwingWorker<List<AttendanceRecord>, Void>() {
            @Override protected List<AttendanceRecord> doInBackground() throws Exception {
                return service.getAllRecords();
            }
            @Override protected void done() {
                try {
                    List<AttendanceRecord> list = get();
                    tableModel.setRowCount(0);
                    for (AttendanceRecord r : list) {
                        tableModel.addRow(new Object[]{
                            r.getStudentId(), r.getStudentName(),
                            r.getTotalClasses(), r.getPresentCount(),
                            String.format("%.1f%%", r.getAttendancePercentage())
                        });
                    }
                    log("Loaded " + list.size() + " record(s) from server.");
                } catch (Exception ex) { log("ERROR loading records: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doGetAllRecords() {
        if (!ensureConnected()) return;
        new SwingWorker<List<AttendanceRecord>, Void>() {
            @Override protected List<AttendanceRecord> doInBackground() throws Exception {
                return service.getAllRecords();
            }
            @Override protected void done() {
                try {
                    List<AttendanceRecord> list = get();
                    tableModel.setRowCount(0);
                    for (AttendanceRecord r : list) {
                        tableModel.addRow(new Object[]{
                            r.getStudentId(),
                            r.getStudentName(),
                            r.getTotalClasses(),
                            r.getPresentCount(),
                            String.format("%.1f%%", r.getAttendancePercentage())
                        });
                    }
                    log("Loaded " + list.size() + " record(s) from server.");
                } catch (Exception ex) { log("ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doClearAll() {
        if (!ensureConnected()) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "This will remove ALL records from the server.\nProceed?",
                "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return service.clearAllRecords();
            }
            @Override protected void done() {
                try {
                    log(get());
                    tableModel.setRowCount(0);
                } catch (Exception ex) { log("ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private boolean ensureConnected() {
        if (service != null) return true;
        log("Not connected. Please click Connect first.");
        setStatus("Not connected — click Connect to start.", DANGER);
        return false;
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(msg + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void setStatus(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setForeground(color);
        });
    }

    // ── Widget factories ──────────────────────────────────────────────────────

    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                titled(title),
                new EmptyBorder(6, 8, 8, 8)));
        return p;
    }

    private JTextField inputField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xBBBBBB), 1, true),
                new EmptyBorder(4, 6, 4, 6)));
        f.setToolTipText(placeholder);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return f;
    }

    private void addRow(JPanel panel, String labelText, JComponent comp) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(2));
        panel.add(comp);
        panel.add(Box.createVerticalStrut(8));
    }

    private JButton styledBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        // Hover effect
        b.addMouseListener(new MouseAdapter() {
            Color orig = bg;
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(orig.darker());
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(orig);
            }
        });
        return b;
    }

    private void styleRb(JRadioButton rb, Color color) {
        rb.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rb.setForeground(color);
        rb.setOpaque(false);
    }

    private JLabel lbl(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(color);
        return l;
    }

    private TitledBorder titled(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                new LineBorder(new Color(0xBBDEFB), 1, true), title);
        b.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setTitleColor(PRIMARY);
        return b;
    }

    private void styleTableColumns() {
        int[] widths = {100, 180, 75, 75, 120};
        for (int i = 0; i < widths.length; i++) {
            recordTable.getColumnModel().getColumn(i).setMinWidth(widths[i]);
            recordTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        // Colour the percentage column by value
        recordTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (val instanceof String) {
                    double pct = Double.parseDouble(((String) val).replace("%", ""));
                    if (!sel) c.setForeground(pct >= 75 ? SUCCESS : DANGER);
                }
                setHorizontalAlignment(CENTER);
                return c;
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ═════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        // Use system look-and-feel for a native feel
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AttendanceClient(host));
    }
}
