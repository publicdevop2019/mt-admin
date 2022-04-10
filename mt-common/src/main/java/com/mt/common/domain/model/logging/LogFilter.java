package com.mt.common.domain.model.logging;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.GenericFilter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


//set UUID & CLIENT_IP for logging purpose,
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogFilter extends GenericFilter {
    private static final String UUID = "UUID";
    private static final String CLIENT_IP = "CLIENT_IP";
    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String uuid = httpRequest.getHeader(UUID);
        if (null == uuid) {
            MDC.put(UUID, java.util.UUID.randomUUID().toString());
        } else {
            MDC.put(UUID, uuid);
        }
        String ipFromHeader = httpRequest.getHeader(X_FORWARDED_FOR);
        if (ipFromHeader != null && ipFromHeader.length() > 0) {
            MDC.put(CLIENT_IP, ipFromHeader);
        } else {
            MDC.put(CLIENT_IP, servletRequest.getRemoteAddr());
        }
        filterChain.doFilter(servletRequest, servletResponse);
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        httpResponse.setHeader(UUID, MDC.get(UUID));
        MDC.clear();
    }
}
