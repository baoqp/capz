package com.capz.core.file;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenOptions {

    public static final String DEFAULT_PERMS = null;
    public static final boolean DEFAULT_READ = true;
    public static final boolean DEFAULT_WRITE = true;
    public static final boolean DEFAULT_CREATE = true;
    public static final boolean DEFAULT_CREATENEW = false;
    public static final boolean DEFAULT_DSYNC = false;
    public static final boolean DEFAULT_SYNC = false;
    public static final boolean DEFAULT_DELETEONCLOSE = false;
    public static final boolean DEFAULT_TRUNCATEEXISTING = false;
    public static final boolean DEFAULT_SPARSE = false;

    /**
     * Whether the file should be opened in append mode by default = false.
     */
    public static final boolean DEFAULT_APPEND = false;

    private String perms = DEFAULT_PERMS;
    private boolean read = DEFAULT_READ;
    private boolean write = DEFAULT_WRITE;
    private boolean create = DEFAULT_CREATE;
    private boolean createNew = DEFAULT_CREATENEW;
    private boolean dsync = DEFAULT_DSYNC;
    private boolean sync = DEFAULT_SYNC;
    private boolean deleteOnClose = DEFAULT_DELETEONCLOSE;
    private boolean truncateExisting = DEFAULT_TRUNCATEEXISTING;
    private boolean sparse = DEFAULT_SPARSE;
    private boolean append = DEFAULT_APPEND;


    public OpenOptions() {
        super();
    }


    public OpenOptions(OpenOptions other) {
        this.perms = other.perms;
        this.read = other.read;
        this.write = other.write;
        this.create = other.create;
        this.createNew = other.createNew;
        this.dsync = other.dsync;
        this.sync = other.sync;
        this.deleteOnClose = other.deleteOnClose;
        this.truncateExisting = other.truncateExisting;
        this.sparse = other.sparse;
        this.append = other.append;
    }



}
