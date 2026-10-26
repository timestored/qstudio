package com.timestored.sync;

import java.io.File;

import lombok.Data;

/** Result types for sync operations. */
public class SyncResult {

    private SyncResult() {} // non-instantiable

    public enum UploadResult { SUCCESS, CONFLICT }

    public enum FileState { LOCAL_NEWER, REMOTE_NEWER, BOTH_CHANGED, IN_SYNC }

    @Data
    public static class FileStatus {
        private final File localFile;
        private final String remotePath;
        private final FileState state;
    }

    @Data
    public static class MirrorResult {
        private final File localDir;
        private final int filesDownloaded;
        private final int filesSkipped;
        private final java.util.List<SkippedFile> skippedFiles;
        private final java.util.Set<String> remotePathsSeen;
    }

    public enum SkipReason { SIZE_LIMIT, GITIGNORED, FILE_COUNT_LIMIT }

    @Data
    public static class SkippedFile {
        private final String remotePath;
        private final SkipReason reason;
        private final long size;
    }
}
