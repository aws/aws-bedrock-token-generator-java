# AWS Bedrock Token Generator for Java

[![Build Status](https://github.com/aws/aws-bedrock-token-generator-java/workflows/Build/badge.svg)](https://github.com/aws/aws-bedrock-token-generator-java/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/software.amazon.bedrock/aws-bedrock-token-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/software.amazon.bedrock/aws-bedrock-token-generator)
[![Apache 2.0 License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

The **AWS Bedrock Token Generator for Java** is a lightweight utility library that generates short-term bearer tokens for AWS Bedrock API authentication. This library simplifies the process of creating secure, time-limited tokens that can be used to authenticate with AWS Bedrock services without exposing long-term credentials.

## Features

- ✅ **Simple API**: Single method to generate bearer tokens
- ✅ **Secure**: Uses AWS SigV4 signing with 12-hour token expiration
- ✅ **Multi-region support**: Works with any AWS region where Bedrock is available
- ✅ **AWS SDK Integration**: Seamlessly works with AWS SDK credential providers
- ✅ **Lightweight**: Minimal dependencies, focused functionality
- ✅ **Well-tested**: Comprehensive unit tests with multiple scenarios

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>software.amazon.bedrock</groupId>
    <artifactId>aws-bedrock-token-generator</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
implementation 'software.amazon.bedrock:aws-bedrock-token-generator:1.0.0'
```

## Quick Start

### Basic Usage

```java
import software.amazon.bedrock.token.BedrockTokenGenerator;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

// Create token generator
BedrockTokenGenerator tokenGenerator = new BedrockTokenGenerator();

// Generate token using default credentials
String bearerToken = tokenGenerator.getToken(
    DefaultCredentialsProvider.create().resolveCredentials(),
    Region.US_WEST_2.id()
);

// Use the token for API calls (valid for 12 hours)
System.out.println("Bearer Token: " + bearerToken);
```

### Using with Specific Credentials

```java
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

// Create specific credentials
AwsBasicCredentials credentials = AwsBasicCredentials.create(
    "your-access-key-id",
    "your-secret-access-key"
);

// Generate token
BedrockTokenGenerator tokenGenerator = new BedrockTokenGenerator();
String bearerToken = tokenGenerator.getToken(credentials, "us-east-1");
```

## API Reference

### BedrockTokenGenerator

#### `getToken(AwsCredentials credentials, String region)`

Generates a bearer token for AWS Bedrock API authentication.

**Parameters:**
- `credentials` (AwsCredentials): AWS credentials to use for signing
- `region` (String): AWS region identifier (e.g., "us-west-2")

**Returns:**
- `String`: A bearer token valid for 12 hours, prefixed with "bedrock-api-key-"

**Example:**
```java
BedrockTokenGenerator generator = new BedrockTokenGenerator();
String token = generator.getToken(credentials, "us-west-2");
```

## Token Format

The generated tokens follow this format:
```
bedrock-api-key-<base64-encoded-presigned-url>&Version=1
```

- **Prefix**: `bedrock-api-key-` identifies the token type
- **Payload**: Base64-encoded presigned URL with embedded credentials
- **Version**: `&Version=1` for future compatibility
- **Expiration**: 12 hours from generation time

## Security Considerations

- **Token Expiration**: Tokens are valid for 12 hours and cannot be renewed
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
import software.amazon.awssdk.core.exception.SdkException;

public class BedrockTokenExample {
    public static void main(String[] args) {
        try {
            BedrockTokenGenerator tokenGenerator = new BedrockTokenGenerator();
            
            String token = tokenGenerator.getToken(
                DefaultCredentialsProvider.create().resolveCredentials(),
                "us-west-2"
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

// Default credentials (recommended)
AwsCredentials defaultCreds = DefaultCredentialsProvider.create().resolveCredentials();

// Environment variables
AwsCredentials envCreds = EnvironmentVariableCredentialsProvider.create().resolveCredentials();

// System properties
AwsCredentials sysCreds = SystemPropertyCredentialsProvider.create().resolveCredentials();

// Profile-based credentials
AwsCredentials profileCreds = ProfileCredentialsProvider.create("my-profile").resolveCredentials();

// Generate tokens with any credential provider
BedrockTokenGenerator generator = new BedrockTokenGenerator();
String token = generator.getToken(defaultCreds, "us-west-2");
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
- `aws-bedrock-token-generator-1.0.0.jar` - Main library with dependencies
- `aws-bedrock-token-generator-1.0.0-sources.jar` - Source code
- `aws-bedrock-token-generator-1.0.0-javadoc.jar` - API documentation

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to contribute to this project.

### Development Setup

1. **Prerequisites**: Java 8+, Maven 3.6+
2. **Clone**: `git clone https://github.com/aws/aws-bedrock-token-generator-java.git`
3. **Build**: `mvn clean compile`
4. **Test**: `mvn test`
5. **Package**: `mvn package`

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
