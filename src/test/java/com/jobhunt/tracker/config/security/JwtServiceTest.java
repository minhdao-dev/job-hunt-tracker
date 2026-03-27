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
    private static final long NEG_EXPIRATION = -1000L;

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
        @DisplayName("Generate token → trả về string không rỗng")
        void generateToken_returnsNonBlankString() {
            String token = jwtService.generateToken(userId, email);

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Generate token → có đúng 3 phần phân cách bởi dấu chấm (JWT format)")
        void generateToken_hasCorrectJwtFormat() {
            String token = jwtService.generateToken(userId, email);

            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Cùng input + cùng thời điểm → token giống nhau (deterministic)")
        void generateToken_sameInputSameTime_producesSameToken() {
            String token1 = jwtService.generateToken(userId, email);
            String token2 = jwtService.generateToken(userId, email);

            assertThat(token1).isNotBlank();
            assertThat(token2).isNotBlank();
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
        @DisplayName("Token bị tamper (sửa payload) → false")
        void isTokenValid_tamperedToken_returnsFalse() {
            String token = jwtService.generateToken(userId, email);

            String[] parts = token.split("\\.");
            String tamperedToken = parts[0] + ".INVALIDDPAYLOAD." + parts[2];

            assertThat(jwtService.isTokenValid(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("Token rác (không phải JWT) → false")
        void isTokenValid_randomString_returnsFalse() {
            assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
            assertThat(jwtService.isTokenValid("totallygarbage")).isFalse();
            assertThat(jwtService.isTokenValid("")).isFalse();
        }

        @Test
        @DisplayName("Token ký bằng secret khác → false")
        void isTokenValid_tokenSignedWithDifferentSecret_returnsFalse() {

            JwtService otherService = new JwtService();
            ReflectionTestUtils.setField(otherService, "jwtSecret",
                    "completely-different-secret-key-at-least-32-chars!");
            ReflectionTestUtils.setField(otherService, "jwtExpiration", EXPIRATION);

            String tokenFromOtherService = otherService.generateToken(userId, email);

            assertThat(jwtService.isTokenValid(tokenFromOtherService)).isFalse();
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
        @DisplayName("Extract userId → đúng UUID đã dùng khi generate")
        void extractUserId_returnsCorrectUUID() {
            String token = jwtService.generateToken(userId, email);

            UUID extracted = jwtService.extractUserId(token);

            assertThat(extracted).isEqualTo(userId);
        }

        @Test
        @DisplayName("Các userId khác nhau → extract ra đúng từng cái")
        void extractUserId_differentUsers_returnsCorrectUUID() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            String token1 = jwtService.generateToken(userId1, "user1@example.com");
            String token2 = jwtService.generateToken(userId2, "user2@example.com");

            assertThat(jwtService.extractUserId(token1)).isEqualTo(userId1);
            assertThat(jwtService.extractUserId(token2)).isEqualTo(userId2);
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
        @DisplayName("Extract email → đúng email đã dùng khi generate")
        void extractEmail_returnsCorrectEmail() {
            String token = jwtService.generateToken(userId, email);

            String extracted = jwtService.extractEmail(token);

            assertThat(extracted).isEqualTo(email);
        }

        @Test
        @DisplayName("Các email khác nhau → extract ra đúng từng cái")
        void extractEmail_differentEmails_returnsCorrectEmail() {
            String email1 = "alice@example.com";
            String email2 = "bob@example.com";

            String token1 = jwtService.generateToken(UUID.randomUUID(), email1);
            String token2 = jwtService.generateToken(UUID.randomUUID(), email2);

            assertThat(jwtService.extractEmail(token1)).isEqualTo(email1);
            assertThat(jwtService.extractEmail(token2)).isEqualTo(email2);
        }

        @Test
        @DisplayName("Token không hợp lệ → ném exception")
        void extractEmail_invalidToken_throwsException() {
            assertThatThrownBy(() -> jwtService.extractEmail("invalid.token.here"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("getExpirationTime()")
    class GetExpirationTimeTests {

        @Test
        @DisplayName("getExpirationTime() → đúng giá trị đã inject")
        void getExpirationTime_returnsConfiguredValue() {
            assertThat(jwtService.getExpirationTime()).isEqualTo(EXPIRATION);
        }
    }

    @Nested
    @DisplayName("Round-trip: generate → validate → extract")
    class RoundTripTests {

        @Test
        @DisplayName("Generate token → validate → extract userId + email → tất cả đúng")
        void roundTrip_generateAndExtract_allFieldsMatch() {

            UUID expectedUserId = UUID.randomUUID();
            String expectedEmail = "roundtrip@example.com";

            String token = jwtService.generateToken(expectedUserId, expectedEmail);

            assertThat(jwtService.isTokenValid(token)).isTrue();
            assertThat(jwtService.extractUserId(token)).isEqualTo(expectedUserId);
            assertThat(jwtService.extractEmail(token)).isEqualTo(expectedEmail);
        }
    }
}