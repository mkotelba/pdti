package gov.hhs.onc.pdti.ws.impl;

import gov.hhs.onc.pdti.DirectoryStandard;
import gov.hhs.onc.pdti.DirectoryStandardId;
import gov.hhs.onc.pdti.data.federation.impl.SoapHeaderProperties;
import gov.hhs.onc.pdti.service.DirectoryService;
import gov.hhs.onc.pdti.ws.api.BatchRequest;
import gov.hhs.onc.pdti.ws.api.BatchResponse;
import gov.hhs.onc.pdti.ws.api.ProviderInformationDirectoryPortType;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.AddressingFeature.Responses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sun.xml.ws.developer.JAXWSProperties;

@DirectoryStandard(DirectoryStandardId.IHE)
@Scope("singleton")
@Service("providerInfoDir")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(wsdlLocation = "META-INF/wsdl/HPD_ProviderInformationDirectory.wsdl", serviceName = "ProviderInformationDirectory_Service", targetNamespace = "urn:ihe:iti:hpd:2010", portName = "ProviderInformationDirectory_Port_Soap")
@Addressing(enabled=true, required=true, responses = Responses.ANONYMOUS)
public class ProviderInformationDirectoryImpl extends AbstractProviderInformationDirectory<BatchRequest, BatchResponse> implements
        ProviderInformationDirectoryPortType {

	@Resource
	WebServiceContext context;
	@Resource
	ApplicationContext appContext;
    @Override
    @WebMethod(operationName = "ProviderInformationQueryRequest", action = "urn:ihe:iti:2010:ProviderInformationQuery")
    @WebResult(name = "batchResponse", targetNamespace = "urn:oasis:names:tc:DSML:2:0:core")
    public BatchResponse providerInformationQueryRequest(
            @WebParam(name = "batchRequest") BatchRequest queryRequest) {
    	String addressingAction = (String) context.getMessageContext().get(JAXWSProperties.ADDRESSING_ACTION);
		String addressingTo = (String) context.getMessageContext().get(JAXWSProperties.ADDRESSING_TO);
		String addressingFrom = (String) context.getMessageContext().get(JAXWSProperties.ADDRESSING_FROM);
		String addressingMessageId = (String) context.getMessageContext().get(JAXWSProperties.ADDRESSING_MESSAGEID);	
		
		((SoapHeaderProperties)appContext.getBean("headerProperties")).setAction(addressingAction);
		((SoapHeaderProperties)appContext.getBean("headerProperties")).setTo(addressingTo);
		((SoapHeaderProperties)appContext.getBean("headerProperties")).setReplyTo(addressingFrom);
		((SoapHeaderProperties)appContext.getBean("headerProperties")).setMessageId(addressingMessageId);
        return this.dirService.processRequest(queryRequest);
    }

    @Autowired
    @DirectoryStandard(DirectoryStandardId.IHE)
    @Override
    protected void setDirectoryService(DirectoryService<BatchRequest, BatchResponse> dirService) {
        this.dirService = dirService;
    }
}
