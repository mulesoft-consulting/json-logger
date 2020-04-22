/**
 * (c) 2003-2018 MuleSoft, Inc. This software is protected under international copyright law. All use of this software is subject to
 * MuleSoft's Master Subscription Agreement (or other Terms of Service) separately entered into between you and MuleSoft. If such an
 * agreement is not in place, you may not use the software.
 */
package org.mule.extension.jsonlogger.internal.destinations.amq.client;

import com.mulesoft.mq.restclient.impl.OAuthCredentials;
import com.mulesoft.mq.restclient.internal.Request;
import com.mulesoft.mq.restclient.internal.RequestBuilder;
import com.mulesoft.mq.restclient.internal.Response;
import com.mulesoft.mq.restclient.internal.client.AbstractCourierRestClient;
import org.apache.commons.io.IOUtils;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.entity.multipart.HttpPart;
import org.mule.runtime.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class AsyncMuleCourierRestClient extends AbstractCourierRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncMuleCourierRestClient.class);
    private static final int RESPONSE_TIMEOUT_MILLIS = 60000;

    private final HttpClient httpClient;
    private final Scheduler scheduler;

    public AsyncMuleCourierRestClient(String courierApiUrl, OAuthCredentials oAuthCredentials,
                                      String userAgentInfo, HttpClient httpClient, Scheduler scheduler) {
        super(courierApiUrl, oAuthCredentials, userAgentInfo);
        this.httpClient = httpClient;
        this.scheduler = scheduler;
    }

    @Override
    protected Observable<Response> process(Request request) {
        logProcessStart(request);
        return Observable.create(subscriber -> httpClient.sendAsync(((MuleBasedRequest) request).getHttpRequest(),
                                                                    RESPONSE_TIMEOUT_MILLIS, true, null)
                .whenCompleteAsync((response, exception) -> {
                    if (exception != null) {
                        logProcessError(request, exception);
                        subscriber.onError(exception);
                    } else {
                        try {
                            Response mqResponse = convert(response);
                            logProcessSuccess(request, mqResponse);
                            subscriber.onNext(mqResponse);
                        } finally {
                            subscriber.onCompleted();
                        }
                    }
                }, command -> {
                    try {
                        scheduler.submit(command);
                    } catch (Exception e){
                        subscriber.onError(e);
                        LOGGER.debug("An error occurred while processing the request: " + e.getMessage());
                    }
                }));
    }

    protected static Response convert(HttpResponse httpResponse) {
        return new Response() {

            @Override
            public String getBody() {
                try (InputStream stream = httpResponse.getEntity().getContent()) {
                    return IOUtils.toString(stream, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Can not retrieve response body.", e);
                }
            }

            @Override
            public boolean isOk() {
                return getStatusCode() >= 200 && getStatusCode() < 300;
            }

            @Override
            public String getStatusText() {
                return httpResponse.getReasonPhrase();
            }

            @Override
            public int getStatusCode() {
                return httpResponse.getStatusCode();
            }

            @Override
            public String getHeader(String name) {
                return httpResponse.getHeaderValue(name);
            }

            @Override
            public boolean isUnauthorized() {
                return getStatusCode() == 401;
            }
        };
    }


    @Override
    protected RequestBuilder newRequestBuilder() {
        return new RequestBuilder() {

            private HttpRequestBuilder httpRequestBuilder = HttpRequest.builder();
            private MultiMap<String, String> queryParams = new MultiMap<>();
            private HttpEntity entity;

            @Override
            public RequestBuilder wrap(Request request) {
                MuleBasedRequest muleBasedRequest = (MuleBasedRequest) request;
                use(request.getMethod());
                to(request.getUrl());
                queryParams = muleBasedRequest.httpRequest.getQueryParams();
                entity = muleBasedRequest.httpRequest.getEntity();
                return this;
            }

            @Override
            public RequestBuilder use(Method method) {
                httpRequestBuilder.method(method.name());
                return this;
            }

            @Override
            public RequestBuilder to(String url) {
                httpRequestBuilder.uri(url);
                return this;
            }

            @Override
            public RequestBuilder withBody(String body) {
                this.entity = new ByteArrayHttpEntity(body.getBytes());
                return this;
            }

            @Override
            public RequestBuilder withHeader(String name, String value) {
                httpRequestBuilder.addHeader(name, value);
                return this;
            }

            @Override
            public RequestBuilder withFormParam(String name, String value) {
                if (!(entity instanceof MultipartHttpEntity)) {
                    this.entity = new MultipartHttpEntity(new ArrayList<>());
                }
                ((MultipartHttpEntity) this.entity).getParts().add(new HttpPart(name, value.getBytes(), null, 0));
                return this;
            }

            @Override
            public RequestBuilder withQueryParam(String name, String value) {
                queryParams.put(name, value);
                return this;
            }

            @Override
            public RequestBuilder waitingUpTo(long duration, TimeUnit unit) {
                // TODO: Manage timeouts
                return this;
            }

            @Override
            public Request build() {
                httpRequestBuilder.queryParams(queryParams);
                if (entity != null) {
                    httpRequestBuilder.entity(entity);
                }
                return new MuleBasedRequest(httpRequestBuilder.build());
            }
        };
    }


    class MuleBasedRequest implements Request {

        private HttpRequest httpRequest;

        public MuleBasedRequest(HttpRequest httpRequest) {
            this.httpRequest = httpRequest;
        }

        @Override
        public RequestBuilder.Method getMethod() {
            return RequestBuilder.Method.valueOf(httpRequest.getMethod());
        }

        @Override
        public String getUrl() {
            return httpRequest.getUri().toString();
        }

        public HttpRequest getHttpRequest() {
            return httpRequest;
        }

        @Override
        public String toString() {
            return httpRequest.toString();
        }
    }

}