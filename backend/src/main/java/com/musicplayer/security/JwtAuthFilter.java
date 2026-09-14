package com.musicplayer.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StreamTicketService streamTicketService;

    public JwtAuthFilter(JwtUtil jwtUtil, StreamTicketService streamTicketService) {
        this.jwtUtil = jwtUtil;
        this.streamTicketService = streamTicketService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            authenticateWithJwt(header.substring(7));
        } else {
            authenticateWithStreamTicket(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateWithJwt(String token) {
        if (!jwtUtil.validateToken(token)) return;
        Claims claims = jwtUtil.parseToken(token);
        setAuthentication(claims.getSubject(), claims.get("role", String.class));
    }

    /**
     * 浏览器的 {@code EventSource} 无法设置请求头，所以 SSE 接口需要一种不放进查询串的凭据。
     *
     * <p>这里只接受 {@link StreamTicketService} 签发的一次性票据，<b>不再接受 {@code ?token=<JWT>}</b>：
     * 把长期有效的 JWT 放进 URL 会让它进入服务端访问日志、浏览器历史和 Referer，且在被撤销前一直可用。
     * 票据是一次性、30 秒、且绑定到签发票据时指定的资源的。
     */
    private void authenticateWithStreamTicket(HttpServletRequest request) {
        streamTicketService
                .consume(request.getParameter("ticket"), lastPathSegment(request.getRequestURI()))
                .ifPresent(ticket -> setAuthentication(ticket.userId(), ticket.role()));
    }

    private void setAuthentication(String userId, String role) {
        if (userId == null || role == null) return;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    /** 取 URI 的最后一段作为资源标识（例如 {@code /api/ai/analysis/<songId>} 的 songId）。 */
    private static String lastPathSegment(String uri) {
        if (uri == null) return null;
        int index = uri.lastIndexOf('/');
        return index < 0 ? uri : uri.substring(index + 1);
    }
}
