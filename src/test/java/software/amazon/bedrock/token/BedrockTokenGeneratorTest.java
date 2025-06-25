/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.bedrock.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the BedrockTokenGenerator class.
 */
public class BedrockTokenGeneratorTest {

    private BedrockTokenGenerator tokenGenerator;
    private AwsCredentials credentials;

    @BeforeEach
    public void setup() {
        // Setup test credentials and token generator instance
        tokenGenerator = new BedrockTokenGenerator();
        credentials = AwsBasicCredentials.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
    }

    @Test
    public void testGenerateToken_ReturnsNonNullToken() {
        // Act
        String token = tokenGenerator.getToken(credentials, "us-west-2");
        
        // Assert
        assertNotNull(token, "Token should not be null");
        assertTrue(token.length() > 0, "Token should not be empty");
    }

    @Test
    public void testGenerateToken_StartsWithCorrectPrefix() {
        // Act
        String token = tokenGenerator.getToken(credentials, "us-west-2");
        
        // Assert
        assertTrue(token.startsWith("bedrock-api-key-"), 
                "Token should start with the correct prefix");
    }

    @ParameterizedTest
    @ValueSource(strings = {"us-east-1", "us-west-2", "eu-west-1", "ap-northeast-1"})
    public void testGenerateToken_WithDifferentRegions(String region) {
        // Act
        String token = tokenGenerator.getToken(credentials, region);
        
        // Assert
        assertNotNull(token, "Token should not be null for region: " + region);
        assertTrue(token.startsWith("bedrock-api-key-"), 
                "Token should start with the correct prefix for region: " + region);
    }

    @Test
    public void testGenerateToken_TokenIsBase64Encoded() {
        // Act
        String token = tokenGenerator.getToken(credentials, "us-west-2");
        
        // Assert
        String tokenWithoutPrefix = token.substring("bedrock-api-key-".length());
        
        // This will throw an IllegalArgumentException if the string is not valid Base64
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(tokenWithoutPrefix);
            assertNotNull(decoded, "Decoded token should not be null");
        } catch (IllegalArgumentException e) {
            fail("Token is not valid Base64: " + e.getMessage());
        }
    }

    @Test
    public void testGenerateToken_ContainsVersionInfo() {
        // Act
        String token = tokenGenerator.getToken(credentials, "us-west-2");
        
        // Assert
        String tokenWithoutPrefix = token.substring("bedrock-api-key-".length());
        byte[] decoded = java.util.Base64.getDecoder().decode(tokenWithoutPrefix);
        String decodedString = new String(decoded, StandardCharsets.UTF_8);
        assertTrue(decodedString.contains("&Version=1"), 
                "Decoded token should contain version information");
    }

    @Test
    public void testGenerateToken_DifferentCredentialsProduceDifferentTokens() {
        // Arrange
        AwsCredentials credentials1 = AwsBasicCredentials.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        AwsCredentials credentials2 = AwsBasicCredentials.create("AKIAI44QH8DHBEXAMPLE", "je7MtGbClwBF/2Zp9Utk/h3yCo8nvbEXAMPLEKEY");
        
        // Act
        String token1 = tokenGenerator.getToken(credentials1, "us-west-2");
        String token2 = tokenGenerator.getToken(credentials2, "us-west-2");
        
        // Assert
        assertNotEquals(token1, token2, "Different credentials should produce different tokens");
    }
}
