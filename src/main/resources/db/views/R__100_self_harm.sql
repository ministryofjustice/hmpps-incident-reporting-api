drop materialized view if exists self_harm_summary;

CREATE MATERIALIZED VIEW self_harm_summary
AS
select r.id                       as report_id,
       r.location,
       r.incident_date_and_time,
       pi.id                      as prisoner_involvement_id,
       pi.prisoner_number,
       pi.last_name,
       pi.first_name,
       (select q1res.response
        from question q
               join response q1res on q1res.question_id = q.id
        where q.report_id = r.id
          and q.question = 'WHERE DID THE INCIDENT TAKE PLACE'
          and q1res.sequence = 0) as q1_location,
       (select q1res.additional_information
        from question q
               join response q1res on q1res.question_id = q.id
        where q.report_id = r.id
          and q.question = 'WHERE DID THE INCIDENT TAKE PLACE'
          and q1res.sequence = 0) as additional_comment,
       (select string_agg(
                 case
                   when q.question = 'DID SELF HARM METHOD INVOLVE CUTTING' then 'Cutting'
                   when q.question = 'DID SELF HARM METHOD INVOLVE HANGING' then 'Hanging'
                   when q.question = 'DID SELF HARM METHOD INVOLVE SELF STRANGULATION' then 'Self Strangulation'
                   when q.question = 'DID SELF HARM INVOLVE SELF POISONING/ OVERDOSE/SWALLOWING OBJECTS'
                     then 'Poisoning / Overdose / Swallowing'
                   when q.question = 'DID SELF HARM METHOD INVOLVE BURNING' then 'Burning'
                   else 'Other' end,
                 ', ')
        from question q
               join response res on res.question_id = q.id
        where q.report_id = r.id
          and q.question in ('DID SELF HARM METHOD INVOLVE CUTTING',
                             'DID SELF HARM METHOD INVOLVE HANGING',
                             'DID SELF HARM METHOD INVOLVE SELF STRANGULATION',
                             'DID SELF HARM INVOLVE SELF POISONING/ OVERDOSE/SWALLOWING OBJECTS',
                             'DID SELF HARM METHOD INVOLVE BURNING', 'WAS ANY OTHER SELF HARM METHOD INVOLVED')
          and res.response = 'YES')
                                  as category,

       (select string_agg(res.response, ', ')
        from question q
               join response res on res.question_id = q.id
        where q.report_id = r.id
          and q.question in ('TYPE OF IMPLEMENT USED',
                             'LIGATURE TYPE',
                             'SELF POISONING/OVERDOSE/SUBSTANCES/SWALLOWING',
                             'TYPE OF BURNING'))
                                  as materials_used,
       (select string_agg(res.response, ', ')
        from question q
               join response res on res.question_id = q.id
        where q.report_id = r.id
          and q.question = 'WHAT OTHER METHOD OF SELF HARM WAS INVOLVED')
                                  as other_method
from report r
       join prisoner_involvement pi on r.id = pi.report_id and pi.prisoner_role = 'PERPETRATOR'
where r.type = 'SELF_HARM_1'
  and r.status != 'DUPLICATE'

