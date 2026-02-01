# Testing: JUnit 5 & Mockito
*How to prove your code works without running the server*

## 1. Why Mock? (The "Unit" in Unit Test)
We want to test `AuthService.login()`.
**The Problem:** `AuthService` depends on `UserRepository`. If we run a real test, it needs a real database connection. That is slow and brittle.

**The Solution:** We create a **Fake (Mock)** Repository.
*   **We say:** "Hey MockRepo, when someone asks for 'admin', just pretend you found a user."
*   **Result:** We can test the login logic instantly, without Postgres.

---

## 2. The Anatomy of a Test Class

```java
@ExtendWith(MockitoExtension.class) // Enables the Mocking framework
class AuthServiceTest {

    @Mock
    private UserRepository userRepository; // The Fake DB

    @InjectMocks
    private AuthService authService; // The Real Service (with the fake DB inside it)

    @Test
    void login_Success() {
        // 1. ARRANGE (Setup the scenario)
        User fakeUser = new User();
        fakeUser.setUsername("teow");
        fakeUser.setPassword("hashed_123"); 
        
        // Teach the mock what to do
        when(userRepository.findByUsername("teow")).thenReturn(Optional.of(fakeUser));

        // 2. ACT (Run the method)
        LoginRequest request = new LoginRequest("teow", "123");
        UserDTO result = authService.login(request);

        // 3. ASSERT (Verify the result)
        assertNotNull(result);
        assertEquals("teow", result.getUsername());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        // 1. ARRANGE
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // 2. ACT & ASSERT
        assertThrows(RuntimeException.class, () -> {
            authService.login(new LoginRequest("ghost", "123"));
        });
    }
}
```

## 3. Key Annotations
*   `@Test`: Marks a method as a test case.
*   `@Mock`: Creates a dummy object (e.g., a dummy Repo).
*   `@InjectMocks`: Creates the real object (`AuthService`) and puts the mocks inside it.
*   `when(...).thenReturn(...)`: The "Script". You are telling the mock how to behave.

## Summary for Interviews
> "I use **JUnit 5** for the test runner and **Mockito** to mock out dependencies like Repositories. This allows me to test business logic in isolation (Unit Testing) ensuring my service handles edge cases like 'User Not Found' correctly without needing a live database connection."
