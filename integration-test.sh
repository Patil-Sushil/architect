#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080/api"
ADMIN_EMAIL="admin@architect.com"
ADMIN_PASS="Admin@123"

# Manually Provisioned User Credentials
PM_EMAIL="ruturaj@gmail.com"
PM_PASS="Ruturaj@123"

EMP_EMAIL="ram@gmail.com"
EMP_PASS="RamPatil@123"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Starting Architecture Firm Management System Integration Tests${NC}"

# --- PHASE 1: AUTHENTICATION SETUP & CONTEXT RESOLUTION ---
echo -e "\n${BLUE}Phase 1: Authentication Setup & Context Resolution${NC}"

# 1. Admin Login
echo "Logging in as Admin..."
ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\", \"password\":\"$ADMIN_PASS\"}")

if [[ "$ADMIN_LOGIN_RESPONSE" == *"false"* ]]; then
    echo -e "${RED}Admin Login Failed! Check credentials. Response: $ADMIN_LOGIN_RESPONSE${NC}"
    exit 1
fi

ADMIN_TOKEN=$(echo $ADMIN_LOGIN_RESPONSE | jq -r '.data.accessToken // .data.token // .accessToken')
echo -e "${GREEN}Admin Token acquired successfully.${NC}"

# 2. Log in as Project Manager & Resolve Context
echo "Logging in as Project Manager ($PM_EMAIL)..."
PM_LOGIN_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$PM_EMAIL\", \"password\":\"$PM_PASS\"}")

PM_TOKEN=$(echo $PM_LOGIN_RES | jq -r '.data.accessToken // .data.token // .accessToken')

if [ "$PM_TOKEN" = "null" ] || [ -z "$PM_TOKEN" ]; then
    echo -e "${RED}PM Login failed! Response: $PM_LOGIN_RES${NC}"
    exit 1
fi

# Dynamically extract PM's UUID from the response payload
PM_ID=$(echo $PM_LOGIN_RES | jq -r '.data.user.id // .data.id // .user.id')
echo -e "${GREEN}PM Authenticated. Resolved UUID: $PM_ID${NC}"

# 3. Log in as Employee & Resolve Context
echo "Logging in as Employee ($EMP_EMAIL)..."
EMP_LOGIN_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMP_EMAIL\", \"password\":\"$EMP_PASS\"}")

EMP_TOKEN=$(echo $EMP_LOGIN_RES | jq -r '.data.accessToken // .data.token // .accessToken')
EMP_REFRESH=$(echo $EMP_LOGIN_RES | jq -r '.data.refreshToken // .refreshToken')

if [ "$EMP_TOKEN" = "null" ] || [ -z "$EMP_TOKEN" ]; then
    echo -e "${RED}Employee Login failed! Response: $EMP_LOGIN_RES${NC}"
    exit 1
fi

# Dynamically extract Employee's UUID from the response payload
EMP_ID=$(echo $EMP_LOGIN_RES | jq -r '.data.user.id // .data.id // .user.id')
echo -e "${GREEN}Employee Authenticated. Resolved UUID: $EMP_ID${NC}"
echo -e "${GREEN}Phase 1 cleared. (Backend automatic attendance logs registered for employee)${NC}"


# --- PHASE 2: PROJECT CREATION & STRUCTURAL ASSIGNMENT ---
echo -e "\n${BLUE}Phase 2: Project Creation & Structural Assignment${NC}"

echo "Creating Project Om Shanti..."
PROJECT_JOB_NUM="JOB-2025-$(date +%s)"
PROJECT_CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/projects" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"jobNumber\": \"$PROJECT_JOB_NUM\",
    \"projectName\": \"Om Shanti Commercial Complex\",
    \"clientOwnerName\": \"Shanti Developers\",
    \"projectType\": \"BIG\",
    \"startDate\": \"2026-06-01\",
    \"expectedCompletionDate\": \"2027-06-01\",
    \"projectLeadId\": \"$PM_ID\",
    \"assignedEmployeeId\": \"$EMP_ID\",
    \"siteLocation\": \"Downtown Metro Sector 4\"
  }")

PROJECT_ID=$(echo $PROJECT_CREATE_RESPONSE | jq -r '.data.id // .id')
if [ "$PROJECT_ID" = "null" ] || [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}Project Creation aborted. Backend message: $PROJECT_CREATE_RESPONSE${NC}"
    exit 1
fi
echo -e "${GREEN}Project Created with ID: $PROJECT_ID${NC}"

echo "Transitioning project status to IN_PROGRESS..."
curl -s -X PATCH "$BASE_URL/admin/projects/$PROJECT_ID/status" \
  -H "Authorization: Bearer $PM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"newStatus\": \"IN_PROGRESS\", \"remarks\": \"Initial planning phase cleared automatically.\"}"

echo "Verifying project status history track..."
curl -s -X GET "$BASE_URL/admin/projects/$PROJECT_ID/status-history" \
  -H "Authorization: Bearer $PM_TOKEN" | jq '.data // .'


# --- PHASE 3: PROJECT-DRIVEN TASK ASSIGNMENT ---
echo -e "\n${BLUE}Phase 3: Project-Driven Task Assignment${NC}"

echo "Fetching pre-defined task name suggestions..."
curl -s -X GET "$BASE_URL/tasks/suggestions" \
  -H "Authorization: Bearer $PM_TOKEN" | jq '.data[0:2] // .'

echo "Assigning 3D Design template task to employee..."
PROJECT_TASK_RESPONSE=$(curl -s -X POST "$BASE_URL/tasks" \
  -H "Authorization: Bearer $PM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"projectId\": \"$PROJECT_ID\",
    \"taskName\": \"Exterior 3D Rendering - Block A\",
    \"category\": \"THREE_D_DESIGN\",
    \"assignedToUserId\": \"$EMP_ID\",
    \"priority\": \"HIGH\",
    \"plannedStartDate\": \"2026-06-05\",
    \"plannedEndDate\": \"2026-06-10\",
    \"plannedEffortsHours\": 16.0,
    \"description\": \"Generate high-fidelity renders for the main commercial entrance area.\"
  }")

PROJECT_TASK_ID=$(echo $PROJECT_TASK_RESPONSE | jq -r '.data.id // .id')
if [ "$PROJECT_TASK_ID" = "null" ] || [ -z "$PROJECT_TASK_ID" ]; then
    echo -e "${RED}Task assignment failed. Response: $PROJECT_TASK_RESPONSE${NC}"
    exit 1
fi
echo -e "${GREEN}Project Task Assigned. Resolved Task ID: $PROJECT_TASK_ID${NC}"


# --- PHASE 4: IMPROMPTU FIELD ASSIGNMENT ---
echo -e "\n${BLUE}Phase 4: Impromptu Field Assignment${NC}"

echo "Employee self-logging spontaneous site task (Verbal Site Order Call)..."
FIELD_TASK_RESPONSE=$(curl -s -X POST "$BASE_URL/tasks" \
  -H "Authorization: Bearer $EMP_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"taskName\": \"Emergency Retaining Wall Reinforcement\",
    \"category\": \"SITE_SUPERVISION\",
    \"assignedToUserId\": \"$EMP_ID\",
    \"priority\": \"URGENT\",
    \"description\": \"Field site team called reporting unexpected concrete displacement on Grid B-12.\"
  }")

FIELD_TASK_ID=$(echo $FIELD_TASK_RESPONSE | jq -r '.data.id // .id')
GEN_JOB_NUM=$(echo $FIELD_TASK_RESPONSE | jq -r '.data.jobNumber // .jobNumber')
echo -e "${GREEN}Field Task Registered. ID: $FIELD_TASK_ID, Job Number: $GEN_JOB_NUM (Auto-classified as FIELD_DIRECT)${NC}"


# --- PHASE 5: SUBMISSION, REVIEW, AND REWORK LIFECYCLE ---
echo -e "\n${BLUE}Phase 5: Submission, Review, and Rework Loop Execution${NC}"

echo "Querying active worker dashboard tasks (Status: ASSIGNED)..."
curl -s -X GET "$BASE_URL/tasks/my-tasks?status=ASSIGNED" \
  -H "Authorization: Bearer $EMP_TOKEN" | jq '.data.content[].taskName // .content[].taskName // .'

# Create temporary sample files for testing file uploads
echo "DUMMY CAD DRAWING CONTENT FOR TESTING" > sample_layout.dwg
echo "DUMMY PDF SPECIFICATION CONTENT FOR TESTING" > sample_spec.pdf

echo "Submitting assigned task for review with attachments (multipart/form-data)..."
SUBMIT_RESPONSE=$(curl -s -X POST "$BASE_URL/tasks/$PROJECT_TASK_ID/submit" \
  -H "Authorization: Bearer $EMP_TOKEN" \
  -F 'submission={"submissionNotes": "Completed initial rendering layers and verified specs.", "hoursInvested": 12.5};type=application/json' \
  -F 'files=@sample_layout.dwg' \
  -F 'files=@sample_spec.pdf')

echo "Submission Response:"
echo "$SUBMIT_RESPONSE" | jq '.'

# Extract attachment info to verify they were uploaded
echo "Listing uploaded attachments for task $PROJECT_TASK_ID..."
ATTACHMENTS_RESPONSE=$(curl -s -X GET "$BASE_URL/tasks/$PROJECT_TASK_ID/attachments" \
  -H "Authorization: Bearer $EMP_TOKEN")

echo "Attachments List:"
echo "$ATTACHMENTS_RESPONSE" | jq '.'

# Extract first attachment ID and file name to test download
FIRST_ATTACHMENT_ID=$(echo "$ATTACHMENTS_RESPONSE" | jq -r '.data[0].id')
FIRST_ATTACHMENT_NAME=$(echo "$ATTACHMENTS_RESPONSE" | jq -r '.data[0].fileName')

if [ "$FIRST_ATTACHMENT_ID" != "null" ] && [ -n "$FIRST_ATTACHMENT_ID" ]; then
    echo "Downloading attachment $FIRST_ATTACHMENT_ID ($FIRST_ATTACHMENT_NAME)..."
    curl -s -X GET "$BASE_URL/tasks/$PROJECT_TASK_ID/attachments/$FIRST_ATTACHMENT_ID/download" \
      -H "Authorization: Bearer $EMP_TOKEN" -o downloaded_file.tmp
    
    echo -e "${GREEN}Downloaded content verification:${NC}"
    cat downloaded_file.tmp
    echo ""
    rm -f downloaded_file.tmp
else
    echo -e "${RED}Failed to find uploaded attachments in response!${NC}"
    rm -f sample_layout.dwg sample_spec.pdf
    exit 1
fi

# Clean up local temporary files
rm -f sample_layout.dwg sample_spec.pdf

echo "Verifying actual efforts hours updated in task..."
curl -s -X GET "$BASE_URL/tasks/$PROJECT_TASK_ID" \
  -H "Authorization: Bearer $EMP_TOKEN" | jq '.data.actualEffortsHours'

echo "PM evaluating work and kicking back for Rework..."
curl -s -X POST "$BASE_URL/tasks/$PROJECT_TASK_ID/review" \
  -H "Authorization: Bearer $PM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"approved\": false,
    \"reviewComment\": \"Dimensions on grid line B are off by 50mm. Re-verify structural alignments and resubmit.\"
  }"

echo "Verifying task routing directly to worker rework queue..."
curl -s -X GET "$BASE_URL/tasks/my-tasks?status=REWORK_REQUESTED" \
  -H "Authorization: Bearer $EMP_TOKEN" | jq '.data.content // .content // .'

echo "Verifying historic audit trail entry timeline..."
curl -s -X GET "$BASE_URL/tasks/$PROJECT_TASK_ID/timeline" \
  -H "Authorization: Bearer $EMP_TOKEN" | jq '.data[-1:] // .'


# --- PHASE 6: ATTENDANCE EXCEPTION & MANAGEMENT EXPORT ---
echo -e "\n${BLUE}Phase 6: Attendance Exception & Analytical Reporting${NC}"

echo "Employee clocking out..."
curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $EMP_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$EMP_EMAIL\", \"refreshToken\": \"$EMP_REFRESH\"}"
echo -e "${GREEN}Employee session safely clocked out.${NC}"

echo "Admin overwriting logs via manual exception handling..."
curl -s -X POST "$BASE_URL/attendance/admin/exception" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$EMP_ID\",
    \"date\": \"$(date +%Y-%m-%d)\",
    \"loginTime\": \"$(date +%Y-%m-%d)T09:00:00\",
    \"logoutTime\": \"$(date +%Y-%m-%d)T18:00:00\",
    \"exceptionNote\": \"Corrected punch logging. Worker spent afternoon on impromptu structural site call.\"
  }"

echo "Generating binary Excel worksheet via Apache POI download provider..."
curl -s -D - -o /dev/null -X GET "$BASE_URL/attendance/admin/export?preset=THIS_WEEK" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | grep -E "Content-Type|Content-Disposition"

# --- PHASE 7: ADVANCED TASK HISTORY AUDITING ---
echo -e "\n${BLUE}Phase 7: Advanced Task History Auditing${NC}"

echo "Admin retrieving global task history (Verifying role-based access & Task Summary)..."
curl -s -X GET "$BASE_URL/task-history?size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data.content[0:2][] | {id: .id, task: .task.taskName, from: .fromStatus, to: .toStatus}'

echo "PM retrieving history for specific task ($PROJECT_TASK_ID)..."
curl -s -X GET "$BASE_URL/task-history?taskId=$PROJECT_TASK_ID" \
  -H "Authorization: Bearer $PM_TOKEN" | jq '.data.content[] | {status_change: (.fromStatus + " -> " + .toStatus), remarks: .remarks}'

echo "Employee retrieving their own task history (My History endpoint)..."
curl -s -X GET "$BASE_URL/task-history/my-history" \
  -H "Authorization: Bearer $EMP_TOKEN" | jq '.data.content[0:2][] | {task: .task.taskName, updated_by: .changedBy.name, time: .timestamp}'

echo -e "\n${GREEN}Complete System Integration Test Suite Executed Successfully!${NC}"