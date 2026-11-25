
package com.labresource.platform.notification.repository;

import com.labresource.platform.notification.entity.PushDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceTokenRepository
        extends JpaRepository<PushDeviceToken, Long> {

    List<PushDeviceToken> findByUserIdAndActiveTrue(Long userId);

    List<PushDeviceToken> findByUserId(Long userId);

    Optional<PushDeviceToken> findByDeviceToken(String deviceToken);

    Optional<PushDeviceToken> findByDeviceTokenAndUserId(
            String deviceToken, Long userId);

    boolean existsByUserIdAndActiveTrue(Long userId);

    void deleteByDeviceTokenAndUserId(
            String deviceToken, Long userId);
}
