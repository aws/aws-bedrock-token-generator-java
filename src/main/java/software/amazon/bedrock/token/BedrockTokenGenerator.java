/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.bedrock.token;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Utility class for generating bearer tokens for AWS Bedrock API authentication.
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
    private static final Duration TOKEN_DURATION = Duration.ofHours(12);

    /**
     * Default constructor.
     */
    public BedrockTokenGenerator() {
        // Default constructor for future extensibility
    }

    /**
     * Generates a bearer token for AWS Bedrock API authentication.
     *
     * @param credentials AWS credentials to use for signing
     * @param region AWS region to use for the token
     * @return A bearer token string valid for 12 hours, with version information embedded
     */
    public String getToken(AwsCredentials credentials, String region) {
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
                .putProperty(AwsV4HttpSigner.REGION_NAME, region)
                .putProperty(AwsV4HttpSigner.AUTH_LOCATION, AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
                .putProperty(AwsV4HttpSigner.EXPIRATION_DURATION, TOKEN_DURATION)
                .request(sdkHttpRequest)
                .build();

        SdkHttpRequest signedRequest = signer.sign(signRequest).request();
        String uri = signedRequest.getUri().toString();

        String encodedString = Base64.getEncoder().
                encodeToString((uri.replaceFirst("^https://", "") + TOKEN_VERSION).
                        getBytes(StandardCharsets.UTF_8));

        return AUTH_PREFIX + encodedString;
    }
}
