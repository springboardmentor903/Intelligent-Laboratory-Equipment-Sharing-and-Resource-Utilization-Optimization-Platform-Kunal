# Intelligent Laboratory Equipment Sharing and Resource Utilization Optimization Platform

A centralized web-based platform designed to manage laboratory equipment, simplify resource booking, improve equipment utilization, and support efficient laboratory resource management.

**Developed by:** Kunal Choudhary  
**Program:** Infosys Springboard Internship  
**Project Type:** Full Stack Web Application

---

## 1. Project Overview

Laboratories in colleges and institutions use different types of equipment for experiments, research, and academic activities. Managing these resources manually can create problems such as booking conflicts, poor resource utilization, lack of availability information, and difficulty tracking equipment usage.

The Intelligent Laboratory Equipment Sharing and Resource Utilization Optimization Platform aims to solve these problems through a centralized digital platform.

The application provides a common interface for managing laboratory resources, users, bookings, maintenance requests, and other laboratory operations.

It consists of a React-based frontend, a Spring Boot backend, and a PostgreSQL database.

## 2. Project Objectives

- Provide centralized management of laboratory equipment.
- Make equipment availability easier to check.
- Simplify laboratory equipment booking.
- Reduce booking conflicts and improve resource utilization.
- Manage users and their roles.
- Support institution and department management.
- Maintain booking and resource information.
- Provide a foundation for maintenance and resource-sharing workflows.

## 3. Technology Stack

| Component | Technology |
|---|---|
| Frontend | React.js |
| Programming Language | JavaScript |
| Styling | Tailwind CSS |
| Routing | React Router |
| HTTP Client | Axios |
| Icons | Lucide React |
| Frontend Build Tool | Vite |
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

Additional integrations configured or planned in the backend include Redis, Elasticsearch, Apache Kafka, AWS S3, and email services. These require separate setup and credentials.

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
      | Dashboard         |
      +---------+---------+
                |
                | HTTP Requests
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
      | Departments       |
      | Institutions      |
      +-------------------+
```

### Architecture Description

1. Frontend: Provides the user interface using React.
2. API Layer: Handles communication between frontend and backend using Axios.
3. Controller Layer: Handles incoming REST API requests.
4. Service Layer: Contains application business logic.
5. Repository Layer: Communicates with the database using Spring Data JPA.
6. Database Layer: Stores application information in PostgreSQL.

## 5. Main Modules

### User Management
- User registration and login.
- User profile management.
- Role-based access control.
- Authentication and authorization.

### Institution Management
- Institution information management.
- Institution-related operations.

### Department Management
- Department creation and management.
- Department information associated with institutions.

### Equipment and Resource Management
- Laboratory equipment management.
- Resource information and availability.
- Resource-related operations.

### Booking Management
- Equipment booking requests.
- Booking status management.
- Booking-related operations and history.

### Maintenance Management
- Maintenance request management.
- Equipment maintenance-related operations.

### Resource Sharing
- Resource sharing between eligible users or departments.

### Billing Management
- Billing record management.

### Notification Management
- Notification-related operations.

### Calibration Management
- Equipment calibration-related operations.

## 6. User Roles

The project defines role-based access for different categories of users.

- RESEARCHER_STUDENT
- LAB_TECHNICIAN
- LAB_MANAGER
- DEPARTMENT_HEAD
- INSTITUTION_ADMIN
- SYSTEM_ADMIN

Each role is intended to have access according to its responsibilities and authorization permissions.

## 7. Booking Workflow

The booking process is designed around the following workflow:

1. User logs into the application.
2. User checks available laboratory equipment.
3. User selects the required equipment.
4. User submits a booking request.
5. Backend validates the booking request and availability.
6. Booking request is created with PENDING status.
7. Authorized staff reviews the request.
8. Request is approved or rejected.
9. Equipment usage and booking status can be tracked.

### Booking Status

- PENDING
- APPROVED
- REJECTED
- IN_USE
- COMPLETED
- CANCELLED
- NO_SHOW

## 8. Frontend Features and Pages

The React frontend contains the following pages and components:

- Home
- Login
- Registration
- Dashboard
- Equipment
- Bookings
- Maintenance
- Profile
- Forgot Password
- Reset Password
- OAuth Callback
- Protected Routes
- Application Layout

The frontend communicates with the Spring Boot backend through API requests.

## 9. Backend Modules

The backend follows a modular package structure.

Major backend packages include:

- billing
- booking
- common
- department
- institution
- maintenance
- notification
- resource
- sharing
- user

The backend uses REST controllers, services, repositories, entities, and security components to organize application functionality.

## 10. Project Folder Structure

```text
lab-resource-platform/
│
├── .gitignore
├── README.md
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   ├── App.jsx
│   │   └── main.jsx
│   │
│   ├── package.json
│   └── vite.config.js
│
└── lab-resource-platform/
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    │
    └── src/
        └── main/
            ├── java/
            │   └── com/labresource/platform/
            │       ├── billing/
            │       ├── booking/
            │       ├── common/
            │       ├── department/
            │       ├── institution/
            │       ├── maintenance/
            │       ├── notification/
            │       ├── resource/
            │       ├── sharing/
            │       └── user/
            │
            └── resources/
                └── application.properties
```

## 11. Prerequisites

Install the following software before running the project:

- Java JDK 17
- Node.js and npm
- PostgreSQL
- Git
- IntelliJ IDEA (recommended)
- Postman (for API testing)

Optional services such as Redis, Elasticsearch, and Kafka need separate installation and configuration if their integrations are used.

## 12. Installation and Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/springboardmentor903/Intelligent-Laboratory-Equipment-Sharing-and-Resource-Utilization-Optimization-Platform-Kunal.git
```

Move into the project directory:

```bash
cd Intelligent-Laboratory-Equipment-Sharing-and-Resource-Utilization-Optimization-Platform-Kunal
```

### Step 2: Database Setup

1. Open PostgreSQL or pgAdmin.
2. Create a database named `labdb`.
3. Configure the database username and password in the backend environment.

SQL command:

```sql
CREATE DATABASE labdb;
```

### Step 3: Backend Setup

Open a terminal inside the backend folder:

```bash
cd lab-resource-platform
```

Set the required environment variables in PowerShell according to your local configuration:

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

Open another terminal and navigate to the frontend folder:

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

Database passwords, JWT secrets, OAuth secrets, and email credentials should be kept private and should never be committed to GitHub.

## 14. API Development

The Spring Boot backend provides REST controllers for:

- Users
- Roles
- Institutions
- Departments
- Resources
- Bookings
- Maintenance Requests
- Notifications
- Billing Records
- Resource Sharing
- Calibration

API endpoints can be tested using Postman.

The exact endpoint paths, HTTP methods, request formats, and authorization requirements should be checked in the respective controller classes.

## 15. Security

The application uses Spring Security and JWT-based authentication.

Security-related objectives include:

- Authentication of users.
- Authorization based on assigned roles.
- Protection of restricted API endpoints.
- Secure password handling.
- Environment-based configuration of sensitive credentials.

Production deployment should additionally use HTTPS, secure secret management, appropriate CORS settings, and database migration practices.

## 16. Testing

The following tools and checks can be used during development:

- Postman for backend API testing.
- Browser developer tools for frontend debugging.
- PostgreSQL and pgAdmin for database verification.
- Maven for backend build verification.
- npm scripts for frontend development and build verification.

Frontend commands:

```bash
npm run dev
npm run build
npm run lint
```

Backend build command:

```powershell
.\mvnw.cmd clean package
```

## 17. Development Milestones

Project development is organized into milestones.

| Milestone | Work | Status |
|---|---|---|
| Milestone 1 | Core platform setup | Completed |
| Milestone 2 | Authentication and resource management | Completed |
| Milestone 3 | Further platform development | Upcoming |
| Future Milestones | Additional modules and enhancements | Planned |

Milestone 1 and Milestone 2 have been committed together.

Initial Git commit:

```text
5c93605
```

Commit message:

```text
Milestone 1-2: Core Platform Setup, Authentication and Resource Management
```

## 18. Future Enhancements

Possible future improvements include:

- Advanced equipment search and filtering.
- Resource utilization analytics.
- Automated booking notifications.
- Equipment maintenance scheduling.
- Improved resource-sharing workflows.
- Detailed usage reports.
- Search optimization using Elasticsearch.
- Caching using Redis.
- Event processing using Kafka.
- Cloud-based file storage.

These enhancements will be added and verified as development progresses.

## 19. Version Control

Git and GitHub are used to maintain project source code and milestone-wise development history.

Repository:

[GitHub Repository](https://github.com/springboardmentor903/Intelligent-Laboratory-Equipment-Sharing-and-Resource-Utilization-Optimization-Platform-Kunal)

## 20. Author

**Kunal Choudhary**

B.Tech – Computer Science and Engineering

Infosys Springboard Internship Project

---

**Intelligent Laboratory Equipment Sharing and Resource Utilization Optimization Platform**

*Built to support better laboratory resource management and equipment utilization.*