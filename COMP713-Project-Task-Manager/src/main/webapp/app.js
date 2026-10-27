
/*
 * COMP713 Distributed and Mobile Systems
 * Project and Task Management System
 *
 * Browser client communicating with Jakarta REST API.
 * Demo identity selection is NOT authentication.
 */

const $ = id => document.getElementById(id);

const statusValues = [
    "TODO",
    "IN_PROGRESS",
    "DONE"
];

let users = [];
let selectedProject = null;


// --------------------------------------
// Display messages
// --------------------------------------

function message(text, kind = "success") {
    $("message").textContent = text;
    $("message").className = `message ${kind}`;
}


// --------------------------------------
// REST API communication
// --------------------------------------

async function api(method, path, body) {

    const options = {
        method,
        headers: {
            Accept: "application/json"
        }
    };

    if (body !== undefined) {
        options.headers["Content-Type"] =
            "application/json";

        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(
            `api/${path}`,
            options
        );

        const raw = await response.text();

        let data;

        try {
            data = raw ? JSON.parse(raw) : null;
        } catch {
            data = {
                error: raw || "Invalid response"
            };
        }

        $("apiActivity").textContent =
            `${method} api/${path} → ${response.status}`;

        if (!response.ok) {
            throw new Error(
                data?.error || `HTTP ${response.status}`
            );
        }

        return data;

    } catch (error) {

        $("apiActivity").textContent =
            `${method} api/${path} → ${error.message}`;

        throw error;
    }
}


// --------------------------------------
// Error handling
// --------------------------------------

async function attempt(action) {
    try {
        await action();
    } catch (error) {
        message(
            error.message || "Request failed",
            "error"
        );
    }
}


// --------------------------------------
// HTML helper methods
// --------------------------------------

function empty(container, text) {

    container.replaceChildren();

    const note = document.createElement("p");

    note.className = "empty-message";
    note.textContent = text;

    container.append(note);
}

function element(tag, text, className) {

    const node = document.createElement(tag);

    node.textContent = text;

    if (className) {
        node.className = className;
    }

    return node;
}

function makeSelect(values, current) {

    const select = document.createElement("select");

    for (const [value, label] of values) {
        select.add(new Option(label, value));
    }

    select.value =
        current == null ? "" : String(current);

    return select;
}


// --------------------------------------
// Employee management
// --------------------------------------

function employees() {

    return users.filter(
        user => user.role === "EMPLOYEE"
    );
}

function assigneeChoices() {

    return [
        ["", "Unassigned"],
        ...employees().map(
            user => [
                String(user.id),
                user.displayName
            ]
        )
    ];
}

function renderEmployeeOptions() {

    const selector = $("employeeSelect");

    const previous = selector.value;

    selector.replaceChildren(
        new Option("Select an employee", "")
    );

    for (const user of employees()) {
        selector.add(
            new Option(
                user.displayName,
                String(user.id)
            )
        );
    }

    if (
        [...selector.options].some(
            option => option.value === previous
        )
    ) {
        selector.value = previous;
    }

    $("taskAssignee").replaceChildren(
        ...assigneeChoices().map(
            ([value, label]) =>
                new Option(label, value)
        )
    );
}

async function loadUsers() {

    users = await api("GET", "users");

    const list = $("userList");

    list.replaceChildren();

    if (users.length === 0) {
        empty(list, "No team members yet.");
    }

    for (const user of users) {

        list.append(
            element(
                "p",
                `${user.displayName} (${user.username}) — ${user.role}`
            )
        );
    }

    renderEmployeeOptions();
}


// --------------------------------------
// Project management
// --------------------------------------

async function loadProjects() {

    const projects = await api(
        "GET",
        "projects"
    );

    const list = $("projectList");

    list.replaceChildren();

    if (projects.length === 0) {
        empty(list, "No projects yet.");
    }

    for (const project of projects) {

        const card = element(
            "div",
            "",
            "project-item"
        );

        const title = element(
            "h3",
            project.name
        );

        const description = element(
            "p",
            project.description || "No description"
        );

        const open = element(
            "button",
            "View project tasks",
            "secondary-button"
        );

        open.type = "button";

        open.addEventListener(
            "click",
            () => attempt(async () => {

                selectedProject = project;

                $("selectedProjectName").textContent =
                    project.name;

                $("selectedProjectDescription").textContent =
                    project.description || "";

                $("taskSection").classList.remove(
                    "hidden"
                );

                await loadTasks();
            })
        );

        card.append(
            title,
            description,
            open
        );

        list.append(card);
    }
}

// --------------------------------------
// Location check-ins (from the Android app)
// --------------------------------------

function checkInLine(checkIn) {

    const line = element("p", "", "check-in-line");

    if (!checkIn) {
        line.textContent = "No check-ins yet.";
        return line;
    }

    const when = new Date(checkIn.checkedInAt).toLocaleString();
    const who = checkIn.employeeName || `user ${checkIn.userId}`;

    if (checkIn.source === "GPS") {

        const lat = checkIn.latitude.toFixed(5);
        const lon = checkIn.longitude.toFixed(5);

        const accuracy = checkIn.accuracyMetres == null
            ? ""
            : ` (±${Math.round(checkIn.accuracyMetres)} m)`;

        const map = document.createElement("a");
        map.href =
            `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=17/${lat}/${lon}`;
        map.target = "_blank";
        map.rel = "noopener";
        map.textContent = `${lat}, ${lon}`;

        line.append(
            `Last check-in: ${who}, ${when}, GPS `,
            map,
            accuracy
        );

    } else {
        line.textContent =
            `Last check-in: ${who}, ${when}, manual location "${checkIn.locationNote}"`;
    }

    return line;
}

// --------------------------------------
// Display tasks for selected project
// --------------------------------------

async function loadTasks() {

    if (!selectedProject) {
        return;
    }

    const tasks = await api(
        "GET",
        `projects/${selectedProject.id}/tasks`
    );

    const checkIns = await api(
        "GET",
        `projects/${selectedProject.id}/check-ins`
    );

    // Check-ins arrive newest first, so the first one per task is the latest.
    const latestCheckIn = new Map();

    for (const checkIn of checkIns) {
        if (!latestCheckIn.has(checkIn.taskId)) {
            latestCheckIn.set(checkIn.taskId, checkIn);
        }
    }

    const list = $("taskList");

    list.replaceChildren();

    if (!tasks.length) {
        empty(
            list,
            "No tasks in this project yet."
        );
    }

    for (const task of tasks) {

        const card = element(
            "div",
            "",
            "task-item"
        );

        card.append(
            element("h4", task.title),

            element(
                "p",
                task.description || "No description"
            ),

            element(
                "p",
                `Assigned to: ${
                    task.assigneeName || "Unassigned"
                }`
            ),
            
            checkInLine(latestCheckIn.get(task.id))
        );

        // Assign or reassign an employee.

        const assignee = makeSelect(
            assigneeChoices(),
            task.assignedUserId
        );

        assignee.setAttribute(
            "aria-label",
            `Assign employee for ${task.title}`
        );

        const assign = element(
            "button",
            "Save assignment"
        );

        assign.type = "button";

        assign.addEventListener(
            "click",
            () => attempt(async () => {

                await api(
                    "PUT",
                    `projects/${selectedProject.id}/tasks/${task.id}/assignment`,
                    {
                        assignedUserId:
                            assignee.value
                                ? Number(assignee.value)
                                : null
                    }
                );

                message("Assignment saved.");

                await loadTasks();
                await loadEmployeeTasks();
            })
        );

        const assignmentRow = element(
            "div",
            "",
            "task-assignment-row"
        );

        assignmentRow.append(
            assignee,
            assign
        );

        // Change task status.

        const status = makeSelect(
            statusValues.map(
                value => [
                    value,
                    value.replace("_", " ")
                ]
            ),
            task.status
        );

        status.setAttribute(
            "aria-label",
            `Status for ${task.title}`
        );

        const save = element(
            "button",
            "Update status"
        );

        save.type = "button";

        save.addEventListener(
            "click",
            () => attempt(async () => {

                await saveTaskStatus(
                    task,
                    status.value
                );
            })
        );

        const statusRow = element(
            "div",
            "",
            "task-status-row"
        );

        statusRow.append(
            status,
            save
        );

        card.append(
            assignmentRow,
            statusRow
        );

        list.append(card);
    }
}


// --------------------------------------
// Update task status
// --------------------------------------

async function saveTaskStatus(task, status) {

    await api(
        "PUT",
        `projects/${task.projectId}/tasks/${task.id}/status`,
        { status }
    );

    message("Task status saved.");

    await loadTasks();
    await loadEmployeeTasks();
}


// --------------------------------------
// Employee's assigned tasks
// --------------------------------------

async function loadEmployeeTasks() {

    const employeeId =
        $("employeeSelect").value;

    const list = $("employeeTaskList");

    if (!employeeId) {

        empty(
            list,
            "Select an employee to see their tasks."
        );

        return;
    }

    const tasks = await api(
        "GET",
        `users/${employeeId}/tasks`
    );

    list.replaceChildren();

    if (!tasks.length) {

        empty(
            list,
            "No tasks assigned to this employee."
        );
    }

    for (const task of tasks) {

        const card = element(
            "div",
            "",
            "task-item"
        );

        card.append(
            element("h4", task.title),

            element(
                "p",
                task.description || "No description"
            ),

            element(
                "p",
                `Project ID: ${task.projectId}`
            )
        );

        const select = makeSelect(
            statusValues.map(
                value => [
                    value,
                    value.replace("_", " ")
                ]
            ),
            task.status
        );

        select.setAttribute(
            "aria-label",
            `Employee task status for ${task.title}`
        );

        const button = element(
            "button",
            "Save my progress"
        );

        button.type = "button";

        button.addEventListener(
            "click",
            () => attempt(
                () => saveTaskStatus(
                    task,
                    select.value
                )
            )
        );

        const row = element(
            "div",
            "",
            "task-status-row"
        );

        row.append(
            select,
            button
        );

        card.append(row);

        list.append(card);
    }
}

// --------------------------------------
// Activity log (written by the message consumer)
// --------------------------------------

async function loadActivity() {

    const list = $("activityList");

    const entries = await api(
        "GET",
        "activity?limit=20"
    );

    list.replaceChildren();

    if (!entries.length) {

        empty(
            list,
            "No activity yet. Is the task-event-consumer running?"
        );

        return;
    }

    for (const entry of entries) {

        const when =
            new Date(entry.processedAt).toLocaleTimeString();

        const title =
            entry.taskTitle || `Task ${entry.taskId}`;

        let what;

        if (entry.eventType === "TASK_ASSIGNED") {
            what = entry.assignedUserId
                ? `assigned to ${entry.assigneeName || "user " + entry.assignedUserId}`
                : "unassigned";
        } else {
            what = `status changed to ${(entry.status || "").replace("_", " ")}`;
        }

        const item = element("div", "", "activity-item");

        item.append(
            element("span", when, "activity-time"),
            element("span", `${title}: ${what}`)
        );

        if (entry.attempts > 1) {
            item.append(
                element(
                    "span",
                    `(after ${entry.attempts} attempts)`,
                    "activity-retry"
                )
            );
        }

        list.append(item);
    }
}

// --------------------------------------
// Live updates (WebSocket)
// --------------------------------------
//
// The server pushes a message whenever a task changes or an employee
// checks in (from Android or another browser). The page reloads only
// the affected sections - no manual refresh and no polling.
// If the connection drops, it reconnects with a growing delay and
// reloads everything, because changes may have been missed meanwhile.

let liveSocket = null;
let reconnectDelayMs = 1000;
let liveConnectedBefore = false;
let livePaused = false;
const liveLatencies = [];
const LIVE_TARGET_MS = 1000;

function setLiveStatus(text, kind) {
    $("liveStatus").textContent = text;
    $("liveStatus").className = `live-status ${kind}`;
}

function connectLiveUpdates() {

    const url = new URL("ws/updates", window.location.href);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";

    setLiveStatus("Connecting…", "connecting");

    liveSocket = new WebSocket(url);

    liveSocket.addEventListener("open", () => {

        reconnectDelayMs = 1000;
        setLiveStatus("Connected — this page updates automatically", "connected");

        if (liveConnectedBefore) {
            // We may have missed changes while disconnected.
            attempt(restoreCurrentState);
            message("Live updates reconnected. Page refreshed with the latest data.");
        }

        liveConnectedBefore = true;
    });

    liveSocket.addEventListener("message", event => {

        let update;

        try {
            update = JSON.parse(event.data);
        } catch {
            return;
        }

        attempt(() => handleLiveUpdate(update));
    });

    liveSocket.addEventListener("close", () => {

        if (livePaused) {
            setLiveStatus("Disconnected on purpose — click Reconnect", "disconnected");
            return;
        }

        const seconds = Math.round(reconnectDelayMs / 1000);

        setLiveStatus(
            `Disconnected — reconnecting in ${seconds} s`,
            "disconnected"
        );

        setTimeout(connectLiveUpdates, reconnectDelayMs);
        reconnectDelayMs = Math.min(reconnectDelayMs * 2, 10000);
    });
}

async function restoreCurrentState() {

    await loadUsers();
    await loadProjects();
    await loadTasks();
    await loadEmployeeTasks();
    await loadActivity();
}

// Demo/test button: drop the connection, let another user make
// changes, then reconnect and check the page catches up.
function toggleLiveConnection() {

    if (!livePaused) {
        livePaused = true;
        $("liveToggleButton").textContent = "Reconnect live updates";
        liveSocket.close();
    } else {
        livePaused = false;
        $("liveToggleButton").textContent = "Disconnect live updates";
        connectLiveUpdates();
    }
}

async function handleLiveUpdate(update) {

    if (update.type === "CONNECTED") {
        return;
    }

    if (typeof update.sentAtMillis === "number") {
        recordLiveLatency(Date.now() - update.sentAtMillis);
    }

    const time = new Date().toLocaleTimeString();
    $("liveLast").textContent = `${time}: ${describeLiveUpdate(update)}`;

    if (selectedProject && selectedProject.id === update.projectId) {
        await loadTasks();
    }

    await loadEmployeeTasks();

    // The message consumer writes the activity log a moment later.
    if (update.type === "TASK_UPDATED") {
        setTimeout(() => attempt(loadActivity), 1500);
    }
}

function describeLiveUpdate(update) {

    const title = `"${update.title}"`;

    if (update.type === "CHECK_IN_CREATED") {
        return `${update.employeeName || "An employee"} checked in at ${title} (${update.source})`;
    }

    if (update.change === "CREATED") {
        return `New task ${title}`;
    }

    if (update.change === "ASSIGNMENT") {
        return `${title} assigned to ${update.assigneeName || "nobody"}`;
    }

    return `${title} changed to ${String(update.status).replace("_", " ")}`;
}

function recordLiveLatency(ms) {

    liveLatencies.push(Math.max(0, ms));

    const sorted = [...liveLatencies].sort((a, b) => a - b);
    const pick = fraction =>
        sorted[Math.min(sorted.length - 1, Math.floor((sorted.length - 1) * fraction))];

    const p95 = pick(0.95);
    const verdict = p95 <= LIVE_TARGET_MS ? "PASS" : "FAIL";

    $("liveLatency").textContent =
        `Delivery time (server → this page): last ${Math.round(ms)} ms, ` +
        `median ${Math.round(pick(0.5))} ms, p95 ${Math.round(p95)} ms ` +
        `over ${sorted.length} messages. Target p95 ≤ ${LIVE_TARGET_MS} ms: ${verdict}`;
}

// --------------------------------------
// Create user
// --------------------------------------

$("userForm").addEventListener(
    "submit",
    event => {

        event.preventDefault();

        attempt(async () => {

            await api(
                "POST",
                "users",
                {
                    username:
                        $("username").value.trim(),

                    displayName:
                        $("displayName").value.trim(),

                    role:
                        $("userRole").value
                }
            );

            $("userForm").reset();

            await loadUsers();

            if (selectedProject) {
                await loadTasks();
            }

            message("Team member created.");
        });
    }
);


// --------------------------------------
// Create project
// --------------------------------------

$("projectForm").addEventListener(
    "submit",
    event => {

        event.preventDefault();

        attempt(async () => {

            await api(
                "POST",
                "projects",
                {
                    name:
                        $("projectName").value.trim(),

                    description:
                        $("projectDescription").value.trim()
                }
            );

            $("projectForm").reset();

            await loadProjects();

            message("Project created.");
        });
    }
);


// --------------------------------------
// Create task
// --------------------------------------

$("taskForm").addEventListener(
    "submit",
    event => {

        event.preventDefault();

        attempt(async () => {

            if (!selectedProject) {
                throw new Error(
                    "Select a project first."
                );
            }

            await api(
                "POST",
                `projects/${selectedProject.id}/tasks`,
                {
                    title:
                        $("taskTitle").value.trim(),

                    description:
                        $("taskDescription").value.trim(),

                    status:
                        $("taskStatus").value,

                    assignedUserId:
                        $("taskAssignee").value
                            ? Number(
                                $("taskAssignee").value
                              )
                            : null
                }
            );

            $("taskForm").reset();

            await loadTasks();
            await loadEmployeeTasks();

            message("Task created.");
        });
    }
);


// --------------------------------------
// Refresh buttons
// --------------------------------------

$("refreshProjectsButton").addEventListener(
    "click",
    () => attempt(loadProjects)
);

$("refreshUsersButton").addEventListener(
    "click",
    () => attempt(async () => {

        await loadUsers();

        if (selectedProject) {
            await loadTasks();
        }
    })
);

$("refreshTasksButton").addEventListener(
    "click",
    () => attempt(loadTasks)
);

$("employeeSelect").addEventListener(
    "change",
    () => attempt(loadEmployeeTasks)
);

$("refreshEmployeeButton").addEventListener(
    "click",
    () => attempt(loadEmployeeTasks)
);

$("refreshActivityButton").addEventListener(
    "click",
    () => attempt(loadActivity)
);


// --------------------------------------
// Initial application load
// --------------------------------------

attempt(async () => {

    await loadUsers();
    await loadProjects();
    await loadActivity();

});

$("liveToggleButton").addEventListener("click", toggleLiveConnection);

connectLiveUpdates();