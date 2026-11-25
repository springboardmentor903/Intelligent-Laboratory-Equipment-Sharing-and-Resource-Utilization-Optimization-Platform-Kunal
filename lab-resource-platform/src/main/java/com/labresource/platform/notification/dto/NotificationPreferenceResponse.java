
package com.labresource.platform.notification.dto;

import com.labresource.platform.notification.entity.NotificationPreference;

public class NotificationPreferenceResponse {

    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    private boolean phoneNumberAvailable;

    public NotificationPreferenceResponse(
            NotificationPreference preference,
            boolean phoneNumberAvailable
    ) {
        this.emailEnabled = preference.getEmailEnabled();
        this.smsEnabled = preference.getSmsEnabled();
        this.pushEnabled = preference.getPushEnabled();
        this.inAppEnabled = true;
        this.phoneNumberAvailable = phoneNumberAvailable;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public Boolean getInAppEnabled() {
        return true;
    }

    public boolean isPhoneNumberAvailable() {
        return phoneNumberAvailable;
    }
}
