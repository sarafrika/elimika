-- Organisation competitions (events) for the org dashboard "Competition" page.
-- A competition is an org-scoped event with a category, date, venue and capacity.
-- Team registrations live in a child table; the teams count is derived from it.

CREATE TABLE competitions
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    organisation_uuid UUID                     NOT NULL,
    name              VARCHAR(255)             NOT NULL,
    category          VARCHAR(120),
    event_date        TIMESTAMP WITH TIME ZONE,
    venue_name        VARCHAR(255),
    capacity          INTEGER,
    status            VARCHAR(50)              NOT NULL        DEFAULT 'Upcoming',
    description       TEXT,

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_competitions_organisation ON competitions (organisation_uuid);

COMMENT ON TABLE competitions IS
    'Org-scoped competitions/events shown on the organisation Competition page.';

CREATE TABLE competition_teams
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    competition_uuid  UUID                     NOT NULL REFERENCES competitions (uuid) ON DELETE CASCADE,
    team_name         VARCHAR(255)             NOT NULL,

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL        DEFAULT 'system',
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_competition_teams_competition ON competition_teams (competition_uuid);

COMMENT ON TABLE competition_teams IS
    'Team registrations for a competition. Teams count is derived from these rows.';
