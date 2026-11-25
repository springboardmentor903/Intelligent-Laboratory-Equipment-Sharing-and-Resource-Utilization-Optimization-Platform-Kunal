
package com.labresource.platform.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PushDeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    @Size(max = 2000, message = "Device token is too long")
    private String deviceToken;

    @NotBlank(message = "Platform is required")
    @Size(max = 30, message = "Platform must not exceed 30 characters")
    private String platform;

    @Size(max = 150, message = "Device name must not exceed 150 characters")
    private String deviceName;

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
