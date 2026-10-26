package com.timestored.qstudio;

import java.awt.*;
import javax.swing.*;

import com.timestored.sync.DirectorySyncer;
import com.timestored.sync.SyncResult.MirrorResult;

/**
 * Modal progress dialog shown during remote folder sync.
 * Call {@link #run()} to start the mirror and block until complete or cancelled.
 */
class SyncProgressDialog {

    interface SyncCallback {
        void onSuccess(MirrorResult result);
        void onCancelled();
        void onError(Exception ex);
    }

    private final Window owner;
    private final DirectorySyncer syncer;
    private final int maxFiles;
    private final String host;
    private final SyncCallback callback;

    SyncProgressDialog(Window owner, DirectorySyncer syncer, String host, int maxFiles, SyncCallback callback) {
        this.owner = owner;
        this.syncer = syncer;
        this.host = host;
        this.maxFiles = maxFiles;
        this.callback = callback;
    }

    void run() {
        JLabel statusLabel = new JLabel("Connecting to " + host + "...");
        JLabel fileLabel = new JLabel(" ");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        JButton cancelButton = new JButton("Cancel");

        JLabel footerLabel = new JLabel("Files under 10MB synced. Limit: " + maxFiles + " files. Change in Settings > Preferences.");
        footerLabel.setForeground(Color.GRAY);

        Box content = Box.createVerticalBox();
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(progressBar);
        content.add(Box.createVerticalStrut(8));
        content.add(fileLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(cancelButton);
        content.add(Box.createVerticalStrut(8));
        content.add(footerLabel);

        JDialog dialog = new JDialog(owner, "Syncing Remote Folder", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(content);
        dialog.setSize(675, 200);
        dialog.setLocationRelativeTo(owner);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingWorker<MirrorResult, String> worker = new SwingWorker<MirrorResult, String>() {
            @Override protected MirrorResult doInBackground() throws Exception {
                return syncer.mirrorRemoteToLocal(new DirectorySyncer.MirrorProgressListener() {
                    @Override public void onFileDownloaded(String remotePath, int downloadedSoFar, int totalToDownload) {
                        String name = remotePath.substring(remotePath.lastIndexOf('/') + 1);
                        publish(downloadedSoFar + " / " + totalToDownload + " files \u2014 " + name);
                    }
                    @Override public void onDirectoryScanned(String dirPath) {
                        publish("Scanning " + dirPath);
                    }
                });
            }

            @Override protected void process(java.util.List<String> chunks) {
                String latest = chunks.get(chunks.size() - 1);
                if (latest.startsWith("Scanning ")) {
                    statusLabel.setText(latest);
                } else {
                    fileLabel.setText(latest);
                }
            }

            @Override protected void done() {
                dialog.dispose();
                if (isCancelled()) {
                    callback.onCancelled();
                } else {
                    try {
                        callback.onSuccess(get());
                    } catch (Exception ex) {
                        callback.onError(ex);
                    }
                }
            }
        };

        cancelButton.addActionListener(e -> {
            worker.cancel(true);
            dialog.dispose();
        });

        worker.execute();
        dialog.setVisible(true);
    }
}
