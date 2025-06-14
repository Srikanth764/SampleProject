# Splunk Queries for User Management Application

These queries are designed to work with logs generated by the User Management API, assuming logs are ingested into Splunk with `sourcetype="java_app_logs"` and originate from the `com.example.crudapp` package.

## 1. General Application Errors
Shows a timeline of warnings and errors originating from the app.
```splunk
sourcetype="java_app_logs" (level=WARN OR level=ERROR) "com.example.crudapp"
| timechart count by level
```

## 2. User Creation Activity
**Count of users created per hour:**
```splunk
sourcetype="java_app_logs" "Successfully created user with ID:"
| timechart span=1h count as created_users
```
**Details of created users:**
```splunk
sourcetype="java_app_logs" "Successfully created user with ID:"
| rex field=_raw "Successfully created user with ID: (?<userId>\S+)\. Name: '(?<userName>[^']*)', Email: '(?<userEmail>[^']*)'"
| table _time, userId, userName, userEmail
```

## 3. "User Not Found" (GET Requests)
Occurrences of "User not found" for GET requests, counted by user ID.
```splunk
sourcetype="java_app_logs" "User not found. Responding with status 404." "GET /api/users/"
| rex field=_raw "GET /api/users/(?<userId>\S+) - User not found"
| stats count by userId
| sort -count
```

## 4. User Update Activity
**Count of successful updates per hour:**
```splunk
sourcetype="java_app_logs" "User updated successfully." "PUT /api/users/"
| timechart span=1h count as updated_users
```
**"User not found" during update attempts:**
```splunk
sourcetype="java_app_logs" "Update failed, user not found." "PUT /api/users/"
| rex field=_raw "PUT /api/users/(?<userId>\S+) - Update failed"
| stats count by userId
| sort -count
```

## 5. User Deletion Activity
**Count of successful deletions per hour:**
```splunk
sourcetype="java_app_logs" "User deleted successfully." "DELETE /api/users/"
| timechart span=1h count as deleted_users
```
**"User not found" during delete attempts:**
```splunk
sourcetype="java_app_logs" "Delete failed, user not found." "DELETE /api/users/"
| rex field=_raw "DELETE /api/users/(?<userId>\S+) - Delete failed"
| stats count by userId
| sort -count
```

## 6. All Logs for a Specific User ID
Replace `{USER_ID}` with the actual user ID.
```splunk
sourcetype="java_app_logs" "com.example.crudapp" ("ID: {USER_ID}" OR "/api/users/{USER_ID}" OR "user with ID: {USER_ID}")
| sort -_time
```

## 7. API Endpoint Usage Frequency
Shows how often each API endpoint (and method) is being used.
```splunk
sourcetype="java_app_logs" "com.example.crudapp.controller.UserController" "Received request"
| rex field=_raw "(?<httpMethod>POST|GET|PUT|DELETE) (?<apiEndpoint>/api/users[^ ]*)"
| stats count by httpMethod, apiEndpoint
| sort -count
```

## 8. Service Layer Validation Failures
Counts validation failures logged by `UserService`.
```splunk
sourcetype="java_app_logs" "com.example.crudapp.service.UserService" level=WARN ("failed:" OR "is null" OR "is empty")
| rex field=_raw "User (?<operationType>\w+) failed: (?<reason>.+)"
| stats count by operationType, reason
| sort -count
```
