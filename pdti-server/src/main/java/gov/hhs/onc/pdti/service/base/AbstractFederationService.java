package gov.hhs.onc.pdti.service.base;


import gov.hhs.onc.pdti.DirectoryType;
import gov.hhs.onc.pdti.DirectoryTypeId;
import gov.hhs.onc.pdti.data.DirectoryDescriptor;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryInterceptorException;
import gov.hhs.onc.pdti.interceptor.DirectoryRequestInterceptor;
import gov.hhs.onc.pdti.interceptor.DirectoryResponseInterceptor;
import gov.hhs.onc.pdti.service.FederationService;
import gov.hhs.onc.pdti.service.exception.DirectoryFederationException;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;

public abstract class AbstractFederationService<T, U> extends AbstractBaseService<T, U> implements FederationService<T, U> {

	protected List<DirectoryDescriptor> fedDirs;

	protected void interceptRequests(DirectoryDescriptor fedDir, String fedDirId, String reqId, T fedQueryReq, U fedQueryResp)
			throws DirectoryInterceptorException {
		if (this.reqInterceptors != null) {
			for (DirectoryRequestInterceptor<T, U> fedReqInterceptor : this.reqInterceptors) {
				if (this.isFederatedInterceptor(fedReqInterceptor)) {
					try {
						fedReqInterceptor.interceptRequest(fedDir, reqId, fedQueryReq, fedQueryResp);

						logRequestInterception(fedDirId, reqId, fedQueryReq,
								fedReqInterceptor);
					} catch (DirectoryInterceptorException e) {
						throw e;
					} catch (Throwable th) {
						throw new DirectoryInterceptorException("Unable to intercept (class=" + fedReqInterceptor.getClass().getName()
								+ ") federated request (directoryId=" + fedDirId + ", requestId=" + reqId + ", responseClass=" + fedQueryResp.getClass().getName()
								+ ").", th);
					}
				}
			}
		}
	}

	protected void interceptResponses(DirectoryDescriptor fedDir, String fedDirId, String reqId, T fedQueryReq, U fedQueryResp)
			throws DirectoryInterceptorException {
		if (this.reqInterceptors != null) {
			for (DirectoryResponseInterceptor<T, U> fedRespInterceptor : this.respInterceptors) {
				if (!this.isFederatedInterceptor(fedRespInterceptor)) {
					try {
						fedRespInterceptor.interceptResponse(fedDir, reqId, fedQueryReq, fedQueryResp);

						logResponseInterception(fedDirId, reqId, fedQueryResp,
								fedRespInterceptor);
					} catch (DirectoryInterceptorException e) {
						throw e;
					} catch (Throwable th) {
						throw new DirectoryInterceptorException("Intercepted (class=" + fedRespInterceptor.getClass().getName()
								+ ") federated response (directoryId=" + fedDirId + ", requestId=" + reqId + ", responseClass=" + fedQueryResp.getClass().getName()
								+ ").", th);
					}
				}
			}
		}
	}

	@Override
	public List<U> federate(T queryReq) throws DirectoryFederationException {
		List<U> queryResps = new ArrayList<>();

		if (this.fedDirs != null) {
			for (DirectoryDescriptor fedDir : this.fedDirs) {
				if (fedDir.isEnabled()) {
					queryResps.add(this.federate(fedDir, queryReq));
				}
			}
		}
		return queryResps;
	}

	protected boolean isFederatedInterceptor(DirectoryInterceptor<T, U> dirInterceptor) {
		DirectoryType dirInterceptorType;
		return ((dirInterceptorType = AnnotationUtils.findAnnotation(dirInterceptor.getClass(), DirectoryType.class)) == null)
				|| (dirInterceptorType.value() == DirectoryTypeId.FEDERATED);
	}

	protected abstract void setFederatedDirs(List<DirectoryDescriptor> federatedDirs);
}
