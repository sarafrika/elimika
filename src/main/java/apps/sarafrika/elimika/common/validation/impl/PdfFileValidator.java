package apps.sarafrika.elimika.common.validation.impl;

import apps.sarafrika.elimika.common.validation.PdfFile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class PdfFileValidator implements ConstraintValidator<PdfFile, Object> {

    @Override
    public void initialize(PdfFile constraintAnnotation) {
        // Initialization logic if needed
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        switch (value) {
            case null -> {
                return true; // null value is considered valid
            }


            // If it's a list, validate each file in the list
            case List<?> files -> {
                for (Object fileObj : files) {
                    if (fileObj instanceof MultipartFile multipartFile) {
                        if (!isValidPdf(multipartFile)) {
                            return false; // return false if any file is not a PDF
                        }
                    } else {
                        return false; // not a valid MultipartFile
                    }
                }
                return true; // all files are valid PDFs
            }


            // If it's a single file, validate the file directly
            case MultipartFile multipartFile -> {
                return isValidPdf(multipartFile);
            }
            default -> {
                return false; // not a valid type
            }
        }
    }

    // Helper method to check if the file is a PDF
    private boolean isValidPdf(MultipartFile file) {
        return file != null && "application/pdf".equals(file.getContentType());
    }
}

