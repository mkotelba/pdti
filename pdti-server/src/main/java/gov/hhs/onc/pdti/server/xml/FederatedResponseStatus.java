/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.hhs.onc.pdti.server.xml;

import gov.hhs.onc.pdti.ws.api.LDAPResultCode;
import java.io.Serializable;

/**
 *
 * @author Wilmertech
 */
public class FederatedResponseStatus implements Serializable {
    
    protected String federatedRequestId;    
    protected String directoryId;    
    protected LDAPResultCode resultCode;    
    protected String resultMessage;

    public String getFederatedRequestId() {
        return federatedRequestId;
    }

    public void setFederatedRequestId(String federatedRequestId) {
        this.federatedRequestId = federatedRequestId;
    }

    public String getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(String directoryId) {
        this.directoryId = directoryId;
    }

    public LDAPResultCode getResultCode() {
        return resultCode;
    }

    public void setResultCode(LDAPResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
    
}
