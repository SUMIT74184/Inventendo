package org.example.auth.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Entity.OAuth2Provider;
import org.example.auth.Entity.Role;
import org.example.auth.Entity.User;
import org.example.auth.Repository.UserRepository;
import org.example.auth.Util.JwtUtil;
import org.example.auth.config.AuthKafkaListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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
public class CustomOAuth2UserService extends DefaultOAuth2UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String,String> redisTemplate;
    private final KafkaTemplate<String,String> kafkaTemplate;
    private final AuthKafkaListener authKafkaListener;

    /* *
     * Called by Spring Security after OAuth2 provider returns user data.
     * We either load existing user or create a new one (auto-registration).
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Provider provider = OAuth2Provider.valueOf(registrationId.toUpperCase());

        Map<String,Object> attributes = oauth2User.getAttributes();

        String oauth2Id = extractOAuth2Id(provider,attributes);
        String email = extractOAuth2Id(provider,attributes);
        String firstName = extractOAuth2Id(provider,attributes);
        String lastName = extractOAuth2Id(provider,attributes);
        String profilePicture = extractProfilePicture(provider,attributes);

        log.info("OAuth2 login attempt: prvoider={}, auth2Id={}, email={}",provider,oauth2Id,email);

        User user = userRepository.findByOauth2IdAndProvider(oauth2Id,provider)
                .orElseThrow(()->{
                   return userRepository .findByEmail(email)
                           .map(existingUser -> linkOAuth2ToExistingUser(existingUser,provider,oauth2Id,profilePicture))
                           .orElseGet(()-> createNewOAuth2User(provider,oauth2Id,email,firstName,lastName,profilePicture));
                });

        // Store OAuth2User attributes in a custom implementation that also holds our User entity
        return new CustomOAuth2User(oauth2User,user);

    }
    /* *
     * Link OAuth2 account to existing email/password account.
     * This allows users to login via email OR Google for the same account.
     */
    private User linkOAuth2ToExistingUser(User user, OAuth2Provider provider,String oauth2Id,String profilePicture){
        log.info("Linking OAuth2 account to existing user: email={},provider={}",user.getEmail(),provider);
        user.setOauth2Id(oauth2Id);
        user.setProvider(provider);
        if (profilePicture!=null && user.getProfilePictureUrl()==null){
            user.setProfilePictureUrl(profilePicture);
        }
        return userRepository.save(user);
    }


    private User createNewOAuth2User(
            OAuth2Provider provider,
            String oauth2Id,
            String email,
            String firstName,
            String lastName,
            String profilePicture
    ){
        log.info("Auto-registering new OAuth2 user: provider={}, email={}",provider,email);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .firstName(firstName)
                .lastName(lastName)
                .tenantId("default")
                .provider(provider)
                .oauth2Id(oauth2Id)
                .profilePictureUrl(profilePicture)
                .roles(Set.of(Role.STAFF)) //Default role
                .enabled(true)
                .accountNonLocked(true)
                .build();

        User savedUser =userRepository.save(user);

        //Publish event so Email service can send welcome email
        publishEvent("user.registerd.oauth2",savedUser);
    }

    private String extractOAuth2Id(OAuth2Provider provider, Map<String, Object> attributes) {
    }

}
