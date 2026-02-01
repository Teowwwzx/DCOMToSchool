# Spring Boot Migration & Fintech Concepts
*Revision Notes for Ant International Interview Prep*

## 1. Core Architecture: RMI vs. REST

### The Legacy Approach: Java RMI (Remote Method Invocation)
*   **How it works:** Java-to-Java direct communication. The client calls methods on a remote object as if it were local.
*   **The Problem (Why we are moving away):**
    *   **Coupling:** Both client and server MUST be written in Java.
    *   **Stateful:** Keeps connections open, which consumes server memory. Hard to scale to millions of users.
    *   **Firewalls:** Uses random non-standard ports, often blocked by corporate firewalls.

### The Modern Approach: Spring Boot REST API
*   **How it works:** Communication via HTTP using standard methods (GET, POST, PUT, DELETE) and JSON data.
*   **Why Ant International wants this:**
    *   **Interoperability:** Works with Web (React), Mobile (iOS/Android), and other Payment Gateways.
    *   **Stateless:** Server handles the request and forgets the user. Easy to scale horizontally (add more servers).
    *   **Standard:** Uses port 80/443, friendly to cloud infrastructure.

---

## 2. HTTP Semantics & Error Handling

*Reliable error handling is crucial for "Mission Critical" financial apps.*

| Status Code | Name | Meaning in Fintech Context |
| :--- | :--- | :--- |
| **200** | OK | Success. The transaction or query completed. |
| **201** | Created | Success. A new resource (e.g., a new User or Payslip) was created. |
| **400** | Bad Request | Client error. Invalid input (e.g., negative salary amount). |
| **401** | Unauthorized | Authentication failed. Wrong password or missing token. |
| **403** | Forbidden | Authenticated, but no permission (e.g., Employee trying to access HR Admin data). |
| **404** | Not Found | The resource (Payslip ID 999) does not exist. |
| **500** | Internal Server Error | Server crash or DB connection failure. **Never** leak stack traces here! |

---

## 3. Data Layer: JDBC vs. JPA (Hibernate)

### The Legacy Approach: Manual JDBC
*   **Code:** `Connection conn = DriverManager.getConnection(...);`
*   **The Problem:**
    *   Boilerplate code (opening/closing connections).
    *   SQL Injection risk if string concatenation is used (though PreparedStatement helps).
    *   Hard to refactor (SQL strings scattered everywhere).

### The Modern Approach: Spring Data JPA
*   **Code:** `public interface UserRepository extends JpaRepository<User, Integer> { ... }`
*   **Why it's better:**
    *   **ORM (Object-Relational Mapping):** Maps Java Classes (`User`) directly to DB Tables.
    *   **Productivity:** Automatic implementation of basic CRUD (save, delete, findById).
    *   **Database Agnostic:** Easier to switch between databases (e.g., PostgreSQL to Oracle) without rewriting SQL.

---

## 4. Key "Fintech" Keywords for Interviews

*   **Transactional:** (`@Transactional`) Ensures a series of DB steps either **ALL** succeed or **ALL** fail. Crucial for money transfers.
*   **Idempotency:** Ensuring that if a client sends the same payment request twice (due to lag), the money is only deducted once.
*   **Statelessness:** The key to high scalability in cloud environments.
