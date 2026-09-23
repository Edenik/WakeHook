package ai.wakehook.app.ui

/**
 * Builds the copy-paste prompt a user hands to their AI agent (Claude, etc.) so it knows how to
 * read/write the user's alarms directly in Google Drive. No network calls, no app hooks — the
 * agent reaches Drive itself, per the `wakehook.json` self-describing protocol (Task 2).
 */
object AgentPrompt {
    fun build(fileLink: String?): String {
        val linkLine = if (fileLink != null) "\nFile link: $fileLink\n" else ""
        return """
            You can manage my alarms in the WakeHook Android app by editing a file named
            wakehook.json, which lives inside the "WakeHook" folder in my Google Drive.$linkLine
            The file is self-describing (it has its own "instructions" and "example" fields), but
            here is the schema for each entry in its "alarms" array:

            {
              "id": string,                 // unique id; keep it stable across edits
              "hour": 0-23,
              "minute": 0-59,
              "label": string,               // short description, e.g. "Gym"
              "daysOfWeek": [0-6],            // 0=Sunday..6=Saturday; weekly repeat, empty if not weekly
              "dates": ["yyyy-MM-dd", ...],   // specific one-time calendar dates, empty if not date-based
              "enabled": true/false,
              "source": string                // who created/owns it, e.g. "claude"
            }

            How to manage alarms:
            - To ADD an alarm: append a new object to "alarms" with a fresh unique "id" and
              "source" set to your name (e.g. "claude").
            - To EDIT an alarm: find it by "id" (or by matching hour/minute/label) and update its
              fields in place.
            - To CANCEL an alarm: either remove its object from "alarms", or set "enabled": false
              to disable it without deleting.
            - To LIST alarms: read the "alarms" array and summarize each entry's time, label, and
              schedule (daysOfWeek or dates).

            Save the file back to wakehook.json when you're done; the WakeHook app picks up your
            changes on its next sync.
        """.trimIndent()
    }
}
