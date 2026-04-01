package com.jobhunt.tracker.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";
    private static final long EXPIRATION = 900_000L;
    private static final long NEG_EXPIRATION = -1_000L;

    private UUID userId;
    private String email;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        userId = UUID.randomUUID();
        email = "test@example.com";
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateTokenTests {

        @Test
        @DisplayName("Trả về string không rỗng")
        void generateToken_returnsNonBlankString() {
            String token = jwtService.generateToken(userId, email);

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Đúng 3 phần phân cách bởi dấu chấm — JWT format")
        void generateToken_hasCorrectJwtFormat() {
            String token = jwtService.generateToken(userId, email);

            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Mỗi lần generate → jti khác nhau")
        void generateToken_eachCall_producesUniqueJti() {
            String token1 = jwtService.generateToken(userId, email);
            String token2 = jwtService.generateToken(userId, email);

            assertThat(jwtService.extractJti(token1))
                    .isNotEqualTo(jwtService.extractJti(token2));
        }

        @Test
        @DisplayName("jti trong token là UUID hợp lệ")
        void generateToken_jtiIsValidUUID() {
            String token = jwtService.generateToken(userId, email);
            String jti = jwtService.extractJti(token);

            assertThatCode(() -> {
                UUID ignored = UUID.fromString(jti);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValidTests {

        @Test
        @DisplayName("Token hợp lệ → true")
        void isTokenValid_validToken_returnsTrue() {
            String token = jwtService.generateToken(userId, email);

            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("Token bị tamper payload → false")
        void isTokenValid_tamperedToken_returnsFalse() {
            String token = jwtService.generateToken(userId, email);
            String[] parts = token.split("\\.");
            String tampered = parts[0] + ".INVALIDDPAYLOAD." + parts[2];

            assertThat(jwtService.isTokenValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("Chuỗi rác không phải JWT → false")
        void isTokenValid_randomString_returnsFalse() {
            assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
            assertThat(jwtService.isTokenValid("totallygarbage")).isFalse();
            assertThat(jwtService.isTokenValid("")).isFalse();
        }

        @Test
        @DisplayName("Token ký bằng secret khác → false")
        void isTokenValid_differentSecret_returnsFalse() {
            JwtService other = new JwtService();
            ReflectionTestUtils.setField(other, "jwtSecret",
                    "completely-different-secret-key-at-least-32-chars!");
            ReflectionTestUtils.setField(other, "jwtExpiration", EXPIRATION);

            String foreignToken = other.generateToken(userId, email);

            assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
        }

        @Test
        @DisplayName("Token đã hết hạn → false")
        void isTokenValid_expiredToken_returnsFalse() {
            JwtService expiredService = new JwtService();
            ReflectionTestUtils.setField(expiredService, "jwtSecret", SECRET);
            ReflectionTestUtils.setField(expiredService, "jwtExpiration", NEG_EXPIRATION);

            String expiredToken = expiredService.generateToken(userId, email);

            assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractUserId()")
    class ExtractUserIdTests {

        @Test
        @DisplayName("Extract đúng UUID đã dùng khi generate")
        void extractUserId_returnsCorrectUUID() {
            String token = jwtService.generateToken(userId, email);

            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        }

        @Test
        @DisplayName("Các userId khác nhau → extract đúng từng cái")
        void extractUserId_differentUsers_returnsCorrectUUID() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            String token1 = jwtService.generateToken(id1, "u1@test.com");
            String token2 = jwtService.generateToken(id2, "u2@test.com");

            assertThat(jwtService.extractUserId(token1)).isEqualTo(id1);
            assertThat(jwtService.extractUserId(token2)).isEqualTo(id2);
        }

        @Test
        @DisplayName("Token không hợp lệ → ném exception")
        void extractUserId_invalidToken_throwsException() {
            assertThatThrownBy(() -> jwtService.extractUserId("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmailTests {

        @Test
        @DisplayName("Extract đúng email đã dùng khi generate")
        void extractEmail_returnsCorrectEmail() {
            String token = jwtService.generateToken(userId, email);

            assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        }

        @Test
        @DisplayName("Các email khác nhau → extract đúng từng cái")
        void extractEmail_differentEmails_returnsCorrectEmail() {
            String e1 = "alice@test.com";
            String e2 = "bob@test.com";

            String token1 = jwtService.generateToken(UUID.randomUUID(), e1);
            String token2 = jwtService.generateToken(UUID.randomUUID(), e2);

            assertThat(jwtService.extractEmail(token1)).isEqualTo(e1);
            assertThat(jwtService.extractEmail(token2)).isEqualTo(e2);
        }

        @Test
        @DisplayName("Token không hợp lệ → ném exception")
        void extractEmail_invalidToken_throwsException() {
            assertThatThrownBy(() -> jwtService.extractEmail("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("extractJti()")
    class ExtractJtiTests {

        @Test
        @DisplayName("Token hợp lệ → trả về jti không rỗng")
        void extractJti_validToken_returnsNonBlankJti() {
            String token = jwtService.generateToken(userId, email);

            assertThat(jwtService.extractJti(token)).isNotBlank();
        }

        @Test
        @DisplayName("Mỗi token có jti khác nhau — dù cùng userId + email")
        void extractJti_eachToken_hasUniqueJti() {
            String token1 = jwtService.generateToken(userId, email);
            String token2 = jwtService.generateToken(userId, email);

            assertThat(jwtService.extractJti(token1))
                    .isNotEqualTo(jwtService.extractJti(token2));
        }

        @Test
        @DisplayName("jti là UUID hợp lệ")
        void extractJti_jtiIsValidUUID() {
            String token = jwtService.generateToken(userId, email);
            String jti = jwtService.extractJti(token);

            assertThatCode(() -> {
                UUID ignored = UUID.fromString(jti);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Cùng token → extractJti() luôn trả về cùng giá trị")
        void extractJti_sameToken_returnsSameJti() {
            String token = jwtService.generateToken(userId, email);

            assertThat(jwtService.extractJti(token))
                    .isEqualTo(jwtService.extractJti(token));
        }

        @Test
        @DisplayName("Token không hợp lệ → ném exception")
        void extractJti_invalidToken_throwsException() {
            assertThatThrownBy(() -> jwtService.extractJti("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("getRemainingTtlMillis()")
    class GetRemainingTtlMillisTests {

        @Test
        @DisplayName("Token vừa tạo → TTL xấp xỉ EXPIRATION (sai số < 1 giây)")
        void getRemainingTtlMillis_freshToken_returnsApproxExpiration() {
            String token = jwtService.generateToken(userId, email);

            long ttl = jwtService.getRemainingTtlMillis(token);

            assertThat(ttl)
                    .isLessThanOrEqualTo(EXPIRATION)
                    .isGreaterThan(EXPIRATION - 1_000);
        }

        @Test
        @DisplayName("Token đã hết hạn → trả về 0")
        void getRemainingTtlMillis_expiredToken_returnsZero() {
            JwtService expiredService = new JwtService();
            ReflectionTestUtils.setField(expiredService, "jwtSecret", SECRET);
            ReflectionTestUtils.setField(expiredService, "jwtExpiration", NEG_EXPIRATION);

            String expiredToken = expiredService.generateToken(userId, email);

            assertThat(jwtService.getRemainingTtlMillis(expiredToken)).isZero();
        }

        @Test
        @DisplayName("TTL phải dương và nhỏ hơn EXPIRATION")
        void getRemainingTtlMillis_validToken_returnsPositiveValue() {
            String token = jwtService.generateToken(userId, email);

            long ttl = jwtService.getRemainingTtlMillis(token);

            assertThat(ttl)
                    .isPositive()
                    .isLessThanOrEqualTo(EXPIRATION);
        }
    }

    @Nested
    @DisplayName("getExpirationTime()")
    class GetExpirationTimeTests {

        @Test
        @DisplayName("Trả về đúng giá trị đã inject")
        void getExpirationTime_returnsConfiguredValue() {
            assertThat(jwtService.getExpirationTime()).isEqualTo(EXPIRATION);
        }
    }

    @Nested
    @DisplayName("Round-trip: generate → validate → extract tất cả fields")
    class RoundTripTests {

        @Test
        @DisplayName("Generate → validate → extract userId + email + jti → tất cả đúng")
        void roundTrip_allFieldsMatch() {
            UUID expectedUserId = UUID.randomUUID();
            String expectedEmail = "roundtrip@example.com";

            String token = jwtService.generateToken(expectedUserId, expectedEmail);

            assertThat(jwtService.isTokenValid(token)).isTrue();
            assertThat(jwtService.extractUserId(token)).isEqualTo(expectedUserId);
            assertThat(jwtService.extractEmail(token)).isEqualTo(expectedEmail);
            assertThat(jwtService.extractJti(token)).isNotBlank();
            assertThat(jwtService.getRemainingTtlMillis(token)).isPositive();
        }

        @Test
        @DisplayName("jti sau round-trip có thể dùng làm Redis key hợp lệ")
        void roundTrip_jtiSuitableAsRedisKey() {
            String token = jwtService.generateToken(userId, email);
            String jti = jwtService.extractJti(token);
            String redisKey = "blacklist:jti:" + jti;

            assertThat(redisKey).isNotBlank();
            assertThat(redisKey).startsWith("blacklist:jti:");
            assertThat(redisKey).doesNotContain(" ");
        }
    }
}