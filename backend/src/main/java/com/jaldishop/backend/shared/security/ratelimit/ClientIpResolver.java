package com.jaldishop.backend.shared.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import org.springframework.core.env.Environment;

/** Only for the public-only Render Free deployment; re-evaluate before enabling private ingress. */
final class ClientIpResolver {
    private final boolean render;

    ClientIpResolver(Environment environment) {
        // Deployment metadata, never the HTTP Host header or a request-supplied flag.
        render = "true".equals(environment.getProperty("RENDER"))
                && "jaldishop-api.onrender.com".equals(environment.getProperty("RENDER_EXTERNAL_HOSTNAME"));
    }

    static final class Unavailable extends RuntimeException {}

    String resolve(HttpServletRequest request) {
        if (!render) return request.getRemoteAddr();
        // Render public ingress traverses Cloudflare, which generates CF-Connecting-IP.
        // XFF can contain attacker-controlled prefixes and the proxy count is not guaranteed.
        var values = Collections.list(request.getHeaders("CF-Connecting-IP"));
        if (values.size() == 1) {
            String literal = values.getFirst().trim();
            boolean ipv4 = literal.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
            boolean ipv6 = literal.contains(":") && literal.matches("[0-9a-fA-F:.]{2,45}");
            if (ipv4 || ipv6) {
                try {
                    if (ipv4) for (String part : literal.split("\\.")) {
                        if (Integer.parseInt(part) > 255 || (part.length() > 1 && part.startsWith("0")))
                            throw new IllegalArgumentException();
                    }
                    return InetAddress.getByName(literal).getHostAddress(); // Numeric literals only: no DNS.
                } catch (UnknownHostException | IllegalArgumentException invalid) {
                    // Fail closed below; never fall back to a shared proxy counter on Render.
                }
            }
        }
        throw new Unavailable();
    }
}
