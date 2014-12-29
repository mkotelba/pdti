package gov.hhs.onc.pdti.service.base;


import gov.hhs.onc.pdti.DirectoryType;
import gov.hhs.onc.pdti.DirectoryTypeId;
import gov.hhs.onc.pdti.data.DirectoryDataService;
import gov.hhs.onc.pdti.data.DirectoryDescriptor;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptorException;
import gov.hhs.onc.pdti.interceptor.DirectoryRequestInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryResponseInterceptor;
import gov.hhs.onc.pdti.service.DirectoryService;
import gov.hhs.onc.pdti.service.FederationService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;

public abstract class AbstractDirectoryService<T, U> extends AbstractFederatedBaseService<T, U> implements DirectoryService<T, U> {

	@Autowired(required = false)
	protected List<DirectoryDataService<?>> dataServices;
	protected DirectoryDescriptor dirDesc;
	protected FederationService<T, U> fedService;

	protected void interceptRequests(DirectoryDescriptor dirDesc, String dirId, String reqId, T queryReq, U queryResp) throws DirectoryInterceptorException {
		if (this.reqInterceptors != null) {
			for (DirectoryRequestInterceptor<T, U> reqInterceptor : this.reqInterceptors) {
				if (this.isServiceInterceptor(reqInterceptor)) {
					try {
						reqInterceptor.interceptRequest(dirDesc, reqId, queryReq, queryResp);

						logRequestInterception(dirId, reqId, queryReq, reqInterceptor);
					} catch (DirectoryInterceptorException e) {
						throw e;
					} catch (Throwable th) {
						throw new DirectoryInterceptorException("Unable to intercept (class=" + reqInterceptor.getClass().getName() + ") request (directoryId="
								+ dirId + ", requestId=" + reqId + ", requestClass=" + queryReq.getClass().getName() + ").", th);
					}
				}
			}
		}
	}

	protected void interceptResponses(DirectoryDescriptor dirDesc, String dirId, String reqId, T queryReq, U queryResp) throws DirectoryInterceptorException {
		if (this.respInterceptors != null) {
			for (DirectoryResponseInterceptor<T, U> respInterceptor : this.respInterceptors) {
				if (this.isServiceInterceptor(respInterceptor)) {
					try {
						respInterceptor.interceptResponse(dirDesc, reqId, queryReq, queryResp);

						logResponseInterception(dirId, reqId, queryResp, respInterceptor);
					} catch (DirectoryInterceptorException e) {
						throw e;
					} catch (Throwable th) {
						throw new DirectoryInterceptorException("Unable to intercept (class=" + respInterceptor.getClass().getName() + ") response (directoryId="
								+ dirId + ", requestId=" + reqId + ", responseClass=" + queryResp.getClass().getName() + ").", th);
					}
				}
			}
		}
	}

	protected boolean isServiceInterceptor(DirectoryInterceptor<T, U> dirInterceptor) {
		DirectoryType dirInterceptorType;
		return ((dirInterceptorType = AnnotationUtils.findAnnotation(dirInterceptor.getClass(), DirectoryType.class)) == null)
				|| (dirInterceptorType.value() == DirectoryTypeId.MAIN);
	}

	protected abstract void setDirectoryDescriptor(DirectoryDescriptor dirDesc);
	protected abstract void setFederationService(FederationService<T, U> fedService);
}
