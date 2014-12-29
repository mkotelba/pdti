package gov.hhs.onc.pdti.service;

import gov.hhs.onc.pdti.data.DirectoryDescriptor;
import gov.hhs.onc.pdti.service.exception.DirectoryFederationException;

import java.util.List;

public interface FederationService<T, U> {
    public List<U> federate(T queryReq) throws DirectoryFederationException;

    public U federate(DirectoryDescriptor fedDir, T queryReq) throws DirectoryFederationException;
}
