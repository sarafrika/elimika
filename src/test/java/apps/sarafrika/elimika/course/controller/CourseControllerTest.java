package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import apps.sarafrika.elimika.course.internal.LessonMediaValidationService;
import apps.sarafrika.elimika.course.service.*;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;
    @Mock
    private LessonService lessonService;
    @Mock
    private LessonContentService lessonContentService;
    @Mock
    private CourseAssessmentService courseAssessmentService;
    @Mock
    private CourseRequirementService courseRequirementService;
    @Mock
    private CourseTrainingRequirementService courseTrainingRequirementService;
    @Mock
    private CourseTrainingApplicationService courseTrainingApplicationService;
    @Mock
    private CourseEnrollmentService courseEnrollmentService;
    @Mock
    private CourseCategoryService courseCategoryService;
    @Mock
    private CourseReviewService courseReviewService;
    @Mock
    private StorageService storageService;
    @Mock
    private LessonMediaValidationService lessonMediaValidationService;

    private StorageProperties storageProperties;
    private CourseController courseController;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setBaseUrl("https://api.elimika.sarafrika.com");
        StorageProperties.Folders folders = new StorageProperties.Folders();
        folders.setCourseMaterials("course_materials");
        folders.setCourseThumbnails("course_thumbnails");
        storageProperties.setFolders(folders);

        courseController = new CourseController(
                courseService,
                lessonService,
                lessonContentService,
                courseAssessmentService,
                courseRequirementService,
                courseTrainingRequirementService,
                courseTrainingApplicationService,
                courseEnrollmentService,
                courseCategoryService,
                courseReviewService,
                storageService,
                storageProperties,
                lessonMediaValidationService
        );
    }

    @Test
    void uploadLessonMediaBuildsPreviewUrlThroughContentMediaEndpoint() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID contentTypeUuid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "intro.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );
        String storedPath = "course_materials/" + courseUuid + "/lessons/" + lessonUuid + "/intro.pdf";

        doNothing().when(lessonMediaValidationService).validateForLessonContent(file);
        when(storageService.store(file, "course_materials/" + courseUuid + "/lessons/" + lessonUuid))
                .thenReturn(storedPath);
        when(storageService.getContentType(storedPath)).thenReturn(MediaType.APPLICATION_PDF_VALUE);
        when(lessonContentService.createLessonContent(any(LessonContentDTO.class)))
                .thenAnswer(invocation -> {
                    LessonContentDTO request = invocation.getArgument(0);
                    return new LessonContentDTO(
                            UUID.randomUUID(),
                            request.lessonUuid(),
                            request.contentTypeUuid(),
                            request.title(),
                            request.description(),
                            request.contentText(),
                            request.fileUrl(),
                            request.fileSizeBytes(),
                            request.mimeType(),
                            1,
                            request.isRequired(),
                            null,
                            null,
                            null,
                            null
                    );
                });

        ResponseEntity<ApiResponse<LessonContentDTO>> response = courseController.uploadLessonMedia(
                courseUuid,
                lessonUuid,
                file,
                contentTypeUuid,
                "Lesson PDF",
                "Previewable handout",
                true
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().data());
        assertEquals(
                "https://api.elimika.sarafrika.com/api/v1/courses/content-media/" + storedPath,
                response.getBody().data().fileUrl()
        );
        assertEquals(MediaType.APPLICATION_PDF_VALUE, response.getBody().data().mimeType());
    }

    @Test
    void getCourseContentMediaServesStoredLessonFileInline() {
        String storedPath = "course_materials/course-uuid/lessons/lesson-uuid/video.mp4";
        ByteArrayResource resource = new ByteArrayResource("video".getBytes());

        when(storageService.load(storedPath)).thenReturn(resource);
        when(storageService.getContentType(storedPath)).thenReturn("video/mp4");

        ResponseEntity<org.springframework.core.io.Resource> response =
                courseController.getCourseContentMedia(storedPath);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("video/mp4"), response.getHeaders().getContentType());
        assertEquals(
                "inline; filename=\"video.mp4\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        assertSame(resource, response.getBody());
    }
}
