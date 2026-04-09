# Software Requirements Specification (SRS) - BookUrMedical Platform

## 1. Introduction
### 1.1 Purpose
This document provides a comprehensive overview of the requirements for the **BookUrMedical** platform. It describes the functional and non-functional requirements for the Patient, Doctor, and Admin modules developed to facilitate seamless medical consultations, case management, and platform administration.

### 1.2 Project Overview
BookUrMedical is a digital healthcare ecosystem designed to connect patients with medical specialists (General and Ayurvedic) worldwide. The platform handles everything from initial enquiry and case sheet submission to doctor assignment, appointment booking, and virtual/in-person consultations.

---

## 2. User Roles & Access Control
The platform supports four primary user roles:
| Role | Description |
| :--- | :--- |
| **Patient** | Searches for doctors/hospitals, submits medical cases, books appointments, and attends consultations. |
| **Doctor** | Reviews assigned patient cases, manages availability, and conducts consultations. |
| **Moderate Doctor** | A senior clinical role that audits cases submitted by doctors before finalization. |
| **Admin** | Manages the overall platform operations, assigns doctors to patients, tracks finances, and monitors system logs. |

---

## 3. Functional Requirements

### 3.1 Patient Module
#### 3.1.1 Discovery & Information
- **Landing Page**: View platform statistics, services, partner hospitals (Ayurvedic & General), and user testimonials.
- **Service Catalog**: Browse specialized medical services and wellness packages.
- **Provider Search**: Search for doctors by specialization and hospitals by region.

#### 3.1.2 Case Management
- **Onboarding/Profile**: Complete medical profile including personal details and insurance info.
- **Case Sheet Submission**: Upload medical history, current symptoms, and relevant diagnostic reports.
- **Track Status**: Monitor case progress (e.g., "Awaiting Assignment", "Doctor Assigned", "Review Completed").

#### 3.1.3 Appointment Booking
- **Doctor Selection**: Choose a specialist based on the assigned case.
- **Scheduling**: Select available dates and time slots via an interactive calendar.
- **Visit Type**: Choose between "In-person" and "Virtual/Video" consultations.
- **Payment Integration**: Proceed to secure payment for consultation fees.

---

### 3.2 Doctor Module
#### 3.2.1 Clinical Workflow Hub (Dashboard)
- **Portfolio Management**: View all assigned patients and their preparation readiness (Case sheet completion, report uploads, payment status).
- **Urgency Tracking**: Identify and prioritize "Critical" or "Urgent" cases via color-coded alerts.
- **Today's Queue**: Access a chronological list of scheduled consultations for the current day.

#### 3.2.2 Case Action Center
- **Case Review**: Detailed view of patient history and uploaded documents.
- **Communication**: Initiate messaging or request further information from the patient.
- **Moderation Request**: Submit completed reviews to a Moderate Doctor for audit and approval.
- **Consultation Execution**: "Join Call" functionality for virtual appointments.

#### 3.2.3 Availability Management
- **Slot Management**: Configure available days and hours for patient bookings.

---

### 3.3 Admin Module
#### 3.3.1 Operations Dashboard
- **KPI Monitoring**: Real-time tracking of total patients, active workflows, pending reviews, and revenue.
- **Revenue Analytics**: Visual trends for monthly revenue and registration growth.

#### 3.3.2 Patient & Case Management
- **Doctor Assignment**: Manually or automatically assign specialized doctors to new patient cases.
- **Status Audit**: Track every patient's journey from registration to consultation completion.
- **Finance Portal**: Monitor transaction history and payment settlements from patient fees.

#### 3.3.3 System Administration
- **Security Audit Feed**: Live logging of security events, data integrity flags, and system errors.
- **User Management**: Manage credentials and roles for doctors and staff.

---

## 4. Technical Architecture
### 4.1 Frontend
- **Framework**: Next.js 14+ (App Router).
- **Styling**: Tailwind CSS for responsive components and layout.
- **UI Components**: Radix UI (via Shadcn/ui) for accessible headers, sidebars, and forms.
- **Data Visualization**: Recharts for administrative and performance graphs.

### 4.2 Backend
- **Technology**: Java Spring Boot.
- **Database**: MongoDB (used for syncing patient portfolios and clinical data).
- **Authentication**: JWT-based secure authentication for all protected routes.

---

## 5. Non-Functional Requirements
### 5.1 Performance
- Dashboard sync should occur in the background without blocking UI interaction.
- Typical page load for metrics should be under 1.5 seconds.

### 5.2 Security
- HIPPA/GDPR compliance for medical record handling (Encryption at rest and in transit).
- Distinct protected routes for Doctor/Admin scopes using middleware or server-side checks.

### 5.3 Scalability
- Modular component design allows for adding new service types (e.g., Surgery, Rehabilitation) without altering core architecture.
