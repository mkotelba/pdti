package gov.hhs.onc.pdti.error;

import gov.hhs.onc.pdti.ws.api.ErrorResponse;
import gov.hhs.onc.pdti.ws.api.ErrorResponse.ErrorType;

public interface DirectoryErrorBuilder {
    public ErrorResponse buildErrorResponse(String reqId, ErrorType errType, Throwable th);
}
