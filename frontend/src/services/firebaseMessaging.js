
import { getApp, getApps, initializeApp } from 'firebase/app';
import {
    deleteToken,
    getMessaging,
    getToken,
    isSupported,
    onMessage,
} from 'firebase/messaging';

const firebaseConfig = {
    apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
    authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
    projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
    storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
    messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
    appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

const firebaseApp = getApps().length
    ? getApp()
    : initializeApp(firebaseConfig);

let messagingPromise;

async function getMessagingInstance() {
    const supported = await isSupported();

    if (!supported) {
        throw new Error(
            'Firebase messaging is not supported in this browser.'
        );
    }

    if (!messagingPromise) {
        messagingPromise = Promise.resolve(getMessaging(firebaseApp));
    }

    return messagingPromise;
}

function getApiRoot(apiBaseUrl) {
    const configured =
        apiBaseUrl ||
        import.meta.env.VITE_API_BASE_URL ||
        'http://localhost:8080/api';

    return configured.replace(/\/api\/?$/, '').replace(/\/$/, '');
}

async function registerDevice(token, apiBaseUrl, authToken) {
    const response = await fetch(
        `${getApiRoot(apiBaseUrl)}/api/notifications/devices`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${authToken}`,
            },
            body: JSON.stringify({
                deviceToken: token,
                platform: 'WEB',
                deviceName: navigator.userAgent,
            }),
        }
    );

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Unable to register push device.');
    }
}

export async function enableWebPush({
                                        authToken,
                                        apiBaseUrl,
                                        vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY,
                                    }) {
    if (!authToken) {
        throw new Error('Please login before enabling push notifications.');
    }

    if (!('Notification' in window)) {
        throw new Error('Browser notifications are not supported.');
    }

    if (!vapidKey) {
        throw new Error('Firebase VAPID key is missing in frontend .env.');
    }

    const permission = await Notification.requestPermission();

    if (permission !== 'granted') {
        throw new Error('Notification permission was not granted.');
    }

    if (!('serviceWorker' in navigator)) {
        throw new Error('Service workers are not supported in this browser.');
    }

    const registration = await navigator.serviceWorker.register(
        '/firebase-messaging-sw.js'
    );

    await navigator.serviceWorker.ready;

    const messaging = await getMessagingInstance();

    const token = await getToken(messaging, {
        vapidKey,
        serviceWorkerRegistration: registration,
    });

    if (!token) {
        throw new Error('Firebase registration token was not generated.');
    }

    await registerDevice(token, apiBaseUrl, authToken);

    return token;
}

export async function listenForForegroundMessages(callback) {
    try {
        const messaging = await getMessagingInstance();

        return onMessage(messaging, (payload) => {
            callback?.(payload);
        });
    } catch (error) {
        console.warn(
            'Foreground push listener unavailable:',
            error.message
        );

        return () => {};
    }
}

export async function disableWebPush({ authToken, apiBaseUrl }) {
    try {
        const messaging = await getMessagingInstance();

        const registration = await navigator.serviceWorker.getRegistration(
            '/firebase-messaging-sw.js'
        );

        let token = null;

        if (registration) {
            token = await getToken(messaging, {
                vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
                serviceWorkerRegistration: registration,
            });
        }

        if (token && authToken) {
            try {
                const response = await fetch(
                    `${getApiRoot(apiBaseUrl)}/api/notifications/devices?token=${encodeURIComponent(token)}`,
                    {
                        method: 'DELETE',
                        headers: {
                            Authorization: `Bearer ${authToken}`,
                        },
                    }
                );

                // 404 means the device is already absent for this user.
                if (!response.ok && response.status !== 404) {
                    console.warn(
                        'Unable to remove push device from backend:',
                        await response.text()
                    );
                }
            } catch (error) {
                console.warn(
                    'Backend push-device cleanup failed:',
                    error.message
                );
            }
        }

        await deleteToken(messaging);
    } catch (error) {
        console.warn('Firebase push cleanup warning:', error.message);
    }
}
