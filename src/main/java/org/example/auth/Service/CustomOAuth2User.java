package org.example.auth.Service;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.auth.Entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
/* *
 * CustomOAuth2User: Wrapper around OAuth2User that also holds our User entity.
 *
 * Why we need this:
 * - OAuth2User has attributes from Google/Facebook (email, name, picture)
 * - Our User entity has tenantId, roles, userId (database fields)
 * - OAuth2LoginSuccessHandler needs BOTH to generate JWT with all required claims
 *
 * This class delegates all OAuth2User methods to the wrapped oauth2User,
 * while also exposing our User entity via getUser().
 */
@RequiredArgsConstructor
@Getter
public class CustomOAuth2User implements OAuth2User{

    private final OAuth2User oAuth2User;
    private final User user;

    @Override
    public Map<String ,Object>getAttributes(){
        return oAuth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public String getName(){
        return user.getEmail();
    }
}
