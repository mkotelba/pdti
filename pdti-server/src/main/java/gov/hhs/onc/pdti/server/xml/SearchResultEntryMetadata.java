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
public class SearchResultEntryMetadata implements Serializable {
    
    protected String directoryId;    
    protected String directoryURI;

    public String getDirectoryId() {
        return directoryId;
    }

    public void setDirectoryId(String directoryId) {
        this.directoryId = directoryId;
    }

    public String getDirectoryURI() {
        return directoryURI;
    }

    public void setDirectoryURI(String directoryURI) {
        this.directoryURI = directoryURI;
    }
        
}
