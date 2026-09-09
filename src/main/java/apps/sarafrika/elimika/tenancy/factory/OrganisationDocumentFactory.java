package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.OrganisationDocumentDTO;
import apps.sarafrika.elimika.tenancy.entity.OrganisationDocument;

public class OrganisationDocumentFactory {

    private OrganisationDocumentFactory() {
    }

    public static OrganisationDocumentDTO toDTO(OrganisationDocument entity) {
        if (entity == null) {
            return null;
        }

        return new OrganisationDocumentDTO(
                entity.getUuid(),
                entity.getOrganisationUuid(),
                entity.getDocumentTypeUuid(),
                entity.getOriginalFilename(),
                entity.getStoredFilename(),
                entity.getFilePath(),
                entity.getFileSizeBytes(),
                entity.getMimeType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getUploadDate(),
                entity.getIsVerified(),
                entity.getStatus(),
                entity.getExpiryDate(),
                entity.getCreatedDate()
        );
    }

    public static OrganisationDocument toEntity(OrganisationDocumentDTO dto) {
        if (dto == null) {
            return null;
        }

        OrganisationDocument entity = new OrganisationDocument();
        entity.setOrganisationUuid(dto.organisationUuid());
        entity.setDocumentTypeUuid(dto.documentTypeUuid());
        entity.setOriginalFilename(dto.originalFilename());
        entity.setStoredFilename(dto.storedFilename());
        entity.setFilePath(dto.filePath());
        entity.setFileSizeBytes(dto.fileSizeBytes());
        entity.setMimeType(dto.mimeType());
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setExpiryDate(dto.expiryDate());
        return entity;
    }
}
