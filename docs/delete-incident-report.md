# Permanently deleting an incident report

Very occasionally a prison asks for a report to be removed completely, usually because it was submitted by
mistake and contains sensitive information that should never have been recorded in incident reporting.
Changing the report's status does not help, because anyone with access can still open it. The only option
is to delete it.

This is expected to happen about once a quarter. There is no screen for it yet, so a developer has to call
the API directly. Treat every request as exceptional and get it confirmed in writing before acting on it.

## Before you start

- The request needs a ticket in the IR project that records who asked, which report and why.
- The prison will need to submit a new report without the sensitive information, if the incident still
  needs reporting. Tell them this before the report is deleted.
- **There is no undo.** Once deleted, the report cannot be recovered from the service. Check the reference
  and prison twice.

## Who can do it

The delete endpoint needs the `DELETE_INCIDENT_REPORTS` role and the `write` scope. No user role or client
in normal use has it, and the front end cannot delete reports. A developer needs the role added to their
own HMPPS Auth account and calls the API using a user token for that account. Ask for the role to be
removed again once the work is done.

## What it removes

`DELETE /incident-reports/{id}` removes the report outright. The report is not marked as deleted and kept;
it is gone. Everything attached to it goes too:

- questions and responses, with their history
- prisoners and staff involved
- status history
- correction requests
- description addendums

If another report was marked as a duplicate of the deleted one, that link is cleared.

The API then publishes an `incident.report.deleted` event, and the NOMIS update service deletes the
matching NOMIS incident, including its questions, parties, requirements and history. The NOMIS incident
number is the same as the report reference. Nothing syncs incidents from NOMIS into this service any
more, so the report cannot come back from NOMIS.

## What it does not remove

Tell the person who asked that the following copies remain:

| Where | What is kept | Who to ask |
|---|---|---|
| HMPPS Audit | The full content of the report from every create and amend, and again from the delete itself. Access is restricted. | HMPPS Audit team |
| Reporting (DPR) | The delete should reach the current reporting data. Older copies held in the data lake may remain. | DPR team |
| NOMIS change logs | Any history NOMIS keeps of its own incident tables. | Syscon |
| Database backups | The report stays in backups until they expire. | Cloud Platform |

Application Insights only records the report's id, reference and prison, not its content.

## Steps

All calls go to the production API, `https://incident-reporting-api.hmpps.service.justice.gov.uk`.

1. Find the report's id from its reference:

   ```
   GET /incident-reports/reference/{reportReference}
   ```

   Check that the prison (`location`), type and incident date match the request.

2. Delete it using the `id` from step 1:

   ```
   DELETE /incident-reports/{id}
   ```

   The response contains the whole deleted report. Do not save it to a file or paste it into the ticket.

3. Repeat the call in step 1. It should now return 404.

4. Confirm the NOMIS side. The NOMIS update service records an `incident-delete-success` event with the
   NOMIS incident number (`nomisIncidentId`). If NOMIS no longer holds the incident, the message fails
   and ends up on the incidents dead-letter queue instead. Check with Syscon if in doubt.

5. Update the ticket with the date and time of the delete and the outcome of step 4, and let the prison
   know it is done, including the copies listed above that remain.

## If something goes wrong

- **403 Forbidden**: the token is missing the role or the `write` scope.
- **404 Not Found** on the delete: the report has already gone, or the id is wrong. Check step 1 again.
- **NOMIS delete failed**: the report is gone from this service but not from NOMIS. Ask Syscon to remove
  the NOMIS incident by hand, quoting the incident number.
