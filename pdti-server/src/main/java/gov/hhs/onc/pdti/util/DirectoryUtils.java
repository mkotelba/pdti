package gov.hhs.onc.pdti.util;

import gov.hhs.onc.pdti.ws.api.BatchRequest;
import gov.hhs.onc.pdti.ws.api.BatchResponse;
import gov.hhs.onc.pdti.ws.api.DsmlMessage;
import gov.hhs.onc.pdti.ws.api.ErrorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;

public abstract class DirectoryUtils {
    private final static String REQ_ID_DEFAULT_PREFIX = "pdti_";

    private final static QName XML_SCHEMA_STR_QUAL_NAME = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string", "xsd");

    public static BatchRequest setRequestId(BatchRequest batchReq, String reqId) {
        batchReq.setRequestId(reqId);

        for (DsmlMessage batchReqMsg : batchReq.getBatchRequests()) {
            batchReqMsg.setRequestId(reqId);
        }

        return batchReq;
    }

    public static BatchResponse setRequestId(BatchResponse batchResp, String reqId) {

        batchResp.setRequestId(reqId);

        for (JAXBElement<?> batchRespItem : batchResp.getBatchResponses()) {
            if (DsmlMessage.class.isAssignableFrom(batchRespItem.getDeclaredType())) {
                ((DsmlMessage) batchRespItem.getValue()).setRequestId(reqId);
            } else if (ErrorResponse.class.isAssignableFrom(batchRespItem.getDeclaredType())) {
                ((ErrorResponse) batchRespItem.getValue()).setRequestId(reqId);
            } 
        }

        return batchResp;
    }

    public static String defaultRequestId(String reqId) {
        return !StringUtils.isBlank(reqId) ? reqId : REQ_ID_DEFAULT_PREFIX + UUID.randomUUID();
    }

    public static Dn replaceAncestorDn(Dn dn, Dn newAncestorDn) throws LdapInvalidDnException {
        List<Rdn> rdns = new ArrayList<>(dn.getRdns().subList(0, dn.size() - newAncestorDn.size()));
        rdns.addAll(newAncestorDn.getRdns());

        return new Dn(rdns.toArray(new Rdn[rdns.size()]));
    }

    public static JAXBElement<String> getStackTraceJaxbElement(Throwable th) {
        return new JAXBElement<>(XML_SCHEMA_STR_QUAL_NAME, String.class, DirectoryUtils.class, ExceptionUtils.getStackTrace(th));
    }
}
