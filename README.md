# Application Data Schema & Features (Updated)

## Project & Task Hierarchy

### Projects
Each **Project** acts as a parent container for multiple **Tasks**.

| Column | Type | Notes |
|---|---|---|
| id | INTEGER (PK) | Primary key. |
| projectName | TEXT | Unique project name. |
| taskItemNumber | INTEGER | Count of tasks in this project. Updated via trigger. |
| startDate | INTEGER (epoch ms) | Project creation/start timestamp. |
| oldestExpirationDate | INTEGER (epoch ms) | Minimum dueAt across tasks. Computed field. |
| userSatisfaction | REAL | Aggregated satisfaction metric. |

### Tasks (Child of Project)
| Column | Type | Notes |
|---|---|---|
| id | INTEGER (PK) | |
| projectId | INTEGER (FK → Project.id) | Parent link. |
| title | TEXT | |
| description | TEXT | |
| status | TEXT | Enum: `OPEN`, `IN_PROGRESS`, `DONE`, `CANCELLED`. |
| dueAt | INTEGER (epoch ms) | Due date. |
| expectedDuration | INTEGER | In minutes/hours. |
| actualDuration | INTEGER | Recorded via stopwatch. |
| createdAt | INTEGER | Timestamp. |
| updatedAt | INTEGER | Timestamp. |

---

# Task View Features

- **Stopwatch Timer** — Start/stop/reset to measure actual time worked. Writes to `actualDuration`.
- **Expected Duration Input** — unchanged (form field).
- **Due Date Input** — unchanged (calendar picker).
- **Expected Finish Date Input** — unchanged (calendar picker).
- **Markdown Note Section** — collapsible; opens in popup dialog when tapped.
- **Pie Chart** — visual ratio of `actualDuration / expectedDuration`.


# Navigation Flow
```mermaid
flowchart TD
  HOME([HOME]) --> MainPage[Main Page]
  MainPage --> ThemeEdit[Theme Edit Page]
  MainPage --> ProjectDashboard[Project Dashboard]
  ProjectDashboard --> TasksDashboard[Tasks Dashboard]
  TasksDashboard --> TaskView[Task View]
  TaskView --> TaskEdit[Task Edit]
```

# Data Model (ER)
```mermaid
erDiagram
  PROJECT {
    INT id PK
    TEXT projectName
    INT taskItemNumber
    LONG startDate
    LONG oldestExpirationDate
    FLOAT userSatisfaction
  }
  TASK {
    INT id PK
    INT projectId FK
    TEXT title
    TEXT description
    TEXT status
    LONG dueAt
    LONG expFinishEpoch
    LONG expDurMinutes
    LONG actMinutes
    LONG createdAt
    LONG updatedAt
    LONG runningSinceEpoch
    TEXT note
  }
  TASK_TIME_LOG {
    INT id PK
    INT taskId FK
    LONG startedAt
    LONG endedAt
    LONG durationSec
  }
  CHECKLIST_ITEM {
    INT id PK
    INT taskId FK
    TEXT text
    BOOL isChecked
    INT orderIndex
  }
  REMINDER {
    INT id PK
    INT taskId FK
    LONG remindAt
    TEXT channel
    LONG deliveredAt
  }
  TAG {
    INT id PK
    TEXT name
    TEXT color
  }
  TASK_TAG {
    INT taskId FK
    INT tagId FK
  }

  PROJECT ||--o{ TASK : contains
  TASK ||--o{ CHECKLIST_ITEM : includes
  TASK ||--o{ REMINDER : triggers
  TASK ||--o{ TASK_TAG : tagged_with
  TASK ||--o{ TASK_TIME_LOG : tracked_by
  TAG ||--o{ TASK_TAG : maps
```



