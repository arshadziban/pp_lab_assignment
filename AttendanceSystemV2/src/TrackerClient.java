import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * GUI client for the Attendance Tracker v2.
 *
 * Design: dark sidebar navigation on the left, content cards on the right,
 * green/amber/red status palette — visually distinct from V1.
 * All RMI calls run on SwingWorker threads to keep the UI responsive.
 */
public class TrackerClient extends JFrame {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color SIDEBAR_BG   = new Color(0x1A1A2E);
    private static final Color SIDEBAR_SEL  = new Color(0x16213E);
    private static final Color ACCENT_TEAL  = new Color(0x0F9B8E);
    private static final Color ACCENT_AMBER = new Color(0xE8A838);
    private static final Color ACCENT_RED   = new Color(0xE84545);
    private static final Color CONTENT_BG   = new Color(0xF0F4F8);
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color TEXT_DARK    = new Color(0x1A1A2E);
    private static final Color TEXT_MUTED   = new Color(0x6B7280);
    private static final Color GOOD_GREEN   = new Color(0x10B981);
    private static final Color WARN_RED     = new Color(0xEF4444);
    private static final Color LOG_BG       = new Color(0x0D1117);
    private static final Color LOG_FG       = new Color(0x58A6FF);

    // ── RMI state ─────────────────────────────────────────────────────────────
    private TrackingService service;

    // ── UI components ─────────────────────────────────────────────────────────
    private JTextField hostInput;
    private JButton    connectBtn;
    private JLabel     connIndicator;

    private JTextField markIdField, markNameField;
    private JRadioButton presentRb, absentRb;

    private JTextField queryIdField;

    private DefaultTableModel tableModel;
    private JTable            dataTable;

    private JTextArea activityLog;
    private JLabel    footerLabel;

    // ── Sidebar nav tracking ──────────────────────────────────────────────────
    private final JPanel[] navBtns = new JPanel[3];
    private final JPanel[] pages   = new JPanel[3];
    private int activePage = 0;

    // ─────────────────────────────────────────────────────────────────────────
    public TrackerClient(String host) {
        super("Attendance Tracker  v2.0");
        buildFrame(host);
    }

    private void buildFrame(String defaultHost) {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildTopBar(defaultHost), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(buildContentArea(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TOP BAR
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel buildTopBar(String defaultHost) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SIDEBAR_BG);
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Left: branding
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setOpaque(false);
        JLabel icon  = new JLabel("◈");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 22));
        icon.setForeground(ACCENT_TEAL);
        JLabel title = new JLabel("Attendance Tracker");
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JLabel ver = new JLabel("v2.0");
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ver.setForeground(new Color(0x9CA3AF));
        brand.add(icon);
        brand.add(title);
        brand.add(ver);

        // Right: connection bar
        JPanel conn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        conn.setOpaque(false);

        JLabel hostLbl = sideLabel("Host:");
        hostLbl.setForeground(new Color(0x9CA3AF));
        hostInput  = new JTextField(defaultHost, 14);
        styleInput(hostInput);
        hostInput.setBackground(new Color(0x0D1117));
        hostInput.setForeground(Color.WHITE);
        hostInput.setCaretColor(ACCENT_TEAL);

        connIndicator = new JLabel("●");
        connIndicator.setFont(new Font("Segoe UI", Font.BOLD, 16));
        connIndicator.setForeground(ACCENT_RED);
        connIndicator.setToolTipText("Disconnected");

        connectBtn = roundBtn("Connect", ACCENT_TEAL);
        connectBtn.addActionListener(e -> doConnect());

        conn.add(hostLbl);
        conn.add(hostInput);
        conn.add(connIndicator);
        conn.add(connectBtn);

        bar.add(brand, BorderLayout.WEST);
        bar.add(conn,  BorderLayout.EAST);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SIDEBAR
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(190, 0));
        side.setBorder(new EmptyBorder(20, 0, 20, 0));

        String[] labels = {"Mark Attendance", "Query Student", "All Records"};
        String[] icons  = {"✎", "⌕", "▤"};

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            navBtns[i] = navItem(icons[i], labels[i], i == 0);
            navBtns[i].addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { switchPage(idx); }
                @Override public void mouseEntered(MouseEvent e) {
                    if (activePage != idx) navBtns[idx].setBackground(new Color(0x0F3460));
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (activePage != idx) navBtns[idx].setBackground(SIDEBAR_BG);
                }
            });
            side.add(navBtns[i]);
            side.add(Box.createVerticalStrut(4));
        }
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel navItem(String icon, String label, boolean active) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        p.setBackground(active ? SIDEBAR_SEL : SIDEBAR_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (active) p.setBorder(new MatteBorder(0, 3, 0, 0, ACCENT_TEAL));
        else        p.setBorder(new EmptyBorder(0, 3, 0, 0));

        JLabel ico = new JLabel(icon);
        ico.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        ico.setForeground(active ? ACCENT_TEAL : new Color(0x9CA3AF));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        lbl.setForeground(active ? Color.WHITE : new Color(0x9CA3AF));

        p.add(ico);
        p.add(lbl);
        return p;
    }

    private void switchPage(int idx) {
        // Update nav appearance
        for (int i = 0; i < navBtns.length; i++) {
            boolean sel = (i == idx);
            navBtns[i].setBackground(sel ? SIDEBAR_SEL : SIDEBAR_BG);
            navBtns[i].setBorder(sel
                    ? new MatteBorder(0, 3, 0, 0, ACCENT_TEAL)
                    : new EmptyBorder(0, 3, 0, 0));
            JLabel ico = (JLabel) ((JPanel) navBtns[i]).getComponent(0);
            JLabel lbl = (JLabel) ((JPanel) navBtns[i]).getComponent(1);
            ico.setForeground(sel ? ACCENT_TEAL : new Color(0x9CA3AF));
            lbl.setForeground(sel ? Color.WHITE  : new Color(0x9CA3AF));
            lbl.setFont(new Font("Segoe UI", sel ? Font.BOLD : Font.PLAIN, 13));
        }
        // Swap page
        pages[activePage].setVisible(false);
        pages[idx].setVisible(true);
        activePage = idx;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CONTENT AREA  (card stack)
    // ═════════════════════════════════════════════════════════════════════════

    private JPanel buildContentArea() {
        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(CONTENT_BG);
        area.setBorder(new EmptyBorder(20, 20, 0, 20));

        pages[0] = buildMarkPage();
        pages[1] = buildQueryPage();
        pages[2] = buildAllRecordsPage();

        pages[1].setVisible(false);
        pages[2].setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        for (JPanel page : pages) {
            area.add(page, gbc);
        }
        return area;
    }

    // ── Page 0: Mark Attendance ───────────────────────────────────────────────
    private JPanel buildMarkPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);

        page.add(pageHeading("✎  Mark Attendance",
                "Record a student's presence or absence for today's class."), BorderLayout.NORTH);

        JPanel form = card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints g = formGbc();

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        form.add(fieldLabel("Student ID  *"), g);
        g.gridy = 1;
        markIdField = styledField("e.g. T001");
        form.add(markIdField, g);

        g.gridy = 2; g.insets = new Insets(14, 0, 4, 0);
        form.add(fieldLabel("Student Name"), g);
        g.gridy = 3; g.insets = new Insets(0, 0, 0, 0);
        markNameField = styledField("e.g. Farhan Ahmed");
        form.add(markNameField, g);

        g.gridy = 4; g.insets = new Insets(16, 0, 6, 0);
        form.add(fieldLabel("Attendance Status  *"), g);

        g.gridy = 5; g.gridwidth = 1; g.insets = new Insets(0, 0, 0, 10);
        presentRb = statusRadio("Present", GOOD_GREEN);
        presentRb.setSelected(true);
        absentRb  = statusRadio("Absent",  WARN_RED);
        ButtonGroup bg = new ButtonGroup();
        bg.add(presentRb);
        bg.add(absentRb);

        JPanel rbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rbRow.setOpaque(false);
        rbRow.add(presentRb);
        rbRow.add(Box.createHorizontalStrut(20));
        rbRow.add(absentRb);
        g.gridwidth = 2; g.insets = new Insets(0, 0, 0, 0);
        form.add(rbRow, g);

        g.gridy = 6; g.insets = new Insets(22, 0, 0, 0);
        JButton markBtn = roundBtn("  ✔  Record Attendance", ACCENT_TEAL);
        markBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        markBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 42));
        markBtn.addActionListener(e -> doMarkAttendance());
        form.add(markBtn, g);

        // Pad to full width
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setOpaque(false);
        formWrapper.add(form, BorderLayout.NORTH);

        page.add(formWrapper, BorderLayout.CENTER);
        page.add(buildLogPanel(), BorderLayout.SOUTH);
        return page;
    }

    // ── Page 1: Query Student ─────────────────────────────────────────────────
    private JPanel buildQueryPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);

        page.add(pageHeading("⌕  Query Student",
                "Look up a student's attendance history or overall percentage."), BorderLayout.NORTH);

        JPanel form = card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints g = formGbc();

        g.gridx = 0; g.gridy = 0;
        form.add(fieldLabel("Student ID  *"), g);
        g.gridy = 1;
        queryIdField = styledField("Enter student ID");
        form.add(queryIdField, g);

        g.gridy = 2; g.insets = new Insets(18, 0, 0, 0);
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);
        JButton histBtn = roundBtn("View History",     ACCENT_TEAL);
        JButton pctBtn  = roundBtn("Get Percentage",   ACCENT_AMBER);
        histBtn.addActionListener(e -> doFetchHistory());
        pctBtn.addActionListener(e -> doFetchPercentage());
        btnRow.add(histBtn);
        btnRow.add(pctBtn);
        form.add(btnRow, g);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(form, BorderLayout.NORTH);
        page.add(wrapper, BorderLayout.CENTER);
        page.add(buildLogPanel(), BorderLayout.SOUTH);
        return page;
    }

    // ── Page 2: All Records ───────────────────────────────────────────────────
    private JPanel buildAllRecordsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);

        page.add(pageHeading("▤  All Records",
                "Browse the complete attendance table or reset the server data."), BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        JButton refreshBtn = roundBtn("⟳  Refresh", ACCENT_TEAL);
        JButton resetBtn   = roundBtn("⊘  Reset All", ACCENT_RED);
        refreshBtn.addActionListener(e -> doFetchAll(true));
        resetBtn.addActionListener(e -> doResetAll());
        toolbar.add(refreshBtn);
        toolbar.add(resetBtn);

        // Table inside a card
        String[] cols = {"Student ID", "Name", "Total Classes", "Present", "Attendance %"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        dataTable = new JTable(tableModel);
        dataTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dataTable.setRowHeight(30);
        dataTable.setShowGrid(false);
        dataTable.setIntercellSpacing(new Dimension(0, 0));
        dataTable.setSelectionBackground(new Color(0xD1FAE5));
        dataTable.setSelectionForeground(TEXT_DARK);
        dataTable.setFillsViewportHeight(true);

        // Striped rows renderer
        dataTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
                if (col == 4 && val instanceof String && !sel) {
                    double pct = Double.parseDouble(((String) val).replace("%", ""));
                    c.setForeground(pct >= 75 ? GOOD_GREEN : WARN_RED);
                    ((JLabel) c).setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else if (!sel) {
                    c.setForeground(TEXT_DARK);
                }
                ((JLabel) c).setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        // Header
        JTableHeader header = dataTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(SIDEBAR_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 36));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        // Column widths
        int[] widths = {110, 200, 110, 80, 120};
        for (int i = 0; i < widths.length; i++) {
            dataTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(dataTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel tableCard = card();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(toolbar, BorderLayout.NORTH);
        tableCard.add(Box.createVerticalStrut(10), BorderLayout.CENTER); // temp
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.add(toolbar, BorderLayout.NORTH);
        tableCard.add(scroll,  BorderLayout.CENTER);

        page.add(tableCard, BorderLayout.CENTER);
        return page;
    }

    // ── Shared log panel ──────────────────────────────────────────────────────
    private JPanel buildLogPanel() {
        activityLog = new JTextArea(6, 0);
        activityLog.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        activityLog.setBackground(LOG_BG);
        activityLog.setForeground(LOG_FG);
        activityLog.setEditable(false);
        activityLog.setCaretColor(ACCENT_TEAL);
        activityLog.setBorder(new EmptyBorder(8, 12, 8, 12));
        activityLog.setText("> Activity log — connect to server to begin.\n");

        JScrollPane scroll = new JScrollPane(activityLog);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x30363D)),
                "Activity Log",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 11), new Color(0x8B949E)));
        scroll.setBackground(LOG_BG);

        JButton clrBtn = new JButton("Clear");
        clrBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clrBtn.setForeground(TEXT_MUTED);
        clrBtn.setBorderPainted(false);
        clrBtn.setContentAreaFilled(false);
        clrBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clrBtn.addActionListener(e -> activityLog.setText(""));

        JPanel logPanel = new JPanel(new BorderLayout(0, 4));
        logPanel.setOpaque(false);
        logPanel.add(scroll,  BorderLayout.CENTER);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(clrBtn);
        logPanel.add(btnRow, BorderLayout.SOUTH);
        return logPanel;
    }

    // ── Footer ─────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel foot = new JPanel(new BorderLayout());
        foot.setBackground(SIDEBAR_BG);
        foot.setBorder(new EmptyBorder(6, 16, 6, 16));
        footerLabel = new JLabel("Not connected  —  start the server then click Connect.");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(0x9CA3AF));
        foot.add(footerLabel, BorderLayout.WEST);

        JLabel copy = new JLabel("Distributed Attendance Tracker  |  Java RMI  |  v2.0");
        copy.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        copy.setForeground(new Color(0x4B5563));
        foot.add(copy, BorderLayout.EAST);
        return foot;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RMI ACTIONS
    // ═════════════════════════════════════════════════════════════════════════

    private void doConnect() {
        String host = hostInput.getText().trim();
        setFooter("Connecting to " + host + " ...", ACCENT_AMBER);
        connectBtn.setEnabled(false);

        new SwingWorker<TrackingService, Void>() {
            @Override protected TrackingService doInBackground() throws Exception {
                Registry reg = LocateRegistry.getRegistry(host, 1099);
                return (TrackingService) reg.lookup("TrackingService");
            }
            @Override protected void done() {
                connectBtn.setEnabled(true);
                try {
                    service = get();
                    connIndicator.setForeground(GOOD_GREEN);
                    connIndicator.setToolTipText("Connected");
                    connectBtn.setText("Reconnect");
                    setFooter("Connected to " + host + ":1099", GOOD_GREEN);
                    logLine("> Connected to RMI server at " + host + ":1099");
                    logLine("> Demo students T001-T004 available on server.");
                    doFetchAll(false);
                } catch (Exception ex) {
                    setFooter("Connection failed: " + ex.getCause().getMessage(), ACCENT_RED);
                    logLine("> ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void doMarkAttendance() {
        if (!guardConnected()) return;
        String id     = markIdField.getText().trim();
        String name   = markNameField.getText().trim();
        String status = presentRb.isSelected() ? "Present" : "Absent";
        if (id.isEmpty()) { warn("Student ID is required."); return; }

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return service.recordAttendance(id, name, status);
            }
            @Override protected void done() {
                try {
                    String res = get();
                    logLine("> " + res);
                    setFooter(res, res.startsWith("[OK]") ? GOOD_GREEN : ACCENT_RED);
                    doFetchAll(false);
                } catch (Exception ex) { logLine("> ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doFetchHistory() {
        if (!guardConnected()) return;
        String id = queryIdField.getText().trim();
        if (id.isEmpty()) { warn("Enter a Student ID first."); return; }

        new SwingWorker<StudentRecord, Void>() {
            @Override protected StudentRecord doInBackground() throws Exception {
                return service.fetchRecord(id);
            }
            @Override protected void done() {
                try {
                    StudentRecord rec = get();
                    if (rec == null) { logLine("> No record found for: " + id); return; }
                    StringBuilder sb = new StringBuilder();
                    sb.append("> ─── History: ").append(rec.getStudentId())
                      .append("  (").append(rec.getStudentName()).append(")\n");
                    List<StudentRecord.Entry> entries = rec.getEntries();
                    for (int i = 0; i < entries.size(); i++) {
                        StudentRecord.Entry e = entries.get(i);
                        sb.append(String.format(">   Class %2d  |  %-8s  |  %s%n",
                                i + 1, e.status, e.timestamp));
                    }
                    sb.append(String.format(">   Summary: %d/%d classes  |  %.1f%%%n",
                            rec.presentCount(), rec.totalClasses(), rec.percentage()));
                    logLine(sb.toString());
                } catch (Exception ex) { logLine("> ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doFetchPercentage() {
        if (!guardConnected()) return;
        String id = queryIdField.getText().trim();
        if (id.isEmpty()) { warn("Enter a Student ID first."); return; }

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return service.fetchPercentage(id);
            }
            @Override protected void done() {
                try { logLine("> " + get()); } catch (Exception ex) { logLine("> ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doFetchAll(boolean verbose) {
        if (service == null) return;

        new SwingWorker<List<StudentRecord>, Void>() {
            @Override protected List<StudentRecord> doInBackground() throws Exception {
                return service.fetchAll();
            }
            @Override protected void done() {
                try {
                    List<StudentRecord> list = get();
                    tableModel.setRowCount(0);
                    for (StudentRecord r : list) {
                        tableModel.addRow(new Object[]{
                            r.getStudentId(), r.getStudentName(),
                            r.totalClasses(), r.presentCount(),
                            String.format("%.1f%%", r.percentage())
                        });
                    }
                    if (verbose) logLine("> Loaded " + list.size() + " record(s) from server.");
                } catch (Exception ex) { logLine("> ERROR loading records: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void doResetAll() {
        if (!guardConnected()) return;
        int choice = JOptionPane.showOptionDialog(this,
                "This will permanently remove ALL records from the server.\nAre you sure?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new Object[]{"Yes, Reset", "Cancel"}, "Cancel");
        if (choice != 0) return;

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return service.resetAll();
            }
            @Override protected void done() {
                try {
                    String res = get();
                    logLine("> " + res);
                    tableModel.setRowCount(0);
                    setFooter(res, ACCENT_AMBER);
                } catch (Exception ex) { logLine("> ERROR: " + ex.getMessage()); }
            }
        }.execute();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private boolean guardConnected() {
        if (service != null) return true;
        logLine("> Not connected. Click Connect in the top bar first.");
        setFooter("Not connected — click Connect.", ACCENT_RED);
        return false;
    }

    private void logLine(String text) {
        SwingUtilities.invokeLater(() -> {
            if (activityLog != null) {
                activityLog.append(text.endsWith("\n") ? text : text + "\n");
                activityLog.setCaretPosition(activityLog.getDocument().getLength());
            }
        });
    }

    private void setFooter(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            footerLabel.setText(text);
            footerLabel.setForeground(color);
        });
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Required", JOptionPane.WARNING_MESSAGE);
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xE5E7EB), 1, true),
                new EmptyBorder(18, 20, 18, 20)));
        return p;
    }

    private JPanel pageHeading(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t.setForeground(TEXT_DARK);
        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        s.setForeground(TEXT_MUTED);
        p.add(t);
        p.add(Box.createVerticalStrut(4));
        p.add(s);
        p.add(Box.createVerticalStrut(12));
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JLabel sideLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(new Color(0x9CA3AF));
        return l;
    }

    private JTextField styledField(String tip) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setToolTipText(tip);
        styleInput(f);
        return f;
    }

    private void styleInput(JTextField f) {
        f.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xD1D5DB), 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        f.setBackground(Color.WHITE);
        f.setForeground(TEXT_DARK);
        f.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private JRadioButton statusRadio(String label, Color color) {
        JRadioButton rb = new JRadioButton(label);
        rb.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rb.setForeground(color);
        rb.setOpaque(false);
        return rb;
    }

    private JButton roundBtn(String label, Color bg) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 16, 6, 16));
        return b;
    }

    private GridBagConstraints formGbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 4, 0);
        g.anchor = GridBagConstraints.NORTHWEST;
        return g;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ═════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new TrackerClient(host));
    }
}
