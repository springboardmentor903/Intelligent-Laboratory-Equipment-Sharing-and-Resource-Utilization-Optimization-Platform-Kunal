
# Lab Resource Utilization Platform

## T1: Architecture and Workflow Design

### 1. Project Overview

The Lab Resource Utilization Platform is a web-based application designed to manage laboratory resources, equipment bookings, availability, approvals, and utilization.

### 2. Technology Stack

- Backend: Java 17 and Spring Boot
- Database: PostgreSQL
- ORM: Spring Data JPA and Hibernate
- Authentication: JWT
- Authorization: Role-Based Access Control (RBAC)
- API Testing: Postman
- Build Tool: Maven

### 3. System Architecture

The application follows a layered architecture.

1. Client Layer: Users access the platform through a frontend or API client.
2. Controller Layer: REST controllers receive HTTP requests.
3. Service Layer: Business logic validates bookings and manages workflows.
4. Repository Layer: Spring Data JPA communicates with PostgreSQL.
5. Database Layer: PostgreSQL stores users, institutions, departments, resources, bookings, and booking history.

### 4. User Roles

- RESEARCHER_STUDENT: Views resources and creates or cancels own bookings.
- LAB_TECHNICIAN: Manages laboratory resources and booking operations.
- LAB_MANAGER: Approves or rejects bookings and manages laboratory activities.
- DEPARTMENT_HEAD: Department-level role.
- INSTITUTION_ADMIN: Institution-level role.
- SYSTEM_ADMIN: System administration role.

### 5. Booking Workflow

1. The student selects a laboratory resource.
2. The student chooses a start time and end time.
3. The system validates the requested time slot.
4. The system checks overlapping active bookings against resource capacity.
5. A valid booking is created with PENDING status.
6. An authorized laboratory staff member approves or rejects the request.
7. An approved booking can move to IN_USE and then COMPLETED.
8. Eligible bookings can be cancelled, and approved bookings can be marked NO_SHOW.
9. Status changes are recorded in booking history for auditing.

### 6. Booking Statuses

- PENDING
- APPROVED
- REJECTED
- IN_USE
- COMPLETED
- CANCELLED
- NO_SHOW

CONFIRMED may be supported as an additional status if required by the project specification.

### 7. Database Components

- User: Stores user information and role associations.
- Institution: Stores institution information.
- Department: Stores department information.
- Resource: Stores equipment details and resource quantities.
- Booking: Stores resource reservation details and booking status.
- BookingStatusHistory: Records booking status changes and audit information.

### 8. Security

- JWT is used to authenticate API requests.
- RBAC restricts protected operations according to user roles.
- Students can access their own bookings and cancel eligible bookings.
- Protected management endpoints require authorized roles.

### 9. Future Integrations

The planned architecture can be extended with:

- Google OAuth2 authentication
- Redis caching
- Elasticsearch search
- Kafka or RabbitMQ messaging
- Cloud file storage
- Maintenance management
- Resource sharing and billing
- Notifications and utilization reports

These integrations must be implemented and verified separately before they are considered complete.

### 10. Conclusion

The platform uses a layered Spring Boot architecture with PostgreSQL persistence, JWT authentication, role-based authorization, resource inventory APIs, booking management, and status history tracking.
