package com.example.springbootapp.model.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "media_files", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"folder", "file_name"})
})
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folder", nullable = false, length = 64)
    private String folder;

    @Column(name = "file_name", nullable = false, length = 128)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public MediaFile() {}

    public MediaFile(String folder, String fileName, String contentType, byte[] data) {
        this.folder = folder;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
