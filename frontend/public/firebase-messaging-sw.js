importScripts(
    "https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js"
);

importScripts(
    "https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js"
);

firebase.initializeApp({
    apiKey: "AIzaSyAjWhwUSiiz2bJ5zsTzxiYm-sgRR7mtFU",
    authDomain: "lab-resource-platform-9c224.firebaseapp.com",
    projectId: "lab-resource-platform-9c224",
    storageBucket: "lab-resource-platform-9c224.firebasestorage.app",
    messagingSenderId: "970195004231",
    appId: "1:970195004231:web:27e70290bd756a7d1b7a62"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(payload => {
    const title =
        payload.notification?.title || "Lab Resource Notification";

    self.registration.showNotification(title, {
        body: payload.notification?.body || "You have a new notification",
        icon: "/favicon.ico",
        data: payload.data || {}
    });
});

self.addEventListener("notificationclick", event => {
    event.notification.close();

    event.waitUntil(
        clients.openWindow("/notifications")
    );
});