package gov.hhs.onc.pdti.data.federation;

import gov.hhs.onc.pdti.data.DirectoryDataException;

public class DirectoryFederationException extends DirectoryDataException {
    public DirectoryFederationException() {
        super();
    }

    public DirectoryFederationException(String str) {
        super(str);
    }

    public DirectoryFederationException(String str, Throwable throwable) {
        super(str, throwable);
    }

    public DirectoryFederationException(Throwable throwable) {
        super(throwable);
    }
}
