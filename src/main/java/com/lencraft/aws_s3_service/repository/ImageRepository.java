package com.lencraft.aws_s3_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.lencraft.aws_s3_service.model.Image;

import java.util.Optional;


@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    void deleteByUrl(String url);

    Optional<Image> findByUrl(String url);
}
