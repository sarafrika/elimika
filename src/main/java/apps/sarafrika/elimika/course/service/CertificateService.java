package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CertificateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for comprehensive certificate management operations.
 * Provides methods for certificate lifecycle management, generation, verification, and analytics.
 */
public interface CertificateService {

    // ===== BASIC CRUD OPERATIONS =====

    /**
     * Creates a new certificate record.
     *
     * @param certificateDTO the certificate data to create
     * @return the created certificate DTO
     */
    CertificateDTO createCertificate(CertificateDTO certificateDTO);

    /**
     * Retrieves a certificate by its UUID.
     *
     * @param uuid the certificate UUID
     * @return the certificate DTO
     * @throws apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException if certificate not found
     */
    CertificateDTO getCertificateByUuid(UUID uuid);

    /**
     * Retrieves all certificates with pagination.
     *
     * @param pageable pagination parameters
     * @return paginated certificate DTOs
     */
    Page<CertificateDTO> getAllCertificates(Pageable pageable);

    /**
     * Updates an existing certificate.
     *
     * @param uuid the certificate UUID to update
     * @param certificateDTO the updated certificate data
     * @return the updated certificate DTO
     * @throws apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException if certificate not found
     */
    CertificateDTO updateCertificate(UUID uuid, CertificateDTO certificateDTO);

    /**
     * Deletes a certificate by UUID.
     *
     * @param uuid the certificate UUID to delete
     * @throws apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException if certificate not found
     */
    void deleteCertificate(UUID uuid);

    /**
     * Searches certificates using dynamic criteria.
     *
     * @param searchParams search parameters map
     * @param pageable pagination parameters
     * @return paginated search results
     */
    Page<CertificateDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ===== CERTIFICATE GENERATION =====

    /**
     * Generates a certificate for course completion.
     *
     * @param studentUuid the student UUID
     * @param courseUuid the course UUID
     * @param finalGrade the final grade achieved
     * @return the generated certificate DTO
     * @throws IllegalStateException if student is not eligible or certificate already exists
     */
    CertificateDTO generateCourseCertificate(UUID studentUuid, UUID courseUuid, BigDecimal finalGrade);

    /**
     * Generates a certificate for program completion.
     *
     * @param studentUuid the student UUID
     * @param programUuid the program UUID
     * @param finalGrade the final grade achieved
     * @return the generated certificate DTO
     * @throws IllegalStateException if student is not eligible or certificate already exists
     */
    CertificateDTO generateProgramCertificate(UUID studentUuid, UUID programUuid, BigDecimal finalGrade);

    // ===== ELIGIBILITY CHECKS =====

    /**
     * Checks if a student is eligible for a course certificate.
     *
     * @param studentUuid the student UUID
     * @param courseUuid the course UUID
     * @return true if eligible, false otherwise
     */
    boolean isEligibleForCourseCertificate(UUID studentUuid, UUID courseUuid);

    /**
     * Checks if a student is eligible for a program certificate.
     *
     * @param studentUuid the student UUID
     * @param programUuid the program UUID
     * @return true if eligible, false otherwise
     */
    boolean isEligibleForProgramCertificate(UUID studentUuid, UUID programUuid);

    // ===== CERTIFICATE VERIFICATION =====

    /**
     * Verifies if a certificate is valid using its certificate number.
     *
     * @param certificateNumber the certificate number to verify
     * @return true if certificate is valid, false otherwise
     */
    boolean verifyCertificate(String certificateNumber);

    /**
     * Retrieves a certificate by its certificate number.
     *
     * @param certificateNumber the certificate number
     * @return the certificate DTO or null if not found
     */
    CertificateDTO getCertificateByNumber(String certificateNumber);

    // ===== CERTIFICATE MANAGEMENT =====

    /**
     * Revokes a certificate with a specified reason.
     *
     * @param certificateUuid the certificate UUID to revoke
     * @param reason the reason for revocation
     * @throws apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException if certificate not found
     */
    void revokeCertificate(UUID certificateUuid, String reason);

    /**
     * Generates and updates the certificate URL.
     *
     * @param certificateUuid the certificate UUID
     * @param certificateUrl the generated certificate URL
     * @return the updated certificate DTO
     * @throws apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException if certificate not found
     */
    CertificateDTO generateCertificateUrl(UUID certificateUuid, String certificateUrl);

    // ===== STUDENT CERTIFICATES =====

    /**
     * Retrieves all certificates for a specific student.
     *
     * @param studentUuid the student UUID
     * @return list of certificate DTOs
     */
    List<CertificateDTO> getCertificatesByStudent(UUID studentUuid);

    /**
     * Retrieves all downloadable certificates for a specific student.
     *
     * @param studentUuid the student UUID
     * @return list of downloadable certificate DTOs
     */
    List<CertificateDTO> getDownloadableCertificates(UUID studentUuid);

    /**
     * Retrieves all valid certificates for a specific student.
     *
     * @param studentUuid the student UUID
     * @return list of valid certificate DTOs
     */
    List<CertificateDTO> getValidCertificates(UUID studentUuid);

    // ===== CERTIFICATE ANALYTICS =====

    /**
     * Retrieves all certificates issued for course completions.
     *
     * @return list of course certificate DTOs
     */
    List<CertificateDTO> getCourseCertificates();

    /**
     * Retrieves all certificates issued for program completions.
     *
     * @return list of program certificate DTOs
     */
    List<CertificateDTO> getProgramCertificates();

    /**
     * Retrieves all revoked certificates.
     *
     * @return list of revoked certificate DTOs
     */
    List<CertificateDTO> getRevokedCertificates();
}