package com.timestored.sync;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.timestored.misc.IOUtils;
import com.timestored.sync.SyncResult.*;

/**
 * Keeps a local cache directory in sync with a remote SFTP directory.
 * All operations are blocking. No Swing, no threads.
 */
public class DirectorySyncer implements Closeable {

    private static final Logger LOG = Logger.getLogger(DirectorySyncer.class.getName());

    private static final Pattern EXCLUDED_FOLDER_PATTERN = Pattern.compile(
        "^\\..*|^target$|^node_modules$|^__pycache__$|^site-packages$|^venv$|^build$|^dist$|^__MACOSX$|.*-ve$");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final int DEFAULT_MAX_FILES = 2000;
    private static final int DEFAULT_MAX_DEPTH = 20;
    private static final int PARALLEL_CHANNELS = 8;
    private int maxFiles = DEFAULT_MAX_FILES;
    private int maxDepth = DEFAULT_MAX_DEPTH;
    private volatile boolean limitExceeded = false;
    private volatile String limitExceededMessage = null;

    private final SftpClient client;
    private final String remoteDir;
    private final File localRoot;
    private final File localCacheRoot;
    private final String hostPrefix;
    private final Properties syncTimes;
    private final File syncTimesFile;
    private List<String> gitignorePatterns = new ArrayList<>();

    public DirectorySyncer(SftpClient client, String host, int port, String remoteDir, File localCacheRoot) {
        this.client = client;
        this.remoteDir = remoteDir;
        this.localCacheRoot = localCacheRoot;
        this.hostPrefix = sanitizeHost(host, port);
        String remoteSuffix = remoteDir.startsWith("/") ? remoteDir.substring(1) : remoteDir;
        this.localRoot = new File(localCacheRoot, hostPrefix + "/" + remoteSuffix);
        this.localRoot.mkdirs();
        this.syncTimesFile = new File(localRoot, ".syncTimes.properties");
        this.syncTimes = loadSyncTimes();
    }

    public File getLocalRoot() { return localRoot; }
    public void setMaxFiles(int max) { this.maxFiles = max; }
    public void setMaxDepth(int max) { this.maxDepth = max; }
    public boolean wasLimitExceeded() { return limitExceeded; }
    public String getLimitExceededMessage() { return limitExceededMessage; }

    public File toLocalFile(String remotePath) {
        String relative = remotePath.startsWith("/") ? remotePath.substring(1) : remotePath;
        return new File(localCacheRoot, hostPrefix + "/" + relative);
    }

    public String toRemotePath(File localFile) {
        String localAbs = localFile.getAbsolutePath().replace('\\', '/');
        String cacheAbs = localCacheRoot.getAbsolutePath().replace('\\', '/');
        String relative = localAbs.substring(cacheAbs.length());
        String withoutHost = relative.substring(hostPrefix.length() + 1);
        return withoutHost;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public boolean isRemoteFile(File localFile) {
        String localAbs = localFile.getAbsolutePath().replace('\\', '/');
        String rootAbs = localRoot.getAbsolutePath().replace('\\', '/');
        return localAbs.startsWith(rootAbs);
    }

    public List<File> findUntrackedFiles() {
        List<File> untracked = new java.util.ArrayList<>();
        findUntrackedRecursive(localRoot, untracked);
        return untracked;
    }

    private void findUntrackedRecursive(File dir, List<File> untracked) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                findUntrackedRecursive(f, untracked);
            } else if (!f.getName().equals(".syncTimes.properties")) {
                String remotePath = toRemotePath(f);
                if (getSyncTime(remotePath) < 0) {
                    untracked.add(f);
                }
            }
        }
    }

    /**
     * Find local cache files that no longer exist on the remote.
     * @param remotePathsSeen set of all remote file paths from the latest mirror scan
     * @return local files whose remote counterparts are missing
     */
    public List<File> findOrphanedFiles(java.util.Set<String> remotePathsSeen) {
        List<File> orphans = new ArrayList<>();
        findOrphanedRecursive(localRoot, remotePathsSeen, orphans);
        return orphans;
    }

    private void findOrphanedRecursive(File dir, java.util.Set<String> remotePathsSeen, List<File> orphans) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                findOrphanedRecursive(f, remotePathsSeen, orphans);
            } else if (!f.getName().equals(".syncTimes.properties")) {
                String remotePath = toRemotePath(f);
                if (!remotePathsSeen.contains(remotePath)) {
                    orphans.add(f);
                }
            }
        }
    }

    /** Delete a local orphaned file and remove its syncTime entry. */
    public void deleteOrphanedFile(File localFile) {
        String remotePath = toRemotePath(localFile);
        syncTimes.remove(remotePath);
        saveSyncTimes();
        localFile.delete();
    }

    /** Delete empty directories under localRoot, bottom-up. */
    public void cleanEmptyDirectories() {
        cleanEmptyDirsRecursive(localRoot);
    }

    private void cleanEmptyDirsRecursive(File dir) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                cleanEmptyDirsRecursive(f);
                File[] remaining = f.listFiles();
                if (remaining != null && remaining.length == 0) {
                    f.delete();
                }
            }
        }
    }

    /** Recursively delete the local cache directory. Returns true if fully deleted. */
    public boolean deleteLocalCache() {
        boolean ok = deleteRecursive(localRoot);
        if (!ok) {
            LOG.warning("Could not fully delete cache at: " + localRoot.getAbsolutePath());
        }
        return ok;
    }

    private static boolean deleteRecursive(File f) {
        boolean allOk = true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    if (!deleteRecursive(c)) allOk = false;
                }
            }
        }
        if (!f.delete() && f.exists()) {
            LOG.warning("Failed to delete: " + f.getAbsolutePath());
            allOk = false;
        }
        return allOk;
    }

    public long getSyncTime(String remotePath) {
        String val = syncTimes.getProperty(remotePath);
        return val != null ? Long.parseLong(val) : -1;
    }

    void setSyncTime(String remotePath, long epochSeconds) {
        syncTimes.setProperty(remotePath, String.valueOf(epochSeconds));
        saveSyncTimes();
    }

    static boolean isExcludedFolder(String folderName) {
        return EXCLUDED_FOLDER_PATTERN.matcher(folderName).matches();
    }

    /** Check if a path is excluded by .gitignore patterns */
    boolean isGitignored(String name) {
        for (String pattern : gitignorePatterns) {
            if (matchGitignorePattern(pattern, name)) return true;
        }
        return false;
    }

    /** Check if a folder or file should be excluded (default patterns + .gitignore) */
    boolean isExcluded(String name, boolean isDirectory) {
        if (isDirectory && isExcludedFolder(name)) return true;
        return isGitignored(name);
    }

    private void loadGitignore() {
        gitignorePatterns.clear();
        try {
            String gitignorePath = remoteDir + "/.gitignore";
            File tempFile = File.createTempFile("gitignore", ".tmp");
            tempFile.deleteOnExit();
            client.download(gitignorePath, tempFile);
            String content = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            gitignorePatterns = parseGitignore(content);
            tempFile.delete();
            LOG.info("Loaded .gitignore with " + gitignorePatterns.size() + " patterns");
        } catch (IOException e) {
            // No .gitignore or not readable — use defaults only
            LOG.fine("No .gitignore found at remote root: " + e.getMessage());
        }
    }

    /**
     * Parse .gitignore content into a list of patterns.
     * Supports: folder names, *.ext globs, directory/ suffix, # comments.
     * Does NOT support: negation (!), complex globs (**), anchored paths.
     */
    static List<String> parseGitignore(String content) {
        List<String> patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("!")) continue; // negation not supported
                // Remove trailing / (directory marker) — we match by name
                if (line.endsWith("/")) line = line.substring(0, line.length() - 1);
                patterns.add(line);
            }
        } catch (IOException e) {
            // shouldn't happen with StringReader
        }
        return patterns;
    }

    /** Match a single .gitignore pattern against a file/folder name */
    static boolean matchGitignorePattern(String pattern, String name) {
        // Simple glob matching: * matches any chars, ? matches one char
        if (pattern.contains("*") || pattern.contains("?")) {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                return matcher.matches(java.nio.file.Path.of(name));
            } catch (Exception e) {
                return false;
            }
        }
        // Exact name match
        return pattern.equals(name);
    }

    private Properties loadSyncTimes() {
        Properties p = new Properties();
        if (syncTimesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(syncTimesFile)) {
                p.load(fis);
            } catch (IOException e) {
                LOG.warning("Could not load syncTimes: " + e.getMessage());
            }
        }
        return p;
    }

    private void saveSyncTimes() {
        syncTimesFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(syncTimesFile)) {
            syncTimes.store(fos, null);
        } catch (IOException e) {
            LOG.warning("Could not save syncTimes: " + e.getMessage());
        }
    }

    private static String sanitizeHost(String host, int port) {
        return host.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + port;
    }

    /** Callback for mirror progress. */
    public interface MirrorProgressListener {
        void onFileDownloaded(String remotePath, int downloadedSoFar, int totalToDownload);
        void onDirectoryScanned(String remotePath);
    }

    public MirrorResult mirrorRemoteToLocal() throws IOException {
        return mirrorRemoteToLocal(null);
    }

    public MirrorResult mirrorRemoteToLocal(MirrorProgressListener listener) throws IOException {
        loadGitignore();
        limitExceeded = false;
        limitExceededMessage = null;
        AtomicInteger downloaded = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger totalToDownload = new AtomicInteger(0);
        List<SyncResult.SkippedFile> skippedFilesList = new CopyOnWriteArrayList<>();
        java.util.Set<String> remotePathsSeen = ConcurrentHashMap.newKeySet();

        // Per-thread SFTP channels for true parallel I/O
        ThreadLocal<net.schmizz.sshj.sftp.SFTPClient> channelLocal = ThreadLocal.withInitial(() -> {
            try {
                return client.newChannel();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to open SFTP channel, falling back to shared client", e);
                return null;
            }
        });

        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_CHANNELS);
        List<net.schmizz.sshj.sftp.SFTPClient> openedChannels = new CopyOnWriteArrayList<>();

        try {
            CountDownLatch done = new CountDownLatch(1);
            AtomicInteger activeTasks = new AtomicInteger(1);

            submitMirrorTask(pool, channelLocal, openedChannels, remoteDir, 0,
                    downloaded, skipped, totalToDownload, skippedFilesList, remotePathsSeen,
                    activeTasks, done, listener);

            done.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
            // Close extra SFTP channels
            for (net.schmizz.sshj.sftp.SFTPClient ch : openedChannels) {
                try { ch.close(); } catch (Exception e) {}
            }
        }

        return new MirrorResult(localRoot, downloaded.get(), skipped.get(), skippedFilesList, remotePathsSeen);
    }

    private void submitMirrorTask(ExecutorService pool,
            ThreadLocal<net.schmizz.sshj.sftp.SFTPClient> channelLocal,
            List<net.schmizz.sshj.sftp.SFTPClient> openedChannels,
            String remotePath, int depth,
            AtomicInteger downloaded, AtomicInteger skipped, AtomicInteger totalToDownload,
            List<SyncResult.SkippedFile> skippedFilesList, java.util.Set<String> remotePathsSeen,
            AtomicInteger activeTasks, CountDownLatch done, MirrorProgressListener listener) {

        pool.submit(() -> {
            try {
                if (depth > maxDepth) {
                    limitExceeded = true;
                    limitExceededMessage = "Max depth (" + maxDepth + ") exceeded at: " + remotePath;
                    return;
                }
                if (downloaded.get() >= maxFiles) {
                    limitExceeded = true;
                    limitExceededMessage = "Max files (" + maxFiles + ") exceeded";
                    return;
                }

                if (listener != null) listener.onDirectoryScanned(remotePath);

                // Get thread-local SFTP channel
                net.schmizz.sshj.sftp.SFTPClient channel = channelLocal.get();
                if (channel != null && !openedChannels.contains(channel)) {
                    openedChannels.add(channel);
                }

                // List directory using thread's own channel
                List<RemoteFileInfo> entries;
                try {
                    if (channel != null) {
                        entries = listDirWithChannel(channel, remotePath);
                    } else {
                        synchronized (client) { entries = client.listDir(remotePath); }
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to list " + remotePath, e);
                    return;
                }

                // Phase 1: scan entries — submit subdirs, count files to download
                List<RemoteFileInfo> toDownload = new ArrayList<>();
                for (RemoteFileInfo entry : entries) {
                    if (downloaded.get() + totalToDownload.get() >= maxFiles) {
                        limitExceeded = true;
                        limitExceededMessage = "Max files (" + maxFiles + ") exceeded";
                        if (!entry.isDirectory()) {
                            skippedFilesList.add(new SyncResult.SkippedFile(entry.getPath(), SyncResult.SkipReason.FILE_COUNT_LIMIT, entry.getSize()));
                        }
                        break;
                    }
                    if (entry.isDirectory()) {
                        if (!isExcluded(entry.getName(), true)) {
                            activeTasks.incrementAndGet();
                            submitMirrorTask(pool, channelLocal, openedChannels, entry.getPath(), depth + 1,
                                    downloaded, skipped, totalToDownload, skippedFilesList, remotePathsSeen,
                                    activeTasks, done, listener);
                        }
                    } else {
                        remotePathsSeen.add(entry.getPath());
                        if (isGitignored(entry.getName())) {
                            skippedFilesList.add(new SyncResult.SkippedFile(entry.getPath(), SyncResult.SkipReason.GITIGNORED, entry.getSize()));
                            skipped.incrementAndGet();
                            continue;
                        }
                        if (entry.getSize() > MAX_FILE_SIZE) {
                            skippedFilesList.add(new SyncResult.SkippedFile(entry.getPath(), SyncResult.SkipReason.SIZE_LIMIT, entry.getSize()));
                            skipped.incrementAndGet();
                            continue;
                        }
                        long existingSyncTime = getSyncTime(entry.getPath());
                        if (existingSyncTime == entry.getMtime()) continue;
                        toDownload.add(entry);
                        totalToDownload.incrementAndGet();
                    }
                }

                // Phase 2: submit individual file downloads to pool
                for (RemoteFileInfo entry : toDownload) {
                    activeTasks.incrementAndGet();
                    submitDownloadTask(pool, channelLocal, openedChannels, entry,
                            downloaded, totalToDownload, activeTasks, done, listener);
                }
            } finally {
                if (activeTasks.decrementAndGet() == 0) {
                    done.countDown();
                }
            }
        });
    }

    private void submitDownloadTask(ExecutorService pool,
            ThreadLocal<net.schmizz.sshj.sftp.SFTPClient> channelLocal,
            List<net.schmizz.sshj.sftp.SFTPClient> openedChannels,
            RemoteFileInfo entry,
            AtomicInteger downloaded, AtomicInteger totalToDownload,
            AtomicInteger activeTasks, CountDownLatch done, MirrorProgressListener listener) {

        pool.submit(() -> {
            try {
                net.schmizz.sshj.sftp.SFTPClient channel = channelLocal.get();
                if (channel != null && !openedChannels.contains(channel)) {
                    openedChannels.add(channel);
                }
                File localFile = toLocalFile(entry.getPath());
                if (channel != null) {
                    localFile.getParentFile().mkdirs();
                    channel.get(entry.getPath(), localFile.getAbsolutePath());
                } else {
                    synchronized (client) {
                        client.download(entry.getPath(), localFile);
                    }
                }
                setSyncTime(entry.getPath(), entry.getMtime());
                int count = downloaded.incrementAndGet();
                if (listener != null) listener.onFileDownloaded(entry.getPath(), count, totalToDownload.get());
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to download " + entry.getPath(), e);
            } finally {
                if (activeTasks.decrementAndGet() == 0) {
                    done.countDown();
                }
            }
        });
    }

    /** List directory using a specific SFTP channel (for parallel operations) */
    private static List<RemoteFileInfo> listDirWithChannel(net.schmizz.sshj.sftp.SFTPClient channel, String remotePath) throws IOException {
        List<RemoteFileInfo> result = new ArrayList<>();
        for (net.schmizz.sshj.sftp.RemoteResourceInfo rri : channel.ls(remotePath)) {
            net.schmizz.sshj.sftp.FileAttributes attrs = rri.getAttributes();
            result.add(new RemoteFileInfo(rri.getName(), remotePath + "/" + rri.getName(),
                    attrs.getSize(), attrs.getMtime(), rri.isDirectory()));
        }
        return result;
    }

    public void uploadNewFile(File localFile) throws IOException {
        String remotePath = toRemotePath(localFile);
        // Ensure remote parent directory exists
        String parentPath = remotePath.substring(0, remotePath.lastIndexOf('/'));
        if (!parentPath.isEmpty()) {
            client.mkdirs(parentPath);
        }
        client.upload(localFile, remotePath);
        RemoteFileInfo info = client.stat(remotePath);
        long mtime = info != null ? info.getMtime() : System.currentTimeMillis() / 1000;
        setSyncTime(remotePath, mtime);
    }

    public UploadResult upload(File localFile) throws IOException {
        String remotePath = toRemotePath(localFile);
        RemoteFileInfo info = client.stat(remotePath);
        long lastSync = getSyncTime(remotePath);
        if (info != null && lastSync >= 0 && info.getMtime() > lastSync) {
            return UploadResult.CONFLICT;
        }
        return forceUpload(localFile);
    }

    public UploadResult forceUpload(File localFile) throws IOException {
        String remotePath = toRemotePath(localFile);
        client.upload(localFile, remotePath);
        RemoteFileInfo info = client.stat(remotePath);
        long mtime = info != null ? info.getMtime() : System.currentTimeMillis() / 1000;
        setSyncTime(remotePath, mtime);
        return UploadResult.SUCCESS;
    }

    public void download(String remotePath) throws IOException {
        File localFile = toLocalFile(remotePath);
        client.download(remotePath, localFile);
        RemoteFileInfo info = client.stat(remotePath);
        long mtime = info != null ? info.getMtime() : System.currentTimeMillis() / 1000;
        setSyncTime(remotePath, mtime);
    }

    public long getRemoteMtime(File localFile) throws IOException {
        if (!isRemoteFile(localFile)) return -1;
        String remotePath = toRemotePath(localFile);
        RemoteFileInfo info = client.stat(remotePath);
        return info != null ? info.getMtime() : -1;
    }

    public List<FileStatus> checkForConflicts() throws IOException {
        List<FileStatus> conflicts = new java.util.ArrayList<>();
        for (String remotePath : syncTimes.stringPropertyNames()) {
            long syncTime = getSyncTime(remotePath);
            File localFile = toLocalFile(remotePath);
            if (!localFile.exists()) continue;

            long localMtime = localFile.lastModified() / 1000;
            RemoteFileInfo info = client.stat(remotePath);
            long remoteMtime = info != null ? info.getMtime() : -1;

            boolean localChanged = localMtime > syncTime;
            boolean remoteChanged = remoteMtime > syncTime;

            FileState state;
            if (localChanged && remoteChanged) {
                state = FileState.BOTH_CHANGED;
            } else if (localChanged) {
                state = FileState.LOCAL_NEWER;
            } else if (remoteChanged) {
                state = FileState.REMOTE_NEWER;
            } else {
                state = FileState.IN_SYNC;
            }

            if (state != FileState.IN_SYNC) {
                conflicts.add(new FileStatus(localFile, remotePath, state));
            }
        }
        return conflicts;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
