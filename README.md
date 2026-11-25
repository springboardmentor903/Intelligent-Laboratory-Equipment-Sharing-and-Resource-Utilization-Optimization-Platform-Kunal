# Intelligent Laboratory Equipment Sharing and Resource Utilization Optimization Platform

A centralized web-based platform designed to manage laboratory equipment, simplify resource booking, improve equipment utilization, and support efficient laboratory resource management.

**Developed by:** Kunal Choudhary  
**Program:** Infosys Springboard Internship  
**Project Type:** Full Stack Web Application

---

## 1. Project Overview

The Intelligent Laboratory Equipment Sharing and Resource Utilization Optimization Platform is a full-stack web application developed to simplify laboratory operations across institutions.

It provides a centralized system for managing laboratory equipment, users, institutions, departments, bookings, maintenance, calibration, notifications, resource sharing, billing, and reporting.

The platform aims to reduce manual work, improve equipment utilization, prevent booking conflicts, and provide better visibility into laboratory resources.

## 2. Project Objectives

- Centralized management of laboratory equipment.
- Simplified equipment booking and availability tracking.
- Improved resource utilization across departments.
- Role-based authentication and authorization.
- Maintenance and preventive maintenance management.
- Equipment calibration tracking and reminders.
- Document and certificate storage.
- Notifications and technician task management.
- Usage charges, budgets, invoices, and financial summaries.
- Reporting and analytics for institutional decision-making.

## 3. Technology Stack

| Component | Technology |
|---|---|
| Frontend | React.js |
| Programming Language | JavaScript |
| Styling | CSS, Tailwind CSS |
| Routing | React Router |
| HTTP Client | Axios |
| Icons | Lucide React |
| Build Tool | Vite |
| Backend | Java, Spring Boot |
| Backend Framework | Spring MVC |
| ORM | Hibernate |
| Data Access | Spring Data JPA |
| Database | PostgreSQL |
| Authentication | Spring Security, JWT |
| OAuth | Google OAuth2 |
| Build Tool | Maven |
| API Testing | Postman |
| Version Control | Git and GitHub |

Additional integrations include or may require separate configuration for Redis, Elasticsearch, Apache Kafka, AWS S3, Firebase, and email/SMS services.

## 4. System Architecture

The application follows a client-server architecture.

```text
              USER
                |
                v
       +-------------------+
       |   React Frontend  |
       |                   |
       | Pages & Components|
       | Authentication    |
       | Dashboards        |
       +---------+---------+
                 |
                 | HTTP / REST APIs
                 | Axios
                 v
       +-------------------+
       | Spring Boot API   |
       |                   |
       | REST Controllers  |
       | Service Layer     |
       | Repository Layer  |
       | Spring Security   |
       +---------+---------+
                 |
                 | JPA / Hibernate
                 v
       +-------------------+
       |   PostgreSQL      |
       |                   |
       | Users             |
       | Resources         |
       | Bookings          |
       | Maintenance       |
       | Billing           |
       | Notifications     |
       +-------------------+
```

### Architecture Description

1. **Frontend:** React provides the user interface.
2. **API Layer:** Axios communicates with the Spring Boot backend.
3. **Controller Layer:** Handles REST API requests.
4. **Service Layer:** Contains business logic.
5. **Repository Layer:** Uses Spring Data JPA to access data.
6. **Database Layer:** PostgreSQL stores application information.

## 5. Main Modules and Features

### 5.1 User and Authentication Management
- User registration and login.
- Profile management.
- JWT-based authentication.
- Role-based access control.
- Google OAuth2 integration.

### 5.2 Institution and Department Management
- Institution information management.
- Department creation and management.
- Institution working-hours configuration.
- Institution-level resource administration.

### 5.3 Equipment and Resource Management
- Laboratory equipment management.
- Resource availability information.
- Equipment lifecycle information.
- Resource search and management.

### 5.4 Booking Management
- Equipment booking requests.
- Booking approval and rejection.
- Booking history and status tracking.
- Booking reminders.
- Waitlist management and notifications.

### 5.5 Maintenance Management
- Maintenance request creation and management.
- Approval and rejection workflow.
- Automatic corrective work order creation.
- Work order assignment and status tracking.
- Preventive maintenance schedules.
- Maintenance history.
- Equipment downtime tracking and analytics.

### 5.6 Calibration Management
- Equipment calibration overview.
- Latest calibration and next due dates.
- Calibration status tracking.
- Calibration reminders.
- Automatic calibration work orders.
- Calibration completion records.

### 5.7 Technician Task Management
- Technician directory.
- Technician task assignment.
- Technician workload overview.
- Task status tracking.

### 5.8 Document and Certificate Management
- Equipment-related document storage.
- Certificate and manual management.
- Document access through the application.

### 5.9 Notification Management
- In-app notifications.
- Notification preferences.
- Real-time notification support.
- Push notification integration.
- Email and SMS notification services.
- Notification delivery and retry handling.

### 5.10 Billing and Financial Management
- Department budget management.
- Invoice creation and management.
- Invoice PDF generation.
- Usage charge management.
- Equipment cost overview.
- Financial summary.

### 5.11 Dashboard and Analytics
- Institution dashboard.
- Institution equipment analytics.
- Manager dashboard.
- Researcher dashboard.
- Equipment lifecycle information.
- Resource utilization insights.

### 5.12 Reports Management
- Report preview.
- Department usage reports.
- Maintenance reports.
- Procurement cost reports.
- Resource-sharing reports.
- Report export functionality.

### 5.13 Resource Sharing
- Resource sharing between eligible users and departments.
- Resource-sharing operations and management.

## 6. User Roles

The application supports role-based access for different categories of users:

- `RESEARCHER_STUDENT`
- `LAB_TECHNICIAN`
- `LAB_MANAGER`
- `DEPARTMENT_HEAD`
- `INSTITUTION_ADMIN`
- `SYSTEM_ADMIN`

Each role receives access according to its responsibilities and authorization permissions.

## 7. Booking Workflow

1. User logs into the application.
2. User checks available laboratory equipment.
3. User selects the required equipment.
4. User submits a booking request.
5. Backend validates availability and booking details.
6. Booking request is created.
7. Authorized staff reviews the request.
8. Request is approved or rejected.
9. Equipment usage and booking status can be tracked.

### Booking Status

- `PENDING`
- `APPROVED`
- `REJECTED`
- `IN_USE`
- `COMPLETED`
- `CANCELLED`
- `NO_SHOW`

## 8. Frontend Features and Pages

The React frontend includes pages and components for:

- Home and authentication.
- Registration and login.
- Dashboard and role-based dashboards.
- Equipment management.
- Bookings and booking details.
- Maintenance requests.
- Maintenance overview and history.
- Preventive maintenance schedules.
- Work orders.
- Calibration overview and reminders.
- Technician tasks and directory.
- Document management.
- Notifications and preferences.
- Reports and analytics.
- Budgets and financial summaries.
- Invoices and usage charges.
- User profile and protected routes.

## 9. Backend Modules

The Spring Boot backend follows a modular package structure.

Major packages include:

- `billing`
- `booking`
- `budget`
- `common`
- `dashboard`
- `department`
- `institution`
- `maintenance`
- `notification`
- `report`
- `resource`
- `sharing`
- `storage`
- `usage`
- `user`

Each module uses controllers, services, repositories, DTOs, entities, and supporting configuration classes as required.

## 10. Project Folder Structure

```text
lab-resource-platform/
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   │   ├── notifications/
│   │   │   ├── t9/
│   │   │   └── t10/
│   │   ├── services/
│   │   ├── styles/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
│
└── lab-resource-platform/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── com/labresource/platform/
            │       ├── billing/
            │       ├── booking/
            │       ├── budget/
            │       ├── common/
            │       ├── dashboard/
            │       ├── department/
            │       ├── institution/
            │       ├── maintenance/
            │       ├── notification/
            │       ├── report/
            │       ├── resource/
            │       ├── sharing/
            │       ├── storage/
            │       ├── usage/
            │       └── user/
            │
            └── resources/
                └── application.properties
```

## 11. Prerequisites

Install the following software:

- Java JDK 17
- Node.js and npm
- PostgreSQL
- Git
- IntelliJ IDEA (recommended)
- Postman (for API testing)

Optional integrations such as Redis, Elasticsearch, and Kafka require separate installation and configuration.

## 12. Installation and Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/springboardmentor903/Intelligent-Laboratory-Equipment-Sharing-and-Resource-Utilization-Optimization-Platform-Kunal.git
```

```bash
cd Intelligent-Laboratory-Equipment-Sharing-and-Resource-Utilization-Optimization-Platform-Kunal
```

### Step 2: Database Setup

Open PostgreSQL or pgAdmin and create a database named `labdb`.

```sql
CREATE DATABASE labdb;
```

Configure the database username and password according to your local environment.

### Step 3: Backend Setup

Open a terminal inside the backend directory:

```bash
cd lab-resource-platform
```

Set the required environment variables in PowerShell:

```powershell
$env:DB_PASSWORD="your_database_password"
$env:JWT_SECRET="your_long_random_jwt_secret"
$env:GOOGLE_CLIENT_SECRET="your_google_client_secret"
$env:MAIL_USERNAME="your_email"
$env:MAIL_APP_PASSWORD="your_mail_app_password"
```

Run the Spring Boot application:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

### Step 4: Frontend Setup

Open another terminal:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the frontend:

```bash
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

Keep both frontend and backend applications running during development.

## 13. Important Configuration

| Configuration | Value |
|---|---|
| Backend Port | 8080 |
| Frontend Port | 5173 |
| Database | PostgreSQL |
| Database Name | labdb |
| Database Port | 5432 |
| Redis Port | 6379 |
| Elasticsearch Port | 9200 |
| Kafka Port | 9092 |

Database passwords, JWT secrets, OAuth credentials, and email credentials must be kept private. Never commit actual secrets to GitHub.

## 14. API Development

The backend provides REST APIs for:

- Users and roles.
- Institutions and departments.
- Equipment and resources.
- Bookings and waitlists.
- Maintenance requests and work orders.
- Calibration and technician tasks.
- Notifications and preferences.
- Documents and storage.
- Budgets, invoices, and usage charges.
- Dashboards, analytics, and reports.
- Resource sharing.

Check the respective controller classes for exact endpoint paths, HTTP methods, request formats, and authorization requirements.

## 15. Security

The application uses Spring Security and JWT-based authentication.

Security features and objectives include:

- User authentication.
- Role-based authorization.
- Protected API endpoints.
- Secure password handling.
- Environment-based configuration of credentials.

Production deployment should additionally use HTTPS, secure secret management, appropriate CORS settings, and database migration practices.

## 16. Testing and Build Commands

### Frontend

```bash
npm run dev
npm run build
npm run lint
```

### Backend

```powershell
.\mvnw.cmd clean package
```

Postman can be used for API testing. Browser developer tools can be used for frontend debugging, and pgAdmin can be used for database verification.

## 17. Development Milestones

| Milestone | Work | Status |
|---|---|---|
| Milestone 1 | Core platform setup | Completed |
| Milestone 2 | Authentication and resource management | Completed |
| Milestone 3 | Maintenance, calibration, notifications, documents, dashboards, reports, billing and usage management | Completed |
| Future Milestones | Further enhancements and deployment | Planned |

### Milestone 3 Highlights

- Maintenance requests, work orders, preventive schedules, and downtime.
- Calibration tracking, reminders, and technician tasks.
- Certificate, manual, and document storage.
- Notification management and delivery support.
- Institution dashboards and analytics.
- Reports and export functionality.
- Budget, invoice, and financial management.
- Usage charges and cost overview.

## 18. Future Enhancements

Possible future improvements include:

- Advanced equipment search and filtering.
- Further resource utilization optimization.
- Enhanced analytics and reporting.
- Improved cloud deployment.
- Additional notification integrations.
- Performance optimization and scalability improvements.

## 19. Version Control

Git and GitHub are used for source code management and milestone-wise development history.

**Repository:**  
https://github.com/springboardmentor903/Intelligent-Laboratory-Equipment-Sharing-and-Resource-Utilization-Optimization-Platform-Kunal

### Recent Milestone 3 Commit

```text
4ee4ed6
```

Commit message:

```text
Complete Milestone 3 implementation
```

## 20. Author

**Kunal Choudhary**

B.Tech – Computer Science and Engineering

Infosys Springboard Internship Project

---

**Intelligent Laboratory Equipment Sharing and Resource Utilization Optimization Platform**

*Built to support better laboratory resource management and equipment utilization.*
