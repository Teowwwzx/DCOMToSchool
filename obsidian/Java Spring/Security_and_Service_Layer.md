# Security & Service Layer: The "Guard" and the "Worker"
*Understanding Spring Security and Business Logic*

## 1. `SecurityConfig.java`: The "Bouncer"
*Is this like FastAPI's main file?* **Sort of.**

In **FastAPI**, you often define middleware (like CORS) and authentication in your main app creation.
In **Spring Boot**, `SecurityConfig` is a specialized file dedicated **only** to security rules.

### Key Concepts in this file:

1.  **"Who can go where?" (Authorization):**
    *   `requestMatchers("/api/auth/**").permitAll()`: "Let anyone visit the login page."
    *   `requestMatchers("/api/admin/**").hasRole("ADMIN")`: "Only Admins can enter this area."
    *   `anyRequest().authenticated()`: "For everything else, you must show ID."

2.  **"No Sessions Here" (Statelessness):**
    *   `.sessionManagement(... .sessionCreationPolicy(SessionCreationPolicy.STATELESS))`: This is CRITICAL for the Ant International "High Performance" requirement. We are telling the server: **"Do not remember users. Check their token on every single request."**

3.  **CSRF Disabled:**
    *   `.csrf(AbstractHttpConfigurer::disable)`: CSRF protection is for browser forms with sessions. Since we are using stateless REST APIs, we turn this off.

---

## 2. `AuthService.java`: The "Worker"
This is where the actual work happens. The **Controller** receives the request, but it hands it to the **Service** to do the thinking.

### The Login Workflow (Modern vs. Legacy)

1.  **Legacy (Your old code):**
    *   User sends password.
    *   Server manually hashes it.
    *   Server compares strings.
    *   Server keeps connection open.

2.  **Modern (`AuthService`):**
    *   **AuthenticationManager:** Spring's built-in tool. You just pass it the username/password.
    *   **"Magic" Check:** It automatically checks the DB using your `UserRepository` and verifies the hash using **BCrypt** (industry standard).
    *   **Token Generation:** If correct, the service generates a **JWT (JSON Web Token)** and gives it to the user.

### Why this structure? (Separation of Concerns)
*   **Repository:** "I just fetch data."
*   **Service:** "I apply rules (hash passwords, check account status)."
*   **Controller:** "I talk to the internet (receive JSON, send JSON)."

This separation makes the code **Testable** (a key JD requirement). You can test the logic in `AuthService` without needing a real database or a real HTTP request.
