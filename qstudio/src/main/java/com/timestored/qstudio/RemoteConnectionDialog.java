package com.timestored.qstudio;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.timestored.swingxx.SwingUtils;
import com.timestored.sync.RemoteFileInfo;
import com.timestored.sync.SftpClient;

import lombok.Data;
import lombok.Getter;

class RemoteConnectionDialog extends JDialog {

    private static final Logger LOG = Logger.getLogger(RemoteConnectionDialog.class.getName());

    private final JTextField hostField = new JTextField(30);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(22, 1, 65535, 1));
    private final JTextField usernameField = new JTextField(30);
    private final JPasswordField passwordField = new JPasswordField(30);
    private final JTextField keyFileField = new JTextField(40);
    private final JButton keyBrowseButton = new JButton("...");
    private final JPasswordField passphraseField = new JPasswordField(40);
    private final JTextField remotePathField = new JTextField(50);
    private final JButton browseButton = new JButton("Browse...");

    @Getter private ConnectionResult result;

    @Data
    static class ConnectionResult {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final File privateKey;
        private final String passphrase;
        private final String remotePath;
    }

    RemoteConnectionDialog(Window owner, MyPreferences prefs) {
        super(owner, "Open Remote SSH Folder", ModalityType.APPLICATION_MODAL);
        SwingUtils.addEscapeCloseListener(this);

        hostField.setText(prefs.getRemoteHost());
        portSpinner.setValue(prefs.getRemotePort());
        usernameField.setText(prefs.getRemoteUsername());
        remotePathField.setText(prefs.getRemotePath());
        passwordField.setText(prefs.getRemotePassword());
        keyFileField.setText(prefs.getRemoteKeyPath());
        passphraseField.setText(prefs.getRemotePassphrase());

        JPanel form = buildForm();
        JPanel buttons = buildButtons(prefs);

        setLayout(new BorderLayout(10, 10));
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setSize(800, getPreferredSize().height + 20);
        setMinimumSize(new java.awt.Dimension(800, getSize().height));
        setLocationRelativeTo(owner);
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(p, gbc, row++, "Host:", hostField);
        addRow(p, gbc, row++, "Port:", portSpinner);
        addRow(p, gbc, row++, "Username:", usernameField);
        addRow(p, gbc, row++, "Password:", passwordField);

        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Private Key (optional):"), gbc);
        JPanel keyPanel = new JPanel(new BorderLayout(4, 0));
        keyPanel.add(keyFileField, BorderLayout.CENTER);
        JPanel keyButtonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
        keyButtonPanel.add(keyBrowseButton);
        // Detect button — scans for SSH keys only when clicked
        JButton detectBtn = new JButton("Detect");
        detectBtn.setToolTipText("Scan for SSH keys in ~/.ssh/ and PuTTY");
        detectBtn.addActionListener(e -> {
            List<File> detectedKeys = detectSshKeys();
            if (detectedKeys.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No SSH keys found in ~/.ssh/ or PuTTY sessions.",
                    "No Keys Found", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JPopupMenu popup = new JPopupMenu();
                for (File key : detectedKeys) {
                    JMenuItem item = new JMenuItem(key.getAbsolutePath());
                    item.addActionListener(ae -> keyFileField.setText(key.getAbsolutePath()));
                    popup.add(item);
                }
                popup.show(detectBtn, 0, detectBtn.getHeight());
            }
        });
        keyButtonPanel.add(detectBtn);
        keyPanel.add(keyButtonPanel, BorderLayout.EAST);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(keyPanel, gbc);
        gbc.weightx = 0;
        row++;

        addRow(p, gbc, row++, "Passphrase:", passphraseField);
        addRow(p, gbc, row++, "Remote Directory:", remotePathField);

        gbc.gridx = 1; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        p.add(browseButton, gbc);

        keyBrowseButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                keyFileField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        browseButton.addActionListener(e -> browseRemoteDirectory());

        return p;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        p.add(field, gbc);
    }

    private JPanel buildButtons(MyPreferences prefs) {
        JPanel p = new JPanel();
        JButton okButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            String host = hostField.getText().trim();
            int port = (Integer) portSpinner.getValue();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String keyPath = keyFileField.getText().trim();
            String passphrase = new String(passphraseField.getPassword());
            String remotePath = remotePathField.getText().trim();

            if (host.isEmpty() || username.isEmpty() || remotePath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Host, Username, and Remote Directory are required.");
                return;
            }

            File privateKey = keyPath.isEmpty() ? null : new File(keyPath);
            result = new ConnectionResult(host, port, username, password,
                    privateKey, passphrase.isEmpty() ? null : passphrase, remotePath);

            prefs.setRemoteHost(host);
            prefs.setRemotePort(port);
            prefs.setRemoteUsername(username);
            prefs.setRemotePath(remotePath);
            prefs.setRemotePassword(password);
            prefs.setRemoteKeyPath(keyPath);
            prefs.setRemotePassphrase(passphrase);

            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        p.add(okButton);
        p.add(cancelButton);
        getRootPane().setDefaultButton(okButton);
        return p;
    }

    private SftpClient createClient() throws IOException {
        String host = hostField.getText().trim();
        int port = (Integer) portSpinner.getValue();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String keyPath = keyFileField.getText().trim();
        String passphrase = new String(passphraseField.getPassword());

        SftpClient client;
        if (!keyPath.isEmpty()) {
            client = new SftpClient(host, port, username, new File(keyPath),
                    passphrase.isEmpty() ? null : passphrase);
        } else {
            client = new SftpClient(host, port, username, password);
        }
        client.connect();
        return client;
    }

    private void browseRemoteDirectory() {
        SftpClient client = null;
        try {
            client = createClient();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Connection failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            LOG.log(Level.WARNING, "Browse remote failed", ex);
            return;
        }

        final SftpClient sftpClient = client;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("/");
        rootNode.add(new DefaultMutableTreeNode("Loading..."));
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(true);

        // Lazy-load children when a node is expanded
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                if (node.getChildCount() == 1 && node.getFirstChild().toString().equals("Loading...")) {
                    // Load children asynchronously to avoid freezing the GUI
                    String path = getNodePath(node);
                    new SwingWorker<List<RemoteFileInfo>, Void>() {
                        @Override protected List<RemoteFileInfo> doInBackground() throws Exception {
                            return sftpClient.listDir(path);
                        }
                        @Override protected void done() {
                            node.removeAllChildren();
                            try {
                                List<RemoteFileInfo> entries = get();
                                for (RemoteFileInfo entry : entries) {
                                    if (entry.isDirectory()) {
                                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(entry.getName());
                                        child.add(new DefaultMutableTreeNode("Loading..."));
                                        node.add(child);
                                    }
                                }
                                if (node.getChildCount() == 0) {
                                    node.add(new DefaultMutableTreeNode("(empty)"));
                                }
                            } catch (Exception ex) {
                                node.add(new DefaultMutableTreeNode("(error: " + ex.getMessage() + ")"));
                                LOG.log(Level.WARNING, "Failed to list " + path, ex);
                            }
                            treeModel.reload(node);
                        }
                    }.execute();
                }
            }
            @Override public void treeWillCollapse(TreeExpansionEvent event) {}
        });

        // Eagerly load root's children
        rootNode.removeAllChildren();
        try {
            List<RemoteFileInfo> entries = sftpClient.listDir("/");
            for (RemoteFileInfo entry : entries) {
                if (entry.isDirectory()) {
                    DefaultMutableTreeNode child = new DefaultMutableTreeNode(entry.getName());
                    child.add(new DefaultMutableTreeNode("Loading..."));
                    rootNode.add(child);
                }
            }
        } catch (IOException ex) {
            rootNode.add(new DefaultMutableTreeNode("(error: " + ex.getMessage() + ")"));
        }
        treeModel.reload(rootNode);

        // Pre-expand to current path, or user's home directory if empty
        String currentPath = remotePathField.getText().trim();
        if (currentPath.isEmpty()) {
            try {
                currentPath = sftpClient.getHomeDirectory();
                remotePathField.setText(currentPath);
            } catch (IOException ex) {
                LOG.fine("Could not detect home directory: " + ex.getMessage());
            }
        }
        if (!currentPath.isEmpty()) {
            expandToPath(tree, treeModel, rootNode, currentPath, sftpClient);
        }

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new java.awt.Dimension(600, 400));

        JButton selectButton = new JButton("Select");
        JButton cancelBtn = new JButton("Cancel");

        JDialog browseDialog = new JDialog(this, "Select Remote Directory", true);
        SwingUtils.addEscapeCloseListener(browseDialog);

        selectButton.setEnabled(false);
        tree.addTreeSelectionListener(e -> {
            TreePath sel = tree.getSelectionPath();
            if (sel == null) {
                selectButton.setEnabled(false);
                return;
            }
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) sel.getLastPathComponent();
            String nodePath = getNodePath(selected);
            // Block selecting root "/"
            selectButton.setEnabled(!nodePath.equals("/"));
        });

        selectButton.addActionListener(e -> {
            TreePath sel = tree.getSelectionPath();
            if (sel != null) {
                DefaultMutableTreeNode selected = (DefaultMutableTreeNode) sel.getLastPathComponent();
                String selectedPath = getNodePath(selected);
                if (!selectedPath.equals("/")) {
                    remotePathField.setText(selectedPath);
                    browseDialog.dispose();
                }
            }
        });
        cancelBtn.addActionListener(e -> browseDialog.dispose());

        // Double-click to select (non-root only)
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath sel = tree.getSelectionPath();
                    if (sel != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) sel.getLastPathComponent();
                        String path = getNodePath(node);
                        if (!path.equals("/")) {
                            remotePathField.setText(path);
                            browseDialog.dispose();
                        }
                    }
                }
            }
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(selectButton);
        btnPanel.add(cancelBtn);

        browseDialog.setLayout(new BorderLayout(5, 5));
        browseDialog.add(scrollPane, BorderLayout.CENTER);
        browseDialog.add(btnPanel, BorderLayout.SOUTH);
        browseDialog.pack();
        browseDialog.setLocationRelativeTo(this);

        browseDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                sftpClient.close();
            }
        });

        browseDialog.setVisible(true);
    }

    private static String getNodePath(DefaultMutableTreeNode node) {
        StringBuilder sb = new StringBuilder();
        Object[] path = node.getUserObjectPath();
        for (int i = 0; i < path.length; i++) {
            String part = path[i].toString();
            if (i == 0) {
                sb.append("/");
            } else {
                if (sb.length() > 1) sb.append("/");
                sb.append(part);
            }
        }
        return sb.toString();
    }

    private static void expandToPath(JTree tree, DefaultTreeModel model, DefaultMutableTreeNode root,
                                      String remotePath, SftpClient client) {
        String[] parts = remotePath.split("/");
        DefaultMutableTreeNode current = root;
        for (String part : parts) {
            if (part.isEmpty()) continue;
            // Load children if needed
            if (current.getChildCount() == 1 && current.getFirstChild().toString().equals("Loading...")) {
                current.removeAllChildren();
                String parentPath = getNodePath(current);
                try {
                    List<RemoteFileInfo> entries = client.listDir(parentPath);
                    for (RemoteFileInfo entry : entries) {
                        if (entry.isDirectory()) {
                            DefaultMutableTreeNode child = new DefaultMutableTreeNode(entry.getName());
                            child.add(new DefaultMutableTreeNode("Loading..."));
                            current.add(child);
                        }
                    }
                } catch (IOException ex) {
                    return;
                }
                model.reload(current);
            }
            // Find matching child
            DefaultMutableTreeNode match = null;
            for (int i = 0; i < current.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) current.getChildAt(i);
                if (child.getUserObject().toString().equals(part)) {
                    match = child;
                    break;
                }
            }
            if (match == null) return;
            current = match;
        }
        TreePath treePath = new TreePath(current.getPath());
        tree.expandPath(treePath.getParentPath());
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
    }

    /** Detect SSH keys in standard locations + PuTTY registry. PuTTY keys listed first. */
    private static List<File> detectSshKeys() {
        List<File> puttyKeys = new java.util.ArrayList<>();
        List<File> sshKeys = new java.util.ArrayList<>();
        String home = System.getProperty("user.home");
        if (home != null) {
            File sshDir = new File(home, ".ssh");
            if (sshDir.isDirectory()) {
                // Standard OpenSSH keys
                for (String name : new String[]{"id_ed25519", "id_rsa", "id_ecdsa"}) {
                    File keyFile = new File(sshDir, name);
                    if (keyFile.isFile()) sshKeys.add(keyFile);
                }
                // PuTTY .ppk files in .ssh
                File[] ppkFiles = sshDir.listFiles((d, n) -> n.endsWith(".ppk"));
                if (ppkFiles != null) {
                    for (File f : ppkFiles) puttyKeys.add(f);
                }
            }
        }
        // Try PuTTY registry for private key paths (Windows only)
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{
                    "reg", "query", "HKCU\\Software\\SimonTatham\\PuTTY\\Sessions", "/s", "/v", "PublicKeyFile"});
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("PublicKeyFile") && line.contains("REG_SZ")) {
                        String path = line.substring(line.indexOf("REG_SZ") + 6).trim();
                        if (!path.isEmpty()) {
                            File keyFile = new File(path);
                            if (keyFile.isFile() && !puttyKeys.contains(keyFile)) puttyKeys.add(keyFile);
                        }
                    }
                }
                p.waitFor();
            } catch (Exception e) {
                // Registry not available or no PuTTY sessions — ignore
            }
        }
        // PuTTY keys first, then standard SSH keys
        List<File> result = new java.util.ArrayList<>(puttyKeys);
        result.addAll(sshKeys);
        return result;
    }
}
