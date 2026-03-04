# Bizhub – User Management Application

## Overview
This project was developed as part of the PIDEV – 3rd Year Engineering Program at **Esprit School of Engineering** (Academic Year 2025–2026).  
It consists of a desktop application built with JavaFX for managing users in a business hub environment. The app allows administrators to view, edit, delete, and export user data, with features like filtering, pagination, and real-time updates.

## Features
- User listing with pagination and filtering by type, search query, and active status.
- Multi-selection for bulk operations like deletion.
- User editing and creation via modal forms.
- Export user data to CSV.
- Real-time auto-refresh every 30 seconds.
- Admin-only access controls.
- Custom-styled dialogs and UI components.

## Tech Stack
### Frontend
- JavaFX (for UI components, dialogs, and styling with CSS).
### Backend
- Spring Boot (for services and data handling).
- SQL (for database operations).
- Maven (for project build and dependency management).

## Architecture
The application follows a Model-View-Controller (MVC) pattern:  
- **Model**: User entities and service layers (e.g., `Services.users()`).  
- **View**: JavaFX FXML layouts and custom cells (e.g., `UserCardCell`).  
- **Controller**: Handles UI logic, events, and data binding (e.g., `UserManagementController`).  
Data flows from the SQL database through Spring Boot services to the JavaFX UI, with Maven managing dependencies.

## Contributors
- skander101 (Developer)

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
