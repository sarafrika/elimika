package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.QuizQuestionOptionDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizQuestionDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizQuestionOptionDTO;
import apps.sarafrika.elimika.course.service.QuizAttemptService;
import apps.sarafrika.elimika.course.service.QuizQuestionOptionService;
import apps.sarafrika.elimika.course.service.QuizQuestionService;
import apps.sarafrika.elimika.course.service.QuizService;
import apps.sarafrika.elimika.course.service.StudentQuizSubmissionService;
import apps.sarafrika.elimika.course.service.StudentQuizViewService;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import apps.sarafrika.elimika.course.util.enums.QuizScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock
    private QuizService quizService;
    @Mock
    private QuizQuestionService quizQuestionService;
    @Mock
    private QuizQuestionOptionService quizQuestionOptionService;
    @Mock
    private QuizAttemptService quizAttemptService;
    @Mock
    private StudentQuizViewService studentQuizViewService;
    @Mock
    private StudentQuizSubmissionService studentQuizSubmissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        QuizController controller = new QuizController(
                quizService,
                quizQuestionService,
                quizQuestionOptionService,
                quizAttemptService,
                studentQuizViewService,
                studentQuizSubmissionService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void studentQuizViewOmitsConfiguredAnswerKeyFields() throws Exception {
        UUID quizUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID questionUuid = UUID.randomUUID();
        UUID optionUuid = UUID.randomUUID();

        StudentQuizDTO quiz = new StudentQuizDTO(
                quizUuid,
                lessonUuid,
                QuizScope.COURSE_TEMPLATE,
                null,
                "Safety quiz",
                "Answer key must stay hidden",
                "Choose carefully",
                15,
                1,
                BigDecimal.valueOf(70),
                List.of(new StudentQuizQuestionDTO(
                        questionUuid,
                        quizUuid,
                        "Which option is safe?",
                        QuestionType.MULTIPLE_CHOICE,
                        BigDecimal.ONE,
                        1,
                        List.of(new StudentQuizQuestionOptionDTO(
                                optionUuid,
                                questionUuid,
                                "Student-visible option text",
                                1
                        ))
                ))
        );

        when(studentQuizViewService.getStudentQuiz(quizUuid, enrollmentUuid)).thenReturn(quiz);

        mockMvc.perform(get("/api/v1/quizzes/{quizUuid}/student-view", quizUuid)
                        .param("enrollment_uuid", enrollmentUuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].options[0].option_text")
                        .value("Student-visible option text"))
                .andExpect(jsonPath("$.data.questions[0].options[0].is_correct").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].is_incorrect").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].correctness_status").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].option_category").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].option_summary").doesNotExist());
    }

    @Test
    void managementOptionEndpointStillReturnsConfiguredCorrectness() throws Exception {
        UUID quizUuid = UUID.randomUUID();
        UUID questionUuid = UUID.randomUUID();
        UUID optionUuid = UUID.randomUUID();

        QuizQuestionOptionDTO option = new QuizQuestionOptionDTO(
                optionUuid,
                questionUuid,
                "Correct answer",
                true,
                1,
                null,
                null,
                null,
                null
        );

        when(quizQuestionOptionService.search(eq(Map.of("questionUuid", questionUuid.toString())), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(option)));

        mockMvc.perform(get("/api/v1/quizzes/{quizUuid}/questions/{questionUuid}/options", quizUuid, questionUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].is_correct").value(true))
                .andExpect(jsonPath("$.data.content[0].correctness_status").value("Correct Answer"));
    }
}
