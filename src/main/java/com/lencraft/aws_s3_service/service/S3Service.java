package com.lencraft.aws_s3_service.service;

import com.lencraft.aws_s3_service.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import com.lencraft.aws_s3_service.model.Image;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;


@Service
public class S3Service {

    private final S3Client s3Client;
    private final ImageRepository imageRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client, ImageRepository imageRepository) {
        this.s3Client = s3Client;
        this.imageRepository = imageRepository;
    }

    /**
     * Upload file lên S3 và lưu URL vào database
     */
    public String uploadFile(File file) {
        try {
            String key = "uploads/" + UUID.randomUUID() + "-"+file.getName();
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(Files.probeContentType(file.toPath()))
                            .build(),
                    RequestBody.fromFile(file));

            // URL công khai
            String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + key;

            // Lưu URL vào database
            Image image = new Image(fileUrl);
            imageRepository.save(image);

            return fileUrl;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi upload file lên S3", e);
        }
    }

    /**
     * Xóa file trên S3 và database
     */
    public void deleteFile(String fileUrl) {
        try {
            // Lấy key từ URL
            String key = extractKeyFromUrl(fileUrl);

            // Xóa trên S3
            DeleteObjectResponse response = s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );

            if (response.sdkHttpResponse().isSuccessful()) {
                System.out.println("File đã xóa trên S3: " + fileUrl);

                // Xóa trên database
                Optional<Image> imageOptional = imageRepository.findByUrl(fileUrl);
                imageOptional.ifPresent(imageRepository::delete);
                System.out.println("Bản ghi đã xóa khỏi database: " + fileUrl);
            } else {
                throw new RuntimeException("Xóa file trên S3 thất bại");
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa file", e);
        }
    }

    /**
     * Trích xuất key từ URL S3
     */
    private String extractKeyFromUrl(String fileUrl) {
        String baseUrl = "https://" + bucketName + ".s3.amazonaws.com/";
        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        } else {
            throw new IllegalArgumentException("URL không hợp lệ: " + fileUrl);
        }
    }

}


