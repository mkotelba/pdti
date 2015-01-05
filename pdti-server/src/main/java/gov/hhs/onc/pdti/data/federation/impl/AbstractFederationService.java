package gov.hhs.onc.pdti.data.federation.impl;


import gov.hhs.onc.pdti.DirectoryType;
import gov.hhs.onc.pdti.DirectoryTypeId;
import gov.hhs.onc.pdti.data.DirectoryDescriptor;
import gov.hhs.onc.pdti.data.federation.DirectoryFederationException;
import gov.hhs.onc.pdti.data.federation.FederationService;
import gov.hhs.onc.pdti.error.DirectoryErrorBuilder;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptorException;
import gov.hhs.onc.pdti.interceptor.DirectoryRequestInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryResponseInterceptor;
import gov.hhs.onc.pdti.jaxb.DirectoryJaxb2Marshaller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;

public abstract class AbstractFederationService<T, U> implements FederationService<T, U> {
	@Autowired
    protected DirectoryJaxb2Marshaller dirJaxb2Marshaller;
    @Autowired
    protected DirectoryErrorBuilder errBuilder;

    protected List<DirectoryDescriptor> fedDirs;

    protected Set<DirectoryRequestInterceptor<T, U>> fedReqInterceptors;

    protected Set<DirectoryResponseInterceptor<T, U>> fedRespInterceptors;

    private final static Logger LOGGER = Logger.getLogger(AbstractFederationService.class);

    @Override
    public List<U> federate(T queryReq) throws DirectoryFederationException {
        List<U> queryResps = new ArrayList<>();

        if (this.fedDirs != null) {
            for (DirectoryDescriptor fedDir : this.fedDirs) {
                if (!fedDir.isEnabled()) {
                    LOGGER.trace("Skipping disabled federated directory (directoryId=" + fedDir.getDirectoryId() + ").");
                    continue;
                }

                queryResps.add(this.federate(fedDir, queryReq));
            }
        }

        return queryResps;
    }

    protected void interceptRequests(DirectoryDescriptor fedDir, String fedDirId, String reqId, T fedQueryReq, U fedQueryResp)
            throws DirectoryInterceptorException {
        if (this.fedReqInterceptors != null) {
            for (DirectoryRequestInterceptor fedReqInterceptor : this.fedReqInterceptors) {
                if (!this.isFederatedInterceptor(fedReqInterceptor)) {
                    continue;
                }

                try {
                    fedReqInterceptor.interceptRequest(fedDir, reqId, fedQueryReq, fedQueryResp);

                    LOGGER.trace("Intercepted (class=" + fedReqInterceptor.getClass().getName() + ") federated request (directoryId=" + fedDirId
                            + ", requestId=" + reqId + ", requestClass=" + fedQueryReq.getClass().getName() + ").");
                } catch (DirectoryInterceptorException e) {
                    throw e;
                } catch (Throwable th) {
                    throw new DirectoryInterceptorException("Unable to intercept (class=" + fedReqInterceptor.getClass().getName()
                            + ") federated request (directoryId=" + fedDirId + ", requestId=" + reqId + ", responseClass=" + fedQueryResp.getClass().getName()
                            + ").", th);
                }
            }
        }
    }

    protected void interceptResponses(DirectoryDescriptor fedDir, String fedDirId, String reqId, T fedQueryReq, U fedQueryResp)
            throws DirectoryInterceptorException {
        if (this.fedRespInterceptors != null) {
            for (DirectoryResponseInterceptor fedRespInterceptor : this.fedRespInterceptors) {
                if (!this.isFederatedInterceptor(fedRespInterceptor)) {
                    continue;
                }

                try {
                    fedRespInterceptor.interceptResponse(fedDir, reqId, fedQueryReq, fedQueryResp);

                    LOGGER.trace("Intercepted (class=" + fedRespInterceptor.getClass().getName() + ") federated response (directoryId=" + fedDirId
                            + ", requestId=" + reqId + ", responseClass=" + fedQueryResp.getClass().getName() + ").");
                } catch (DirectoryInterceptorException e) {
                    throw e;
                } catch (Throwable th) {
                    throw new DirectoryInterceptorException("Intercepted (class=" + fedRespInterceptor.getClass().getName()
                            + ") federated response (directoryId=" + fedDirId + ", requestId=" + reqId + ", responseClass=" + fedQueryResp.getClass().getName()
                            + ").", th);
                }
            }
        }
    }

    protected boolean isFederatedInterceptor(DirectoryInterceptor<T, U> dirInterceptor) {
        DirectoryType dirInterceptorType;

        return ((dirInterceptorType = AnnotationUtils.findAnnotation(dirInterceptor.getClass(), DirectoryType.class)) == null)
                || (dirInterceptorType.value() == DirectoryTypeId.FEDERATED);
    }

    protected abstract void addError(String fedDirId, String reqId, U fedQueryResp, Throwable th);

    protected abstract void setFederatedDirs(List<DirectoryDescriptor> federatedDirs);

    protected abstract void setFederatedRequestInterceptors(SortedSet<DirectoryRequestInterceptor<T, U>> fedReqInterceptors);

    protected abstract void setFederatedResponseInterceptors(SortedSet<DirectoryResponseInterceptor<T, U>> fedRespInterceptors);
}
