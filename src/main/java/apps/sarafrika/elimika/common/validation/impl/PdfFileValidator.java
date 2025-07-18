package apps.sarafrika.elimika.common.validation.impl;

import apps.sarafrika.elimika.common.validation.PdfFile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class PdfFileValidator implements ConstraintValidator<PdfFile, Object> {

    @Override
    public void initialize(PdfFile constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        switch (value) {
            case null -> {
                return true;
            }

            case List<?> files -> {
                for (Object fileObj : files) {
                    if (fileObj instanceof MultipartFile multipartFile) {
                        if (!isValidPdf(multipartFile)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                return true;
            }

            case MultipartFile multipartFile -> {
                return isValidPdf(multipartFile);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean isValidPdf(MultipartFile file) {
        return file != null && "application/pdf".equals(file.getContentType());
    }
}

