# COMP713 Project & Task Manager

## Project Overview

The Project & Task Manager is a distributed web application developed for COMP713 Distributed and Mobile Systems.

The application allows a user to create projects and manage tasks that belong to each project.

The main functions of the application are:

- Create a new project
- View existing projects
- Create tasks for a selected project
- View tasks belonging to a project
- Update task status
- Store project and task data persistently

Supported task statuses are:

- TODO
- IN_PROGRESS
- DONE

---

## Project Option

This project implements:

**Option A: Distributed Web/API Application**

The application uses HTTP request-response communication between a browser-based client and a server-side REST API.

The system also includes persistent data storage for project and task information.

---

## Architecture

The project follows a layered architecture based on the structure used in the COMP713 laboratory exercises.

```text
Browser / JavaScript Client
        |
        | HTTP / JSON
        v
Jakarta REST Resource
        |
        v
Service Layer
        |
        v
Repository Layer
        |
        | JDBC
        v
H2 Database