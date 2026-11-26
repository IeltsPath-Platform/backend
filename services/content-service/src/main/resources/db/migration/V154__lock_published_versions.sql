CREATE OR REPLACE FUNCTION forbid_published_version_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'PUBLISHED' THEN
        IF TG_TABLE_NAME = 'question_versions'
           AND ROW(to_jsonb(NEW)->'stem', to_jsonb(NEW)->'options', to_jsonb(NEW)->'answer_spec',
                   to_jsonb(NEW)->'schema_version', to_jsonb(NEW)->'explanation', to_jsonb(NEW)->'difficulty',
                   to_jsonb(NEW)->'hint', to_jsonb(NEW)->'guide', to_jsonb(NEW)->'guide_steps')
               IS DISTINCT FROM
               ROW(to_jsonb(OLD)->'stem', to_jsonb(OLD)->'options', to_jsonb(OLD)->'answer_spec',
                   to_jsonb(OLD)->'schema_version', to_jsonb(OLD)->'explanation', to_jsonb(OLD)->'difficulty',
                   to_jsonb(OLD)->'hint', to_jsonb(OLD)->'guide', to_jsonb(OLD)->'guide_steps') THEN
            RAISE EXCEPTION USING
                ERRCODE = '23514',
                MESSAGE = 'published_version_content_locked';
        ELSIF TG_TABLE_NAME = 'content_package_versions'
           AND ROW(to_jsonb(NEW)->'rules', to_jsonb(NEW)->'schema_version', to_jsonb(NEW)->'version_number')
               IS DISTINCT FROM
               ROW(to_jsonb(OLD)->'rules', to_jsonb(OLD)->'schema_version', to_jsonb(OLD)->'version_number') THEN
            RAISE EXCEPTION USING
                ERRCODE = '23514',
                MESSAGE = 'published_version_content_locked';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_question_versions_published_content_lock
    BEFORE UPDATE ON question_versions
    FOR EACH ROW
    EXECUTE FUNCTION forbid_published_version_change();

CREATE TRIGGER trg_content_package_versions_published_content_lock
    BEFORE UPDATE ON content_package_versions
    FOR EACH ROW
    EXECUTE FUNCTION forbid_published_version_change();
