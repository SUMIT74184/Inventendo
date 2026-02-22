package org.example.tenantmvc.Security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-Code";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantCode = httpRequest.getHeader(TENANT_HEADER);

        if (tenantCode != null && !tenantCode.isEmpty()) {
            TenantContext.setCurrentTenant(tenantCode);
            log.debug("Tenant context set for: {}", tenantCode);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }


}