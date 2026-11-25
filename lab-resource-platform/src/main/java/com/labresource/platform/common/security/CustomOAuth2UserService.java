
package com.labresource.platform.common.security;

import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauthUser = super.loadUser(userRequest);

        Map<String, Object> attributes = oauthUser.getAttributes();

        String email = (String) attributes.get("email");
        String googleId = (String) attributes.get("sub");
        String name = (String) attributes.get("name");

        if (email == null || email.isBlank()
                || googleId == null || googleId.isBlank()) {

            throw new OAuth2AuthenticationException(
                    "Google account did not provide email or user ID"
            );
        }

        email = email.trim().toLowerCase();

        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        "No existing account found for this Google email. "
                                + "Register through the existing registration API first."
                ));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new OAuth2AuthenticationException(
                    "This user account is inactive"
            );
        }

        if (user.getGoogleId() != null
                && !user.getGoogleId().equals(googleId)) {

            throw new OAuth2AuthenticationException(
                    "Google account does not match the linked account"
            );
        }

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
        }

        user.setAuthProvider("GOOGLE");

        if (name != null && !name.isBlank()
                && (user.getFullName() == null
                || user.getFullName().isBlank())) {

            user.setFullName(name);
        }

        user.setEmailVerified(true);

        userRepository.save(user);

        String roleName = user.getRole() != null
                ? user.getRole().getName()
                : "RESEARCHER_STUDENT";

        if (roleName.startsWith("ROLE_")) {
            roleName = roleName.substring(5);
        }

        return new DefaultOAuth2User(
                Collections.singleton(
                        new SimpleGrantedAuthority("ROLE_" + roleName)
                ),
                attributes,
                "email"
        );
    }
}
