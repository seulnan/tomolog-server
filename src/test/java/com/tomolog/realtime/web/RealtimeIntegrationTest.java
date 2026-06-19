package com.tomolog.realtime.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomolog.support.SharedContainers;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * SPEC §8 WebSocket integration test: a real STOMP client connects (JWT in the CONNECT frame),
 * subscribes to a room topic, a REST join is triggered by another user, and the client must receive
 * the MEMBER_JOINED broadcast. The subscription is settled before the REST call to avoid the
 * subscribe-before-broadcast race.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RealtimeIntegrationTest {

  @LocalServerPort private int port;
  @Autowired private TestRestTemplate rest;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    SharedContainers.register(registry);
  }

  private String token(String oauthId) {
    Map<?, ?> body =
        rest.postForObject(url("/api/auth/dev-login"), Map.of("oauthId", oauthId), Map.class);
    return (String) body.get("accessToken");
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private long createRoom(String hostToken) {
    HttpHeaders headers = bearer(hostToken);
    Map<?, ?> body =
        rest.exchange(
                url("/api/rooms"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "실시간방", "capacity", 4), headers),
                Map.class)
            .getBody();
    return ((Number) body.get("id")).longValue();
  }

  private HttpHeaders bearer(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private BlockingQueue<Map> connectAndSubscribe(String token, long roomId) throws Exception {
    WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    client.setMessageConverter(new MappingJackson2MessageConverter());
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + token);
    StompSession session =
        client
            .connectAsync(
                url("/ws").replace("http", "ws"),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    BlockingQueue<Map> received = new LinkedBlockingQueue<>();
    session.subscribe(
        "/topic/rooms/" + roomId,
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return Map.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            received.add((Map) payload);
          }
        });
    return received;
  }

  @Test
  void memberJoined_isBroadcastToSubscribedClient() throws Exception {
    String hostToken = token("rt-host");
    String joinerToken = token("rt-joiner");
    long roomId = createRoom(hostToken);

    BlockingQueue<Map> received = connectAndSubscribe(hostToken, roomId);
    Thread.sleep(700); // let the SUBSCRIBE settle before triggering the broadcast

    rest.exchange(
        url("/api/rooms/" + roomId + "/join"),
        HttpMethod.POST,
        new HttpEntity<>(bearer(joinerToken)),
        Void.class);

    Map<?, ?> event = received.poll(10, TimeUnit.SECONDS);
    assertThat(event).as("MEMBER_JOINED frame received").isNotNull();
    assertThat(event.get("type")).isEqualTo("MEMBER_JOINED");
    assertThat(event.get("roomId")).isEqualTo((int) roomId);
  }
}
