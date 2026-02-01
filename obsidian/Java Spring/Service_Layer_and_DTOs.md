# Service Layer & DTOs: Handling Data Flow
*Structuring Data for the API*

## 1. DTO (Data Transfer Object) = Schema
*Is this like a FastAPI Schema (Pydantic)?* **EXACTLY.**

*   **Python (FastAPI):** You use **Pydantic Models** (schemas) to define what data calls `IN` and what goes `OUT` of your API.
    *   `class UserCreate(BaseModel): username: str, password: str`
*   **Java (Spring Boot):** We use **DTOs (Data Transfer Objects)**.
    *   `UserDTO.java`

### Why not just use the Entity (`User.java`)?
This is a **MAJOR** interview question.
*   **Security:** Your `User` Entity has a `password` field (the hash). You **NEVER** want to send the password field back to the frontend in a JSON response.
*   **Decoupling:** Your database table might have 50 columns. Your mobile app might only need 3. The DTO lets you shape the data specifically for the API consumer.

### `UserDTO.java`
This class defines exactly what the outside world sees.
```java
@Data
@Builder
public class UserDTO {
    private String username;
    private String email;
    private String role;
    // NOTICE: No password field here!
}
```

---

## 2. `AuthService.java`
This layer connects the "Schema" (DTO) to the "Model" (Entity).

### Typical Workflow in `AuthService`:
1.  **Receive DTO:** Controller passes a `LoginRequestDTO` (username, password).
2.  **Logic:** Service verifies the password.
3.  **Fetch Entity:** Service gets the full `User` entity from the DB.
4.  **Convert:** Service converts `User` entity -> `UserResponseDTO` (removing sensitive info).
5.  **Return DTO:** The clean DTO is sent back to the Controller.

### Code Example (Mental Model):
```java
public UserDTO register(RegisterRequest request) {
    // 1. Convert Request DTO -> Entity
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPassword(encode(request.getPassword())); // Logic
    
    // 2. Save Entity
    userRepository.save(user);
    
    // 3. Convert Entity -> Response DTO
    return new UserDTO(user.getUsername(), user.getEmail());
}
```

## Summary for Interviews
> "I use DTOs to decouple my internal database structure (Entities) from the external API contract. This prevents over-fetching data and accidental leakage of sensitive fields like password hashes or internal IDs."
