package org.example.auth.Util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Service.UserDetailsServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final RedisTemplate<String,String>redisTemplate;


    protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull  HttpServletResponse response,
          @NonNull  FilterChain filterChain
    ) throws ServletException, IOException{

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        //All JWT tokens use "Bearer" prefix ....this is the HTTP standard
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        //Extract token by removing "Bearer " (7 characters) prefix
        jwt = authHeader.substring(7);

        try {
            //Check if this token was blacklisted (user logged out)
            //When user logs,out we add their token to Redis blacklist
            String blacklistKey = "blacklisted_token:" + jwt;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))){
                log.debug("Token is blacklisted (user logged out)");
                filterChain.doFilter(request,response);
                return;
            }

            userEmail = jwtUtil.extractEmail(jwt);

            /* *
             * SecurityContextHolder.getContext().getAuthentication() == null means:
             *"This request has not been authenticated yet in this thread
             * We only authenticate once per request, so we check this to avoid re-authenticating.
             *
             */

            if (userEmail!=null && SecurityContextHolder.getContext().getAuthentication()==null){
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if(jwtUtil.isTokenValid(jwt,userDetails.getUsername())){
                    /* * Parameters:
                     *   1. principal = the UserDetails (who they are)
                            *   2. credentials = null (we don't need password after JWT validation)
                            *   3. authorities = their roles/permissions
                            */
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    //Adds request metadata (IP address, session ID) to the auth object
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    //SET the authenticated user in the thread's security context
                    //Now controllers can call SecurityContextHolder.getContext().getAuthentication()
                    // to know who is making this request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }


        } catch (Exception e){
            log.info("Cannot set user authentication: {}",e.getMessage());

        }

        filterChain.doFilter(request,response); // Continue to next filter/controller
    }

}
