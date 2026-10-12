# Saúde em Dia

**An Android application for organizing medical exams, exam requests, and prescriptions.**

Saúde em Dia is a personal health information management application designed to help users organize medical records in one place. The app provides a structured way to register, browse, and manage medical exams and prescriptions, making health information easier to access and maintain.

## Features

### Medical Exam Management

* Register and manage medical exam records.
* Store exam dates, names, specialties, doctors, and institutions.
* Record exam results and conclusions.
* Associate documents and external links with exam records.
* Filter and browse exams by relevant information.

### Exam Request Management

* Register medical exam requests.
* Track request information and status.
* Associate exam results with their corresponding requests.

### Prescription Management

* Register medical prescriptions.
* Store prescription dates, doctor information, and institution details.
* Keep prescription documents and medication information associated with each record.

### Data Management

* Store application data using Supabase.
* Organize medical information through a structured database.
* Use database functions (RPC) for selected operations.

## Technologies

| Technology      | Purpose                                       |
| --------------- | --------------------------------------------- |
| Kotlin          | Main application programming language         |
| Jetpack Compose | Declarative Android UI                        |
| Material 3      | UI components and visual design               |
| Supabase        | Backend services and database                 |
| PostgreSQL      | Relational data storage                       |
| Supabase RPC    | Database operations exposed as functions      |
| Android Studio  | Development environment                       |
| Gradle          | Build configuration and dependency management |

## Architecture

The application follows an Android architecture based on Kotlin and Jetpack Compose, with Supabase providing backend data services.

The main responsibilities are separated into:

* **User interface:** Compose screens and Material 3 components.
* **Application logic:** ViewModels and state management.
* **Data access:** Supabase queries and RPC calls.
* **Backend:** PostgreSQL tables, views, functions, and access policies.

## Database

The backend uses Supabase and PostgreSQL.

The main data entities include:

* **Medical exams:** Exam details, results, conclusions, and document references.
* **Exam requests:** Requested procedures and their status.
* **Prescriptions:** Prescription details, dates, medical professionals, documents, and medication information.
* **Patients:** Patient records associated with medical information.

Database views can provide application-ready data, while PostgreSQL functions encapsulate selected create and delete operations.

## Project Structure

The project follows the standard Android application structure.

```text
SaudeEmDia/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/
│               └── .../
│                   ├── ui/
│                   ├── viewmodel/
│                   └── ...
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
└── README.md
```

*Note: The package-level folders above are illustrative. Replace them with the actual structure of your project.*

## Configuration

### Requirements

* Android Studio
* A compatible JDK and Android SDK
* A Supabase project
* A Gemini API key, if Gemini-powered features are enabled

### Local configuration

Configure the required API keys and service settings locally, using the project's expected Gradle properties.

For example, the project may use `local.properties` to supply values consumed by Gradle and generated `BuildConfig` fields.

**Do not commit API keys, credentials, or other secrets to version control.** Ensure `local.properties` is excluded by `.gitignore`. Any API key embedded in an Android application can potentially be extracted from the compiled app, so production secrets must remain on a secure backend.

### Supabase setup

To run the application against your own backend:

1. Create a Supabase project.
2. Configure the required database tables, views, functions, and access policies.
3. Set the appropriate Supabase project URL and publishable/anonymous client key in your local configuration.
4. Build and run the application from Android Studio.

The database schema and RPC functions must match the versions expected by the application.

## Development Status

This project is under active development. Features, database structures, and application screens may evolve as the application grows.

## License

No license has been specified yet. All rights are reserved by default unless a license is added to this repository.
