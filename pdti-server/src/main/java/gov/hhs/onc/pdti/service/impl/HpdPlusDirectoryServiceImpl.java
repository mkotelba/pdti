package gov.hhs.onc.pdti.service.impl;

import gov.hhs.onc.pdti.DirectoryStandard;
import gov.hhs.onc.pdti.DirectoryStandardId;
import gov.hhs.onc.pdti.DirectoryType;
import gov.hhs.onc.pdti.DirectoryTypeId;
import gov.hhs.onc.pdti.data.DirectoryDataService;
import gov.hhs.onc.pdti.data.DirectoryDescriptor;
import gov.hhs.onc.pdti.data.federation.FederationService;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptorException;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptorNoOpException;
import gov.hhs.onc.pdti.interceptor.DirectoryRequestInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryResponseInterceptor;
import gov.hhs.onc.pdti.service.DirectoryService;
import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;
import gov.hhs.onc.pdti.statistics.service.PdtiAuditLog;
import gov.hhs.onc.pdti.statistics.service.impl.PdtiAuditLogImpl;
import gov.hhs.onc.pdti.util.DirectoryUtils;
import gov.hhs.onc.pdti.ws.api.BatchRequest;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusErrorType;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusMetadataProperties;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusMetadataProperty;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusMetadataPropertyName;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusRequest;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusRequestMetadata;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusResponse;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusResponseMetadata;
import gov.hhs.onc.pdti.ws.api.hpdplus.ObjectFactory;
import java.util.Date;
import java.util.SortedSet;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.XmlMappingException;
import org.springframework.stereotype.Service;

@DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
@Scope("singleton")
@Service("hpdPlusDirService")
public class HpdPlusDirectoryServiceImpl extends AbstractDirectoryService<HpdPlusRequest, HpdPlusResponse> implements
        DirectoryService<HpdPlusRequest, HpdPlusResponse> {

    private final static Logger LOGGER = Logger.getLogger(HpdPlusDirectoryServiceImpl.class);

    @Autowired
    @DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
    private ObjectFactory hpdPlusObjectFactory;

    @Override
    public HpdPlusResponse processRequest(HpdPlusRequest hpdPlusReq) {
        String dirId = this.dirDesc.getDirectoryId(), reqId = DirectoryUtils.defaultRequestId(hpdPlusReq.getRequestId());
        BatchRequest batchReq = hpdPlusReq.getBatchRequest();
        HpdPlusRequestMetadata reqMeta = hpdPlusReq.getRequestMetadata();
        HpdPlusResponse hpdPlusResp = this.hpdPlusObjectFactory.createHpdPlusResponse();
        DirectoryInterceptorNoOpException noOpException = null;
        PDTIStatisticsEntity entity = new PDTIStatisticsEntity();
        entity.setBaseDn(dirId);
        entity.setCreationDate(new Date());
        entity.setPdRequestType("HpdPlusRequest");
        try {
            this.interceptRequests(dirDesc, dirId, reqId, hpdPlusReq, hpdPlusResp);
        } catch (DirectoryInterceptorNoOpException e) {
            noOpException = e;
        } catch (DirectoryInterceptorException e) {
            this.addError(dirId, reqId, hpdPlusResp, e);
        }

        try {
            String hpdPlusReqStr = this.dirJaxb2Marshaller.marshal(this.hpdPlusObjectFactory.createHpdPlusRequest(hpdPlusReq));

            if (noOpException != null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Skipping processing of HPD Plus request (directoryId=" + dirId + ", requestId=" + reqId + "):\n" + hpdPlusReqStr,
                            noOpException);
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Skipping processing of HPD Plus request (directoryId=" + dirId + ", requestId=" + reqId + ").", noOpException);
                }
            } else {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Processing HPD Plus request (directoryId=" + dirId + ", requestId=" + reqId + "):\n" + hpdPlusReqStr);
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Processing HPD Plus request (directoryId=" + dirId + ", requestId=" + reqId + ").");
                }

                if (this.dataServices != null) {
                    for (DirectoryDataService<?> dataService : this.dataServices) {
                        try {
                            hpdPlusResp.getResponseItems().addAll(dataService.processData(batchReq));
                        } catch (Throwable th) {
                            this.addError(dirId, reqId, hpdPlusResp, th);
                        }
                    }
                }

                if (!this.containsMetadataProperty(reqMeta, HpdPlusMetadataPropertyName.DO_NOT_FEDERATE)) {
                    try {
                        hpdPlusResp.getResponseItems().addAll(this.fedService.federate(hpdPlusReq));
                    } catch (Throwable th) {
                        this.addError(dirId, reqId, hpdPlusResp, th);
                    }
                }

                this.transferMetadata(reqMeta, hpdPlusResp);
            }
        } catch (XmlMappingException e) {
            this.addError(dirId, reqId, hpdPlusResp, e);
        }

        try {
            this.interceptResponses(dirDesc, dirId, reqId, hpdPlusReq, hpdPlusResp);
        } catch (DirectoryInterceptorException e) {
            this.addError(dirId, reqId, hpdPlusResp, e);
        }

        try {
            String hpdPlusRespStr = this.dirJaxb2Marshaller.marshal(this.hpdPlusObjectFactory.createHpdPlusResponse(hpdPlusResp));

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processed HPD Plus request (directoryId=" + dirId + ", requestId=" + reqId + ") into HPD Plus response:\n" + hpdPlusRespStr);
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Processed HPD Plus request (directoryId=" + dirId + ", requestId=" + reqId + ") into HPD Plus response.");
            }
        } catch (XmlMappingException e) {
            this.addError(dirId, reqId, hpdPlusResp, e);
        }
        if (null != hpdPlusResp && null != hpdPlusResp.getErrors() && hpdPlusResp.getErrors().size() > 0) {
            entity.setStatus("Error");
        } else {
            entity.setStatus("Success");
        }
        PdtiAuditLog pdtiAuditLogService = PdtiAuditLogImpl.getInstance();
        pdtiAuditLogService.save(entity);
        return hpdPlusResp;
    }

    @Override
    protected void addError(String dirId, String reqId, HpdPlusResponse hpdPlusResp, Throwable th) {
        // TODO: improve error handling
        hpdPlusResp.getErrors().add(this.errBuilder.buildError(dirId, reqId, HpdPlusErrorType.OTHER, th));
    }

    private boolean containsMetadataProperty(HpdPlusRequestMetadata reqMeta, HpdPlusMetadataPropertyName metaPropName) {
        HpdPlusMetadataProperties metaProps = null;

        if ((reqMeta == null) || ((metaProps = reqMeta.getProperties()) == null)) {
            return false;
        }

        for (HpdPlusMetadataProperty metaProp : metaProps.getProperty()) {
            if (metaProp.getName().equals(metaPropName)) {
                return true;
            }
        }

        return false;
    }

    private void transferMetadata(HpdPlusRequestMetadata reqMeta, HpdPlusResponse hpdPlusResp) {
        if (reqMeta != null) {
            HpdPlusResponseMetadata respMeta = this.hpdPlusObjectFactory.createHpdPlusResponseMetadata();
            respMeta.setRequestMetadata((HpdPlusRequestMetadata) reqMeta.clone());

            if (reqMeta.isSetProperties()) {
                respMeta.setProperties((HpdPlusMetadataProperties) respMeta.getRequestMetadata().getProperties().clone());
            }

            hpdPlusResp.setResponseMetadata(respMeta);
        }
    }

    @Autowired
    @DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
    @DirectoryType(DirectoryTypeId.MAIN)
    @Override
    protected void setDirectoryDescriptor(DirectoryDescriptor dirDesc) {
        this.dirDesc = dirDesc;
    }

    @Autowired
    @DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
    @Override
    protected void setFederationService(FederationService<HpdPlusRequest, HpdPlusResponse> fedService) {
        this.fedService = fedService;
    }

    @Autowired(required = false)
    @DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
    @Override
    protected void setRequestInterceptors(SortedSet<DirectoryRequestInterceptor<HpdPlusRequest, HpdPlusResponse>> reqInterceptors) {
        this.reqInterceptors = reqInterceptors;
    }

    @Autowired(required = false)
    @DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
    @Override
    protected void setResponseInterceptors(SortedSet<DirectoryResponseInterceptor<HpdPlusRequest, HpdPlusResponse>> respInterceptors) {
        this.respInterceptors = respInterceptors;
    }
}
