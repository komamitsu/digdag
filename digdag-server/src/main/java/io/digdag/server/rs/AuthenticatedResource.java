package io.digdag.server.rs;

import io.digdag.client.config.Config;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import java.util.Map;

public abstract class AuthenticatedResource
{
    @Context
    protected HttpServletRequest request;

    protected int getSiteId()
    {
        // siteId is set by JwtAuthInterceptor
        // TODO validate before causing NPE. Improve guice-rs to call @PostConstruct
        return (int) request.getAttribute("siteId");
    }

    protected Config getUserInfo()
    {
        return (Config) request.getAttribute("userInfo");
    }

    /**
     * Get request context default secrets.
     */
    protected Map<String, String> getSecrets()
    {
        return (Map<String, String>) request.getAttribute("secrets");
    }
}
