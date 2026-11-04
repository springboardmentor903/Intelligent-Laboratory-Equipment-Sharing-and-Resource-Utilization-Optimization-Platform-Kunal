# T11 — UI Wireframes & Equipment Catalog Planning

## 1. Project

**Project Name:** Lab Resource Utilization Platform

**Objective:** Plan the user interface, application pages, dashboard layout, equipment catalog, booking calendar, and role-based navigation.

## 2. UI Design

- Modern professional interface
- Blue and white color theme
- Responsive layout for desktop, tablet, and mobile
- Sidebar navigation
- Dashboard cards and charts
- Consistent buttons, forms, tables, and icons
- Clear loading, success, empty, and error states
- Accessible forms and readable typography

## 3. Main Pages

1. Login
2. Registration
3. Dashboard
4. Equipment Catalog
5. Equipment Details
6. Booking Calendar
7. My Bookings
8. Maintenance Requests
9. Notifications
10. User Profile

## 4. User Roles

1. Researcher / Student
2. Lab Technician
3. Lab Manager
4. Department Head
5. Institution Administrator
6. System Administrator

## 5. Common Application Layout

### Header
- Platform logo and name
- Notifications icon
- User profile menu
- Logout option

### Sidebar
- Dashboard
- Equipment Catalog
- My Bookings
- Maintenance
- Notifications
- Profile
- Reports, where authorized

### Main Content
- Page title
- Breadcrumbs, where useful
- Page-specific content
- Relevant action buttons
- Loading and error messages

Navigation items and actions must depend on the user's role and permissions.

## 6. Login and Registration Wireframe

### Login Page
- Platform logo and name
- Email field
- Password field
- Show/hide password option
- Forgot Password link
- Sign In button
- Google Sign-In option
- Link to Registration page
- Validation and error messages

### Registration Page
- Full name
- Email
- Password
- Confirm password
- Required registration details
- Create Account button
- Link to Login page
- Validation messages

## 7. Dashboard Layout

### Summary Cards
- Total Equipment
- Available Equipment
- Active Bookings
- Pending Bookings
- Maintenance Requests

### Dashboard Sections
- Resource utilization chart
- Recent bookings
- Equipment availability overview
- Notifications and recent activity

### Dashboard Rules
- Display information relevant to the logged-in user's role.
- Use real backend data after API integration.
- Use clearly identified sample data only for temporary UI previews.

## 8. Equipment Catalog Wireframe

### Header
- Platform logo and name
- Notifications
- User profile

### Sidebar
- Dashboard
- Equipment Catalog
- My Bookings
- Maintenance
- Notifications
- Profile

### Main Content
- Page title: Equipment Catalog
- Search bar
- Category filter
- Availability filter
- Laboratory or department filter, where applicable
- Equipment cards

### Equipment Card Details
- Equipment name
- Equipment image
- Category
- Laboratory location
- Availability status
- View Details button
- Book Equipment button, when authorized

### Equipment Details Page
- Equipment name and image
- Description
- Specifications
- Laboratory location
- Availability information
- Maintenance or calibration information, where available
- Booking action, according to permissions

### Responsive Design
- Desktop: 3-column equipment cards
- Tablet: 2-column equipment cards
- Mobile: 1-column equipment cards

## 9. Booking Calendar Wireframe

### Header
- Page title: Booking Calendar
- Current month and year
- Previous and Next month navigation

### Calendar
- Monthly calendar view
- Available time slots
- Booked time slots
- Unavailable time slots
- Selected equipment details

### Booking Form
- Equipment selection
- Booking date
- Start time
- End time
- Booking purpose
- Submit Booking button

### Booking Status
- Pending
- Approved
- Rejected
- Cancelled

### User Actions
- View My Bookings
- View booking details
- Cancel booking when permitted
- View booking status
- Show validation errors and booking conflict messages

### Responsive Design
- Desktop: Full monthly calendar
- Tablet: Compact calendar
- Mobile: Calendar and booking form stacked vertically

## 10. My Bookings

- Display the user's booking list
- Show equipment name
- Show booking date and time
- Show booking purpose
- Display booking status
- View booking details
- Cancel booking when permitted
- Filter bookings by status

## 11. Maintenance Requests

- Display maintenance requests
- Show equipment details
- Show issue description
- Display request status
- Submit a maintenance request when authorized
- Update or manage requests according to role permissions

## 12. Notifications

- Booking request updates
- Booking approval or rejection notifications
- Maintenance request updates
- Notification read/unread status
- Mark as read, when supported

## 13. User Profile

- User name
- Email address
- Assigned role
- Department or institution details, when available
- Edit permitted profile information
- Logout option

## 14. Role-Based Navigation and Permissions

### 14.1 Researcher / Student
- Browse Equipment Catalog
- View equipment details
- Request equipment bookings
- View own bookings
- Cancel bookings when permitted
- Manage permitted profile details

### 14.2 Lab Technician
- View assigned equipment
- Update equipment status, when authorized
- View assigned bookings
- Manage maintenance requests, when authorized

### 14.3 Lab Manager
- Manage permitted equipment records
- Review booking requests
- Manage maintenance workflows
- View resource utilization

### 14.4 Department Head
- View department resource information
- Review department bookings and usage
- Access authorized reports

### 14.5 Institution Administrator
- Manage institution-level resources
- Manage departments, when authorized
- View institution-level reports
- Manage permitted institutional settings

### 14.6 System Administrator
- Manage users and roles
- Access system-wide administration
- Manage permitted system configuration
- Monitor system-wide resources

### Permission Rules
- Users can access only authorized pages and actions.
- Users can view and update only permitted profile fields.
- Booking actions depend on role, booking status, and applicable policies.
- Backend APIs must enforce permissions; hiding frontend controls alone is not sufficient.

## 15. Responsive Design and Accessibility

- Desktop, tablet, and mobile layouts
- Consistent spacing and typography
- Clear labels for form fields
- Keyboard-accessible controls
- Sufficient text contrast
- Validation messages near relevant fields
- Loading indicators for API requests
- Empty states when no records are available
- Error messages when an operation fails
- Confirmation before destructive actions

## 16. Technology Plan

- Frontend: React
- Development tool: Vite
- Routing: React Router DOM
- API communication: Axios
- Icons: Lucide React
- Backend: Java and Spring Boot
- Database: PostgreSQL
- Authentication plan: JWT and Google OAuth2

## 17. Next Tasks

- T12: React Project Setup
- T13: Registration & Login UI
- T14: Google OAuth2 + JWT Integration
- T15: Profile, Protected Routes & Role-Based UI
- T16: Equipment Catalog UI
- T17: Booking UI & Availability Calendar

## 18. T11 Completion Checklist

- [ ] Main application pages identified
- [ ] Common layout and navigation planned
- [ ] Login and Registration wireframes documented
- [ ] Dashboard layout documented
- [ ] Equipment Catalog wireframe documented
- [ ] Booking Calendar wireframe documented
- [ ] My Bookings, Maintenance, Notifications, and Profile planned
- [ ] Six user roles documented
- [ ] Role-based navigation and permission rules documented
- [ ] Responsive design and accessibility requirements documented
