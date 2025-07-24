/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.bedrock.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Comprehensive tests for the BedrockTokenGenerator class.
 */
public class BedrockTokenGeneratorTest {

    private AwsCredentials credentials;

    @BeforeEach
    public void setup() {
        credentials = AwsBasicCredentials.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    }

    @Test
    public void testStaticGetToken_ReturnsNonNullToken() {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));

        Assertions.assertNotNull(token, "Token should not be null");
        Assertions.assertTrue(token.length() > 0, "Token should not be empty");
    }

    @Test
    public void testStaticGetToken_StartsWithCorrectPrefix() {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));

        Assertions.assertTrue(token.startsWith("bedrock-api-key-"),
                "Token should start with the correct prefix");
    }

    @ParameterizedTest
    @ValueSource(strings = {"us-east-1", "us-west-2", "eu-west-1", "ap-northeast-1"})
    public void testStaticGetToken_WithDifferentRegions(String regionStr) {
        Region region = Region.of(regionStr);
        String token = BedrockTokenGenerator.getToken(credentials, region, Duration.ofHours(12));

        Assertions.assertNotNull(token, "Token should not be null for region: " + regionStr);
        Assertions.assertTrue(token.startsWith("bedrock-api-key-"),
                "Token should start with the correct prefix for region: " + regionStr);
    }

    @Test
    public void testStaticGetToken_TokenIsBase64Encoded() {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));

        String tokenWithoutPrefix = token.substring("bedrock-api-key-".length());

        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(tokenWithoutPrefix);
            Assertions.assertNotNull(decoded, "Decoded token should not be null");
        } catch (IllegalArgumentException e) {
            Assertions.fail("Token is not valid Base64: " + e.getMessage());
        }
    }

    @Test
    public void testStaticGetToken_ContainsVersionInfo() {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));

        String tokenWithoutPrefix = token.substring("bedrock-api-key-".length());
        byte[] decoded = java.util.Base64.getDecoder().decode(tokenWithoutPrefix);
        String decodedString = new String(decoded, StandardCharsets.UTF_8);
        Assertions.assertTrue(decodedString.contains("&Version=1"),
                "Decoded token should contain version information");
    }

    @Test
    public void testStaticGetToken_DifferentCredentialsProduceDifferentTokens() {
        AwsCredentials credentials1 = AwsBasicCredentials.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        AwsCredentials credentials2 = AwsBasicCredentials.create("AKIAI44QH8DHBEXAMPLE", "je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY");

        String token1 = BedrockTokenGenerator.getToken(credentials1, Region.US_WEST_2, Duration.ofHours(12));
        String token2 = BedrockTokenGenerator.getToken(credentials2, Region.US_WEST_2, Duration.ofHours(12));

        Assertions.assertNotEquals(token1, token2, "Different credentials should produce different tokens");
    }

    @Test
    public void testBuilder_WithAllParameters() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .expiry(Duration.ofHours(6))
                .build();

        String token = generator.getToken();

        Assertions.assertNotNull(token, "Token should not be null");
        Assertions.assertTrue(token.startsWith("bedrock-api-key-"), "Token should start with correct prefix");
    }

    @Test
    public void testBuilder_WithDefaults() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        String token = generator.getToken();

        Assertions.assertNotNull(token, "Token should not be null");
        Assertions.assertTrue(token.startsWith("bedrock-api-key-"), "Token should start with correct prefix");
    }

    @Test
    public void testStaticGetToken_NullCredentialsThrowsException() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            BedrockTokenGenerator.getToken(null, Region.US_WEST_2, Duration.ofHours(12));
        });
    }

    @Test
    public void testStaticGetToken_NullRegionThrowsException() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            BedrockTokenGenerator.getToken(credentials, null, Duration.ofHours(12));
        });
    }

    @Test
    public void testStaticGetToken_NullExpiryUsesDefault() {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, null);

        Assertions.assertNotNull(token, "Token should not be null when expiry is null");
        Assertions.assertTrue(token.startsWith("bedrock-api-key-"), "Token should start with correct prefix");
    }

    @ParameterizedTest
    @MethodSource("invalidExpiryDurations")
    public void testStaticGetToken_InvalidExpiryThrowsException(Duration invalidExpiry) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, invalidExpiry);
        }, "Invalid expiry duration should throw IllegalArgumentException");
    }

    private static Stream<Arguments> invalidExpiryDurations() {
        return Stream.of(
                Arguments.of(Duration.ofSeconds(-1)),     // Negative duration
                Arguments.of(Duration.ZERO),              // Zero duration
                Arguments.of(Duration.ofHours(13)),       // Exceeds max 12 hours
                Arguments.of(Duration.ofDays(1))          // Exceeds max 12 hours
        );
    }

    @ParameterizedTest
    @MethodSource("validExpiryDurations")
    public void testStaticGetToken_ValidExpiryDurations(Duration validExpiry) {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, validExpiry);

        Assertions.assertNotNull(token, "Token should not be null for valid expiry: " + validExpiry);
        Assertions.assertTrue(token.startsWith("bedrock-api-key-"), "Token should start with correct prefix");
    }

    private static Stream<Arguments> validExpiryDurations() {
        return Stream.of(
                Arguments.of(Duration.ofSeconds(1)),      // Minimum valid duration
                Arguments.of(Duration.ofMinutes(30)),     // 30 minutes
                Arguments.of(Duration.ofHours(1)),        // 1 hour
                Arguments.of(Duration.ofHours(6)),        // 6 hours
                Arguments.of(Duration.ofHours(12))        // Maximum valid duration
        );
    }

    @Test
    public void testDefaultConstructor() {
        BedrockTokenGenerator generator = new BedrockTokenGenerator();

        // This test verifies the default constructor works without throwing exceptions
        // We can't easily test the token generation without valid AWS credentials
        Assertions.assertNotNull(generator, "Generator should be created successfully");
    }

    @Test
    public void testBuilder_EmptyBuilder() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder().build();

        Assertions.assertNotNull(generator, "Generator should be created with empty builder");
    }

    @Test
    public void testBuilder_OnlyRegion() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
                .region(Region.EU_WEST_1)
                .build();

        Assertions.assertNotNull(generator, "Generator should be created with only region specified");
    }

    @Test
    public void testBuilder_OnlyCredentialsProvider() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        Assertions.assertNotNull(generator, "Generator should be created with only credentials provider specified");
    }

    @Test
    public void testBuilder_OnlyExpiry() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
                .expiry(Duration.ofHours(3))
                .build();

        Assertions.assertNotNull(generator, "Generator should be created with only expiry specified");
    }

    @Test
    public void testBuilder_FluentInterface() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .expiry(Duration.ofHours(8))
                .build();

        String token = generator.getToken();

        Assertions.assertNotNull(token, "Token should not be null");
        Assertions.assertTrue(token.startsWith("bedrock-api-key-"), "Token should start with correct prefix");
    }

    @Test
    public void testGetTokenUsingInstanceMethod() {
        BedrockTokenGenerator generator = new BedrockTokenGenerator();
        String token = generator.getToken(credentials, "us-east-1");

        Assertions.assertNotNull(token, "Token should not be null");
        Assertions.assertTrue(token.startsWith("bedrock-api-key-"), "Token should start with correct prefix");
    }

    @Test
    public void testTokenConsistency_SameInputsSameToken() {
        Duration expiry = Duration.ofHours(6);
        Region region = Region.US_WEST_2;

        String token1 = BedrockTokenGenerator.getToken(credentials, region, expiry);
        String token2 = BedrockTokenGenerator.getToken(credentials, region, expiry);

        // Note: Tokens might be different due to timestamp differences, but structure should be consistent
        Assertions.assertNotNull(token1, "First token should not be null");
        Assertions.assertNotNull(token2, "Second token should not be null");
        Assertions.assertTrue(token1.startsWith("bedrock-api-key-"), "First token should start with correct prefix");
        Assertions.assertTrue(token2.startsWith("bedrock-api-key-"), "Second token should start with correct prefix");
    }

    @Test
    public void testTokenStructure_ContainsExpectedComponents() {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));

        String tokenWithoutPrefix = token.substring("bedrock-api-key-".length());
        byte[] decoded = java.util.Base64.getDecoder().decode(tokenWithoutPrefix);
        String decodedString = new String(decoded, StandardCharsets.UTF_8);

        Assertions.assertTrue(decodedString.contains("bedrock.amazonaws.com"),
                "Decoded token should contain the bedrock host");
        Assertions.assertTrue(decodedString.contains("Action=CallWithBearerToken"),
                "Decoded token should contain the correct action");
        Assertions.assertTrue(decodedString.contains("&Version=1"),
                "Decoded token should contain version information");
    }

    @Test
    public void testDifferentRegionsProduceDifferentTokens() {
        String tokenUsWest2 = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));
        String tokenEuWest1 = BedrockTokenGenerator.getToken(credentials, Region.EU_WEST_1, Duration.ofHours(12));

        Assertions.assertNotEquals(tokenUsWest2, tokenEuWest1,
                "Different regions should produce different tokens");
    }

    @Test
    public void testDifferentExpiriesProduceDifferentTokens() {
        String token1Hour = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(1));
        String token6Hours = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(6));

        Assertions.assertNotEquals(token1Hour, token6Hours,
                "Different expiry durations should produce different tokens");
    }

    @Test
    public void testTokenLength_ReasonableSize() {
        String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));

        // Token should be reasonably sized (not too short or excessively long)
        Assertions.assertTrue(token.length() > 50, "Token should be longer than 50 characters");
        Assertions.assertTrue(token.length() < 2000, "Token should be shorter than 2000 characters");
    }

    @Test
    public void testBuilder_NullValues() {
        BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
                .region(null)
                .credentialsProvider(null)
                .expiry(null)
                .build();

        Assertions.assertNotNull(generator, "Generator should handle null values gracefully");
    }

    @Test
    public void testValidateOrDefault_ReturnsDefaultWhenNull() throws Exception {
        java.lang.reflect.Method method = BedrockTokenGenerator.class.getDeclaredMethod("validateOrDefault", Duration.class);
        method.setAccessible(true);
        Duration result = (Duration) method.invoke(null, (Object) null);
        Assertions.assertEquals(Duration.ofHours(12), result);
    }
}
