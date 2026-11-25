
import { useEffect, useState } from 'react';
import { Bell, Mail, MessageSquare, Smartphone, Save } from 'lucide-react';
import api from '../../api/client';
import { enableWebPush, disableWebPush } from '../../services/firebaseMessaging';

const initialPreferences = {
    emailEnabled: true,
    smsEnabled: false,
    pushEnabled: false,
};

export default function NotificationPreferences() {
    const [preferences, setPreferences] = useState(initialPreferences);
    const [phone, setPhone] = useState('');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [pushBusy, setPushBusy] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const hasPhone = Boolean(phone && phone.trim().length >= 10);

    useEffect(() => {
        let active = true;

        const load = async () => {
            setLoading(true);
            setError('');

            try {
                const [preferenceResponse, profileResponse] = await Promise.allSettled([
                    api.get('/notifications/preferences'),
                    api.get('/users/me'),
                ]);

                if (!active) return;

                if (preferenceResponse.status === 'fulfilled') {
                    const data = preferenceResponse.value.data || {};
                    setPreferences({
                        emailEnabled: data.emailEnabled ?? true,
                        smsEnabled: data.smsEnabled ?? false,
                        pushEnabled: data.pushEnabled ?? false,
                    });
                } else {
                    setError('Could not load notification preferences.');
                }

                if (profileResponse.status === 'fulfilled') {
                    const data = profileResponse.value.data || {};
                    setPhone(data.phone || data.phoneNumber || data.mobileNumber || '');
                } else {
                    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
                    setPhone(storedUser.phone || storedUser.phoneNumber || '');
                }
            } catch {
                if (active) setError('Could not load notification settings.');
            } finally {
                if (active) setLoading(false);
            }
        };

        load();
        return () => {
            active = false;
        };
    }, []);

    const updatePreference = (key, value) => {
        setMessage('');
        setError('');
        setPreferences((current) => ({ ...current, [key]: value }));
    };

    const savePreferences = async () => {
        setSaving(true);
        setMessage('');
        setError('');

        try {
            const payload = {
                emailEnabled: preferences.emailEnabled,
                smsEnabled: hasPhone && preferences.smsEnabled,
                pushEnabled: preferences.pushEnabled,
            };

            const response = await api.put('/notifications/preferences', payload);
            const saved = response.data || payload;

            setPreferences({
                emailEnabled: saved.emailEnabled ?? payload.emailEnabled,
                smsEnabled: saved.smsEnabled ?? payload.smsEnabled,
                pushEnabled: saved.pushEnabled ?? payload.pushEnabled,
            });

            setMessage('Notification preferences saved successfully.');
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Unable to save preferences. Please try again.'
            );
        } finally {
            setSaving(false);
        }
    };

    const togglePush = async (enabled) => {
        setPushBusy(true);
        setMessage('');
        setError('');

        try {
            const authToken = localStorage.getItem('token');
            const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api')
                .replace(/\/api\/?$/, '');

            if (enabled) {
                await enableWebPush({
                    authToken,
                    apiBaseUrl,
                    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
                });
                setPreferences((current) => ({ ...current, pushEnabled: true }));
                setMessage('Browser push notifications enabled.');
            } else {
                await disableWebPush({ authToken, apiBaseUrl });
                setPreferences((current) => ({ ...current, pushEnabled: false }));
                setMessage('Browser push notifications disabled.');
            }
        } catch (err) {
            setError(err.message || 'Could not update push notification setting.');
        } finally {
            setPushBusy(false);
        }
    };

    if (loading) {
        return <div className="notification-page-state">Loading preferences...</div>;
    }

    return (
        <section className="notification-preferences">
            <div className="notification-preferences-intro">
                <div className="notification-preferences-icon">
                    <Bell size={24} />
                </div>
                <div>
                    <h2>Notification preferences</h2>
                    <p>Choose how you want to receive updates from the lab platform.</p>
                </div>
            </div>

            {message && <div className="notification-success">{message}</div>}
            {error && <div className="notification-alert">{error}</div>}

            <div className="notification-preference-list">
                <div className="notification-preference-row">
                    <div className="notification-preference-info">
                        <span className="preference-icon email"><Mail size={20} /></span>
                        <div>
                            <strong>Email notifications</strong>
                            <p>Receive booking, maintenance and account updates by email.</p>
                        </div>
                    </div>
                    <label className="notification-switch">
                        <input
                            type="checkbox"
                            checked={preferences.emailEnabled}
                            onChange={(event) => updatePreference('emailEnabled', event.target.checked)}
                        />
                        <span />
                    </label>
                </div>

                <div className="notification-preference-row">
                    <div className="notification-preference-info">
                        <span className="preference-icon sms"><MessageSquare size={20} /></span>
                        <div>
                            <strong>SMS notifications</strong>
                            <p>
                                {hasPhone
                                    ? `Send important updates to ${phone}.`
                                    : 'Add a phone number to your profile before enabling SMS.'}
                            </p>
                            {!hasPhone && (
                                <small className="notification-muted">
                                    SMS is unavailable because no phone number is registered.
                                </small>
                            )}
                        </div>
                    </div>
                    <label className="notification-switch">
                        <input
                            type="checkbox"
                            checked={Boolean(hasPhone && preferences.smsEnabled)}
                            disabled={!hasPhone}
                            onChange={(event) => updatePreference('smsEnabled', event.target.checked)}
                        />
                        <span />
                    </label>
                </div>

                <div className="notification-preference-row">
                    <div className="notification-preference-info">
                        <span className="preference-icon push"><Smartphone size={20} /></span>
                        <div>
                            <strong>Browser push notifications</strong>
                            <p>Receive alerts in your browser when you are not on this page.</p>
                            <small className="notification-muted">
                                Your browser will ask permission when you enable this option.
                            </small>
                        </div>
                    </div>
                    <label className={`notification-switch ${pushBusy ? 'disabled' : ''}`}>
                        <input
                            type="checkbox"
                            checked={preferences.pushEnabled}
                            disabled={pushBusy}
                            onChange={(event) => togglePush(event.target.checked)}
                        />
                        <span />
                    </label>
                </div>

                <div className="notification-preference-row">
                    <div className="notification-preference-info">
                        <span className="preference-icon in-app"><Bell size={20} /></span>
                        <div>
                            <strong>In-app notifications</strong>
                            <p>Notifications inside the Lab Resource Platform.</p>
                            <small className="notification-muted">Always enabled for your account.</small>
                        </div>
                    </div>
                    <span className="notification-always-on">Always on</span>
                </div>
            </div>

            <div className="notification-preference-footer">
                <button
                    type="button"
                    className="notification-save-button"
                    onClick={savePreferences}
                    disabled={saving || pushBusy}
                >
                    <Save size={17} />
                    {saving ? 'Saving...' : 'Save preferences'}
                </button>
            </div>
        </section>
    );
}
