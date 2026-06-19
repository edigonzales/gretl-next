package ch.so.agi.gretl.internal.s3;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class S3Engine {

    public void upload(S3UploadRequest request) {
        validateConnection(request.connection());
        if (request.sourceFiles() == null || request.sourceFiles().isEmpty()) {
            throw new IllegalArgumentException("sourceFile, sourceDir or sourceFiles must not be empty");
        }
        requireNotBlank(request.acl(), "acl must not be null");
        try (S3Client client = client(request.connection())) {
            for (Path file : request.sourceFiles()) {
                PutObjectRequest.Builder builder = PutObjectRequest.builder()
                        .bucket(request.connection().bucketName())
                        .key(file.getFileName().toString())
                        .metadata(emptyMap(request.metadata()))
                        .acl(ObjectCannedACL.fromValue(request.acl()));
                if (request.contentType() != null) {
                    builder.contentType(request.contentType());
                }
                client.putObject(builder.build(), RequestBody.fromFile(file));
            }
        }
    }

    public void download(S3DownloadRequest request) throws IOException {
        validateConnection(request.connection());
        requireNotNull(request.downloadDir(), "downloadDir must not be null");
        Files.createDirectories(request.downloadDir());
        try (S3Client client = client(request.connection())) {
            if (request.key() == null || request.key().isBlank()) {
                forEachObject(client, request.connection().bucketName(), object ->
                        downloadOne(client, request.connection().bucketName(), object.key(),
                                request.downloadDir().resolve(object.key())));
            } else {
                String fileName = request.key().substring(request.key().lastIndexOf('/') + 1);
                downloadOne(client, request.connection().bucketName(), request.key(), request.downloadDir().resolve(fileName));
            }
        }
    }

    public void delete(S3DeleteRequest request) {
        validateConnection(request.connection());
        try (S3Client client = client(request.connection())) {
            if (request.key() == null || request.key().isBlank()) {
                forEachObject(client, request.connection().bucketName(),
                        object -> client.deleteObject(builder -> builder
                                .bucket(request.connection().bucketName())
                                .key(object.key())));
            } else {
                client.deleteObject(builder -> builder
                        .bucket(request.connection().bucketName())
                        .key(request.key()));
            }
        }
    }

    public void copyBucket(S3BucketCopyRequest request) {
        validateCredentials(request.accessKey(), request.secretKey());
        requireNotBlank(request.sourceBucket(), "sourceBucket must not be null");
        requireNotBlank(request.targetBucket(), "targetBucket must not be null");
        requireNotBlank(request.acl(), "acl must not be null");

        S3ConnectionSpec source = new S3ConnectionSpec(request.accessKey(), request.secretKey(),
                request.sourceBucket(), request.endpoint(), request.region());
        try (S3Client client = client(source)) {
            forEachObject(client, request.sourceBucket(), object -> {
                CopyObjectRequest.Builder builder = CopyObjectRequest.builder()
                        .copySource(request.sourceBucket() + "/" + object.key())
                        .destinationBucket(request.targetBucket())
                        .destinationKey(object.key())
                        .acl(ObjectCannedACL.fromValue(request.acl()));
                if (request.metadata() != null && !request.metadata().isEmpty()) {
                    builder.metadata(request.metadata());
                    builder.metadataDirective(MetadataDirective.REPLACE);
                }
                client.copyObject(builder.build());
            });
        }
    }

    private static void downloadOne(S3Client client, String bucketName, String key, Path target) {
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build(), target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void forEachObject(S3Client client, String bucketName, ObjectConsumer consumer) {
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucketName).build();
        ListObjectsV2Response response;
        do {
            response = client.listObjectsV2(request);
            for (S3Object object : response.contents()) {
                consumer.accept(object);
            }
            request = request.toBuilder().continuationToken(response.nextContinuationToken()).build();
        } while (response.isTruncated());
    }

    private static S3Client client(S3ConnectionSpec spec) {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(spec.accessKey(), spec.secretKey())))
                .region(Region.of(defaultString(spec.region(), "eu-central-1")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());
        if (spec.endpoint() != null && !spec.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(spec.endpoint()));
        }
        return builder.build();
    }

    private static void validateConnection(S3ConnectionSpec spec) {
        Objects.requireNonNull(spec, "connection must not be null");
        validateCredentials(spec.accessKey(), spec.secretKey());
        requireNotBlank(spec.bucketName(), "bucketName must not be null");
    }

    private static void validateCredentials(String accessKey, String secretKey) {
        requireNotBlank(accessKey, "accessKey must not be null");
        requireNotBlank(secretKey, "secretKey must not be null");
    }

    private static void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Map<String, String> emptyMap(Map<String, String> value) {
        return value == null ? Map.of() : value;
    }

    private interface ObjectConsumer {
        void accept(S3Object object);
    }
}
