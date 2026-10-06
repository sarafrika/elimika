-- The top-level categories course creators pick during onboarding. Existing names are kept as they are.
INSERT INTO course_categories (name, description, created_by)
VALUES ('Music & Performing Arts', 'Music, dance, theatre and other performing arts', 'SYSTEM'),
       ('Visual Arts & Design', 'Drawing, painting, photography and design', 'SYSTEM'),
       ('Technology & Digital Skills', 'Software, data and digital tools', 'SYSTEM'),
       ('Business & Entrepreneurship', 'Starting, running and growing a business', 'SYSTEM'),
       ('Languages & Communication', 'Languages, writing and public speaking', 'SYSTEM'),
       ('Sciences & Mathematics', 'Natural sciences and mathematics', 'SYSTEM'),
       ('Health & Wellbeing', 'Fitness, nutrition and mental wellbeing', 'SYSTEM'),
       ('Trades & Technical Skills', 'Hands-on trades and technical crafts', 'SYSTEM')
ON CONFLICT (name) DO NOTHING;
