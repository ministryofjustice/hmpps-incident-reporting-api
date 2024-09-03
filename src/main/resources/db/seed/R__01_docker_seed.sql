DO
$$
    declare
        v_event_id  UUID;
        v_report_id UUID;

    BEGIN
        select gen_random_uuid() INTO v_event_id;

        insert into event (id, event_reference, event_date_and_time, title, description, prison_id, created_at,
                           modified_by)

        values (v_event_id, nextval('event_sequence'), now(), 'Test Incident', 'A test incident', 'MDI',
                now(), 'IR_CREATOR');

        insert into report (id, event_id, report_reference, title, description, prison_id, type, source, status,
                            incident_date_and_time, reported_at, reported_by, assigned_to, question_set_id, created_at,
                            modified_by)
        values (gen_random_uuid(), v_event_id, nextval('report_sequence'), 'Assault', 'Details of assualt on staff',
                'MDI', 'ASSAULT',
                'NOMIS', 'DRAFT', now(), now(), 'IR_CREATOR', 'IR_VIEWER', null, now(), 'IR_CREATOR')
        RETURNING id INTO v_report_id;

        insert into prisoner_involvement (report_id, prisoner_number, prisoner_role, outcome, comment)
        values (v_report_id, 'A1234AA', 'FIGHTER', 'PLACED_ON_REPORT', 'Adjudication was held');

        insert into staff_involvement (report_id, staff_username, staff_role, comment)
        values (v_report_id, 'ITAG_USER', 'VICTIM', 'Staff had a black eye');

    END;
$$
