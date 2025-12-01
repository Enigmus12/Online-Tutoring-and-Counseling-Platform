package UpLearn.eci.edu.co.service.impl;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.specialized.BlockBlobClient;

@Service
public class AzureBlobStorageService {

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public AzureBlobStorageService(BlobServiceClient blobServiceClient,
                                   @Value("${azure.storage.credentials-container}") String containerName) {
        this.blobServiceClient = blobServiceClient;
        this.containerName = containerName;
    }

    public List<String> uploadFiles(String userId, List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String blobName = buildBlobName(userId, file.getOriginalFilename());
            BlockBlobClient blobClient = containerClient.getBlobClient(blobName).getBlockBlobClient();
            // Convertimos a un ByteArrayInputStream para asegurar soporte de mark/reset.
            byte[] data = file.getBytes();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                blobClient.upload(bais, data.length, true);
            }
            urls.add(blobClient.getBlobUrl());
        }
        return urls;
    }

    private String buildBlobName(String userId, String originalName) {
        String cleanName = originalName == null ? "file" : originalName.replaceAll("\\s+", "_");
        return userId + "/credentials/" + UUID.randomUUID() + "_" + cleanName;
    }

    /**
     * Elimina un blob del contenedor a partir de su URL completa.
     * Retorna true si el blob fue eliminado o no existía, false si hubo error de parsing
     * o si la URL no corresponde al contenedor configurado.
     */
    public boolean deleteByUrl(String blobUrl) {
        if (blobUrl == null || blobUrl.isBlank()) return false;
        try {
            URI uri = new URI(blobUrl);
            String path = uri.getPath(); // /container/blobName
            if (path == null || path.isBlank()) return false;

            String needle = "/" + containerName + "/";
            int idx = path.indexOf(needle);
            if (idx < 0) return false;
            String blobName = path.substring(idx + needle.length());
            if (blobName.isBlank()) return false;

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            return containerClient.getBlobClient(blobName).deleteIfExists();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}