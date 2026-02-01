# Java Syntax & Crypto Deep Dive
*Explaining the AuthService Code Logic*

## 1. The Service Flow (You are correct!)
Your understanding is spot on:
1.  **Input:** `login(LoginRequest request)` receives the **Schema** (DTO) from the controller.
2.  **Database:** `userRepository.findByUsername(...)` generates the SQL to find the raw **Entity**.
3.  **Logic:** The service compares the passwords.
4.  **Output:** `UserDTO.builder()...build()` converts the Entity into a clean **Schema** (DTO) to return to the API.

---

## 2. Lombok Annotations
### `@Data`
*   **What it is:** A Lombok shortcut.
*   **What it generates:**
    *   `getUsername()`, `setUsername()`
    *   `toString()`
    *   `equals()`, `hashCode()`
*   **Why:** So you don't have to write 50 lines of boilerplate code for a simple DTO.

### `.builder()` (The Builder Pattern)
*   **The Problem:** Constructors with many parameters are confusing:
    `new UserDTO(1, "teow", "Teow", "Zx", "email@...", "ADMIN", ...)` -> *Which string is the first name? Which is the last name?*
*   **The Solution (Builder):**
    ```java
    UserDTO.builder()
        .firstName("Teow") // Explicit!
        .lastName("Zx")    // Explicit!
        .build();
    ```
*   **How:** The `@Builder` annotation automatically generates this fluent API for you.

---

## 3. Cryptography (`MessageDigest`)
*   **What is `MessageDigest`?**
    It is Java's standard library class for **Hashing** (creating a digital fingerprint of data). It is part of `java.security`.

*   **`.getInstance("SHA-256")`**
    *   This is the **Factory Pattern**. You don't say `new MessageDigest()`. You ask the static factory method to "get an instance" of a specific algorithm (like SHA-256, MD5, SHA-1).

*   **`byte[]` (Byte Array)**
    *   **Is it a List?** **NO.**
    *   **List (`ArrayList`):** A dynamic, growable collection (`[]` in Python).
    *   **Array (`[]`):** A **fixed-size** block of memory. `byte[]` is a raw sequence of 8-bit integers.
    *   **Why use it here?** Cryptography works on raw binary data (0s and 1s), not high-level objects. `md.digest()` returns the raw binary hash.

### The Logic Explained
```java
// 1. Get the tool
MessageDigest md = MessageDigest.getInstance("SHA-256");

// 2. Hash the password
// "password123".getBytes() -> converts String to raw binary (byte[])
// md.digest(...) -> runs the math, returns the hash as raw binary (byte[])
byte[] hash = md.digest(password.getBytes());

// 3. Convert Binary -> Hex String (Readable)
// BigInteger helps convert the raw binary number into a big math number
BigInteger number = new BigInteger(1, hash);
// Convert that number to Base-16 (Hexadecimal) string
return number.toString(16);
```

## Summary for Interviews
> "I use `MessageDigest` for legacy hashing compliance, but for the modern update, I am switching to `BCryptPasswordEncoder` (Spring Security) because it handles **Salting** automatically, which `MessageDigest` does not."
