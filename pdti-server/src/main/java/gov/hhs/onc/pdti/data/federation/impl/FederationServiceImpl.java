package gov.hhs.onc.pdti.data.federation.impl;


import gov.hhs.onc.pdti.DirectoryStandard;
import gov.hhs.onc.pdti.DirectoryStandardId;
import gov.hhs.onc.pdti.DirectoryType;
import gov.hhs.onc.pdti.DirectoryTypeId;
import gov.hhs.onc.pdti.data.DirectoryDescriptor;
import gov.hhs.onc.pdti.data.federation.DirectoryFederationException;
import gov.hhs.onc.pdti.data.federation.FederationService;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptorException;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptorNoOpException;
import gov.hhs.onc.pdti.interceptor.DirectoryRequestInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryResponseInterceptor;
import gov.hhs.onc.pdti.ws.api.BatchRequest;
import gov.hhs.onc.pdti.ws.api.BatchResponse;
import gov.hhs.onc.pdti.ws.api.ErrorResponse.ErrorType;
import gov.hhs.onc.pdti.ws.api.ObjectFactory;
import gov.hhs.onc.pdti.ws.api.ProviderInformationDirectoryService;
import java.util.List;
import java.util.SortedSet;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@DirectoryStandard(DirectoryStandardId.IHE)
@Scope("singleton")
@Service("fedService")
public class FederationServiceImpl extends AbstractFederationService<BatchRequest, BatchResponse> implements FederationService<BatchRequest, BatchResponse> {
    private final static Logger LOGGER = Logger.getLogger(FederationServiceImpl.class);

    @Autowired
    @DirectoryStandard(DirectoryStandardId.IHE)
    private ObjectFactory objectFactory;

    @Override
    public BatchResponse federate(DirectoryDescriptor fedDir, BatchRequest batchReq) throws DirectoryFederationException {
        String fedDirId = fedDir.getDirectoryId(), reqId = batchReq.getRequestId();
        BatchRequest fedBatchReq = (BatchRequest) batchReq.clone();
        BatchResponse fedBatchResp = this.objectFactory.createBatchResponse();
        DirectoryInterceptorNoOpException noOpException = null;

        try {
            this.interceptRequests(fedDir, fedDirId, reqId, fedBatchReq, fedBatchResp);
        } catch (DirectoryInterceptorNoOpException e) {
            noOpException = e;
        } catch (DirectoryInterceptorException e) {
            this.addError(fedDirId, reqId, fedBatchResp, e);
        }

        if (noOpException != null) {
            LOGGER.debug("Skipping federation to federated directory (directoryId=" + fedDirId + ").", noOpException);
        } else {
            try {
                ProviderInformationDirectoryService fedDirService = new ProviderInformationDirectoryService(fedDir.getWsdlLocation());

                fedBatchResp = fedDirService.getProviderInformationDirectoryPortSoap().providerInformationQueryRequest(fedBatchReq);
            } catch (Throwable th) {
                this.addError(fedDirId, reqId, fedBatchResp, th);
            }
        }

        try {
            this.interceptResponses(fedDir, fedDirId, reqId, fedBatchReq, fedBatchResp);
        } catch (DirectoryInterceptorException e) {
            this.addError(fedDirId, reqId, fedBatchResp, e);
        }

        return fedBatchResp;
    }

    // TODO: improve error handling
    @Override
    protected void addError(String fedDirId, String reqId, BatchResponse fedBatchResp, Throwable th) {
        fedBatchResp.getBatchResponses().add(
                this.objectFactory.createBatchResponseErrorResponse(this.errBuilder.buildErrorResponse(reqId, ErrorType.OTHER, th)));
    }

    @Autowired(required = false)
    @DirectoryStandard(DirectoryStandardId.IHE)
    @DirectoryType(DirectoryTypeId.FEDERATED)
    @Override
    protected void setFederatedDirs(List<DirectoryDescriptor> fedDirs) {
        this.fedDirs = fedDirs;
    }

    @Autowired(required = false)
    @DirectoryStandard(DirectoryStandardId.IHE)
    @Override
    protected void setFederatedRequestInterceptors(SortedSet<DirectoryRequestInterceptor<BatchRequest, BatchResponse>> fedReqInterceptors) {
        this.fedReqInterceptors = fedReqInterceptors;
    }

    @Autowired(required = false)
    @DirectoryStandard(DirectoryStandardId.IHE)
    @Override
    protected void setFederatedResponseInterceptors(SortedSet<DirectoryResponseInterceptor<BatchRequest, BatchResponse>> fedRespInterceptors) {
        this.fedRespInterceptors = fedRespInterceptors;
    }
}
