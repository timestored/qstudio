package com.timestored.sync;

import lombok.Data;

/** Metadata for a single remote file or directory. */
@Data
public class RemoteFileInfo {
    private final String name;
    private final String path;
    private final long size;
    private final long mtime;        // epoch seconds
    private final boolean directory;
}
