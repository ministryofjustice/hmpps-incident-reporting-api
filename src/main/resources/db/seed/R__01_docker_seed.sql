do
$$
    declare
        v_report_id UUID;

    begin
        insert into report (
            id, report_reference,
            title, description,
            type, status, source,
            incident_date_and_time, location,
            reported_at, reported_by, assigned_to,
            created_at,
            modified_by
        )
        values (
            gen_random_uuid(), nextval('report_sequence'),
            'Assault', 'Details of assualt on staff',
            'ASSAULT', 'DRAFT', 'NOMIS',
            now(), 'MDI',
            now(), 'IR_REPORTING_OFFICER', 'IR_REPORTING_OFFICER',
            now(), 'IR_REPORTING_OFFICER'
        )
        returning id into v_report_id;

        insert into prisoner_involvement (
            report_id,
            prisoner_number, prisoner_role, outcome,
            comment, first_name, last_name
        )
        values (
            v_report_id,
            'A1234AA', 'FIGHTER', 'PLACED_ON_REPORT',
            'Adjudication was held', 'Travis', 'Bickle'
        );

        insert into staff_involvement (
            report_id,
            staff_username, staff_role,
            comment, first_name, last_name
        )
        values (
            v_report_id,
            'ITAG_USER', 'VICTIM',
            'Staff had a black eye', 'John', 'Doe'
        );
    end;
$$
