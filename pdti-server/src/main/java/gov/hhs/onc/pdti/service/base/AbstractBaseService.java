package gov.hhs.onc.pdti.service.base;

import gov.hhs.onc.pdti.DirectoryStandard;
import gov.hhs.onc.pdti.DirectoryStandardId;
import gov.hhs.onc.pdti.error.DirectoryErrorBuilder;
import gov.hhs.onc.pdti.interceptor.DirectoryRequestInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryResponseInterceptor;
import gov.hhs.onc.pdti.jaxb.DirectoryJaxb2Marshaller;
import gov.hhs.onc.pdti.ws.api.BatchRequest;
import gov.hhs.onc.pdti.ws.api.BatchResponse;
import gov.hhs.onc.pdti.ws.api.Control;
import gov.hhs.onc.pdti.ws.api.DsmlMessage;
import gov.hhs.onc.pdti.ws.api.ObjectFactory;
import gov.hhs.onc.pdti.ws.api.ErrorResponse.ErrorType;

import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractBaseService<T, U> {
	protected final static Logger LOGGER = Logger.getLogger(AbstractBaseService.class);
	protected static String dirStaticId = "";
	protected static String staticWsdlUrl = "";
	protected static String iheoid = "";
	@Autowired
    protected DirectoryJaxb2Marshaller dirJaxb2Marshaller;
	@Autowired
    @DirectoryStandard(DirectoryStandardId.IHE)
    protected ObjectFactory objectFactory;
    @Autowired
    protected DirectoryErrorBuilder errBuilder;
    protected Set<DirectoryRequestInterceptor<T, U>> reqInterceptors;
    protected Set<DirectoryResponseInterceptor<T, U>> respInterceptors;
    
    protected void logRequestInterception(String dirId, String reqId, T queryReq,
			DirectoryRequestInterceptor<T, U> reqInterceptor) {
		LOGGER.trace("Intercepted (class=" + reqInterceptor.getClass().getName() + ") request (directoryId=" + dirId
				+ ", requestId=" + reqId + ", requestClass=" + queryReq.getClass().getName() + ").");
	}
    
    protected void logResponseInterception(String dirId, String reqId,
			U queryResp,
			DirectoryResponseInterceptor<T, U> respInterceptor) {
		LOGGER.trace("Intercepted (class=" + respInterceptor.getClass().getName() + ") response (directoryId=" + dirId
				+ ", requestId=" + reqId + ", responseClass=" + queryResp.getClass().getName() + ").");
	}
    
    protected void addError(String dirId, String reqId, BatchResponse batchResp, Throwable th) {
		// TODO: improve error handling
		batchResp.getBatchResponses().add(this.objectFactory.createBatchResponseErrorResponse(this.errBuilder.buildErrorResponse(reqId, ErrorType.OTHER, th)));
	}
    
    protected boolean isFederatedRequest(BatchRequest batchReq) {
		if (null != batchReq && null != batchReq.getBatchRequests() && batchReq.getBatchRequests().size() > 0) {
			DsmlMessage dsml = batchReq.getBatchRequests().get(0);
			if (null != dsml && null != dsml.getControl() && dsml.getControl().size() > 0) {
				Control ctrl = dsml.getControl().get(0);
				if (null != dsml.getControl().get(0).getControlValue()) {
					if(ctrl.getType().equals(iheoid)) {
						return true;
					}
				}
			}
		}
		return false;
	}
    
    @Autowired(required = false)
    @DirectoryStandard(DirectoryStandardId.IHE)
    protected void setRequestInterceptors(SortedSet<DirectoryRequestInterceptor<T, U>> reqInterceptors) {
        this.reqInterceptors = reqInterceptors;
    }

    @Autowired(required = false)
    @DirectoryStandard(DirectoryStandardId.IHE)
    protected void setResponseInterceptors(SortedSet<DirectoryResponseInterceptor<T, U>> respInterceptors) {
        this.respInterceptors = respInterceptors;
    }
}
