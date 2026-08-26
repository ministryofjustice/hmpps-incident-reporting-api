-- Data dictionary for the incident reporting schema.
--
-- These comments are read by SchemaSpy (published to GitHub Pages) and by anything else that reads
-- pg_description, including the CSV export for the MOJ Data Catalogue / Glue. Keep them updated when
-- columns are added or their meaning changes - SchemaCommentsTest fails the build if a table or column
-- has no comment.
--
-- Every column comment ends with a sensitivity classification:
--
--   [Sensitivity: NONE]                - not personal data in itself (keys, timestamps, process flags,
--                                        and the reference data in the constant_* tables)
--   [Sensitivity: PERSONAL]            - personal data about a prisoner: identifies or locates them
--   [Sensitivity: STAFF]               - personal data about a member of staff, typically the username
--                                        that performed an action
--   [Sensitivity: SPECIAL-CATEGORY]    - UK GDPR Article 9 data (health, sexuality, religion, race,
--                                        gender reassignment) or criminal offence data under Article 10
--   [Sensitivity: OFFICIAL-SENSITIVE]  - not personal data, but damaging if disclosed
--
-- STAFF is still personal data and still in scope for a staff member's own subject access request. It is
-- separated from PERSONAL so that an extract about prisoners can be reasoned about without staff columns
-- inflating the count, and so staff data can be dropped or pseudonymised independently.
--
-- Four things to understand before using these classifications:
--
--   1. They describe the column's own content, not the row's. Every report concerns an incident in a
--      prison and almost every report names prisoners through prisoner_involvement, so the record as a
--      whole is personal data about them whatever an individual column is marked - that is what matters
--      for a subject access request.
--   2. **This is the most sensitive schema in the Manage Safety set.** Reports cover self-harm, deaths
--      in custody, assaults, sexual assaults, drug finds and disorder. The incident type alone is
--      Article 9 or Article 10 data about everyone named on the report, whichever type it is, and the
--      narrative fields describe health, offending and third parties as a matter of routine.
--   3. **Every free-text column should be assumed to contain more than its question asks.** The
--      description, the addenda and the per-question additional information are written by staff in
--      their own words under time pressure, and in practice carry health detail, named third parties
--      and accounts of offending regardless of what the field is nominally for.
--   4. STAFF and SPECIAL-CATEGORY are mutually exclusive here, and a few columns are arguably both:
--      staff_involvement.staff_role can record a member of staff as suspected of involvement in an
--      incident, which is offence-adjacent data about that person. Those columns are tagged STAFF, so
--      that staff data stays separable, and the comment says where the content is also sensitive.
--      Read the tag as "who is this about", and the comment for "how sensitive is it".
--
-- Question and answer text is treated differently from the answers themselves. question.question and
-- question.label are the wording of the form, identical for every report of a type and derivable from
-- report.type, so they are NONE. What was actually answered - response.code, response.label,
-- response.response, and any additional information on either table - is about the incident and the
-- people in it, so it is SPECIAL-CATEGORY.
--
-- The constant_* tables and analytical_marker are reference data copied from the Kotlin enumerations for
-- the Analytical Platform and DPR. They contain no personal data at all and are the same in every
-- environment. Note the convention recorded in V1_14: they are deliberately not used in foreign key
-- constraints, and any change to the enumerations requires a new migration to keep them in step.

------------------------------------------------------------------------------------------------
-- report - the incident report itself
------------------------------------------------------------------------------------------------

COMMENT ON TABLE report IS 'One incident report. The aggregate root: the people involved, the structured question and answer set, the correction requests and the status trail all hang off it. Reports are created in DPS or synchronised from NOMIS, and move through a status workflow from DRAFT to CLOSED. Changing a report''s type snapshots the previous question set into history rather than discarding it.';

COMMENT ON COLUMN report.id IS 'Primary key. Time-ordered UUID v7, so insert order matches id order. Internal - the reference people quote is report_reference. [Sensitivity: NONE]';
COMMENT ON COLUMN report.report_reference IS 'Human-readable reference for the report, unique across the service. Known as the "incident number" in NOMIS, and the identifier staff quote to each other. Identifies an incident, not a person. [Sensitivity: NONE]';
COMMENT ON COLUMN report.title IS 'Short title for the incident, written by the reporter. Free text, and in practice often names people or summarises what happened, so it carries the same sensitivity as the description despite being short. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN report.description IS 'The reporter''s account of what happened. Unstructured and unbounded, and the single richest source of sensitive data in the schema - it routinely describes injuries, self-harm, health treatment, offending and third parties by name. Later additions are appended as rows in description_addendum rather than edited into this column. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN report.location IS 'Agency (prison) code where the incident happened. Read with prisoner_involvement it indicates where those prisoners were held at the time. Renamed from prison_id in V1_2 and widened, because PECS regions are also valid values here - a report is not always tied to an establishment. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN report.type IS 'What kind of incident this is, for example SELF_HARM, DEATH_OTHER, ASSAULT or FIND_OF_ILLICIT_ITEMS. Drives which questions are asked. This is Article 9 or Article 10 data about everyone named on the report whichever value it holds - that a report exists and is typed SELF_HARM discloses health data about the prisoner it concerns. Values are listed in constant_type and grouped by constant_type_family. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN report.source IS 'Where the report was first created: DPS or NOMIS. Reports migrated or synchronised from NOMIS carry NOMIS. [Sensitivity: NONE]';
COMMENT ON COLUMN report.status IS 'Where the report has got to in the workflow, for example DRAFT, AWAITING_REVIEW, ON_HOLD, NEEDS_UPDATING, CLOSED or DUPLICATE. Values and their meanings are in constant_status, which also records which statuses downstream consumers should ignore. [Sensitivity: NONE]';
COMMENT ON COLUMN report.incident_date_and_time IS 'When the incident happened, as reported. Distinct from reported_at, which is when someone wrote it up - the gap between the two is often days. [Sensitivity: NONE]';
COMMENT ON COLUMN report.reported_at IS 'When the incident was reported. For reports synchronised from NOMIS this is the original NOMIS value, not the sync. [Sensitivity: NONE]';
COMMENT ON COLUMN report.reported_by IS 'Username of the member of staff who reported the incident. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN report.question_set_id IS 'Legacy NOMIS question set identifier, carried over in the original schema and never used since - no Kotlin code reads or writes it, and it is not exposed by the API. Treat as dead. [Sensitivity: NONE]';
COMMENT ON COLUMN report.created_at IS 'When the row was created in this service. For reports synchronised from NOMIS this is the sync, not the incident or the original report - use incident_date_and_time or reported_at for those. [Sensitivity: NONE]';
COMMENT ON COLUMN report.modified_by IS 'Username of the member of staff who last changed the report, or the system user for changes made by the NOMIS sync. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN report.modified_at IS 'When the report was last changed. [Sensitivity: NONE]';
COMMENT ON COLUMN report.modified_in IS 'Which system made the last change: DPS or NOMIS. A report can be created in one and edited in the other, so this is not the same as source. [Sensitivity: NONE]';
COMMENT ON COLUMN report.staff_involvement_done IS 'Whether the reporter has finished recording staff involvement. Distinguishes "no staff were involved" from "not yet filled in", which an empty staff_involvement cannot do on its own. [Sensitivity: NONE]';
COMMENT ON COLUMN report.prisoner_involvement_done IS 'Whether the reporter has finished recording prisoner involvement. Distinguishes "no prisoners were involved" from "not yet filled in", which an empty prisoner_involvement cannot do on its own. [Sensitivity: NONE]';
COMMENT ON COLUMN report.duplicated_report_id IS 'The report this one duplicates, set when it is marked DUPLICATE. Points at report.id, not report_reference. Null on every report that is not a duplicate. [Sensitivity: NONE]';
COMMENT ON COLUMN report.last_user_action IS 'The most recent action taken on the report through a correction request, denormalised here so a list of reports can show it without joining. Mirrors the newest correction_request.user_action and is null until the first one. [Sensitivity: NONE]';

------------------------------------------------------------------------------------------------
-- prisoner_involvement - the prisoners named on a report
------------------------------------------------------------------------------------------------

COMMENT ON TABLE prisoner_involvement IS 'One prisoner named on a report, with how they were involved and what happened to them afterwards. Names are copied in at the time of writing rather than resolved live, so they are a snapshot and may differ from the prisoner''s current record. A report may name several prisoners in different roles, and the whole row is personal data about the prisoner it names.';

COMMENT ON COLUMN prisoner_involvement.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN prisoner_involvement.report_id IS 'The report this involvement belongs to. Foreign key to report.id. [Sensitivity: NONE]';
COMMENT ON COLUMN prisoner_involvement.prisoner_number IS 'NOMIS offender number (noms id) of the prisoner. The link that makes this row, and much of the report, personal data about that prisoner. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN prisoner_involvement.prisoner_role IS 'How the prisoner was involved, for example VICTIM, SUSPECTED_PERPETRATOR, ACTIVE_INVOLVEMENT or PRESENT_AT_SCENE. On an assault or a find of illicit items, recording someone as a suspected perpetrator is an allegation of offending in custody against a named person - criminal offence data under Article 10, which DPA 2018 s.11(2) extends to alleged offences. Values are in constant_prisoner_role. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN prisoner_involvement.outcome IS 'What happened to the prisoner as a result, for example PLACED_ON_REPORT, CHARGED_BY_POLICE, LOCAL_INVESTIGATION or TRANSFER. Records criminal or disciplinary proceedings against a named person. Null where no outcome has been recorded. Values are in constant_prisoner_outcome. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN prisoner_involvement.comment IS 'Free-text note on this prisoner''s involvement. Unstructured and written by hand - in practice describes injuries, behaviour, health and other people, so treat as special category regardless of what any individual comment happens to say. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN prisoner_involvement.sequence IS 'Order of this prisoner within the report''s list. Display order only. [Sensitivity: NONE]';
COMMENT ON COLUMN prisoner_involvement.first_name IS 'Prisoner''s first name as at the time the report was written. A snapshot copied from the prisoner search API, not kept in step afterwards. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN prisoner_involvement.last_name IS 'Prisoner''s last name as at the time the report was written. A snapshot copied from the prisoner search API, not kept in step afterwards. [Sensitivity: PERSONAL]';

------------------------------------------------------------------------------------------------
-- staff_involvement - the staff named on a report
------------------------------------------------------------------------------------------------

COMMENT ON TABLE staff_involvement IS 'One member of staff named on a report, with how they were involved. Names are copied in at the time of writing rather than resolved live. Staff can be named as witnesses, as first on scene, as injured, or as suspected of involvement themselves - so this table is not only an audit of who did the paperwork.';

COMMENT ON COLUMN staff_involvement.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN staff_involvement.report_id IS 'The report this involvement belongs to. Foreign key to report.id. [Sensitivity: NONE]';
COMMENT ON COLUMN staff_involvement.staff_username IS 'DPS username of the member of staff. Nullable since V1_15: staff can be named on a report without having a DPS account, in which case only the names are recorded. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN staff_involvement.staff_role IS 'How the member of staff was involved, for example WITNESS, FIRST_ON_SCENE, ACTIVELY_INVOLVED or SUSPECTED_INVOLVEMENT. Note that SUSPECTED_INVOLVEMENT is an allegation against a named member of staff and is sensitive well beyond an ordinary audit field - tagged STAFF rather than SPECIAL-CATEGORY so staff data stays separable, but handle it accordingly. Values are in constant_staff_role. [Sensitivity: STAFF]';
COMMENT ON COLUMN staff_involvement.comment IS 'Free-text note on this member of staff''s involvement. Unstructured and written by hand - describes what they did or what happened to them, and routinely names prisoners and describes injuries, so treat as special category. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN staff_involvement.sequence IS 'Order of this member of staff within the report''s list. Display order only. [Sensitivity: NONE]';
COMMENT ON COLUMN staff_involvement.first_name IS 'Member of staff''s first name as at the time the report was written. A snapshot, not kept in step afterwards. [Sensitivity: STAFF]';
COMMENT ON COLUMN staff_involvement.last_name IS 'Member of staff''s last name as at the time the report was written. A snapshot, not kept in step afterwards. [Sensitivity: STAFF]';

------------------------------------------------------------------------------------------------
-- description_addendum - text added after submission
------------------------------------------------------------------------------------------------

COMMENT ON TABLE description_addendum IS 'Text added to a report after it was first submitted, in an ordered list. The original description is never edited: additions are appended here instead, so the record of what was said when is preserved. Read report.description followed by these rows in sequence to get the full account.';

COMMENT ON COLUMN description_addendum.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN description_addendum.report_id IS 'The report this addendum belongs to. Foreign key to report.id. [Sensitivity: NONE]';
COMMENT ON COLUMN description_addendum.sequence IS 'Order of this addendum within the report. Read in this order after report.description. [Sensitivity: NONE]';
COMMENT ON COLUMN description_addendum."text" IS 'The added text. Carries the same sensitivity as report.description - written by hand, and in practice describing injuries, health, offending and named third parties. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN description_addendum.created_at IS 'When the addendum was added. [Sensitivity: NONE]';
COMMENT ON COLUMN description_addendum.created_by IS 'Username of the member of staff who added it. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN description_addendum.first_name IS 'First name of the member of staff who added it, snapshotted at the time. [Sensitivity: STAFF]';
COMMENT ON COLUMN description_addendum.last_name IS 'Last name of the member of staff who added it, snapshotted at the time. [Sensitivity: STAFF]';

------------------------------------------------------------------------------------------------
-- correction_request - manager asks the reporter to change something
------------------------------------------------------------------------------------------------

COMMENT ON TABLE correction_request IS 'One request to change a report, or one recorded action taken on it. Despite the name this is the general trail of what people have asked for and done - a data warden sending a report back for more detail, a reporter resubmitting it, a report being marked as a duplicate. The newest row''s user_action is mirrored onto report.last_user_action.';

COMMENT ON COLUMN correction_request.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN correction_request.report_id IS 'The report this request concerns. Foreign key to report.id. [Sensitivity: NONE]';
COMMENT ON COLUMN correction_request.description_of_change IS 'Free-text description of what was asked for or done, written by hand. Quotes and summarises the report''s content, so assume it carries the same health, offending and third-party detail as the description itself. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN correction_request.correction_requested_at IS 'When the request was made or the action recorded. [Sensitivity: NONE]';
COMMENT ON COLUMN correction_request.correction_requested_by IS 'Username of the member of staff who made the request or took the action. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN correction_request.sequence IS 'Order of this request within the report''s trail. [Sensitivity: NONE]';
COMMENT ON COLUMN correction_request.location IS 'Agency (prison) code or PECS region the requester was working in. Read with correction_requested_by it indicates where that member of staff was based. Nullable on older rows written before the column existed. [Sensitivity: STAFF]';
COMMENT ON COLUMN correction_request.user_action IS 'What the user did, for example REQUEST_REVIEW, REQUEST_CORRECTION, RECALL or CLOSE. Values are in constant_user_action. Null on rows written before the column existed. [Sensitivity: NONE]';
COMMENT ON COLUMN correction_request.original_report_reference IS 'Reference of the report this one was found to duplicate, recorded when it is marked as a duplicate. A report_reference rather than an id, and null on every other row. [Sensitivity: NONE]';
COMMENT ON COLUMN correction_request.user_type IS 'Which kind of user acted: REPORTING_OFFICER or DATA_WARDEN. The two have different permissions, so this records the capacity someone acted in rather than their job title. Values are in constant_user_type. Null on rows written before the column existed. [Sensitivity: NONE]';

------------------------------------------------------------------------------------------------
-- status_history and history - the two audit trails
------------------------------------------------------------------------------------------------

COMMENT ON TABLE status_history IS 'Every status the report has held, in order, with who changed it and when. Append-only: the current status is on report.status and this is how it got there. Note that a report can be reopened, so a status can appear more than once for the same report.';

COMMENT ON COLUMN status_history.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN status_history.report_id IS 'The report whose status changed. Foreign key to report.id. [Sensitivity: NONE]';
COMMENT ON COLUMN status_history.status IS 'The status the report moved into. Values and their meanings are in constant_status. [Sensitivity: NONE]';
COMMENT ON COLUMN status_history.changed_at IS 'When the status changed. [Sensitivity: NONE]';
COMMENT ON COLUMN status_history.changed_by IS 'Username of the member of staff who changed it, or the system user for automatic transitions. Identifies a member of staff. [Sensitivity: STAFF]';

COMMENT ON TABLE history IS 'A snapshot of the question and answer set as it stood before the report''s type was changed. Changing the type changes which questions apply, so the previous set is preserved here with its answers in historical_question and historical_response rather than being deleted. One row per type change, so a report retyped twice has two.';

COMMENT ON COLUMN history.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN history.report_id IS 'The report this snapshot belongs to. Foreign key to report.id. [Sensitivity: NONE]';
COMMENT ON COLUMN history.type IS 'The incident type the report held before it was changed - so a report now typed ASSAULT may have a history row typed SELF_HARM. Carries the same sensitivity as report.type, and note that it discloses what the incident was once believed to be. Values are in constant_type. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN history.changed_at IS 'When the type was changed, and therefore when this snapshot was taken. [Sensitivity: NONE]';
COMMENT ON COLUMN history.changed_by IS 'Username of the member of staff who changed the type. Identifies a member of staff. [Sensitivity: STAFF]';

------------------------------------------------------------------------------------------------
-- question and response - the current structured answers
------------------------------------------------------------------------------------------------

COMMENT ON TABLE question IS 'One question asked on a report, in the order it was asked. Which questions apply is decided by report.type; the wording is copied in at the time so a later change to the question set does not rewrite existing reports. Answers hang off this in response - a question with no response rows was asked but not answered.';

COMMENT ON COLUMN question.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN question.report_id IS 'The report this question belongs to. Foreign key to report.id, and unique with code - a question is asked at most once per report. [Sensitivity: NONE]';
COMMENT ON COLUMN question.sequence IS 'Order the question was asked in. [Sensitivity: NONE]';
COMMENT ON COLUMN question.code IS 'Stable code for the question within the incident type''s question set. The join key for anyone analysing answers across reports - the wording changes, the code does not. [Sensitivity: NONE]';
COMMENT ON COLUMN question.question IS 'The question as worded when the report was written. Form wording, identical for every report of that type and derivable from report.type, so not personal data in itself - what was answered is in response. [Sensitivity: NONE]';
COMMENT ON COLUMN question.additional_information IS 'Free text the reporter added against the question rather than against a specific answer. Written by hand, so assume it carries health, offending and third-party detail. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN question.label IS 'Display label for the question, as worded when the report was written. Form wording rather than an answer - see question.question. [Sensitivity: NONE]';

COMMENT ON TABLE response IS 'One answer given to a question. A question can have several responses where more than one option applies, which is why this is a table rather than a column on question. The answer is what makes this schema sensitive: responses record injuries, weapons, methods of self-harm, whether police were involved and what was found.';

COMMENT ON COLUMN response.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN response.question_id IS 'The question this answers. Foreign key to question.id. [Sensitivity: NONE]';
COMMENT ON COLUMN response.sequence IS 'Order of this answer within the question''s answers. [Sensitivity: NONE]';
COMMENT ON COLUMN response.response IS 'The answer as worded when the report was written. Records what actually happened in the incident - injuries, weapons, methods, whether the police attended - about the people named on the report. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN response.response_date IS 'Date given as the answer, where the question asks for one - for example when a death occurred or when treatment was given. Null on answers that are not dates. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN response.additional_information IS 'Free text the reporter added against this answer. Written by hand, so assume it carries health, offending and third-party detail. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN response.recorded_at IS 'When the answer was recorded. [Sensitivity: NONE]';
COMMENT ON COLUMN response.recorded_by IS 'Username of the member of staff who recorded the answer. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN response.code IS 'Stable code for the answer within the question''s option set. The join key for analysing answers across reports, and the key analytical_marker uses to flag answers that indicate serious harm. Discloses what was answered, so it is as sensitive as the wording. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN response.label IS 'Display label for the answer, as worded when the report was written. Discloses what was answered - see response.response. [Sensitivity: SPECIAL-CATEGORY]';

------------------------------------------------------------------------------------------------
-- historical_question and historical_response - the same, snapshotted on a type change
------------------------------------------------------------------------------------------------

COMMENT ON TABLE historical_question IS 'A question from a question set the report no longer uses, preserved when its type was changed. Identical in shape to question except that it hangs off history rather than report. Anything analysing answers over time needs to read both tables, or it will silently miss every report that has been retyped.';

COMMENT ON COLUMN historical_question.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_question.history_id IS 'The type-change snapshot this question belongs to. Foreign key to history.id - the equivalent of question.report_id one level removed. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_question.sequence IS 'Order the question was asked in. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_question.code IS 'Stable code for the question within the question set that applied at the time. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_question.question IS 'The question as worded at the time. Form wording rather than an answer - see question.question. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_question.additional_information IS 'Free text the reporter added against the question. Written by hand, so assume it carries health, offending and third-party detail. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN historical_question.label IS 'Display label for the question, as worded at the time. Form wording rather than an answer. [Sensitivity: NONE]';

COMMENT ON TABLE historical_response IS 'An answer to a question from a question set the report no longer uses, preserved when its type was changed. Identical in shape to response except that it hangs off historical_question. As sensitive as response, and as easy to miss: an extract that reads only response understates what a report contains.';

COMMENT ON COLUMN historical_response.id IS 'Primary key. Surrogate sequence value. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_response.historical_question_id IS 'The historical question this answers. Foreign key to historical_question.id. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_response.sequence IS 'Order of this answer within the question''s answers. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_response.response IS 'The answer as worded at the time. Records what happened in the incident about the people named on the report - see response.response. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN historical_response.response_date IS 'Date given as the answer, where the question asked for one. Null on answers that are not dates. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN historical_response.additional_information IS 'Free text the reporter added against this answer. Written by hand, so assume it carries health, offending and third-party detail. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN historical_response.recorded_at IS 'When the answer was recorded - the original recording, not the snapshot. [Sensitivity: NONE]';
COMMENT ON COLUMN historical_response.recorded_by IS 'Username of the member of staff who recorded the answer. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN historical_response.code IS 'Stable code for the answer within the option set that applied at the time. Discloses what was answered. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN historical_response.label IS 'Display label for the answer, as worded at the time. Discloses what was answered. [Sensitivity: SPECIAL-CATEGORY]';

------------------------------------------------------------------------------------------------
-- analytical_marker - which answers indicate serious harm
------------------------------------------------------------------------------------------------

COMMENT ON TABLE analytical_marker IS 'Maps answer codes to the kinds of serious harm they indicate, so reporting can count incidents involving, say, a hospital admission without hard-coding a list of answer codes. Reference data derived from the AnalyticalMarkerType enumeration - it is about the question set, not about any person, and contains no personal data. One answer can carry several markers, hence the composite key.';

COMMENT ON COLUMN analytical_marker.response_code IS 'Answer code this marker applies to. Matches response.code and historical_response.code. Deliberately not a foreign key - see the note in V1_14 about the reference tables. [Sensitivity: NONE]';
COMMENT ON COLUMN analytical_marker.marker_type IS 'What the answer indicates: SERIOUS_INJURY, SEXUAL_ASSAULT, MEDICAL_TREATMENT_REQUIRED, HOSPITAL_ADMISSION, TRANSMITTABLE_INFECTIOUS_DISEASE, SALIVA_HIT_BODY, HIT_NECK_OR_ABOVE or WEAPON_URINE. Note that this table says which answers are serious, not which reports were - joining it to response is what makes a report''s content special category, not this table on its own. [Sensitivity: NONE]';

------------------------------------------------------------------------------------------------
-- constant_* - reference data copied from the Kotlin enumerations
--
-- These tables exist for the Analytical Platform and DPR, so reports can decode the codes stored on
-- report, prisoner_involvement, staff_involvement and correction_request without reading the source.
-- The application itself never reads them - it uses the enumerations in
-- uk.gov.justice.digital.hmpps.incidentreporting.constants. That means they can only be kept in step by
-- hand: any change to an enumeration REQUIRES a new migration. They are deliberately not used in
-- foreign key constraints, which would make those migrations far harder. NOMIS codes are not included.
--
-- Every column in every one of these tables is reference data about the service's vocabulary rather
-- than about any person, so all are [Sensitivity: NONE] and none is in scope for a subject access
-- request. Each has the same shape: code, sequence, description.
------------------------------------------------------------------------------------------------

COMMENT ON TABLE constant_type IS 'The incident types a report can have, for reporting. Copy of the Type enumeration. Types are retired rather than deleted, because existing reports still reference them.';
COMMENT ON COLUMN constant_type.code IS 'The type code as stored in report.type and history.type, for example SELF_HARM or ASSAULT. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_type.sequence IS 'Display order for the type. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_type.description IS 'Human-readable name of the type. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_type.active IS 'Whether the type can be chosen for new reports. Retired types stay in the table so existing reports still decode. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_type.family_code IS 'The family this type belongs to, matching constant_type_family.code. Types were regrouped into families in V1_16 and their codes renamed to match, so codes on old and new reports differ. [Sensitivity: NONE]';

COMMENT ON TABLE constant_type_family IS 'The families incident types are grouped into, for reporting. Copy of the TypeFamily enumeration. Introduced in V1_16, which also renamed type codes to match.';
COMMENT ON COLUMN constant_type_family.code IS 'The family code, as referenced by constant_type.family_code. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_type_family.sequence IS 'Display order for the family. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_type_family.description IS 'Human-readable name of the family. [Sensitivity: NONE]';

COMMENT ON TABLE constant_status IS 'The statuses a report can hold, for reporting. Copy of the Status enumeration, with the plain-English definition shown to users and a flag for the statuses downstream consumers should skip.';
COMMENT ON COLUMN constant_status.code IS 'The status code as stored in report.status and status_history.status, for example DRAFT or AWAITING_REVIEW. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_status.sequence IS 'Display order for the status. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_status.description IS 'Short human-readable name of the status. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_status.ignore_downstream IS 'Whether downstream consumers should skip reports in this status. True for DUPLICATE, NOT_REPORTABLE, REOPENED and WAS_CLOSED - reports that would otherwise be double-counted or counted when they should not be. Anything aggregating reports should honour this. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_status.definition IS 'Fuller plain-English explanation of what the status means, shown to users and useful when interpreting a status trail. Added in V1_46. [Sensitivity: NONE]';

COMMENT ON TABLE constant_prisoner_role IS 'The ways a prisoner can be involved in an incident, for reporting. Copy of the PrisonerRole enumeration.';
COMMENT ON COLUMN constant_prisoner_role.code IS 'The role code as stored in prisoner_involvement.prisoner_role. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_prisoner_role.sequence IS 'Display order for the role. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_prisoner_role.description IS 'Human-readable name of the role. [Sensitivity: NONE]';

COMMENT ON TABLE constant_prisoner_outcome IS 'The outcomes that can follow for a prisoner involved in an incident, for reporting. Copy of the PrisonerOutcome enumeration.';
COMMENT ON COLUMN constant_prisoner_outcome.code IS 'The outcome code as stored in prisoner_involvement.outcome. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_prisoner_outcome.sequence IS 'Display order for the outcome. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_prisoner_outcome.description IS 'Human-readable name of the outcome. [Sensitivity: NONE]';

COMMENT ON TABLE constant_staff_role IS 'The ways a member of staff can be involved in an incident, for reporting. Copy of the StaffRole enumeration.';
COMMENT ON COLUMN constant_staff_role.code IS 'The role code as stored in staff_involvement.staff_role. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_staff_role.sequence IS 'Display order for the role. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_staff_role.description IS 'Human-readable name of the role. [Sensitivity: NONE]';

COMMENT ON TABLE constant_user_action IS 'The actions a user can take on a report, for reporting. Copy of the UserAction enumeration.';
COMMENT ON COLUMN constant_user_action.code IS 'The action code as stored in correction_request.user_action and report.last_user_action. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_user_action.sequence IS 'Display order for the action. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_user_action.description IS 'Human-readable name of the action. [Sensitivity: NONE]';

COMMENT ON TABLE constant_user_type IS 'The kinds of user who can act on a report, for reporting. Copy of the UserType enumeration - a capacity someone acted in, not a job title.';
COMMENT ON COLUMN constant_user_type.code IS 'The user type code as stored in correction_request.user_type: REPORTING_OFFICER or DATA_WARDEN. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_user_type.sequence IS 'Display order for the user type. [Sensitivity: NONE]';
COMMENT ON COLUMN constant_user_type.description IS 'Human-readable name of the user type. [Sensitivity: NONE]';
