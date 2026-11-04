package com.labresource.platform.common.config;

import com.labresource.platform.common.security.JwtService;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Locale;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketConfig(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(
                    Message<?> message,
                    MessageChannel channel
            ) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(
                                message,
                                StompHeaderAccessor.class
                        );

                if (accessor == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();

                if (StompCommand.CONNECT.equals(command)) {

                    String authorization = accessor.getFirstNativeHeader(
                            "Authorization"
                    );

                    if (authorization == null
                            || !authorization.startsWith("Bearer ")) {
                        throw new IllegalArgumentException(
                                "WebSocket JWT token is required"
                        );
                    }

                    String token = authorization.substring(7).trim();

                    String email = jwtService.extractEmail(token);

                    User user = userRepository.findByEmailWithRole(email)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "WebSocket user not found"
                                    )
                            );

                    if (!Boolean.TRUE.equals(user.getIsActive())) {
                        throw new IllegalArgumentException(
                                "WebSocket user is inactive"
                        );
                    }

                    if (user.getRole() == null
                            || user.getRole().getName() == null) {
                        throw new IllegalArgumentException(
                                "WebSocket user role is missing"
                        );
                    }

                    String roleName = user.getRole().getName()
                            .trim()
                            .toUpperCase(Locale.ROOT);

                    if (roleName.startsWith("ROLE_")) {
                        roleName = roleName.substring(5);
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + roleName
                                            )
                                    )
                            );

                    accessor.setUser(authentication);

                } else if (
                        (StompCommand.SEND.equals(command)
                                || StompCommand.SUBSCRIBE.equals(command))
                                && accessor.getUser() == null
                ) {
                    throw new IllegalArgumentException(
                            "Unauthenticated WebSocket connection"
                    );
                }

                return message;
            }
        });
    }
}