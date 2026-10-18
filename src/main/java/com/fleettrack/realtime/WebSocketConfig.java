package com.fleettrack.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket at /ws. Dispatcher clients subscribe to:
 * <ul>
 *   <li>/topic/couriers - every courier position (live map)</li>
 *   <li>/topic/couriers/{id} - one courier</li>
 *   <li>/topic/deliveries/{id}/eta - ETA updates for one delivery</li>
 * </ul>
 * The in-memory simple broker only knows about clients connected to THIS instance;
 * {@link LiveUpdateSubscriber} feeds it from Redis so every instance gets every update.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Lock this down to your dispatcher UI's origin in production.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
