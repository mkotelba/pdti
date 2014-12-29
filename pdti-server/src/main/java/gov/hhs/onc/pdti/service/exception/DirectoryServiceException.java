package gov.hhs.onc.pdti.service.exception;

import gov.hhs.onc.pdti.DirectoryException;

public class DirectoryServiceException extends DirectoryException {
    public DirectoryServiceException() {
        super();
    }

    public DirectoryServiceException(String str) {
        super(str);
    }

    public DirectoryServiceException(String str, Throwable throwable) {
        super(str, throwable);
    }

    public DirectoryServiceException(Throwable throwable) {
        super(throwable);
    }
}
