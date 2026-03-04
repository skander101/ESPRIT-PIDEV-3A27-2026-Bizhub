# Bizhub – Business Hub Platform

## Overview
This project was developed as part of the PIDEV – 3rd Year Engineering Program at **Esprit School of Engineering** (Academic Year 2025–2026).  
It consists of a comprehensive desktop application built with JavaFX for a unified business hub ecosystem. The platform facilitates user management, e-learning formations, project collaboration, marketplace transactions, investment opportunities, community engagement, and review systems. It serves multiple user roles (Admin, Formateur, Startup) with role-based access controls.

## Features
### Authentication & User Management
- Secure login with session management and role-based access.
- Admin dashboard with statistics and analytics.
- User CRUD operations with pagination (20 per page), filtering by type/search/active status.
- Multi-selection for bulk operations like deletion.
- User profile management with self-service password changes and information updates.
- Export user data to CSV.
- Real-time auto-refresh every 30 seconds.

### E-Learning Module
- Formation (course) listing, creation, and management.
- Formation details with descriptions, dates, location (physical/online), and costs.
- Participation management for formation enrollment.
- Payment processing for formation participation (card/mock provider).
- PDF certificate generation for successful participation.
- Email notifications and attestations on participation completion.
- Online and offline course support.

### Reviews System
- User reviews for formations with ratings (1-5 stars).
- Review listing with user and formation joins.
- Filter reviews by rating.
- Admin deletion of reviews.
- CSV export of reviews.

### Marketplace Module
- Product/Service listings with descriptions, prices, and inventory.
- Shopping cart functionality.
- Order management with status tracking.
- Priority queue engine for order processing.
- Payment integration and notification system.
- Commande (order) tracking and filtering.

### Investment & Projects
- Project creation and management.
- Investment tracking with investor details and contract URLs.
- Investment processing with AI fraud detection.
- Project status tracking and budget management.
- Investment analytics and recommendations.

### Community Features
- User networking and interactions.
- Community forums and discussion spaces.
- User profile viewing and filtering.

### Additional Features
- Custom-styled dark theme with honeycomb branding.
- Advanced CSS styling with gold and navy color palette.
- Modal dialogs with styled alerts.
- Toast notifications.
- PDF generation for certificates and invoices.
- Email integration for notifications and confirmations.
- Role-based visibility (Admin-only controls, user-specific features).

## Tech Stack
### Frontend
- JavaFX (UI components, dialogs, CSS styling, scene management).
- FXML (XML-based UI markup).
- CSS (custom theming with dark mode and honeycomb design).

### Backend
- Spring Boot (services, business logic, dependency injection).
- JDBC / MyBatis (database operations).
- SQL (MySQL/MariaDB).

### Additional Libraries
- iText 7 (PDF generation).
- BCrypt / jBCrypt (password hashing).
- JavaMail (email notifications).
- Maven (build and dependency management).

## Architecture
The application follows a Model-View-Controller (MVC) pattern with layered services:  
- **Model Layer**: Entities (User, Formation, Participation, Review, Investment, Project, Commande, etc.) and Data Access Objects (DAO).  
- **Service Layer**: Business logic and orchestration services (ParticipationService, PaymentService, InvestmentService, CommandeService, etc.).  
- **Controller Layer**: FXML-based controllers handling UI logic, events, and user interactions (UserManagementController, ParticipationManagementController, ReviewManagementController, ProduitServiceController, etc.).  
- **View Layer**: FXML layouts and custom cells (sidebar, dashboards, cards, forms).  
- **Utility Layer**: Common services (AlertHelper, AppSession, NavigationService, FormationEmailService, ParticipationPdfGenerator, etc.).  

Data flows from the SQL database through Spring Boot services to the JavaFX UI. Maven manages all dependencies, and the app uses a centralized AppSession for user context and navigation.

## Contributors
- skander101 (Developer)
- bouzid20 (Developer)
- fatmabgl  (Developer)
- iramtrabelsi3 (Developer)
- Youssefsellami1 (Developer)

## Academic Context
Developed at **Esprit School of Engineering – Tunisia**  
PIDEV – 3A | 2025–2026

## Getting Started
1. Clone the repository: `git clone https://github.com/skander101/Bizhub.git`  
2. Navigate to the project directory: `cd Bizhub`  
3. Ensure Java 11+ and Maven are installed.  
4. Build the project: `mvn clean install`  
5. Run the application: `mvn javafx:run` (assuming JavaFX plugin is configured).  
6. For database setup, configure your SQL database in the Spring Boot properties and run migrations if needed.  
7. Log in as an admin to access user management features.

## Acknowledgments
Thanks to **Esprit School of Engineering** for providing the academic framework and resources for this project. Special thanks to the PIDEV program for fostering practical software development skills.
