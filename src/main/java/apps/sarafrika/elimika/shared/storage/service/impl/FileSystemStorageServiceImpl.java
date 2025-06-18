package apps.sarafrika.elimika.shared.storage.service.impl;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageException;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageFileNotFoundException;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileSystemStorageServiceImpl implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageServiceImpl(StorageProperties properties) {
        if (properties.getLocation().trim().isEmpty()) {
            throw new StorageException("Storage location is not configured.");
        }

        this.rootLocation = Path.of(properties.getLocation());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Failed to initialize storage.", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store file. File is empty.");
            }

            String uniqueFileName = UUID.randomUUID().toString();

            Path targetLocation = rootLocation.resolve(uniqueFileName).normalize().toAbsolutePath();

            if (!targetLocation.getParent().equals(rootLocation.toAbsolutePath())) {
                throw new StorageException("File is not stored in the configured location.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

                return uniqueFileName.toString();
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public Resource load(String fileName) {
        try {
            Path targetLocation = rootLocation.resolve(fileName);

            Resource resource = new UrlResource(targetLocation.toUri());

            if (resource.exists() || resource.isReadable()) {

                return resource;
            }

            throw new StorageFileNotFoundException("Could not read file: " + fileName);
        } catch (MalformedURLException e) {

            throw new StorageFileNotFoundException("Could not read file: " + fileName, e);
        }
    }
}
