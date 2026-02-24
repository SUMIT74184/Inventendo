``````
auth-service/
├── Dockerfile                      # Multi-stage build (Maven → JRE-alpine)
├── pom.xml                         # Dependencies: security, oauth2, jwt, redis, kafka
├── OAUTH2_SETUP.md                 # Guide to get Google/Facebook/Apple credentials
└── src/main/
├── resources/
│   ├── application.yml         # Config for local dev (localhost)
│   └── application-docker.yml  # Config for Docker (service names)
└── java/com/inventory/auth/
├── AuthServiceApplication.java   # @SpringBootApplication @EnableCaching
│
├── entity/
│   ├── User.java           # implements UserDetails, has oauth2Id + provider fields
│   ├── Role.java           # Enum: SUPER_ADMIN, TENANT_ADMIN, MANAGER, STAFF, VIEWER
│   └── OAuth2Provider.java # Enum: LOCAL, GOOGLE, FACEBOOK, APPLE
│
├── repository/
│   └── UserRepository.java # findByEmail, findByOauth2IdAndProvider
│
├── dto/
│   └── AuthDto.java        # All request/response DTOs
│
├── util/
│   └── JwtUtil.java        # Generate/validate JWT, extract claims
│
├── service/
│   ├── UserDetailsServiceImpl.java  # Spring Security bridge, @Cacheable
│   ├── AuthService.java             # register, login, logout, validateToken
│   ├── CustomOAuth2UserService.java # Load/create user from OAuth2 data
│   └── CustomOAuth2User.java        # Wrapper for OAuth2User + User entity
│
├── filter/
│   └── JwtAuthenticationFilter.java # Runs on every request, validates JWT
│
├── controller/
│   └── AuthController.java # REST API: /register, /login, /refresh, etc.
│
└── config/
├── SecurityConfig.java          # Main security config, CORS, OAuth2 setup
├── RedisConfig.java             # RedisTemplate + CacheManager
├── KafkaConfig.java             # ProducerFactory + ConsumerFactory
├── AuthKafkaListener.java       # Listen to tenant.created events
└── OAuth2LoginSuccessHandler.java # Generate JWT after OAuth2 login
``````