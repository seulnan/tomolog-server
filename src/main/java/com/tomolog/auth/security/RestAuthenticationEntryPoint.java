package com.tomolog.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomolog.common.error.ErrorCode;
import com.tomolog.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Writes the unified error envelope for unauthenticated requests (401). Security rejects these
 * before the controller, so the {@code @RestControllerAdvice} never sees them — this keeps the
 * response shape consistent (SPEC §5).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    ErrorResponse body =
        ErrorResponse.of(
            ErrorCode.UNAUTHORIZED,
            ErrorCode.UNAUTHORIZED.defaultMessage(),
            request.getRequestURI());
    response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), body);
  }
}
