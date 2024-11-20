package apps.sarafrika.elimika.shared.storage.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    void init();

    String store(MultipartFile file);

    Resource load(String fileName);
}
