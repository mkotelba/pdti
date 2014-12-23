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
import gov.hhs.onc.pdti.statistics.service.PdtiAuditService;
import gov.hhs.onc.pdti.util.DirectoryUtils;
import gov.hhs.onc.pdti.ws.api.BatchRequest;
import gov.hhs.onc.pdti.ws.api.BatchResponse;
import gov.hhs.onc.pdti.ws.api.Control;
import gov.hhs.onc.pdti.ws.api.DsmlMessage;
import gov.hhs.onc.pdti.ws.api.ErrorResponse.ErrorType;
import gov.hhs.onc.pdti.ws.api.FederatedResponseStatus;
import gov.hhs.onc.pdti.ws.api.FederatedSearchResponseData;
import gov.hhs.onc.pdti.ws.api.ObjectFactory;
import gov.hhs.onc.pdti.ws.api.SearchResponse;
import gov.hhs.onc.pdti.ws.api.SearchResultEntryMetadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.util.Date;
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
import org.springframework.oxm.XmlMappingException;
import org.springframework.stereotype.Service;

@DirectoryStandard(DirectoryStandardId.IHE)
@Scope("singleton")
@Service("dirService")
public class DirectoryServiceImpl extends AbstractDirectoryService<BatchRequest, BatchResponse> implements DirectoryService<BatchRequest, BatchResponse> {

	private final static Logger LOGGER = Logger.getLogger(DirectoryServiceImpl.class);
	private static String iheoid = "";
	@Autowired
	@DirectoryStandard(DirectoryStandardId.IHE)
	private ObjectFactory objectFactory;
	private static String dirStaticId = "";
	private static String staticWsdlUrl = "";
	private boolean isFederatedRequest = false;

	@Autowired
	PdtiAuditService pdtiAuditLogService;

	@Override
	public BatchResponse processRequest(BatchRequest batchReq) {
		DirectoryInterceptorNoOpException noOpException = null;
		boolean isError = false;
		InputStream input = null;
		Properties prop = new Properties();
		String dirId = this.dirDesc.getDirectoryId();
		String wsdlUrl = this.dirDesc.getWsdlLocation().toString();
		dirStaticId = dirId;
		staticWsdlUrl = wsdlUrl;
		String reqId = DirectoryUtils.defaultRequestId(batchReq.getRequestId());
		BatchResponse batchResp = this.objectFactory.createBatchResponse();
		PDTIStatisticsEntity entity = new PDTIStatisticsEntity();
		entity.setBaseDn(dirId);
		entity.setCreationDate(new Date());
		entity.setPdRequestType("BatchRequest");

		String batchReqStr = null;
		try {
			try {
				this.interceptRequests(dirDesc, dirId, reqId, batchReq, batchResp);
				batchReqStr = this.dirJaxb2Marshaller.marshal(this.objectFactory.createBatchRequest(batchReq));
				input = getClass().getClassLoader().getResourceAsStream("federationinfo.properties");
				prop.load(input);
				iheoid = prop.getProperty("ihefederationoid");
			} catch (DirectoryInterceptorNoOpException e) {
				noOpException = e;
			} catch (DirectoryInterceptorException e) {
				isError = true;
				this.addError(dirId, reqId, batchResp, e);
			} catch (Throwable th) {
				isError = true;
				this.addError(dirId, reqId, batchResp, th);
			} finally {
				if (null != input) {
					try {
						input.close();
					} catch (IOException e) {
						isError = true;
						this.addError(dirId, reqId, batchResp, e);
					}
				}
			}

			if (noOpException != null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Skipping processing of DSML batch request (directoryId=" + dirId + ", requestId=" + reqId + "):\n" + batchReqStr,
							noOpException);
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Skipping processing of DSML batch request (directoryId=" + dirId + ", requestId=" + reqId + ").", noOpException);
				}
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Processing DSML batch request (directoryId=" + dirId + ", requestId=" + reqId + "):\n" + batchReqStr);
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Processing DSML batch request (directoryId=" + dirId + ", requestId=" + reqId + ").");
				}
				getFederatedRequestId(batchReq);
				//If Federation is enabled, then a local ldap call and Federation both should happen.. 
				//In the else part only local Directory will be searched.
				if (isFederatedRequest) {
					if (this.dataServices != null) {
						for (DirectoryDataService<?> dataService : this.dataServices) {
							try {
								combineBatchResponses(batchResp, dataService.processData(batchReq));
							} catch (Throwable th) {
								this.addError(dirId, reqId, batchResp, th);
							}
						}
					}
					try {
						combineFederatedBatchResponses(batchResp, batchReq);
						for (BatchResponse batchRespCombineItem : this.fedService.federate(batchReq)) {
							batchResp.getBatchResponses().addAll(batchRespCombineItem.getBatchResponses());
						}
					} catch (Throwable th) {
						isError = true;
						this.addError(dirId, reqId, batchResp, th);
					}
				} else {
					// Call Local LDAP Directory...
					LOGGER.info("Inside Local Directory Call...");
					if (this.dataServices != null) {
						for (DirectoryDataService<?> dataService : this.dataServices) {
							try {
								combineBatchResponses(batchResp, dataService.processData(batchReq));
							} catch (Throwable th) {
								this.addError(dirId, reqId, batchResp, th);
							}
						}
					}
				}
			}
		} catch (XmlMappingException e) {
			this.addError(dirId, reqId, batchResp, e);
		}
		try {
			this.interceptResponses(dirDesc, dirId, reqId, batchReq, batchResp);
		} catch (DirectoryInterceptorException e) {
			isError = true;
			this.addError(dirId, reqId, batchResp, e);
		}

		try {
			String batchRespStr = this.dirJaxb2Marshaller.marshal(this.objectFactory.createBatchResponse(batchResp));

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Processed DSML batch request (directoryId=" + dirId + ", requestId=" + reqId + ") into DSML batch response:\n" + batchRespStr);
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Processed DSML batch request (directoryId=" + dirId + ", requestId=" + reqId + ") into DSML batch response.");
			}
		} catch (XmlMappingException e) {
			isError = true;
			this.addError(dirId, reqId, batchResp, e);
		}
		if (isError) {
			entity.setStatus("Error");
		} else {
			entity.setStatus("Success");
		}
		//PdtiAuditLog pdtiAuditLogService = PdtiAuditLogImpl.getInstance();
		pdtiAuditLogService.savePdtiStatisticsEntity(entity);
		return batchResp;
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

	/**
	 *
	 * @param batchReq
	 * @return boolean
	 */
	private boolean getFederatedRequestId(BatchRequest batchReq) {
		if (null != batchReq && null != batchReq.getBatchRequests() && batchReq.getBatchRequests().size() > 0) {
			DsmlMessage dsml = batchReq.getBatchRequests().get(0);
			if (null != dsml && null != dsml.getControl() && dsml.getControl().size() > 0) {
				Control ctrl = dsml.getControl().get(0);
				if (null != dsml.getControl().get(0).getControlValue()) {
					if(ctrl.getType().equals(iheoid)) {
						isFederatedRequest = true;
					}
				}
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(ctrl.getType());
					LOGGER.trace("isFederatedRequest = " + isFederatedRequest);
				} else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(ctrl.getType());
					LOGGER.trace("isFederatedRequest = " + isFederatedRequest);
				}
			}
		}
		return isFederatedRequest;
	}

	@Override
	protected void addError(String dirId, String reqId, BatchResponse batchResp, Throwable th) {
		// TODO: improve error handling
		batchResp.getBatchResponses().add(this.objectFactory.createBatchResponseErrorResponse(this.errBuilder.buildErrorResponse(reqId, ErrorType.OTHER, th)));
	}

	private static void combineBatchResponses(BatchResponse batchResp, List<BatchResponse> batchRespCombine) {
		for (BatchResponse batchRespCombineItem : batchRespCombine) {
			batchResp.getBatchResponses().addAll(batchRespCombineItem.getBatchResponses());
		}
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

	private static byte[] convertFederatedSearchResponseToBytes(String fedSearchResp) {
		return convertToBytes(fedSearchResp);
	}

	private static byte[] convertSearchResultEntryMetadataToBytes(SearchResultEntryMetadata searchResMetadata) {
		return convertToBytes(searchResMetadata);
	}

	/**
	 *
	 * @param resData
	 * @return byte
	 */
	private static byte[] convertToBytes(Object resData) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(bos).writeObject(resData);
		} catch (IOException e) {
			LOGGER.error(e);
		} 
		return Base64.encodeBase64(bos.toByteArray());
	}

	@Autowired
	@DirectoryStandard(DirectoryStandardId.IHE)
	@DirectoryType(DirectoryTypeId.MAIN)
	@Override
	protected void setDirectoryDescriptor(DirectoryDescriptor dirDesc) {
		this.dirDesc = dirDesc;
	}

	@Autowired
	@DirectoryStandard(DirectoryStandardId.IHE)
	@Override
	protected void setFederationService(FederationService<BatchRequest, BatchResponse> fedService) {
		this.fedService = fedService;
	}

	@Autowired(required = false)
	@DirectoryStandard(DirectoryStandardId.IHE)
	@Override
	protected void setRequestInterceptors(SortedSet<DirectoryRequestInterceptor<BatchRequest, BatchResponse>> reqInterceptors) {
		this.reqInterceptors = reqInterceptors;
	}

	@Autowired(required = false)
	@DirectoryStandard(DirectoryStandardId.IHE)
	@Override
	protected void setResponseInterceptors(SortedSet<DirectoryResponseInterceptor<BatchRequest, BatchResponse>> respInterceptors) {
		this.respInterceptors = respInterceptors;
	}
}
