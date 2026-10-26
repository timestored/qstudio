package com.timestored.qstudio;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.timestored.docs.Document;
import com.timestored.docs.OpenDocumentsModel;
import com.timestored.sync.DirectorySyncer;
import com.timestored.sync.SyncResult.UploadResult;

class RemoteSyncManager implements Closeable {

    private static final Logger LOG = Logger.getLogger(RemoteSyncManager.class.getName());

    private final DirectorySyncer syncer;
    private final OpenDocumentsModel openDocsModel;
    private final ConsolePanel consolePanel;
    private final com.timestored.swingxx.QueryStatusBar statusBar;
    private final String displayHost;
    private final com.timestored.docs.DocumentActions documentActions;
    private final OpenDocumentsModel.Adapter listener;
    private final Timer staleCheckTimer;

    RemoteSyncManager(DirectorySyncer syncer, OpenDocumentsModel openDocsModel,
                      ConsolePanel consolePanel, com.timestored.swingxx.QueryStatusBar statusBar,
                      String user, String host, int port, String remotePath,
                      com.timestored.docs.DocumentActions documentActions) {
        this.syncer = syncer;
        this.openDocsModel = openDocsModel;
        this.consolePanel = consolePanel;
        this.statusBar = statusBar;
        this.documentActions = documentActions;
        this.displayHost = user + "@" + host;
        statusBar.setRemoteStatus("Remote: " + host);
        statusBar.setRemoteStatusTooltip(user + "@" + host + ":" + port + "  " + remotePath);

        this.staleCheckTimer = new Timer(2000, e -> checkRemoteForCurrentDoc());
        this.staleCheckTimer.setRepeats(false);

        this.listener = new OpenDocumentsModel.Adapter() {
            @Override public void docSaved() {
                handleDocSaved();
            }
            @Override public void docSelected(Document document) {
                staleCheckTimer.restart();
            }
        };
        openDocsModel.addListener(listener);
    }

    private void handleDocSaved() {
        Document doc = openDocsModel.getSelectedDocument();
        if (doc == null || doc.getFilePath() == null) return;
        File file = new File(doc.getFilePath());
        if (!syncer.isRemoteFile(file)) return;

        statusBar.setRemoteStatus("Remote: uploading...");
        BackgroundExecutor.EXECUTOR.execute(() -> {
            try {
                if (syncer.getSyncTime(syncer.toRemotePath(file)) < 0) {
                    // New file — no conflict possible
                    syncer.uploadNewFile(file);
                    log("[Remote] Uploaded new file: " + doc.getTitle());
                } else {
                    UploadResult result = syncer.upload(file);
                    if (result == UploadResult.SUCCESS) {
                        log("[Remote] Uploaded: " + doc.getTitle());
                    } else {
                        statusBar.setRemoteStatus("Remote: conflict");
                        SwingUtilities.invokeLater(() -> showConflictDialog(file, doc.getTitle()));
                        return;
                    }
                }
                statusBar.setRemoteStatus("Remote: " + displayHost);
            } catch (IOException e) {
                log("[Remote] Upload failed: " + doc.getTitle() + " — " + e.getMessage());
                statusBar.setRemoteStatusError("Remote: upload failed");
                LOG.log(Level.WARNING, "Upload failed", e);
            }
        });
    }

    void resync(java.awt.Window owner) {
        statusBar.setRemoteStatus("Remote: syncing...");
        BackgroundExecutor.EXECUTOR.execute(() -> {
            try {
                // Re-mirror
                com.timestored.sync.SyncResult.MirrorResult mirrorResult = syncer.mirrorRemoteToLocal();
                log("[Remote] Re-sync: " + mirrorResult.getFilesDownloaded() + " files downloaded");

                if (syncer.wasLimitExceeded()) {
                    String msg = syncer.getLimitExceededMessage();
                    log("[Remote] WARNING: " + msg);
                    javax.swing.SwingUtilities.invokeLater(() ->
                        javax.swing.JOptionPane.showMessageDialog(owner,
                            "Sync limit reached: " + msg + "\n\nSome files may not have been synced.\n"
                            + "You can increase limits in Settings > Preferences > Remote Sync.",
                            "Sync Limit Reached", javax.swing.JOptionPane.WARNING_MESSAGE));
                }

                // Clean up orphaned files (remote-deleted)
                java.util.List<File> orphans = syncer.findOrphanedFiles(mirrorResult.getRemotePathsSeen());
                if (!orphans.isEmpty()) {
                    log("[Remote] Found " + orphans.size() + " locally cached file(s) removed from remote");
                    for (File orphan : orphans) {
                        final File of = orphan;
                        boolean isOpen = openDocsModel.getDocuments().stream()
                                .anyMatch(d -> d.getFilePath() != null && d.getFilePath().equals(of.getAbsolutePath()));

                        if (isOpen) {
                            SwingUtilities.invokeAndWait(() -> {
                                for (com.timestored.docs.Document d : openDocsModel.getDocuments()) {
                                    if (d.getFilePath() != null && d.getFilePath().equals(of.getAbsolutePath())) {
                                        documentActions.closeWithConfirmation(d);
                                        break;
                                    }
                                }
                            });
                            boolean stillOpen = openDocsModel.getDocuments().stream()
                                    .anyMatch(d -> d.getFilePath() != null && d.getFilePath().equals(of.getAbsolutePath()));
                            if (stillOpen) {
                                log("[Remote] Skipped orphan (user kept): " + orphan.getName());
                                continue;
                            }
                        }

                        syncer.deleteOrphanedFile(orphan);
                        log("[Remote] Removed local cache: " + orphan.getName());
                    }
                    syncer.cleanEmptyDirectories();
                }

                // Upload untracked local files
                java.util.List<File> untracked = syncer.findUntrackedFiles();
                for (File f : untracked) {
                    syncer.uploadNewFile(f);
                    log("[Remote] Uploaded new: " + f.getName());
                }
                if (!untracked.isEmpty()) {
                    log("[Remote] Uploaded " + untracked.size() + " new file(s)");
                }

                // Check for conflicts
                java.util.List<com.timestored.sync.SyncResult.FileStatus> conflicts = syncer.checkForConflicts();
                for (com.timestored.sync.SyncResult.FileStatus fs : conflicts) {
                    try {
                        if (fs.getState() == com.timestored.sync.SyncResult.FileState.LOCAL_NEWER) {
                            syncer.forceUpload(fs.getLocalFile());
                            log("[Remote] Uploaded: " + fs.getLocalFile().getName());
                        } else if (fs.getState() == com.timestored.sync.SyncResult.FileState.REMOTE_NEWER) {
                            syncer.download(fs.getRemotePath());
                            final File lf = fs.getLocalFile();
                            SwingUtilities.invokeLater(() -> reloadDocument(lf));
                            log("[Remote] Downloaded: " + fs.getLocalFile().getName());
                        } else if (fs.getState() == com.timestored.sync.SyncResult.FileState.BOTH_CHANGED) {
                            final File lf = fs.getLocalFile();
                            final String title = lf.getName();
                            SwingUtilities.invokeAndWait(() -> showConflictDialog(lf, title));
                        }
                    } catch (Exception e) {
                        log("[Remote] Resolve failed for " + fs.getLocalFile().getName() + ": " + e.getMessage());
                    }
                }

                // Show skipped files summary
                java.util.List<com.timestored.sync.SyncResult.SkippedFile> skippedFiles = mirrorResult.getSkippedFiles();
                if (skippedFiles != null && !skippedFiles.isEmpty()) {
                    showSkippedFilesDialog(owner, skippedFiles);
                }

                statusBar.setRemoteStatus("Remote: " + displayHost);
            } catch (Exception e) {
                log("[Remote] Re-sync failed: " + e.getMessage());
                statusBar.setRemoteStatusError("Remote: error");
                LOG.log(Level.WARNING, "Re-sync failed", e);
            }
        });
    }

    void showSkippedFilesDialog(java.awt.Window owner, java.util.List<com.timestored.sync.SyncResult.SkippedFile> skippedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append(skippedFiles.size()).append(" file(s) were not synced:\n\n");

        int shown = 0;
        for (com.timestored.sync.SyncResult.SkippedFile sf : skippedFiles) {
            if (shown++ >= 20) {
                sb.append("... and ").append(skippedFiles.size() - 20).append(" more\n");
                break;
            }
            String name = sf.getRemotePath().substring(sf.getRemotePath().lastIndexOf('/') + 1);
            switch (sf.getReason()) {
                case SIZE_LIMIT:
                    sb.append("  ").append(name).append(" — over 10MB (").append(sf.getSize() / (1024*1024)).append("MB)\n");
                    break;
                case GITIGNORED:
                    sb.append("  ").append(name).append(" — .gitignore\n");
                    break;
                case FILE_COUNT_LIMIT:
                    sb.append("  ").append(name).append(" — file count limit reached\n");
                    break;
            }
        }
        sb.append("\nAdjust limits in Settings > Preferences > Remote Sync.");

        final String message = sb.toString();
        SwingUtilities.invokeLater(() ->
            javax.swing.JOptionPane.showMessageDialog(owner, message,
                "Skipped Files", javax.swing.JOptionPane.INFORMATION_MESSAGE));
    }

    private void checkRemoteForCurrentDoc() {
        Document doc = openDocsModel.getSelectedDocument();
        if (doc == null || doc.getFilePath() == null) return;
        File file = new File(doc.getFilePath());
        if (!syncer.isRemoteFile(file)) return;

        BackgroundExecutor.EXECUTOR.execute(() -> {
            try {
                long syncTime = syncer.getSyncTime(syncer.toRemotePath(file));
                if (syncTime < 0) return;
                long remoteMtime = syncer.getRemoteMtime(file);
                if (remoteMtime <= syncTime) return;

                if (!doc.hasUnsavedChanges()) {
                    String remotePath = syncer.toRemotePath(file);
                    syncer.download(remotePath);
                    SwingUtilities.invokeLater(() -> reloadDocument(file));
                    log("[Remote] Downloaded: " + doc.getTitle() + " (remote was newer)");
                } else {
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showOptionDialog(null,
                                doc.getTitle() + " has been modified on the remote server.\n"
                                        + "You also have unsaved local changes.",
                                "Remote File Changed",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                new String[]{"Download Latest", "Keep Mine", "Ignore"},
                                "Ignore");
                        if (choice == 0) {
                            BackgroundExecutor.EXECUTOR.execute(() -> {
                                try {
                                    syncer.download(syncer.toRemotePath(file));
                                    SwingUtilities.invokeLater(() -> reloadDocument(file));
                                    log("[Remote] Downloaded: " + doc.getTitle());
                                } catch (IOException e) {
                                    log("[Remote] Download failed: " + doc.getTitle() + " — " + e.getMessage());
                                }
                            });
                        }
                    });
                }
            } catch (IOException e) {
                LOG.log(Level.FINE, "Stale check failed for " + doc.getTitle(), e);
                statusBar.setRemoteStatusError("Remote: error");
            }
        });
    }

    private void showConflictDialog(File file, String title) {
        int choice = JOptionPane.showOptionDialog(null,
                title + " has been modified both locally and on the remote server.",
                "Sync Conflict",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"Upload Mine", "Download Remote", "Ignore"},
                "Ignore");
        if (choice == 0) {
            BackgroundExecutor.EXECUTOR.execute(() -> {
                try {
                    syncer.forceUpload(file);
                    log("[Remote] Force uploaded: " + title);
                } catch (IOException e) {
                    log("[Remote] Force upload failed: " + title + " — " + e.getMessage());
                }
            });
        } else if (choice == 1) {
            BackgroundExecutor.EXECUTOR.execute(() -> {
                try {
                    syncer.download(syncer.toRemotePath(file));
                    SwingUtilities.invokeLater(() -> reloadDocument(file));
                    log("[Remote] Downloaded: " + title);
                } catch (IOException e) {
                    log("[Remote] Download failed: " + title + " — " + e.getMessage());
                }
            });
        }
    }

    /** Reloads a file in the editor by closing and reopening it. Must be called on EDT. */
    private void reloadDocument(File file) {
        for (com.timestored.docs.Document d : openDocsModel.getDocuments()) {
            if (d.getFilePath() != null && d.getFilePath().equals(file.getAbsolutePath())) {
                openDocsModel.closeDocument(d);
                break;
            }
        }
        try {
            openDocsModel.openDocument(file);
        } catch (IOException e) {
            LOG.warning("Could not reopen file: " + e.getMessage());
        }
    }

    /**
     * Close with full cleanup: prompt for unsaved docs, delete cache, disconnect.
     * Must be called on EDT.
     * @return true if close completed, false if user cancelled
     */
    boolean closeWithCleanup() {
        java.util.List<com.timestored.docs.Document> remoteDocs = new java.util.ArrayList<>();
        for (com.timestored.docs.Document doc : openDocsModel.getDocuments()) {
            if (doc.getFilePath() != null && syncer.isRemoteFile(new File(doc.getFilePath()))) {
                remoteDocs.add(doc);
            }
        }

        for (com.timestored.docs.Document doc : remoteDocs) {
            documentActions.closeWithConfirmation(doc);
            if (openDocsModel.getDocuments().contains(doc)) {
                log("[Remote] Close cancelled by user");
                return false;
            }
        }

        close(); // Disconnect first so no file handles are held
        syncer.deleteLocalCache();
        log("[Remote] Disconnected and cache cleared");
        return true;
    }

    boolean isRemoteFolder(File folder) {
        return folder != null && syncer.isRemoteFile(folder);
    }

    private void log(String msg) {
        // Sanitize non-ASCII characters that can appear garbled in the console
        consolePanel.appendMessage(msg.replaceAll("[^\\x20-\\x7E\\n\\r\\t]", "?"));
    }

    @Override
    public void close() {
        staleCheckTimer.stop();
        openDocsModel.removeListener(listener);
        syncer.close();
        statusBar.clearRemoteStatus();
    }
}
