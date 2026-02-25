package org.example.auth.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.Repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    @Cacheable(value = "users" , key = "#email", unless = "#result == null")
//    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        log.debug("Loading user from DB (cache miss) for email:{}",email);
        return userRepository.findByEmail(email)
                .orElseThrow(()->{
                    log.error("User not found with email: {}",email);
                    return new UsernameNotFoundException("User not found");
                });
    }

    @CacheEvict(value = "users" , key = "#email")
    public void evictUserCache(String email){
        log.debug("Evicting cache for user:{}",email);
    }

    @CacheEvict(value = "users",allEntries = true)
    public void evictAllUsersCache(){
        log.info("Evicting entire users cache");
    }


}
