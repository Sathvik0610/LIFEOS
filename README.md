# LIFEOS

# LifeOS – All-in-One Personal Management Suite

LifeOS is a desktop application built with Java Swing and Oracle Database that helps you manage your time, health, finances, and personal profile in one unified dashboard.

![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Oracle](https://img.shields.io/badge/Oracle-Database-red)
![License](https://img.shields.io/badge/license-MIT-green)

## ✨ Features

### 📅 Time Module
- Monthly calendar view with color‑coded task categories
- Hourly day planner showing scheduled tasks
- Create, edit, and delete tasks with start/end times, categories, and notes
- Real‑time current time indicator on the daily timeline

### ❤️ Health Module
- **Overview Tab**: Track daily steps, sleep, weight, and BMI with interactive spinners
- Weekly step goal ring and inline bar charts for steps/sleep trends
- **Activity Journal**: Log workouts (walking, running, cycling, etc.) with duration and estimated calories burned
- **Medications**: Manage prescriptions with dosage, start date, and interval reminders

### 💰 Finance Module
- **Records Tab**: View all income, expenses, and transfers filtered by month
- Add transactions with a built‑in calculator (+, −, ×, ÷) and optional notes
- **Accounts Tab**: Manage multiple accounts (Cash, Bank, Credit Card, Savings) with real‑time balances
- **Analysis Tab**: Pie chart breakdown of monthly expenses by category
- Month navigation bar to switch between months

### 👤 Profile Module
- View and edit personal information (name, email, date of birth, height)
- Change password securely (SHA‑256 hashed)

### 🔐 Security
- User registration and login with SHA‑256 password hashing
- All data is user‑scoped – each user sees only their own information

## 🛠️ Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Frontend     | Java Swing (Flat, modern UI)        |
| Backend      | Java JDBC                           |
| Database     | Oracle Database (19c / 21c)         |
| Security     | SHA‑256 Hashing                     |
| Build        | Manual compilation (or any Java IDE)|

## 🚀 Getting Started

### Prerequisites
- **Java JDK 17** or later
- **Oracle Database** (Express Edition or any 19c+ instance)
- Oracle JDBC driver (`ojdbc17.jar`) – [Download here](https://www.oracle.com/database/technologies/jdbc-drivers-12c-downloads.html)

### 1. Database Setup
1. Connect to your Oracle database as a user with DBA privileges (e.g., `SYS`).
2. Run the first part of `schema.sql` to create the application user:
   ```sql
   CREATE USER C##LIFEOS IDENTIFIED BY 12345;
   GRANT CONNECT, RESOURCE TO C##LIFEOS;
   ALTER USER C##LIFEOS QUOTA UNLIMITED ON users;
Reconnect as C##LIFEOS (password: 12345) and execute the rest of schema.sql to create all tables and indexes.

### 2. Configure Database Connection
   Edit DBConnection.java with your Oracle credentials:

```java
private static final String URL = "jdbc:oracle:thin:@localhost:1521/FREEPDB1";
private static final String USER = "C##LIFEOS";
private static final String PASSWORD = "12345";
```
Adjust the URL if your Oracle service name differs (e.g., XE, ORCL).

### 3. Add JDBC Driver
   Place ojdbc17.jar inside the lib/ folder (create it if it doesn't exist).

### 4. Compile and Run
   Using terminal / command prompt:
#### Compile all Java files (include the JDBC driver in classpath)
```
javac -cp ".;lib/ojdbc17.jar" *.java
```
#### Run the application
```
java -cp ".;lib/ojdbc17.jar" MainSwing
```
Using an IDE (IntelliJ / Eclipse / VS Code):

Add ojdbc17.jar to the project's build path.

Run MainSwing.java as a Java application.

### 5. First Use
   Launch the app – the login screen appears.

Click "New user? Sign Up" to create an account.

After registration, log in and explore the modules via the top navigation bar.
