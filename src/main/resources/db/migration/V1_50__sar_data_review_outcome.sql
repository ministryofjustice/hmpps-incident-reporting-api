-- The Offender SAR team's decisions from the data review (IR-1461), signed off 23 October 2026.
--
-- They agreed the proposal in V1_49 for every column except five, all of them about a member of staff. Their
-- rule is that a member of staff appears in a prisoner's report by surname only, with nothing else about them,
-- so these move from [SAR: Y] to [SAR: N]:
--
--   * staff_involvement.first_name - staff named on a report are shown by last_name alone.
--   * description_addendum.first_name - as above for whoever added to the description.
--   * description_addendum.created_by - the addition is credited to the snapshotted last_name, which they kept,
--     rather than to the username.
--   * correction_request.user_type - the capacity the requester acted in is about the member of staff.
--   * correction_request.location - read with correction_requested_by it says where that member of staff was
--     based.
--
-- Comments only - no schema or data change. The rest of each comment, including its example, is left as it is.

DO $$
DECLARE
  rec      record;
  existing text;
BEGIN
  FOR rec IN
    SELECT *
      FROM (
        VALUES
          ('staff_involvement', 'first_name'),
          ('description_addendum', 'first_name'),
          ('description_addendum', 'created_by'),
          ('correction_request', 'user_type'),
          ('correction_request', 'location')
      ) AS t(table_name, column_name)
  LOOP
    SELECT col_description(a.attrelid, a.attnum)
      INTO existing
      FROM pg_attribute a
     WHERE a.attrelid = format('%I', rec.table_name)::regclass
       AND a.attname = rec.column_name
       AND a.attnum > 0
       AND NOT a.attisdropped;

    IF existing IS NULL OR position('[SAR: Y]' IN existing) = 0 THEN
      RAISE EXCEPTION 'Comment on %.% is not tagged [SAR: Y]', rec.table_name, rec.column_name;
    END IF;

    EXECUTE format(
      'COMMENT ON COLUMN %I.%I IS %L',
      rec.table_name,
      rec.column_name,
      replace(existing, '[SAR: Y]', '[SAR: N]')
    );
  END LOOP;
END $$;
