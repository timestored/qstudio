package com.timestored.sync;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 * SSHJ-based SFTP client. Owns one SSH connection.
 * Connection is lazy (created on first operation).
 * Auto-reconnects once on failure.
 */
public class SftpClient implements Closeable {

    private static final Logger LOG = Logger.getLogger(SftpClient.class.getName());

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final File privateKey;
    private final String passphrase;

    private SSHClient ssh;
    private SFTPClient sftp;

    public SftpClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.privateKey = null;
        this.passphrase = null;
    }

    public SftpClient(String host, int port, String username, File privateKey, String passphrase) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = null;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    public void connect() throws IOException {
        close();
        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(15000);
        ssh.setTimeout(30000);
        ssh.connect(host, port);
        if (privateKey != null) {
            KeyProvider kp = passphrase != null && !passphrase.isEmpty()
                    ? ssh.loadKeys(privateKey.getAbsolutePath(), passphrase)
                    : ssh.loadKeys(privateKey.getAbsolutePath());
            ssh.authPublickey(username, kp);
        } else {
            ssh.authPassword(username, password);
        }
        sftp = ssh.newSFTPClient();
    }

    public boolean isConnected() {
        return ssh != null && ssh.isConnected() && sftp != null;
    }

    /** Get the remote user's home directory by resolving "." */
    public String getHomeDirectory() throws IOException {
        return withReconnect(() -> sftp.canonicalize("."));
    }

    /** Create an additional SFTP channel on the same SSH connection for parallel operations */
    public SFTPClient newChannel() throws IOException {
        if (ssh == null || !ssh.isConnected()) throw new IOException("SSH not connected");
        return ssh.newSFTPClient();
    }

    public List<RemoteFileInfo> listDir(String remotePath) throws IOException {
        return withReconnect(() -> {
            List<RemoteFileInfo> result = new ArrayList<>();
            for (RemoteResourceInfo rri : sftp.ls(remotePath)) {
                FileAttributes attrs = rri.getAttributes();
                result.add(new RemoteFileInfo(
                        rri.getName(),
                        remotePath + "/" + rri.getName(),
                        attrs.getSize(),
                        attrs.getMtime(),
                        rri.isDirectory()));
            }
            return result;
        });
    }

    public void download(String remotePath, File localFile) throws IOException {
        withReconnect(() -> {
            localFile.getParentFile().mkdirs();
            sftp.get(remotePath, localFile.getAbsolutePath());
            return null;
        });
    }

    public void upload(File localFile, String remotePath) throws IOException {
        withReconnect(() -> {
            sftp.put(localFile.getAbsolutePath(), remotePath);
            return null;
        });
    }

    public void mkdirs(String remotePath) throws IOException {
        withReconnect(() -> {
            sftp.mkdirs(remotePath);
            return null;
        });
    }

    /** Returns null if file does not exist. */
    public RemoteFileInfo stat(String remotePath) throws IOException {
        return withReconnect(() -> {
            try {
                FileAttributes attrs = sftp.statExistence(remotePath);
                if (attrs == null) return null;
                String name = remotePath.contains("/")
                        ? remotePath.substring(remotePath.lastIndexOf('/') + 1)
                        : remotePath;
                return new RemoteFileInfo(name, remotePath, attrs.getSize(), attrs.getMtime(),
                        attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY);
            } catch (IOException e) {
                return null;
            }
        });
    }

    @Override
    public void close() {
        if (sftp != null) {
            try { sftp.close(); } catch (Exception e) { LOG.log(Level.FINE, "Error closing SFTP", e); }
            sftp = null;
        }
        if (ssh != null) {
            try { ssh.disconnect(); } catch (Exception e) { LOG.log(Level.FINE, "Error closing SSH", e); }
            ssh = null;
        }
    }

    String getHost() { return host; }
    int getPort() { return port; }

    private <T> T withReconnect(IOSupplier<T> operation) throws IOException {
        ensureConnected();
        try {
            return operation.get();
        } catch (IOException e) {
            LOG.log(Level.INFO, "SFTP operation failed, reconnecting: " + e.getMessage());
            connect();
            return operation.get();
        }
    }

    private void ensureConnected() throws IOException {
        if (!isConnected()) {
            connect();
        }
    }

    @FunctionalInterface
    private interface IOSupplier<T> {
        T get() throws IOException;
    }
}
