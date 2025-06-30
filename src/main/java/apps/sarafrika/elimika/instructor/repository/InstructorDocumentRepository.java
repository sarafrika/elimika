package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstructorDocumentRepository extends JpaRepository<InstructorDocument,Long>,
        JpaSpecificationExecutor<InstructorDocument> {
}
