[//]: # (# SentientGate Backend Service Contracts & API Specifications)

[//]: # ()
[//]: # (This document outlines the request/response payloads, endpoints, gRPC interfaces, and Kafka events for all four backend services in the SentientGate ecosystem:)

[//]: # (1. **ApiGateway**)

[//]: # (2. **LoggingService**)

[//]: # (3. **MCP Service**)

[//]: # (4. **AIService**)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 1. ApiGateway)

[//]: # (The API Gateway acts as the secure entrypoint and rate-limiter, routing external requests to downstreams.)

[//]: # ()
[//]: # (### HTTP REST APIs)

[//]: # ()
[//]: # (#### 1.1 Read Blacklist)

[//]: # (* **Endpoint:** `GET /api/mgmt/blacklist`)

[//]: # (* **Port:** `8079`)

[//]: # (* **Description:** Retrieves all currently blacklisted visitor UUIDs.)

[//]: # (* **Request:** None)

[//]: # (* **Response Body &#40;JSON&#41;:** `List<String>`)

[//]: # (* **Example Response:**)

[//]: # (  ```json)

[//]: # (  [)

[//]: # (    "9d5b78b0-a50d-450f-90db-3cd1ef21b33e",)

[//]: # (    "b8d23e51-c0ef-4f11-888e-648b7eca37db")

[//]: # (  ])

[//]: # (  ```)

[//]: # ()
[//]: # (#### 1.2 Block Visitor Identity)

[//]: # (* **Endpoint:** `POST /api/mgmt/blacklist/{uuid}`)

[//]: # (* **Description:** Manually adds a visitor UUID to the Redis-backed blacklist.)

[//]: # (* **Request:** `uuid` &#40;Path Parameter, String&#41;)

[//]: # (* **Response:** `200 OK` &#40;Empty body&#41;)

[//]: # ()
[//]: # (#### 1.3 Unblock Visitor Identity)

[//]: # (* **Endpoint:** `DELETE /api/mgmt/blacklist/{uuid}`)

[//]: # (* **Description:** Manually removes a visitor UUID from the Redis-backed blacklist.)

[//]: # (* **Request:** `uuid` &#40;Path Parameter, String&#41;)

[//]: # (* **Response:** `200 OK` &#40;Empty body&#41;)

[//]: # ()
[//]: # (### Kafka Event Producers)

[//]: # ()
[//]: # (#### 1.4 General Traffic Logs)

[//]: # (* **Topic:** `user-logs` &#40;configured in `KafkaTopics.USER_LOGS`&#41;)

[//]: # (* **Serialization:** String key &#40;UUID&#41;, JSON value &#40;`LogEvent`&#41;)

[//]: # (* **Payload Structure &#40;`LogEvent`&#41;:**)

[//]: # (  | Field | Type | Description |)

[//]: # (  |-------|------|-------------|)

[//]: # (  | `uuid` | `String` | Visitor unique identifier |)

[//]: # (  | `path` | `String` | HTTP request path |)

[//]: # (  | `method` | `String` | HTTP request method |)

[//]: # (  | `routeId` | `String` | Target downstream service route |)

[//]: # (  | `decision` | `String` | Gateway action &#40;`ALLOWED` / `BLOCKED`&#41; |)

[//]: # (  | `latencyMs` | `long` | Request execution time in ms |)

[//]: # (  | `queryParams` | `String` | Request query parameters |)

[//]: # (  | `clientIp` | `String` | Client IP address |)

[//]: # (  | `statusCode` | `int` | HTTP response status code |)

[//]: # (  | `requestSize` | `long` | Request content length |)

[//]: # (  | `timestamp` | `long` | Epoch timestamp of request &#40;ms&#41; |)

[//]: # (  | `userAgent` | `String` | Client User-Agent |)

[//]: # ()
[//]: # (#### 1.5 Security Event Alerts)

[//]: # (* **Topic:** `security-events` &#40;configured in `KafkaTopics.SECURITY_EVENTS`&#41;)

[//]: # (* **Trigger:** Fired on responses with status codes `< 200` or `>= 300`.)

[//]: # (* **Serialization:** String key &#40;UUID&#41;, JSON value &#40;`SecurityAlertEvent`&#41;)

[//]: # (* **Payload Structure &#40;`SecurityAlertEvent`&#41;:**)

[//]: # (  | Field | Type | Description |)

[//]: # (  |-------|------|-------------|)

[//]: # (  | `uuid` | `String` | Visitor unique identifier |)

[//]: # (  | `errorCode` | `int` | Resulting HTTP status error code |)

[//]: # (  | `reason` | `String` | HTTP status code description |)

[//]: # (  | `attemptedPath` | `String` | Attempted request path |)

[//]: # (  | `method` | `String` | HTTP method used |)

[//]: # (  | `userAgent` | `String` | Client User-Agent |)

[//]: # (  | `clientIp` | `String` | Client IP address |)

[//]: # (  | `alertSeverity` | `String` | Severity &#40;`HIGH` for >=500, `MEDIUM` for 401/403/429, `LOW` otherwise&#41; |)

[//]: # (  | `timestamp` | `long` | Epoch timestamp of alert &#40;ms&#41; |)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 2. LoggingService)

[//]: # (The Logging Service consumes traffic events from Kafka, persists them to a PostgreSQL database, aggregates telemetry for dashboard stats, and serves event history via gRPC.)

[//]: # ()
[//]: # (### HTTP REST APIs)

[//]: # ()
[//]: # (#### 2.1 Query Raw Logs &#40;Paginated&#41;)

[//]: # (* **Endpoint:** `GET /logging-service/api/logs/raw`)

[//]: # (* **Port:** `8010`)

[//]: # (* **Parameters:**)

[//]: # (  * `page` &#40;Query, int, default `0`&#41;)

[//]: # (  * `size` &#40;Query, int, default `20`&#41;)

[//]: # (  * `sortBy` &#40;Query, String, default `"occurredAt"`&#41;)

[//]: # (  * `direction` &#40;Query, String, default `"DESC"`&#41;)

[//]: # (  * `path` &#40;Query, String, optional&#41; - Filter by path)

[//]: # (  * `statusCode` &#40;Query, Integer, optional&#41; - Filter by status)

[//]: # (* **Response Body &#40;JSON&#41;:** Spring Page wrapper containing `GatewayLogEntity` objects.)

[//]: # (* **Example Response:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "content": [)

[//]: # (      {)

[//]: # (        "id": "e0b23fa8-1f19-450f-a9db-88eca373db2f",)

[//]: # (        "clientIp": "127.0.0.1",)

[//]: # (        "visitorId": "u-user123",)

[//]: # (        "path": "/api/users/profile",)

[//]: # (        "method": "GET",)

[//]: # (        "routeId": "user-service",)

[//]: # (        "decision": "ALLOWED",)

[//]: # (        "statusCode": 200,)

[//]: # (        "requestSize": 128,)

[//]: # (        "latencyMs": 14,)

[//]: # (        "queryParams": "{}",)

[//]: # (        "userAgent": "Mozilla/5.0...",)

[//]: # (        "occurredAt": "2026-06-20T06:30:00Z",)

[//]: # (        "ingestedAt": "2026-06-20T06:30:01Z")

[//]: # (      })

[//]: # (    ],)

[//]: # (    "totalPages": 1,)

[//]: # (    "totalElements": 1)

[//]: # (  })

[//]: # (  ```)

[//]: # ()
[//]: # (#### 2.2 Get Dashboard Overview Summary)

[//]: # (* **Endpoint:** `GET /logging-service/api/logs/stats/summary`)

[//]: # (* **Parameters:**)

[//]: # (  * `start` &#40;Query, Instant, required&#41;)

[//]: # (  * `end` &#40;Query, Instant, required&#41;)

[//]: # (* **Response Body &#40;JSON&#41;:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "throughput": 420,)

[//]: # (    "securityBlocks": 12,)

[//]: # (    "p99Latency": 48.5,)

[//]: # (    "totalTraffic": 154000)

[//]: # (  })

[//]: # (  ```)

[//]: # ()
[//]: # (#### 2.3 Get Time-Series Traffic Velocity)

[//]: # (* **Endpoint:** `GET /logging-service/api/logs/stats/velocity`)

[//]: # (* **Parameters:**)

[//]: # (  * `start` &#40;Query, Instant, required&#41;)

[//]: # (  * `end` &#40;Query, Instant, required&#41;)

[//]: # (* **Response Body &#40;JSON&#41;:** List of time bucketed stats)

[//]: # (  ```json)

[//]: # (  [)

[//]: # (    {)

[//]: # (      "minute": "2026-06-20T06:20:00Z",)

[//]: # (      "requestCount": 1200,)

[//]: # (      "errorCount": 15,)

[//]: # (      "rateLimitedCount": 3)

[//]: # (    })

[//]: # (  ])

[//]: # (  ```)

[//]: # ()
[//]: # (#### 2.4 Get IP Aggregated Summary)

[//]: # (* **Endpoint:** `GET /logging-service/{ip}/summary`)

[//]: # (* **Parameters:**)

[//]: # (  * `ip` &#40;Path, String, required&#41;)

[//]: # (  * `start` &#40;Query, Instant, required&#41;)

[//]: # (  * `end` &#40;Query, Instant, required&#41;)

[//]: # (* **Response Body &#40;JSON&#41;:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "clientIp": "192.168.1.50",)

[//]: # (    "totalRequests": 450,)

[//]: # (    "rateLimitedCount": 8,)

[//]: # (    "invalidJwtCount": 2,)

[//]: # (    "uniqueRoutes": 5,)

[//]: # (    "avgLatencyMs": 12.3)

[//]: # (  })

[//]: # (  ```)

[//]: # ()
[//]: # (#### 2.5 Get Route-level Aggregated Stats)

[//]: # (* **Endpoint:** `GET /logging-service/api/logs/routes/summary`)

[//]: # (* **Parameters:**)

[//]: # (  * `start` &#40;Query, Instant, required&#41;)

[//]: # (  * `end` &#40;Query, Instant, required&#41;)

[//]: # (* **Response Body &#40;JSON&#41;:**)

[//]: # (  ```json)

[//]: # (  [)

[//]: # (    {)

[//]: # (      "routeId": "user-service",)

[//]: # (      "requestCount": 25000,)

[//]: # (      "errorRate": 0.02,)

[//]: # (      "avgLatencyMs": 18.4)

[//]: # (    })

[//]: # (  ])

[//]: # (  ```)

[//]: # ()
[//]: # (### gRPC Server &#40;Internal communication&#41;)

[//]: # (Exposed on port `9090` &#40;default&#41; for retrieving user context logs.)

[//]: # (* **Service:** `UserLogEventService`)

[//]: # (* **Method:** `GetUserEvents&#40;UserLogEventsRequest&#41; returns &#40;UserLogEventResponse&#41;`)

[//]: # (* **gRPC Request &#40;`UserLogEventsRequest`&#41;:**)

[//]: # (  ```protobuf)

[//]: # (  message UserLogEventsRequest {)

[//]: # (    string uuid = 1;)

[//]: # (    int32 duration = 2; // duration in minutes)

[//]: # (  })

[//]: # (  ```)

[//]: # (* **gRPC Response &#40;`UserLogEventResponse`&#41;:**)

[//]: # (  ```protobuf)

[//]: # (  message UserLogEventResponse {)

[//]: # (    repeated UserLogEvent user_log_events = 1;)

[//]: # (  })

[//]: # (  ```)

[//]: # (  Where `UserLogEvent` matches the properties in the `LogEvent` Kafka schema.)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 3. MCP Service)

[//]: # (The Mitigation & Control Policy &#40;MCP&#41; Service consumes security alert events from Kafka, retrieves historical client context from the Logging Service, evaluates threat strategies, and enforces blocks in Redis.)

[//]: # ()
[//]: # (### HTTP REST APIs)

[//]: # (* None. &#40;Operates strictly as an event-driven and gRPC consumer&#41;.)

[//]: # ()
[//]: # (### Kafka Event Consumer)

[//]: # (* **Topic:** `security-events` &#40;configured in `KafkaTopicConfig`&#41;)

[//]: # (* **Group ID:** `mcp-analysis-group`)

[//]: # (* **Triggers:** Processes batches of `SecurityAlertEvent`.)

[//]: # ()
[//]: # (### gRPC Client)

[//]: # (* Calls `UserLogEventService` &#40;`GetUserEvents`&#41; in the Logging Service to retrieve the past 10 minutes of request logs for the offending visitor UUID.)

[//]: # ()
[//]: # (### Redis Writer)

[//]: # (* Writes a serialization of `BlockRecord` to Redis with key `blacklist:<uuid>`.)

[//]: # (* **Block Record Schema:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "reason": "AI_BEHAVIORAL_ANOMALY_DETECTED",)

[//]: # (    "severity": "CRITICAL",)

[//]: # (    "blockedAt": 1781944800000,)

[//]: # (    "expiresAt": 1781966400000)

[//]: # (  })

[//]: # (  ```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 4. AIService)

[//]: # (The AI Inference Service takes behavior signals, builds prompts, and requests an anomaly rating from a local Ollama instance &#40;default: `gemma3:latest`&#41;.)

[//]: # ()
[//]: # (### HTTP REST APIs)

[//]: # ()
[//]: # (#### 4.1 Request Behavioral Anomaly Scoring &#40;Original Schema&#41;)

[//]: # (* **Endpoint:** `POST /ai-service/anomaly/analyze`)

[//]: # (* **Port:** `8082`)

[//]: # (* **Request Body &#40;JSON&#41;:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "failureRate": 0.12,)

[//]: # (    "requestsPerMinute": 420,)

[//]: # (    "uniqueRoutesAccessed": 5,)

[//]: # (    "jwtReuseCount": 0,)

[//]: # (    "ipReputationScore": 0.95,)

[//]: # (    "routeSensitivity": "HIGH")

[//]: # (  })

[//]: # (  ```)

[//]: # (* **Response Body &#40;JSON&#41;:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "anomaly": true,)

[//]: # (    "confidence": 0.88,)

[//]: # (    "modelVersion": "v1.0",)

[//]: # (    "inferenceTimeMs": 230)

[//]: # (  })

[//]: # (  ```)

[//]: # ()
[//]: # (#### 4.2 Proposed Integrated Schema &#40;Option A&#41;)

[//]: # (To align with MCP Service's Feign client output and history parsing:)

[//]: # (* **Endpoint:** `POST /ai-service/api/v1/analyze`)

[//]: # (* **Request Body &#40;JSON&#41;:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "uuid": "u-user123",)

[//]: # (    "history": [)

[//]: # (      {)

[//]: # (        "uuid": "u-user123",)

[//]: # (        "path": "/api/users/profile",)

[//]: # (        "method": "GET",)

[//]: # (        "statusCode": 403,)

[//]: # (        "latencyMs": 150,)

[//]: # (        "timestamp": 1781944800000)

[//]: # (      })

[//]: # (    ])

[//]: # (  })

[//]: # (  ```)

[//]: # (* **Response Body &#40;JSON&#41;:**)

[//]: # (  ```json)

[//]: # (  {)

[//]: # (    "isAnomaly": true,)

[//]: # (    "confidenceScore": 0.88,)

[//]: # (    "patternDetected": "AI_BEHAVIORAL_ANOMALY",)

[//]: # (    "suggestedBlockMinutes": 360)

[//]: # (  })

[//]: # (  ```)


shrihari@HP-Victus:~$ ollama list
NAME                   ID              SIZE      MODIFIED     
gemma3:latest          a2af6cc3eb7f    3.3 GB    2 days ago      
deepseek-r1:latest     6995872bfe4c    5.2 GB    7 weeks ago     
deepseek-coder:6.7b    ce298d984115    3.8 GB    2 months ago    
llama3.2:latest        a80c4f17acd5    2.0 GB    5 months ago    
shrihari@HP-Victus:~$ ollama run deepseek-coder:6.7b
>>> create simple student management system project ?
Sure, here's a very basic example of how you could structure such a system
using Python. This is a command line interface (CLI) version and it
doesn't store data in any permanent way - every time the program is run
all changes are discarded. In this simple case, we only have two commands:
'add student', which allows to add a new student with given name, and
'list students', which prints out the names of all registered students.

```python
class Student:
    def __init__(self, name):
        self.name = name

students = []  # list to store the students

def command_add(name):
    student = Student(name)
    students.append(student)
    print("Student added!")

def command_list():
    for i, student in enumerate(students, start=1):
        print(f"{i}. {student.name}")

while True:
    cmd = input("\nEnter a command (add/list/quit): ")
    
    if cmd == 'quit':
        break
    elif cmd == 'add':
        name = input("Enter the student's name: ")
        command_add(name)
    elif cmd == 'list':
        if not students:
            print("No students registered yet.")
        else:
            command_list()
    else:
        print("Unknown command!")
```

This is a very basic and simple version. A real-world student management
system would likely require a database, error checking, user
authentication, input validation etc.

Additionally, this example uses Python's built-in classes to represent
students and manage them in memory. If you plan on using this for
production software, it might be more effective to use a proper database
with SQL or NoSQL based on your requirements. You would also likely want
to abstract the CLI into its own module and handle errors properly etc.


>>> create simple hospital  management system project ?
Creating a complete Hospital Management System (HMS) is quite complex and
large-scale, but I can provide you with a very basic version using Python
to demonstrate the structure of such a program. This example only covers
adding patients into the system and displaying all registered patients.
It's not practical for any real world application as it lacks many
features that would be necessary in an actual HMS.

```python
class Patient:
    def __init__(self, name):
        self.name = name

patients = []  # list to store the patients

def add_patient(name):
    patient = Patient(name)
    patients.append(patient)
    print("Patient added!")

def display_patients():
    if not patients:
        print("No patients registered yet.")
    else:
        for i, patient in enumerate(patients, start=1):
            print(f"{i}. {patient.name}")

while True:
    cmd = input("\nEnter a command (add/display/quit): ")
    
    if cmd == 'quit':
        break
    elif cmd == 'add':
        name = input("Enter the patient's name: ")
        add_patient(name)
    elif cmd == 'display':
        display_patients()
    else:
        print("Unknown command!")
```
In a real-world application, we would need to handle more scenarios like
scheduling appointments, managing doctors and their schedules, billing
patients etc. This code does not cover any of these areas. A complete HMS
would have a database system to store data efficiently, multiple user
roles (like patient, doctor, admin), a front-end interface for users to
interact with, error checking, validation etc., which goes beyond the
scope of this example and requires a full-fledged software development kit
or library.


>>> using spring boot
Sure, here's an example of how you can create a simple REST API using
Spring Boot with Java. This example covers creating endpoints to add
patients into the system and retrieve all registered patients.

Firstly, we need to define our domain model (Patient) and repository for
database operations:

```java
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    
    // getters and setters...
}
```

Then we need a repository for the Patient entity:

```java
import org.springframework.data.repository.CrudRepository;

public interface PatientRepository extends CrudRepository<Patient, Long> 
{}
```

After that, let's define our REST controller:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PatientController {
    @Autowired
    private PatientRepository repository;
    
    // Endpoint to add a patient
    @PostMapping("/patients")
    public void addPatient(@RequestBody Patient patient) {
        repository.save(patient);
    }
  
    // Endpoint to get all patients
    @GetMapping("/patients")
    public Iterable<Patient> getAllPatients() {
        return repository.findAll();
    }
}
```
In this example, `@RestController`, `@RequestBody` and `@PostMapping` are
used to create a RESTful API endpoint for adding patients. The
`@Autowired` annotation is used to inject the `PatientRepository` into our
controller class.

Remember to add necessary dependencies in your `pom.xml` file:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```
Please note that you need to set up your database connection in the
`application.properties` file or through environment variables, as Spring
Boot doesn't provide a default embedded database for simplicity. This
example assumes that you have already done this setup and are ready to go.


>>> cretate student manageme ssystem in the spring boot create lot of api also  
... use themleaf for the frontend
Sure, let's break it down into multiple steps:

1. **Setup your Spring Boot Application**
  - Firstly, you need to set up a new Spring Boot project with web
    dependencies. Use Spring Initializr (https://start.spring.io) for this
    purpose.

2. **Create Student Entity and Repository**
  - Create an entity class `Student` which represents a student in our
    system.
    ```java
    @Entity
    public class Student {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;
        
        private String name;
        
        // getters and setters...
    }
    ```
  - Create a `StudentRepository` for database operations.
    ```java
    import org.springframework.data.repository.CrudRepository;
    
    public interface StudentRepository extends CrudRepository<Student, 
Long> {}
```
3. **Create REST Controllers**
  - Create a `StudentController` with methods for adding and retrieving
    students.
    ```java
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.*;

    @RestController
    public class StudentController {
        @Autowired
        private StudentRepository repository;
        
        @PostMapping("/students")
        public void addStudent(@RequestBody Student student) {
            repository.save(student);
        }
      
        @GetMapping("/students")
        public Iterable<Student> getAllStudents() {
            return repository.findAll();
        }
    }
    ```
4. **Set up Thymeleaf for Views**
  - Firstly, add the following dependency to your `pom.xml`:
    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    ```
  - Create a Thymeleaf view for displaying the students' list:
    In `src/main/resources` create a new folder named `templates` and
    inside it, create a file named `students.html`:
    ```html
    <!DOCTYPE HTML>
    <html xmlns:th="http://www.thymeleaf.org">
        <head>
            <title>Students</title>
        </head>
        <body>
            <table>
                <thead>
                    <tr>
                        <th>Name</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="student : ${students}">
                        <td th:text="${student.name}"></td>
                    </tr>
                </tbody>
            </table>
        </body>
    </html>
    ```
  - Create a controller for the Thymeleaf view:
    ```java
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.GetMapping;

    @Controller
    public class ViewController {
        @Autowired
        private StudentRepository repository;
      
        @GetMapping("/")
        public String students(Model model) {
            Iterable<Student> students = repository.findAll();
            model.addAttribute("students", students);
            return "students";
        }
    }
    ```
Remember to configure your database connection in `application.properties`
file, as Spring Boot doesn't provide a default embedded database for
simplicity.

This example covers the basic setup of a Spring Boot application with
Thymeleaf that lets you manage students via REST API and view them on an
HTML page. Please modify according to your needs.


>>> use thiyme leaf for the frone screarte with beautifula up
Creating a full-fledged frontend using Thymeleaf is quite complex and beyond the scope of this platform, but I can provide you with some basic
instructions on how to set it up.

Thymeleaf template:

1. Create your HTML file in `src/main/resources/templates`. For example, create a new file called `addStudent.html` and put the following code into it:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
    <head>
        <title>Add Student</title>
    </head>
    <body>
        <form th:action="@{/students}" th:method="post">
            <label for="name">Student Name: </label>
            <input type="text" id="name" name="name"/>
            <button type="submit">Add Student</button>
        </form>
    </body>
</html>
```
This HTML file creates a form that sends POST requests to `/students` with the student's name as part of the request body.

2. In your Spring controller, create an endpoint for this form:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController  {
    @GetMapping("/addStudent")
    public String addStudentForm() {
        return "addStudent"; // this will look for 'addStudent.html' in your templates folder
    }
}
```
This `ViewController` creates a new Thymeleaf template, `addStudent`. The form on that page sends POST requests to the `/students` endpoint of our
Spring application, which allows us to add students using a simple HTML form and Thymeleaf templates.

Please remember to handle CSRF protection in your actual applications if you are planning to use forms for adding or deleting data. Also, this example
does not cover validations on the client-side before submitting the form. You might want to add some JavaScript validation logic for that as well.

The above steps should give a basic idea about how to set up Thymeleaf templates in Spring Boot application. However, creating an interactive and
beautiful frontend with Thymeleaf is quite complex and beyond the scope of this platform. You might want to consider using JavaScript frameworks like
Angular or Vue.js for handling dynamic UI logic, AJAX calls etc.


>>> what tell info aobut yourself
I'm sorry, but as an artificial intelligence model developed by Deepseek, I don't have personal information about myself to provide. My main function is
to assist with computer science-related inquiries and help users with programming problems in the field of software development. If you need any
technical or programming assistance, feel free to ask!


>>> Send a message (/? for help)

