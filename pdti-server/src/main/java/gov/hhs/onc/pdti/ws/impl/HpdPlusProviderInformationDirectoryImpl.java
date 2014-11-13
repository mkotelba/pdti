package gov.hhs.onc.pdti.ws.impl;

import gov.hhs.onc.pdti.DirectoryStandard;
import gov.hhs.onc.pdti.DirectoryStandardId;
import gov.hhs.onc.pdti.service.DirectoryService;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusProviderInformationDirectoryPortType;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusRequest;
import gov.hhs.onc.pdti.ws.api.hpdplus.HpdPlusResponse;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.soap.Addressing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
@Scope("singleton")
@Service("hpdPlusProviderInfoDir")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(portName = "Hpd_Plus_ProviderInformationDirectory_Port_Soap", serviceName = "Hpd_Plus_ProviderInformationDirectory_Service", targetNamespace = "urn:gov:hhs:onc:hpdplus:2013")
@Addressing
public class HpdPlusProviderInformationDirectoryImpl extends AbstractProviderInformationDirectory<HpdPlusRequest, HpdPlusResponse> implements
        HpdPlusProviderInformationDirectoryPortType {
    @Override
    @WebMethod(operationName = "Hpd_Plus_ProviderInformationQueryRequest", action = "urn:gov:hhs:onc:hpdplus:2013:Hpd_Plus_ProviderInformationQueryRequest")
    @WebResult(name = "hpdPlusResponse", targetNamespace = "urn:gov:hhs:onc:hpdplus:2013", partName = "queryResponse")
    public HpdPlusResponse hpdPlusProviderInformationQueryRequest(
            @WebParam(name = "hpdPlusRequest", targetNamespace = "urn:gov:hhs:onc:hpdplus:2013", partName = "queryRequest") HpdPlusRequest queryRequest) {

        return this.dirService.processRequest(queryRequest);
    }

    @Autowired
    @DirectoryStandard(DirectoryStandardId.HPD_PLUS_PROPOSED)
    @Override
    protected void setDirectoryService(DirectoryService<HpdPlusRequest, HpdPlusResponse> dirService) {
        this.dirService = dirService;
    }
}
