# AWS Bedrock Token Generator for Java

[![Build Status](https://github.com/aws/aws-bedrock-token-generator-java/workflows/Build/badge.svg)](https://github.com/aws/aws-bedrock-token-generator-java/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/software.amazon.bedrock/aws-bedrock-token-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/software.amazon.bedrock/aws-bedrock-token-generator)
[![Apache 2.0 License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

The **AWS Bedrock Token Generator for Java** is a lightweight utility library that generates short-term bearer tokens for AWS Bedrock API authentication. This library simplifies the process of creating secure, time-limited tokens that can be used to authenticate with AWS Bedrock services without exposing long-term credentials.

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>software.amazon.bedrock</groupId>
    <artifactId>aws-bedrock-token-generator</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
implementation 'software.amazon.bedrock:aws-bedrock-token-generator:1.1.0'
```

## Quick Start

NOTE - You may specify a custom token duration (e.g., 1 hour, 6 hours), but the actual token lifetime will be:
min(specified duration, credentials expiry, 12 hours). Default is set to 12 hours

### Usage 1 - Using Default Providers

```java
import software.amazon.bedrock.token.BedrockTokenGenerator;

// Credentials and region will be picked up from the default provider chain
BedrockTokenGenerator tokenGenerator = BedrockTokenGenerator.builder().build();
tokenGenerator.getToken();
```

### Usage 2 - Passing in Provider and Region

```java
import software.amazon.bedrock.token.BedrockTokenGenerator;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

// Example provider STS Assume Role credentials provider
AwsCredentialsProvider assumeRoleProvider = StsAssumeRoleCredentialsProvider.builder()
        .refreshRequest(AssumeRoleRequest.builder()
                .roleArn("arn:aws:iam::123456789012:role/BedrockRole")
                .roleSessionName("bedrock-token-session")
                .durationSeconds(3600) // 1 hour
                .build())
        .build();

        // Use provider and region with the token generator
        BedrockTokenGenerator tokenGenerator = BedrockTokenGenerator.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(assumeRoleProvider)
                .build();

tokenGenerator.getToken();
```

### Usage 3 - creating token using static method by passing Credentials, Region, and Expiry (Optional)
```java

import software.amazon.bedrock.token.BedrockTokenGenerator;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import java.time.Duration;

// Resolve credentials from default provider for example
AwsCredentials credentials = DefaultCredentialsProvider.create().resolveCredentials();

// Generate bearer token using static method
String bearerToken = BedrockTokenGenerator.getToken(
        credentials,
        Region.US_WEST_2,
        Duration.ofHours(12)
);
```

## API Reference

### BedrockTokenGenerator

#### Static Method: `getToken(AwsCredentials credentials, Region region, Duration expiry)`

Generates a bearer token for AWS Bedrock API authentication using static method.

**Parameters:**
- `credentials` (AwsCredentials): AWS credentials to use for signing
- `region` (Region): AWS region object (e.g., Region.US_WEST_2)
- `expiry` (Duration): Token expiration duration (e.g., Duration.ofHours(12))

**Returns:**
- `String`: A bearer token valid for specified duration, prefixed with "bedrock-api-key-"

**Example:**
```java
String token = BedrockTokenGenerator.getToken(credentials, Region.US_WEST_2, Duration.ofHours(12));
```

#### Builder Pattern: `builder()`

Creates a BedrockTokenGenerator instance using the builder pattern.

**Builder Methods:**
- `region(Region region)`: Set the AWS region
- `credentialsProvider(AwsCredentialsProvider provider)`: Set credentials provider
- `expiry(Duration expiry)`: Set token expiration duration
- `build()`: Create the BedrockTokenGenerator instance

**Instance Method:**
- `getToken()`: Generate token using configured settings

**Example:**
```java
BedrockTokenGenerator generator = BedrockTokenGenerator.builder()
    .region(Region.US_EAST_1)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .expiry(Duration.ofHours(6))
    .build();
String token = generator.getToken();
```

## Token Format

The generated tokens follow this format:
```
bedrock-api-key-<base64-encoded-presigned-url>&Version=1
```

- **Prefix**: `bedrock-api-key-` identifies the token type
- **Payload**: Base64-encoded presigned URL with embedded credentials
- **Version**: `&Version=1` for future compatibility
- **Expiration**: The token has a default expiration of 12 hour. If the expiresIn parameter is specified during token creation, the expiration can be configured up to a maximum of 12 hours. However, the actual token validity period will always
  be the minimum of the requested expiration time and the AWS credentials' expiry time

## Security Considerations

- **Token Expiration**: The token has a default expiration of 12 hour. If the expiry parameter is specified during token creation, the expiration can be configured up to a maximum of 12 hours. However, the actual token validity period will always
  be the minimum of the requested expiration time and the AWS credentials' expiry time. The token must be generated again once it expires,
  as it cannot be refreshed or extended
- **Secure Storage**: Store tokens securely and avoid logging them
- **Credential Management**: Use IAM roles and temporary credentials when possible
- **Network Security**: Always use HTTPS when transmitting tokens
- **Principle of Least Privilege**: Ensure underlying credentials have minimal required permissions

## Requirements

- **Java**: 8 or later
- **AWS SDK**: 2.25.28 or later
- **Dependencies**: Minimal - only AWS SDK auth and HTTP components

## Examples

### Complete Example with Error Handling

```java
import software.amazon.bedrock.token.BedrockTokenGenerator;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.exception.SdkException;
import java.time.Duration;

public class BedrockTokenExample {
    public static void main(String[] args) {
        try {
            // Using static method
            String token = BedrockTokenGenerator.getToken(
                DefaultCredentialsProvider.create().resolveCredentials(),
                Region.US_WEST_2,
                Duration.ofHours(12)
            );
            
            System.out.println("Successfully generated token: " + 
                token.substring(0, 30) + "...");
            
        } catch (SdkException e) {
            System.err.println("Failed to generate token: " + e.getMessage());
        }
    }
}
```

### Using with Different Credential Providers

```java
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import java.time.Duration;

// Default credentials (recommended)
AwsCredentials defaultCreds = DefaultCredentialsProvider.create().resolveCredentials();

// Environment variables
AwsCredentials envCreds = EnvironmentVariableCredentialsProvider.create().resolveCredentials();

// System properties
AwsCredentials sysCreds = SystemPropertyCredentialsProvider.create().resolveCredentials();

// Profile-based credentials
AwsCredentials profileCreds = ProfileCredentialsProvider.create("my-profile").resolveCredentials();

// Generate tokens with any credential provider using static method
String token = BedrockTokenGenerator.getToken(defaultCreds, Region.US_WEST_2, Duration.ofHours(12));
```

### Using Builder with Different Configurations

```java
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import java.time.Duration;

// Builder with custom expiry
BedrockTokenGenerator shortLivedGenerator = BedrockTokenGenerator.builder()
    .region(Region.US_EAST_1)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .expiry(Duration.ofHours(1))
    .build();

BedrockTokenGenerator defaultGenerator = BedrockTokenGenerator.builder()
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build();

String shortToken = shortLivedGenerator.getToken();
String defaultToken = defaultGenerator.getToken();
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/aws/aws-bedrock-token-generator-java.git
cd aws-bedrock-token-generator-java

# Build with Maven
mvn clean compile

# Run tests
mvn test

# Create JAR
mvn package
```

The build will generate:
- `aws-bedrock-token-generator-1.1.0.jar` - Main library with dependencies
- `aws-bedrock-token-generator-1.1.0-sources.jar` - Source code
- `aws-bedrock-token-generator-1.1.0-javadoc.jar` - API documentation

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to contribute to this project.

### Development Setup

1. **Prerequisites**: Java 8+, Maven 3.6+
2. **Clone**: `git clone https://github.com/aws/aws-bedrock-token-generator-java.git`
3. **Build**: `mvn clean compile`
4. **Test**: `mvn test`
   ****5. **Package**: `mvn package`****

## Support

- **Documentation**: [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)
- **Issues**: [GitHub Issues](https://github.com/aws/aws-bedrock-token-generator-java/issues)
- **AWS Support**: [AWS Support Center](https://console.aws.amazon.com/support/)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Related Projects

- [AWS SDK for Java](https://github.com/aws/aws-sdk-java-v2)
- [AWS S3 Access Grants Plugin](https://github.com/aws/aws-s3-accessgrants-plugin-java-v2)
- [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes and version history.
