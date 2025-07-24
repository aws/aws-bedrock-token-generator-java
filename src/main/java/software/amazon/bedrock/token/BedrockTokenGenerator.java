/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.bedrock.token;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

/**
 * BedrockTokenGenerator provides a lightweight utility to generate short-lived AWS Bearer tokens
 * for use with the Amazon Bedrock API.
 */
public class BedrockTokenGenerator {

    private static final String PROTOCOL = "https";
    private static final String DEFAULT_HOST = "bedrock.amazonaws.com";
    private static final String HOST_HEADER = "host";
    private static final String DEFAULT_PATH = "/";
    private static final String QUERY_ACTION_PARAM = "Action";
    private static final String QUERY_ACTION_PARAM_VALUE = "CallWithBearerToken";
    private static final String SERVICE_SIGNING_NAME = "bedrock";
    private static final String AUTH_PREFIX = "bedrock-api-key-";
    private static final String TOKEN_VERSION = "&Version=1";
    private static final Duration DEFAULT_EXPIRY = Duration.ofHours(12);
    private static final Duration MAX_EXPIRY = Duration.ofHours(12);
    private static final String HTTPS_PREFIX = "https://";
    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;
    private final Duration expiry;

    /**
     * Default constructor for who will directly call getToken(AwsCredentials, String).
     */
    public BedrockTokenGenerator() {
        this.region = null;
        this.credentialsProvider = null;
        this.expiry = DEFAULT_EXPIRY;
    }

    /**
     * Private constructor used by the Builder
     * Initializes region, credentials provider, and expiry duration with defaults if not explicitly provided.
     *
     * @param region The AWS region. If null, resolves from DefaultAwsRegionProviderChain.
     * @param provider The AWS credentials provider. If null, uses DefaultCredentialsProvider.
     * @param expiry Token expiration duration. Must be greater than 0 and less than or equal to 12 hours.
     *               If null or invalid, defaults to 12 hours.
     *
     * @throws NullPointerException if region or credentials provider cannot be resolved from defaults
     * @throws SdkClientException if default region or credentials provider cannot be initialized
     * @throws IllegalArgumentException if expiry is less than or equal to 0 or greater than 12 hours
     */
    private BedrockTokenGenerator(Region region, AwsCredentialsProvider provider, Duration expiry) {
        this.region = region != null ? region : new DefaultAwsRegionProviderChain().getRegion();
        this.credentialsProvider = provider != null ? provider : DefaultCredentialsProvider.create();
        
        Objects.requireNonNull(this.region, "Region must not be null and could not be obtained from default provider");
        Objects.requireNonNull(this.credentialsProvider, "CredentialsProvider must not be null and" +
                " could not be initialized from defaults");
        
        this.expiry = validateOrDefault(expiry);
    }

    /**
     * Generates a bearer token using credentialsProvider and region provider during constructor
     *
     * @return A bearer token string.
     * @throws SdkClientException if AWS credentials could not be resolved
     */
    public String getToken() {
        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        return getToken(credentials, region, expiry);
    }

    /**
     * Generates a bearer token using explicit AWS credentials and region.
     *
     * @param credentials AWS credentials.
     * @param region The AWS region.
     * @param expiry Token expiration duration (max 12 hours). If null or invalid, defaults to 12 hours.
     * @return A bearer token string.
     * @throws NullPointerException if credentials or region are null
     * @throws IllegalArgumentException if expiry is less than or equal to 0 or greater than 12 hours
     */
    public static String getToken(AwsCredentials credentials, Region region, Duration expiry) {
        Objects.requireNonNull(credentials, "Credentials must not be null");
        Objects.requireNonNull(region, "Region must not be null");

        expiry = validateOrDefault(expiry);

        AwsV4HttpSigner signer = AwsV4HttpSigner.create();

        SdkHttpFullRequest sdkHttpRequest = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.POST)
                .protocol(PROTOCOL)
                .host(DEFAULT_HOST)
                .encodedPath(DEFAULT_PATH)
                .appendHeader(HOST_HEADER, DEFAULT_HOST)
                .putRawQueryParameter(QUERY_ACTION_PARAM, QUERY_ACTION_PARAM_VALUE)
                .build();

        SignRequest signRequest = SignRequest.builder(credentials)
                .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, SERVICE_SIGNING_NAME)
                .putProperty(AwsV4HttpSigner.REGION_NAME, region.id())
                .putProperty(AwsV4HttpSigner.AUTH_LOCATION, AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
                .putProperty(AwsV4HttpSigner.EXPIRATION_DURATION, expiry)
                .request(sdkHttpRequest)
                .build();

        SdkHttpRequest signedRequest = signer.sign(signRequest).request();
        String uri = signedRequest.getUri().toString();

        String strippedUri = uri.startsWith(HTTPS_PREFIX)
                ? uri.substring(HTTPS_PREFIX.length())
                : uri;

        String encodedString = Base64.getEncoder().encodeToString(
                (strippedUri + TOKEN_VERSION).getBytes(StandardCharsets.UTF_8)
        );

        return AUTH_PREFIX + encodedString;
    }

    /**
     * Generates a bearer token using AWS credentials and region string.
     * Expiry duration defaults to 12 hours.
     * @param credentials AWS credentials.
     * @param region Region name (e.g., "us-east-1").
     * @return A bearer token string.
     * @throws NullPointerException if credentials or region are null
     * @throws IllegalArgumentException if region name is invalid
     */
    public String getToken(AwsCredentials credentials, String region) {
        Objects.requireNonNull(credentials, "Credentials must not be null");
        Objects.requireNonNull(region, "Region must not be null");
        return getToken(credentials, Region.of(region), Duration.ofHours(12));
    }

    /**
     * Validates the expiry duration or returns the default of 12 hours.
     *
     * @param expiry Expiry duration to validate.
     * @return A valid duration (1 second to 12 hours).
     * @throws IllegalArgumentException if expiry is invalid.
     */
    private static Duration validateOrDefault(Duration expiry) {
        if (expiry == null) {
            return DEFAULT_EXPIRY;
        }
        if (expiry.compareTo(Duration.ZERO) <= 0 || expiry.compareTo(MAX_EXPIRY) > 0) {
            throw new IllegalArgumentException("Expiry duration must be greater than 0 and less than or equal to 12 hours.");
        }
        return expiry;
    }

    /**
     * Returns a builder instance for creating a BedrockTokenGenerator with custom configuration.
     *
     * @return A new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for BedrockTokenGenerator.
     * Allows fluent creation of the generator with optional region, credentials provider, and expiry.
     */
    public static class Builder {
        private Region region;
        private AwsCredentialsProvider provider;
        private Duration expiry;

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder credentialsProvider(AwsCredentialsProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder expiry(Duration expiry) {
            this.expiry = expiry;
            return this;
        }

        /**
         * Builds a BedrockTokenGenerator with the configured parameters.
         * 
         * @return A new BedrockTokenGenerator instance
         * @throws SdkClientException if default region or credentials provider cannot be initialized
         * @throws NullPointerException if region or credentials provider cannot be resolved
         * @throws IllegalArgumentException if expiry is less than or equal to 0 or greater than 12 hours
         */
        public BedrockTokenGenerator build() {
            return new BedrockTokenGenerator(region, provider, expiry);
        }
    }
}
