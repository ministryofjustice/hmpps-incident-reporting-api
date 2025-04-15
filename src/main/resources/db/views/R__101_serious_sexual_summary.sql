drop materialized view if exists serious_sexual_assault_summary;

drop materialized view if exists serious_sexual_assault_summary_view;

create materialized view serious_sexual_assault_summary_view as
select distinct r.id as report_id,
                r.report_reference,
                r.incident_date_and_time,
                r.title,
                r.description,
                r.status,
                constant_status.description as status_description,
                r.location,
                pi.id as prisoner_involvement_id,
                pi.prisoner_number,
                pi.prisoner_role,
                pr.description as prisoner_role_description,
                pi.last_name,
                pi.first_name
FROM report r
       join constant_status on r.status = constant_status.code
       join prisoner_involvement pi on r.id = pi.report_id
       join constant_prisoner_role pr on pi.prisoner_role = pr.code
       join question at_incident_question on r.id = at_incident_question.report_id
       join response at_incident_answer on at_incident_question.id = at_incident_answer.question_id
where status != 'DUPLICATE'
  AND ((type = 'ASSAULT_5' and (
  at_incident_question.question IN ('WAS A SERIOUS INJURY SUSTAINED',
                                    'WAS THIS A SEXUAL ASSAULT',
                                    'WAS MEDICAL TREATMENT FOR CONCUSSION OR INTERNAL INJURIES REQUIRED')
    AND at_incident_answer.response IN ('YES'))
  OR ((
        at_incident_question.question IN
        ('IS THE ASSAILANT KNOWN TO HAVE AN INFECTIOUS DISEASE THAT CAN BE TRANSMITTED IN SALIVA')
          AND at_incident_answer.response IN ('YES'))
    AND (
        at_incident_question.question IN ('DID THE SALIVA HIT THE BODY OR CLOTHING OF THE VICTIM(S)')
          AND at_incident_answer.response IN ('YES')))
  OR (
          at_incident_question.question IN ('TYPE OF HOSPITAL ADMISSION')
            AND at_incident_answer.response IN ('INPATIENT (OVERNIGHT ONLY)',
                                                'INPATIENT (OVER 24HR)',
                                                'LIFE SUPPORT'))
  OR (
          at_incident_question.question IN ('WHERE DID IT HIT')
            AND at_incident_answer.response IN ('NECK OR ABOVE'))
  OR (
          at_incident_question.question IN ('DESCRIBE THE WEAPONS USED')
            AND at_incident_answer.response IN ('EXCRETA/URINE')))
  or (type = 'ASSAULT_1' and (
    (
      at_incident_question.question IN ('WAS A SERIOUS INJURY SUSTAINED',
                                        'WAS THIS A SEXUAL ASSAULT',
                                        'DID INJURIES RESULT IN DETENTION IN OUTSIDE HOSPITAL AS AN IN-PATIENT',
                                        'WAS MEDICAL TREATMENT FOR CONCUSSION OR INTERNAL INJURIES REQUIRED')
        AND at_incident_answer.response IN ('YES'))
      OR (
      at_incident_question.question IN ('WHICH SERIOUS INJURIES WERE SUSTAINED')
        AND at_incident_answer.response IN ('BITES',
                                            'BLACK EYE',
                                            'BROKEN NOSE',
                                            'BROKEN TEETH',
                                            'CRUSHING',
                                            'EXTENSIVE/MULTIPLE BRUISING',
                                            'EXTENSIVE OR MULTIPLE BRUISING',
                                            'FRACTURE',
                                            'SCALD OR BURN',
                                            'STABBING',
                                            'TEMPORARY/PERMANENT BLINDNESS')))));

create index if not exists serious_sexual_assault_summary_view_idx
  on serious_sexual_assault_summary_view (report_id, incident_date_and_time, location);
