/**
 * The Course Module provides comprehensive course and training program management for the Elimika platform.
 * 
 * This module follows Spring Modulith principles and serves as the core educational content management system.
 * It provides extensive functionality for creating, managing, and delivering educational content including:
 * - Course and training program lifecycle management
 * - Lesson content creation and organization
 * - Assessment and quiz management with rubric-based scoring
 * - Assignment and submission tracking
 * - Student progress monitoring and analytics
 * - Certificate generation and management
 * - Multi-media content support with validation
 * 
 * Key Features:
 * - Course Management: Complete CRUD operations for courses and training programs
 * - Content Organization: Hierarchical lesson structure with rich media support  
 * - Assessment System: Comprehensive quiz, assignment, and rubric-based evaluation
 * - Progress Tracking: Detailed student progress monitoring across content and lessons
 * - Enrollment Management: Course enrollment and student lifecycle management
 * - Certificate System: Automated certificate generation with customizable templates
 * - Category Management: Flexible course categorization and organization
 * - Requirements Engine: Course prerequisite and completion requirement management
 * - Media Management: File upload, validation, and storage for course materials
 * 
 * Module Boundaries:
 * - Owns: Courses, lessons, assessments, rubrics, assignments, certificates, enrollments, progress tracking
 * - Does Not Own: User authentication, class scheduling, instructor availability, live session management
 * 
 * Key Components:
 * - Course: Core course entity with instructor assignments and content status
 * - TrainingProgram: Structured learning programs containing multiple courses
 * - Lesson & LessonContent: Hierarchical content organization with rich media
 * - Quiz & QuizQuestion: Interactive assessment tools with multiple question types
 * - Assignment & AssignmentSubmission: Project-based learning with submission tracking
 * - AssessmentRubric & RubricScoring: Standardized evaluation criteria and scoring
 * - Certificate & CertificateTemplate: Achievement recognition and credential management
 * - CourseEnrollment & ProgramEnrollment: Student registration and access control
 * - ContentProgress & LessonProgress: Detailed learning analytics and completion tracking
 * 
 * Assessment & Evaluation:
 * - Multi-format quiz support (multiple choice, true/false, essay)
 * - Rubric-based assessment with customizable criteria and scoring levels
 * - Assignment submission and grading workflows
 * - Comprehensive progress tracking at content and lesson levels
 * - Automated certificate generation upon course completion
 * 
 * Integration Points:
 * - Media storage integration for course materials and submissions
 * - Progress analytics and reporting capabilities
 * - Event-driven architecture for enrollment and completion notifications
 * - Integration with instructor and student modules for role-based access
 * - Support for categorization and search functionality
 * 
 * Content Management:
 * - Rich media support with validation (videos, documents, images)
 * - Hierarchical lesson organization with prerequisites
 * - Content status management (draft, published, archived)
 * - Version control and content lifecycle management
 * 
 * @since 1.0.0
 */
@ApplicationModule(
    allowedDependencies = {"shared", "coursecreator :: coursecreator-spi", "tenancy :: tenancy-spi"}
)
package apps.sarafrika.elimika.course;

import org.springframework.modulith.ApplicationModule;
