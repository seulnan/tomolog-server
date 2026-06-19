package com.tomolog.infrastructure.security;

import com.tomolog.infrastructure.jwt.JwtProvider;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Authenticates the STOMP {@code CONNECT} frame by reading the JWT from its native {@code
 * Authorization} header and binding the user id as the session principal, so later SEND/SUBSCRIBE
 * frames know who is acting (SPEC §6, §7).
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;

  public StompAuthChannelInterceptor(JwtProvider jwtProvider) {
    this.jwtProvider = jwtProvider;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }
    String token = resolveToken(accessor.getFirstNativeHeader("Authorization"));
    if (token == null || !jwtProvider.isValid(token)) {
      throw new MessageDeliveryException("STOMP CONNECT 인증 실패");
    }
    Long userId = jwtProvider.getUserId(token);
    accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    return message;
  }

  private String resolveToken(String header) {
    return header != null && header.startsWith(BEARER_PREFIX)
        ? header.substring(BEARER_PREFIX.length())
        : null;
  }
}
