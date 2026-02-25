package org.example.auth.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Entity.OAuth2Provider;
import org.example.auth.Entity.Role;
import org.example.auth.Entity.User;
import org.example.auth.Repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.beans.Encoder;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/* *
 * CustomOAuth2UserService: Handles OAuth2 login (Google, Facebook, Apple).

 * FLOW:
 * 1. User clicks "Login with Google" on frontend
 * 2. Frontend redirects to: GET /oauth2/authorization/google
 * 3. Spring redirects to Google login page
 * 4. User logs in at Google
 * 5. Google redirects back to: GET /login/oauth2/code/google?code=xxx
 * 6. Spring exchanges code for access token (calls Google API)
 * 7. This service's loadUser() is called with the OAuth2User data
 * 8. We check: does this user exist in our DB?
 *    - YES → load existing user
 *    - NO  → create new user (auto-register)
 * 9. OAuth2LoginSuccessHandler generates JWT and returns to frontend

 * KEY DESIGN:
 * - oauth2Id = unique ID from provider (Google: "sub", Facebook: "id")
 * - email from OAuth2 might not be verified — check email_verified claim
 * - No password stored for OAuth2 users — password field = random UUID
 * - If user later wants email/password login, they must "set password" via reset flow
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Called by Spring Security after OAuth2 provider returns user data.
     * We either load existing user or create a new one (auto-registration).
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Provider provider = OAuth2Provider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oauth2User.getAttributes();

        String oauth2Id = extractOAuth2Id(provider, attributes);
        String email = extractEmail(provider, attributes);
        String firstName = extractFirstName(provider, attributes);
        String lastName = extractLastName(provider, attributes);
        String profilePicture = extractProfilePicture(provider, attributes);

        log.info("OAuth2 login attempt: provider={}, oauth2Id={}, email={}", provider, oauth2Id, email);

        User user = userRepository.findByOauth2IdAndProvider(oauth2Id, provider)
                .orElseGet(() -> {
                    // Check if user with this email already exists (they signed up via email/password)
                    return userRepository.findByEmail(email)
                            .map(existingUser -> linkOAuth2ToExistingUser(existingUser, provider, oauth2Id, profilePicture))
                            .orElseGet(() -> createNewOAuth2User(provider, oauth2Id, email, firstName, lastName, profilePicture));
                });

        // Store OAuth2User attributes in a custom implementation that also holds our User entity
        return new CustomOAuth2User(oauth2User, user);
    }

    /**
     * Link OAuth2 account to existing email/password account.
     * This allows users to login via email OR Google for the same account.
     */
    private User linkOAuth2ToExistingUser(User user, OAuth2Provider provider, String oauth2Id, String profilePicture) {
        log.info("Linking OAuth2 account to existing user: email={}, provider={}", user.getEmail(), provider);
        user.setOauth2Id(oauth2Id);
        user.setProvider(provider);
        if (profilePicture != null && user.getProfilePictureUrl() == null) {
            user.setProfilePictureUrl(profilePicture);
        }
        return userRepository.save(user);
    }

    /**
     * Auto-register new user from OAuth2 data.
     * Default tenant: "default" — admin must move them to proper tenant later,
     * OR frontend can ask "which company do you work for?" after OAuth2 login.
     */
    private User createNewOAuth2User(
            OAuth2Provider provider,
            String oauth2Id,
            String email,
            String firstName,
            String lastName,
            String profilePicture
    ) {
        log.info("Auto-registering new OAuth2 user: provider={}, email={}", provider, email);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password — OAuth2 users can't login via email/password
                .firstName(firstName)
                .lastName(lastName)
                .tenantId("default") // IMPORTANT: Update this in onboarding flow
                .provider(provider)
                .oauth2Id(oauth2Id)
                .profilePictureUrl(profilePicture)
                .roles(Set.of(Role.STAFF)) // Default role
                .enabled(true)
                .accountNonLocked(true)
                .build();

        User savedUser = userRepository.save(user);

        return savedUser;
    }

    // ─── ATTRIBUTE EXTRACTORS ────────────────────────────────────────────────────

    private String extractOAuth2Id(OAuth2Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("sub"); // Google uses "sub" as unique ID
            case FACEBOOK -> (String) attributes.get("id");
            case APPLE -> (String) attributes.get("sub");
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    private String extractEmail(OAuth2Provider provider, Map<String, Object> attributes) {
        return (String) attributes.get("email");
    }

    private String extractFirstName(OAuth2Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("given_name");
            case FACEBOOK -> {
                String name = (String) attributes.get("name");
                yield name != null ? name.split(" ")[0] : "User";
            }
            case APPLE -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> nameObj = (Map<String, Object>) attributes.get("name");
                yield nameObj != null ? (String) nameObj.get("firstName") : "User";
            }
            default -> "User";
        };
    }

    private String extractLastName(OAuth2Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("family_name");
            case FACEBOOK -> {
                String name = (String) attributes.get("name");
                String[] parts = name != null ? name.split(" ") : new String[]{};
                yield parts.length > 1 ? parts[parts.length - 1] : "";
            }
            case APPLE -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> nameObj = (Map<String, Object>) attributes.get("name");
                yield nameObj != null ? (String) nameObj.get("lastName") : "";
            }
            default -> "";
        };
    }

    private String extractProfilePicture(OAuth2Provider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("picture");
            case FACEBOOK -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> picture = (Map<String, Object>) attributes.get("picture");
                if (picture != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) picture.get("data");
                    yield data != null ? (String) data.get("url") : null;
                }
                yield null;
            }
            case APPLE -> null; // Apple doesn't provide profile picture in OAuth2 response
            default -> null;
        };
    }
}




