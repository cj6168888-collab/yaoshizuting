package com.yaoshizuting.security;

import com.yaoshizuting.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtils);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterWithoutAuthorizationHeaderLeavesRequestAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute("userId"));
        verify(jwtUtils, never()).validateToken("token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterWithNonBearerAuthorizationHeaderLeavesRequestAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic credentials");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute("userId"));
        verify(jwtUtils, never()).validateToken("credentials");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterWithInvalidBearerTokenDoesNotPopulateAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute("userId"));
        verify(jwtUtils, never()).getUserIdFromToken("invalid-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterWithSuperAdminTokenSetsAllAuthoritiesAndRequestAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer super-token");
        when(jwtUtils.validateToken("super-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("super-token")).thenReturn(10L);
        when(jwtUtils.getMobileFromToken("super-token")).thenReturn("13800138010");
        when(jwtUtils.getRoleFromToken("super-token")).thenReturn(10);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("10", authentication.getPrincipal());
        assertEquals(List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_FINANCE", "ROLE_USER"),
                authentication.getAuthorities().stream().map(Object::toString).toList());
        assertEquals(10L, request.getAttribute("userId"));
        assertEquals("13800138010", request.getAttribute("mobile"));
        assertEquals(10, request.getAttribute("role"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterWithUnknownRoleFallsBackToUserAuthority() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer custom-token");
        when(jwtUtils.validateToken("custom-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("custom-token")).thenReturn(99L);
        when(jwtUtils.getMobileFromToken("custom-token")).thenReturn("13800138099");
        when(jwtUtils.getRoleFromToken("custom-token")).thenReturn(99);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("99", authentication.getPrincipal());
        assertEquals(List.of("ROLE_USER"), authentication.getAuthorities().stream().map(Object::toString).toList());
        assertEquals(99L, request.getAttribute("userId"));
        assertEquals("13800138099", request.getAttribute("mobile"));
        assertEquals(99, request.getAttribute("role"));
        verify(filterChain).doFilter(request, response);
    }
}
