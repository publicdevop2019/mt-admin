package com.mt.proxy.port.adapter.http;

import static com.mt.proxy.infrastructure.AppConstant.REQUEST_ID_HTTP;
import static com.mt.proxy.infrastructure.AppConstant.TRACE_ID_HTTP;
import static com.mt.proxy.infrastructure.AppConstant.TRACE_ID_LOG;

import com.mt.proxy.domain.UniqueIdGeneratorService;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate getRestTemplate(OutgoingReqInterceptor outgoingReqInterceptor) {
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Collections.singletonList(outgoingReqInterceptor));
        return template;
    }

    @Slf4j
    @Component
    public static class OutgoingReqInterceptor implements ClientHttpRequestInterceptor {
        @Autowired
        UniqueIdGeneratorService idGeneratorService;

        @Override
        public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
                                            ClientHttpRequestExecution clientHttpRequestExecution)
            throws IOException {
            String path =
                httpRequest.getURI().getPath() + (httpRequest.getURI().getRawQuery() != null ?
                    ("?" + httpRequest.getURI().getRawQuery()) : "");
            String currentTraceId = MDC.get(TRACE_ID_LOG);
            if (null != currentTraceId) {
                httpRequest.getHeaders().set(TRACE_ID_HTTP, currentTraceId);
            }
            String requestId = idGeneratorService.idString();
            log.debug("request id created {} path {}",
                requestId,
                path);
            httpRequest.getHeaders().set(REQUEST_ID_HTTP, requestId);
            return clientHttpRequestExecution.execute(httpRequest, bytes);
        }
    }
}
