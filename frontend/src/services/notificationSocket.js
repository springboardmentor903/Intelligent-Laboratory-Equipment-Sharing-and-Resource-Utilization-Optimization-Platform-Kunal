
import { Client } from '@stomp/stompjs';

let notificationClient = null;

function getWebSocketUrl() {
    const apiBase =
        import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

    const root = apiBase.replace(/\/api\/?$/, '').replace(/\/$/, '');

    if (root.startsWith('https://')) {
        return `${root.replace('https://', 'wss://')}/ws`;
    }

    if (root.startsWith('http://')) {
        return `${root.replace('http://', 'ws://')}/ws`;
    }

    return `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;
}

export function connectNotificationSocket({
                                              token,
                                              userId,
                                              onUnreadCount,
                                              onConnected,
                                              onError,
                                          }) {
    disconnectNotificationSocket();

    if (!token || !userId) {
        throw new Error('Login token and user ID are required.');
    }

    notificationClient = new Client({
        brokerURL: getWebSocketUrl(),
        connectHeaders: {
            Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        onConnect: () => {
            notificationClient.subscribe(
                `/topic/notifications/${userId}`,
                (message) => {
                    try {
                        const payload = JSON.parse(message.body);

                        if (payload.type === 'UNREAD_COUNT_UPDATED') {
                            onUnreadCount?.(
                                Number(payload.unreadCount ?? payload.count ?? 0)
                            );
                        }
                    } catch (error) {
                        console.error('Invalid notification WebSocket message:', error);
                    }
                }
            );

            onConnected?.();
        },

        onStompError: (frame) => {
            onError?.(frame.headers?.message || 'Notification WebSocket error.');
        },

        onWebSocketError: (error) => {
            onError?.(error);
        },
    });

    notificationClient.activate();
    return notificationClient;
}

export function disconnectNotificationSocket() {
    if (notificationClient) {
        notificationClient.deactivate();
        notificationClient = null;
    }
}
