package gov.hhs.onc.pdti.client.actions;

import gov.hhs.onc.pdti.client.federation.xml.FederatedRequestData;
import gov.hhs.onc.pdti.client.types.ProviderDirectoryTypes;
import gov.hhs.onc.pdti.client.wrappers.SearchResultWrapper;
import gov.hhs.onc.pdti.ws.api.AttributeValueAssertion;
import gov.hhs.onc.pdti.ws.api.BatchRequest;
import gov.hhs.onc.pdti.ws.api.BatchResponse;
import gov.hhs.onc.pdti.ws.api.Control;
import gov.hhs.onc.pdti.ws.api.DsmlAttr;
import gov.hhs.onc.pdti.ws.api.ErrorResponse;
import gov.hhs.onc.pdti.ws.api.Filter;
import gov.hhs.onc.pdti.ws.api.ProviderInformationDirectoryService;
import gov.hhs.onc.pdti.ws.api.SearchRequest;
import gov.hhs.onc.pdti.ws.api.SearchResponse;
import gov.hhs.onc.pdti.ws.api.SearchResultEntry;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.interceptor.ServletRequestAware;

import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimeUtility;

public class Search extends BaseAction implements ServletRequestAware {

    private static final long serialVersionUID = -7807158769818341299L;
    private static final Logger LOGGER = Logger.getLogger(Search.class);
    private static final String OU = "ou=";
    private static final String COMMA = ",";
    private static final String DN = "dn";
    private static final String SINGLE_LEVEL = "singleLevel";
    private static final String DEREF_FINDING_BASE_OBJ = "derefFindingBaseObj";
    private static final String PERIOD = ".";
    private static final String JAXB_TYPE_ERROR_MESSAGE_PREAMBLE = "Unknown JAXBElement value: ";
    private static final String PD_TYPE_ERROR_MESSAGE_PREAMBLE = "Unknown Provider Directory type: ";
    private static final String HPDPLUS_RESPONSE_ITEM_TYPE_ERROR_MESSAGE_PREAMBLE
            = "Unknown HPDPlus response item type: ";
    private static final String SEARCH_ATTRIBUTE_PARAM_NAME = "searchAttribute";
    private static final String SEARCH_STRING_PARAM_NAME = "searchString";
    private static final String REQUIRED_FIELD_MESSAGE = "This field is required.";
    private static final String WSDL_PROPERTY_NAME = "provider.directory.wsdl.url";
    private static final String DETAILS = "details";
    private static final String NO_ID = "NO ID (DSML DIRECTORY)";
    private final String defaultUrl = getText(WSDL_PROPERTY_NAME);

    private gov.hhs.onc.pdti.ws.api.ObjectFactory dsmlBasedObjectFactory = new gov.hhs.onc.pdti.ws.api.ObjectFactory();

    private String url;
    private String providerDirectoryType;
    private String requestId;
    private String textDN;
    private String searchAttribute;
    private String searchString;
    private String[] attributesToRetrieve;
    private SearchResultWrapper[] searchResults = new SearchResultWrapper[0];
    private SearchResultWrapper searchResultToDetail;
    private Boolean showDetails;
    private String dn;
    private List<String> searchErrorMessages = new ArrayList<String>();

    private HttpServletRequest req = null;

    @Override
    public void setServletRequest(HttpServletRequest request) {
        req = request;
    }

    @Validations(requiredFields = {
        @RequiredFieldValidator(type = ValidatorType.SIMPLE, fieldName = SEARCH_ATTRIBUTE_PARAM_NAME, message = REQUIRED_FIELD_MESSAGE),
        @RequiredFieldValidator(type = ValidatorType.SIMPLE, fieldName = SEARCH_STRING_PARAM_NAME, message = REQUIRED_FIELD_MESSAGE)})
    public String execute() {
        LOGGER.debug("execute() called...");
        LOGGER.debug("url =" + url + "=");
        URL wsdlUrl = null;
        requestId = UUID.randomUUID().toString();
        try {
            if (!StringUtils.isEmpty(url)) {
                wsdlUrl = new URL(url);
            } else {
                String baseUrl = "http://" + req.getServerName() + ":" + req.getServerPort() + ProviderDirectoryTypes.getUrl(providerDirectoryType);
                wsdlUrl = new URL(baseUrl);
            }

            LOGGER.debug("wsdlUrl = " + wsdlUrl);
        } catch (MalformedURLException malformedURLException) {
            getSearchErrorMessages().add(malformedURLException.getMessage());
            LOGGER.error(malformedURLException);
            return ERROR;
        }

        if (providerDirectoryType.equals(ProviderDirectoryTypes.DSML_WSDL.toString())) {
            doDsmlSearch(wsdlUrl);
        } else {
            String errorMessage = PD_TYPE_ERROR_MESSAGE_PREAMBLE + providerDirectoryType + PERIOD;
            LOGGER.error(errorMessage);
            searchErrorMessages.add(errorMessage);
            return ERROR;
        }
        if (searchErrorMessages.size() > 0) {
            return ERROR;
        }
        if (Boolean.TRUE.equals(showDetails)) {
            return DETAILS;
        } else {
            return SUCCESS;
        }
    }

    private void doDsmlSearch(URL wsdlUrl) {
        LOGGER.debug("doDsmlSearch(URL)");
        ProviderInformationDirectoryService providerInformationDirectoryService
                = new ProviderInformationDirectoryService(wsdlUrl);
        BatchResponse batchResponse = providerInformationDirectoryService.getProviderInformationDirectoryPortSoap()
                .providerInformationQueryRequest(buildBatchRequest(false));
        List<JAXBElement<?>> batchResponseJAXBElements = batchResponse.getBatchResponses();
        for (JAXBElement<?> batchResponseJAXBElement : batchResponseJAXBElements) {
            Object value = batchResponseJAXBElement.getValue();
            if (value instanceof SearchResponse) {
                SearchResponse searchResponse = (SearchResponse) value;
                if (null != searchResponse.getSearchResultDone().getErrorMessage()) {
                    getSearchErrorMessages().add(searchResponse.getSearchResultDone().getErrorMessage());
                } else {
                    processSearchResultEntries(NO_ID, wsdlUrl.toExternalForm(), searchResponse.getSearchResultEntry());
                }
            } else if (value instanceof ErrorResponse) {
                ErrorResponse errorResponse = (ErrorResponse) value;
                getSearchErrorMessages().add(errorResponse.getMessage());
            } else {
                String errorMessage = JAXB_TYPE_ERROR_MESSAGE_PREAMBLE + value.getClass() + PERIOD;
                LOGGER.error(errorMessage);
                searchErrorMessages.add(errorMessage);
            }
        }
    }

    private void processBatchResponse(String directoryId, String directoryUri, BatchResponse batchResponse) {
        LOGGER.debug("processBatchResponse(BatchRespons)");
        List<JAXBElement<?>> batchResponseJAXBElements = batchResponse.getBatchResponses();
        for (JAXBElement<?> batchResponseJAXBElement : batchResponseJAXBElements) {
            Object value = batchResponseJAXBElement.getValue();
            if (value instanceof SearchResponse) {
                SearchResponse searchResponse = (SearchResponse) value;
                if (null != searchResponse.getSearchResultDone().getErrorMessage()) {
                    getSearchErrorMessages().add(searchResponse.getSearchResultDone().getErrorMessage());
                } else {
                    processSearchResultEntries(directoryId, directoryUri, searchResponse.getSearchResultEntry());
                }
            } else if (value instanceof ErrorResponse) {
                ErrorResponse errorResponse = (ErrorResponse) value;
                getSearchErrorMessages().add(errorResponse.getMessage());
            } else {
                String errorMessage = JAXB_TYPE_ERROR_MESSAGE_PREAMBLE + value.getClass() + PERIOD;
                LOGGER.error(errorMessage);
                searchErrorMessages.add(errorMessage);
            }
        }
    }

    private BatchRequest buildBatchRequest(boolean isHpdPlusRequest) {
        LOGGER.debug("buildBatchRequest()");
        BatchRequest batchRequest = dsmlBasedObjectFactory.createBatchRequest();
        SearchRequest searchRequest = dsmlBasedObjectFactory.createSearchRequest();
        if (!isHpdPlusRequest) {
            if (!StringUtils.isEmpty(requestId)) {
                batchRequest.setRequestId(requestId);
                searchRequest.setRequestId(requestId);
            }
        }

        Control ctrl = new Control();
        ctrl.setType("1.2.3.4.5");
        ctrl.setCriticality(false);
        FederatedRequestData reqData = new FederatedRequestData();
        reqData.setFederatedRequestId("12345");
        ctrl.setControlValue(this.convertToBytes(reqData));
        searchRequest.getControl().clear();
        searchRequest.getControl().add(ctrl);
        //searchRequest.setDn(OU + typeToSearch + COMMA + getText(DN));
        searchRequest.setDn(textDN);
        searchRequest.setScope(SINGLE_LEVEL);
        searchRequest.setDerefAliases(DEREF_FINDING_BASE_OBJ);
        Filter filter = dsmlBasedObjectFactory.createFilter();
        AttributeValueAssertion attributeValueAssertion = dsmlBasedObjectFactory.createAttributeValueAssertion();
        attributeValueAssertion.setName(searchAttribute);
        attributeValueAssertion.setValue(searchString);
        filter.setEqualityMatch(attributeValueAssertion);
        searchRequest.setFilter(filter);
        batchRequest.getBatchRequests().add(searchRequest);
        return batchRequest;
    }

    private void processSearchResultEntries(String directoryId, String directoryUri, List<SearchResultEntry> searchResultEntries) {
        LOGGER.debug(directoryId);
        LOGGER.debug(directoryUri);
        LOGGER.debug("processSearchResultEntries(List<SearchResultEntry>)");
        SearchResultWrapper[] newSearchResults = new SearchResultWrapper[searchResultEntries.size()];
        int index = 0;
        for (SearchResultEntry searchResultEntry : searchResultEntries) {
            SearchResultWrapper searchResultWrapper = new SearchResultWrapper();
            searchResultWrapper.setDn(searchResultEntry.getDn());
            searchResultWrapper.setDirectoryId(directoryId);
            searchResultWrapper.setDirectoryUri(directoryUri);
            List<DsmlAttr> dsmlAttributes = searchResultEntry.getAttr();
            Map<String, List<String>> attributesMap = new TreeMap<String, List<String>>();
            for (DsmlAttr dsmlAttribute : dsmlAttributes) {
                String attributeName = dsmlAttribute.getName();
                List<String> attributeValues = new ArrayList<String>();
                for (String attributeValue : dsmlAttribute.getValue()) {
                    attributeValues.add(attributeValue);
                }
                attributesMap.put(attributeName, attributeValues);
            }
            searchResultWrapper.setAttributes(attributesMap);
            newSearchResults[index] = searchResultWrapper;
            index++;
            if (Boolean.TRUE.equals(showDetails)) {
                if (dn.equals(searchResultEntry.getDn())) {
                    searchResultToDetail = searchResultWrapper;
                    searchResults = null;
                    break;
                }
            }
        }
        searchResults = ArrayUtils.addAll(searchResults, newSearchResults);
    }

    public void setSearchAttribute(String searchAttribute) {
        this.searchAttribute = searchAttribute;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchAttribute() {
        return searchAttribute;
    }

    public String getSearchString() {
        return searchString;
    }

    public SearchResultWrapper[] getSearchResults() {
        return searchResults;
    }

    public List<String> getSearchErrorMessages() {
        return searchErrorMessages;
    }

    public String[] getAttributesToRetrieve() {
        return attributesToRetrieve;
    }

    public void setAttributesToRetrieve(String[] attributesToRetrieve) {
        this.attributesToRetrieve = attributesToRetrieve;
    }

    public SearchResultWrapper getSearchResultToDetail() {
        return searchResultToDetail;
    }

    public void setSearchResultToDetail(SearchResultWrapper searchResultToDetail) {
        this.searchResultToDetail = searchResultToDetail;
    }

    public Boolean isShowDetails() {
        return showDetails;
    }

    public void setShowDetails(Boolean showDetails) {
        this.showDetails = showDetails;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProviderDirectoryType() {
        return providerDirectoryType;
    }

    public void setProviderDirectoryType(String providerDirectoryType) {
        this.providerDirectoryType = providerDirectoryType;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public String getTextDN() {
        return textDN;
    }

    public void setTextDN(String textDN) {
        this.textDN = textDN;
    }

    /**
     *
     * @param reqData
     * @return byte
     */
    private byte[] convertToBytes(FederatedRequestData reqData) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream mout;
        ObjectOutputStream out;
        try {
            mout = MimeUtility.encode(bos, "base64");
            out = new ObjectOutputStream(mout);
            out.writeObject(reqData);
            out.flush();
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
        byte[] bytes = bos.toByteArray();
        return bytes;
    }

}
