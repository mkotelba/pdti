/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.hhs.onc.pdti.server.xml;

import java.io.Serializable;

/**
 *
 * @author Wilmertech
 */
public class FederatedSearchResponseData implements Serializable {
    protected FederatedResponseStatus federatedResponseStatus;

    public FederatedResponseStatus getFederatedResponseStatus() {
        return federatedResponseStatus;
    }

    public void setFederatedResponseStatus(FederatedResponseStatus federatedResponseStatus) {
        this.federatedResponseStatus = federatedResponseStatus;
    }
    
}
