# Hotel Management System

## Overview

Hotel Management System is a comprehensive Java-based application developed using Swing and AWT for the user interface. It follows the MVC (Model-View-Controller) design pattern and leverages Java collections and exception handling to ensure robust and efficient operations. MongoDB, a NoSQL database, is used for data storage, providing flexibility and scalability.

## Features

### User Roles

1. **Manager**
   - Created by default upon program initialization.
   - Manages guest and receptionist accounts.
   - Can add, delete, and modify guest and receptionist details.
   - Can manage room information (add, update).
2. **Receptionist**

   - Can be added by the manager.
   - Manages reservations.
   - Also manages guest accounts.

3. **Guest**
   - Can sign up for an account.
   - Can make and manage reservations.

### Authentication

- All users (manager, receptionist, guest) must sign in to access their respective functionalities.
- Passwords are encrypted in the database for security.
- Password recovery involves sending a reset code to the user's personal email.

### Data Management

- Uses MongoDB to store user details and hotel data.
- Manager-created receptionist accounts have an automatically generated hotel email and initial password.

# Application Structure

This application follows the MVC (Model-View-Controller) pattern to organize its components:

## Model

Represents the data and the business logic. Manages the data of the application such as user accounts, room details, and reservations.

Example classes:

- `Guest`
- `Receptionist`
- `Manager`
- `Room`
- `Reservation`
- `Database`

## View

The GUI components built using Swing and AWT. Displays the data to the user.

Example classes:

- `LoginView`
- `ManagerView`
- `ReceptionistView`
- `GuestView`
- `Components Classes`

## Controller

Handles the user inputs and updates the model and view accordingly. Processes user input, interacts with the model, and updates the view.

Example classes:

- `Controller` (Main controller class)
- `PasswordHashing`
- `QrCode`
- `SendEmail`

## Installation and Setup

### Prerequisites

- Java JDK 17 or higher
- MongoDB
- Maven

### Clone the Repository

```bash
git clone https://github.com/Hermez-anderrahim/Hotel-management-.git
cd Hotel-management-/hotelproject
/mvnw clean install
```
To login as admin :
- email= admin
- password= admin12345#
