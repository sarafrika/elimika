-- 202507021800__insert_initial_data.sql
-- Insert initial reference data

-- Insert content types
INSERT INTO content_types (name, mime_types, max_file_size_mb, created_by)
VALUES ('PDF', ARRAY ['application/pdf'], 50, 'SYSTEM'),
       ('Text', ARRAY ['text/plain', 'text/html'], 1, 'SYSTEM'),
       ('Image', ARRAY ['image/jpeg', 'image/png', 'image/gif', 'image/webp'], 10, 'SYSTEM'),
       ('Video', ARRAY ['video/mp4', 'video/webm', 'video/mpeg'], 500, 'SYSTEM'),
       ('Audio', ARRAY ['audio/mp3', 'audio/wav', 'audio/ogg'], 100, 'SYSTEM');

-- Insert difficulty levels
INSERT INTO difficulty_levels (name, level_order, description, created_by)
VALUES ('Prep', 1, 'Preparatory level for beginners', 'SYSTEM'),
       ('Beginner', 2, 'Basic level for new learners', 'SYSTEM'),
       ('Intermediate', 3, 'Intermediate level for developing skills', 'SYSTEM'),
       ('Advanced', 4, 'Advanced level for experienced learners', 'SYSTEM');

-- Insert grading levels
INSERT INTO grading_levels (name, points, level_order, created_by)
VALUES ('Distinction', 5, 1, 'SYSTEM'),
       ('Merit', 4, 2, 'SYSTEM'),
       ('Pass', 3, 3, 'SYSTEM'),
       ('Fail', 2, 4, 'SYSTEM'),
       ('No Effort', 1, 5, 'SYSTEM');

-- Insert default categories
INSERT INTO categories (name, description, created_by)
VALUES ('Music', 'Music education courses', 'SYSTEM'),
       ('Piano', 'Piano-specific courses', 'SYSTEM'),
       ('Theory', 'Music theory courses', 'SYSTEM'),
       ('Performance', 'Performance-based courses', 'SYSTEM');

-- Create default certificate templates
INSERT INTO certificate_templates (name, template_type, template_html, created_by)
VALUES ('Default Course Completion', 'course_completion',
        '<!DOCTYPE html>
        <html>
        <head><title>Certificate of Completion</title></head>
        <body>
        <div style="text-align: center; padding: 50px;">
        <h1>Certificate of Completion</h1>
        <p>This is to certify that</p>
        <h2>{{student_name}}</h2>
        <p>has successfully completed the course</p>
        <h3>{{course_name}}</h3>
        <p>on {{completion_date}}</p>
        <p>Final Grade: {{final_grade}}%</p>
        </div>
        </body>
        </html>', 'SYSTEM'),
       ('Default Program Completion', 'program_completion',
        '<!DOCTYPE html>
        <html>
        <head><title>Program Completion Certificate</title></head>
        <body>
        <div style="text-align: center; padding: 50px;">
        <h1>Program Completion Certificate</h1>
        <p>This is to certify that</p>
        <h2>{{student_name}}</h2>
        <p>has successfully completed the training program</p>
        <h3>{{program_name}}</h3>
        <p>on {{completion_date}}</p>
        <p>Final Grade: {{final_grade}}%</p>
        <p>Total Duration: {{total_hours}} hours</p>
        </div>
        </body>
        </html>', 'SYSTEM');