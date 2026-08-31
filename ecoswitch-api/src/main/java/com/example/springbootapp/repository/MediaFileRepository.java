package com.example.springbootapp.repository;

import com.example.springbootapp.model.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    Optional<MediaFile> findByFolderAndFileName(String folder, String fileName);
}
