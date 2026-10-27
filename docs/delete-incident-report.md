# Permanently deleting an incident report

Very occasionally a prison asks for a report to be removed completely, usually because it was submitted by
mistake and contains sensitive information that should never have been recorded in incident reporting.
Changing the report's status does not help, because anyone with access can still open it. The only option
is to delete it.

This is expected to happen about once a quarter. There is no screen for it yet, so a developer has to call
the API directly. Treat every request as exceptional and get it confirmed in writing before acting on it.

## Before you start

The request needs a ticket in the IR project. Get the following from the person asking and record it there:

- the report reference and the prison
- why the report needs to be deleted
- confirmation that a manager at the prison has authorised the request

Also:

- Tell them which copies will remain (see [What it does not remove](#what-it-does-not-remove)).
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
| HMPPS Audit | The full content of the report from every create and amend, and again from the delete itself. Access is restricted. | HMPPS Auth and Audit team |
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

   Check that the prison (`location`), type and incident date match the request. Check the prison code
   against the response, not from memory.

2. Delete it using the `id` from step 1:

   ```
   DELETE /incident-reports/{id}
   ```

   The response contains the whole deleted report. Do not save it to a file or paste it into the ticket.

3. Repeat the call in step 1. It should now return 404.

4. Confirm the NOMIS side in Application Insights, as described in
   [Checking it worked](#checking-it-worked).

5. Update the ticket with the date and time of the delete and the outcome of step 4, and let the prison
   know it is done, including the copies listed above that remain.

### Using the IntelliJ HTTP client

Steps 1 to 3 can be run from an IntelliJ `.http` file. `auth-config` is an authentication configuration in
your private environment file (`http-client.private.env.json`). It must sign you in with your own HMPPS
Auth account and ask for the `write` scope, not use the service's client login.

Replace `{reportReference}` with the reference and `{prisonId}` with the prison code from the request,
for example `MDI`.

```
### Look up the incident report by reference
# @no-log
GET {{incident-report-api-url}}/incident-reports/reference/{reportReference}
Authorization: Bearer {{$auth.token("auth-config")}}

> {%
  client.global.clear("reportId")
  const report = response.body
  client.log(`${report.reportReference} ${report.location} ${report.type} ${report.incidentDateAndTime} ${report.status}`)
  if (response.status === 200 && report.location === "{prisonId}") {
    client.global.set("reportId", report.id)
  } else {
    client.log("reportId not set - check the reference and prison")
  }
%}

### Delete the incident report
# @no-log
DELETE {{incident-report-api-url}}/incident-reports/{{reportId}}
Authorization: Bearer {{$auth.token("auth-config")}}

> {%
  client.log(`Delete returned ${response.status}`)
  client.global.clear("reportId")
%}
```

The script after the look-up passes the report's id to the delete, so it never has to be copied by hand.
It is written to guard against deleting the wrong report, but still check the logged details before running the delete:

- It clears any id left over from an earlier run first. IntelliJ keeps these values for the whole session,
  so without this a failed look-up would leave the delete pointing at a previous report.
- It only passes the id on if the report belongs to the expected prison. Otherwise IntelliJ refuses to
  send the delete, reporting an "unsubstituted variable 'reportId'". If you see that, read the look-up's
  log: either the look-up failed or the prison did not match. Check that `{prisonId}` has been replaced.
- It logs only the reference, prison, type, incident date and status, which is enough to check it is the
  right report without showing any of its content.
- The delete clears the id afterwards, so running it a second time does nothing.

Keep `# @no-log` on both requests. Without it IntelliJ saves each response to its request history in the
project's `.idea/httpRequests` folder, leaving a copy of the report you are deleting on your machine. If a
request has already been run without it, delete that entry from the history.

Do not commit the `.http` file with a real reference or id in it.

## Checking it worked

Query the `nomisapi-prod` Application Insights workspace. Logs can arrive up to five minutes late, so wait
at least that long before relying on the result. Replace `<reportReference>` and `<id>`:

```
union
 (AppEvents
   | where AppRoleName in ("hmpps-incident-reporting-api", "hmpps-prisoner-to-nomis-update", "hmpps-prisoner-events")
   | where tostring(Properties) has "<reportReference>" or tostring(Properties) has "<id>"
   | project TimeGenerated, AppRoleName, Name),
 (AppRequests
   | where Url has "<reportReference>" or Url has "<id>"
   | project TimeGenerated, AppRoleName, Name = strcat(Name, " ", ResultCode))
| where TimeGenerated > ago(1h)
| order by TimeGenerated asc
```

A successful delete shows these entries within a couple of seconds of each other:

| Service | Entry |
|---|---|
| hmpps-incident-reporting-api | `DELETE /incident-reports/{id} 200` and `Deleted incident report` |
| hmpps-incident-reporting-api | `INCIDENT_REPORT_DELETED` (the audit message) |
| hmpps-nomis-prisoner-api | `DELETE /incidents/{incidentId} 204` |
| hmpps-prisoner-to-nomis-update | `incident-delete-success` with the NOMIS incident number (`nomisIncidentId`) |
| hmpps-prisoner-events | `INCIDENT-DELETED-CASES` and related entries, with `auditModuleName` `DPS_SYNCHRONISATION` |

## If something goes wrong

- **403 Forbidden**: the token is missing the role or the `write` scope.
- **404 Not Found** on the delete: the report has already gone, or the id is wrong. Check step 1 again.
- **No `incident-delete-success`, or the NOMIS delete failed**: the report is gone from this service but
  not from NOMIS. The message will be on the incidents dead-letter queue of the NOMIS update service. Ask
  Syscon to remove the NOMIS incident by hand, quoting the incident number.

## Related

The same process is kept in Confluence, in
[IRS Runbooks](https://dsdmoj.atlassian.net/wiki/spaces/4546331377/pages/6347522460). Update both together.
