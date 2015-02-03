package gov.hhs.onc.pdti.data.federation.impl;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.springframework.beans.factory.annotation.Autowired;

public class FederatedRequestSoapHeaderHandler implements
		SOAPHandler<SOAPMessageContext> {
// create the following soap header in the code below
//	  <soap-env:Header xmlns:wsa="http://www.w3.org/2005/08/addressing">
//	   <wsa:Action soap-env:mustUnderstand="1">urn:ihe:iti:2010:ProviderInformationQuery</wsa:Action>
//	   <wsa:ReplyTo soap-env:mustUnderstand="1">
//	   	<wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>
//	   </wsa:ReplyTo>
//	   <wsa:MessageID soap-env:mustUnderstand="1">uuid:71ebea54-c462-476b-b9e2-0913426aa288</wsa:MessageID>
//	   <wsa:To soap-env:mustUnderstand="1">http://localhost:8080/pdti-server/ProviderInformationDirectoryService?wsdl</wsa:To>
//	</soap-env:Header>
	@Autowired 
	private SoapHeaderProperties soapHeaderValuesPlaceHolder;
	
	private String action;
	private String to;
	private String replyTo;
	private String messageId;
	
	public FederatedRequestSoapHeaderHandler(String action, String to,
			String replyTo, String messageId) {
		super();
		this.action = action;
		this.to = to;
		this.replyTo = replyTo;
		this.messageId = messageId;
	}

	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		Boolean outboundProperty = 
	            (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
	        if (outboundProperty.booleanValue()) {
	            try {
	                SOAPEnvelope envelope = context.getMessage().getSOAPPart().getEnvelope();
	                SOAPHeader header = envelope.addHeader();
	                header.addNamespaceDeclaration("wsa", "http://www.w3.org/2005/08/addressing");
	                addActionHeader(header);
	                addMessageIdHeader(header);
	                addToHeader(header);
	            } catch (Exception e) {
	                System.out.println("Exception in handler: " + e);
	            }
	        } else {
	            // inbound
	        }
	        return true;
	}

	private void addMessageIdHeader(SOAPHeader header) throws SOAPException {
		SOAPHeaderElement messageId = header.addHeaderElement(new QName("http://www.w3.org/2005/08/addressing", "MessageID", "wsa"));
		messageId.setMustUnderstand(true);
		messageId.addTextNode(this.messageId);
	}

	private void addActionHeader(SOAPHeader header) throws SOAPException {
		SOAPHeaderElement action = header.addHeaderElement(new QName("http://www.w3.org/2005/08/addressing", "Action", "wsa"));
		action.setMustUnderstand(true);
		//action.addTextNode("urn:ihe:iti:2010:ProviderInformationQuery");
		action.addTextNode(this.action);
	}
	
	private void addToHeader(SOAPHeader header) throws SOAPException {
		SOAPHeaderElement action = header.addHeaderElement(new QName("http://www.w3.org/2005/08/addressing", "To", "wsa"));
		action.setMustUnderstand(true);
		action.addTextNode(this.to);
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close(MessageContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<QName> getHeaders() {
		 Set<QName> set = new HashSet<QName>();
	        // Make sure the '[{http://www.w3.org/2005/08/addressing}]Action' header 
	        // is handled in case the device set the 'MustUnderstand' attribute to '1'
	        set.add(new QName("http://www.w3.org/2005/08/addressing", "Action"));
	        return set;
	}

}
