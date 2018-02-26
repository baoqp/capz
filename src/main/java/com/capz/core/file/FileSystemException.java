package com.capz.core.file;

import com.capz.core.Exception.CapzException;

public class FileSystemException extends CapzException {

    public FileSystemException(String message) {
        super(message);
    }

    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileSystemException(Throwable cause) {
        super(cause);
    }
}
