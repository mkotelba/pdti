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
import gov.hhs.onc.pdti.ws.api.Control;
import gov.hhs.onc.pdti.ws.api.FederatedResponseStatus;
import gov.hhs.onc.pdti.ws.api.FederatedSearchResponseData;
import gov.hhs.onc.pdti.ws.api.SearchResponse;
import gov.hhs.onc.pdti.ws.api.SearchResultEntryMetadata;
import gov.hhs.onc.pdti.ws.api.ErrorResponse.ErrorType;
import gov.hhs.onc.pdti.ws.api.ObjectFactory;
import gov.hhs.onc.pdti.ws.api.ProviderInformationDirectoryService;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
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
    
    private static String dirStaticId = "";
	private static String staticWsdlUrl = "";
	
	private static String iheoid = "";

    @Override
    public BatchResponse federate(DirectoryDescriptor fedDir, BatchRequest batchReq) throws DirectoryFederationException {
        String fedDirId = fedDir.getDirectoryId(), reqId = batchReq.getRequestId();
        BatchRequest fedBatchReq = (BatchRequest) batchReq.clone();
        BatchResponse fedBatchResp = this.objectFactory.createBatchResponse();
        DirectoryInterceptorNoOpException noOpException = null;
        InputStream input = null;
		Properties prop = new Properties();
        String dirId = fedDir.getDirectoryId();
		String wsdlUrl = fedDir.getWsdlLocation().toString();
		dirStaticId = dirId;
		staticWsdlUrl = wsdlUrl;
		boolean isError = false;
		String batchReqStr = null;
        try {
            this.interceptRequests(fedDir, fedDirId, reqId, fedBatchReq, fedBatchResp);
            batchReqStr = this.dirJaxb2Marshaller.marshal(this.objectFactory.createBatchRequest(batchReq));
            input = getClass().getClassLoader().getResourceAsStream("federationinfo.properties");
			prop.load(input);
			iheoid = prop.getProperty("ihefederationoid");
        } catch (DirectoryInterceptorNoOpException e) {
            noOpException = e;
        } catch (DirectoryInterceptorException e) {
            this.addError(fedDirId, reqId, fedBatchResp, e);
        }catch (Throwable th) {
			isError = true;
			this.addError(dirId, reqId, fedBatchResp, th);
		} finally {
			if (null != input) {
				try {
					input.close();
				} catch (IOException e) {
					isError = true;
					this.addError(dirId, reqId, fedBatchResp, e);
				}
			}
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

        combineFederatedBatchResponses(fedBatchResp, fedBatchReq);
        return fedBatchResp;
    }

    // TODO: improve error handling
    @Override
    protected void addError(String fedDirId, String reqId, BatchResponse fedBatchResp, Throwable th) {
        fedBatchResp.getBatchResponses().add(
                this.objectFactory.createBatchResponseErrorResponse(this.errBuilder.buildErrorResponse(reqId, ErrorType.OTHER, th)));
    }

    /**
	 *
	 * @param batchRequest
	 * @return Control
	 */
    private Control buildSearchResultEntryMetadaCtrl(BatchRequest batchRequest) {
		Control ctrl = new Control();
		ctrl.setType("1.3.6.1.4.1.19376.1.2.4.4.7");
		ctrl.setCriticality(false);
		try {
			StringWriter stringWriter = new StringWriter();
			JAXBContext jaxbContext = JAXBContext.newInstance(SearchResultEntryMetadata.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			SearchResultEntryMetadata searchResultEntryMetadata = this.objectFactory.createSearchResultEntryMetadata();
			searchResultEntryMetadata.setDirectoryId(dirStaticId);
			searchResultEntryMetadata.setDirectoryURI(staticWsdlUrl);

			QName qName = new QName("gov.hhs.onc.pdti.ws.api", "searchResultEntryMetadata");
			JAXBElement<SearchResultEntryMetadata> root = new JAXBElement<SearchResultEntryMetadata>(qName, SearchResultEntryMetadata.class, searchResultEntryMetadata);
			jaxbMarshaller.marshal(root, stringWriter);			
			ctrl.setControlValue(new String(Base64.encodeBase64(stringWriter.toString().getBytes())));

		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return ctrl;
	}
	
	/**
	 *
	 * @param batchRequest
	 * @return Control
	 */
	private Control buildFederatedResponseDataCtrl(BatchRequest batchRequest) {

		Control ctrl = new Control();
		ctrl.setType("1.3.6.1.4.1.19376.1.2.4.4.8");
		ctrl.setCriticality(false);

		try {
			StringWriter stringWriter = new StringWriter();
			JAXBContext jaxbContext = JAXBContext.newInstance(FederatedSearchResponseData.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			FederatedResponseStatus oStatus = this.objectFactory.createFederatedResponseStatus();
			oStatus.setDirectoryId(dirStaticId);
			oStatus.setFederatedRequestId(iheoid);
			oStatus.setResultMessage("Success");

			FederatedSearchResponseData federatedSearchResponseData = this.objectFactory.createFederatedSearchResponseData();
			federatedSearchResponseData.setFederatedResponseStatus(oStatus);

			QName qName = new QName("gov.hhs.onc.pdti.ws.api", "federatedSearchResponseData");
			JAXBElement<FederatedSearchResponseData> root = new JAXBElement<FederatedSearchResponseData>(qName, FederatedSearchResponseData.class, federatedSearchResponseData);
			jaxbMarshaller.marshal(root, stringWriter);			
			ctrl.setControlValue(new String(Base64.encodeBase64(stringWriter.toString().getBytes())));
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return ctrl;
	}
	
	/**
	 *
	 * @param batchResp
	 * @param batchRespCombine
	 */
	private void combineFederatedBatchResponses(BatchResponse batchResp, BatchRequest batchRequest) {

		int count = batchResp.getBatchResponses().size();
		int responseCount = 0;
		Control searchResultEntryCtrl = buildSearchResultEntryMetadaCtrl(batchRequest);
		Control federatedResponseDataCtrl = buildFederatedResponseDataCtrl(batchRequest);
		while (responseCount < count) {
			if (batchResp.getBatchResponses().get(responseCount).getValue() instanceof SearchResponse) {
				((SearchResponse) batchResp.getBatchResponses().get(responseCount).getValue()).getSearchResultDone().getControl().add(federatedResponseDataCtrl);
				int entryCount = 0;
				int totalEntryCount = ((SearchResponse) batchResp.getBatchResponses().get(responseCount).getValue()).getSearchResultEntry().size();
				while (entryCount < totalEntryCount) {
					((SearchResponse) batchResp.getBatchResponses().get(responseCount).getValue()).getSearchResultEntry().get(entryCount).getControl().add(searchResultEntryCtrl);
					entryCount++;
				}
			}
			responseCount++;
		}
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
