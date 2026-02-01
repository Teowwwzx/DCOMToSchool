# Spring Beans & Dependency Injection
*The "Magic" that connects everything*

## 1. What is a "Bean"?
A **Bean** is just a Java Object that Spring manages for you.
Instead of you creating objects manually (`User service = new UserService()`), Spring creates them at startup and keeps them in a "Container" (ApplicationContext).

## 2. `@Bean` vs. `@Component` (How to create Beans)

### Method 1: The `@Component` family (Automatic)
You've already seen this!
*   `@Service` (on `AuthService`)
*   `@Repository` (on `UserRepository`)
*   `@RestController` (on `AuthController`)

These are all specialized versions of `@Component`. When Spring starts, it scans your code, finds these classes, creates an instance of each, and stores them as **Beans**.

### Method 2: The `@Bean` annotation (Manual)
Sometimes you need to create a Bean from a class you **didn't write** (like a third-party library, e.g., `BCryptPasswordEncoder`). You can't add `@Service` to their source code!

So, you write a configuration method:

```java
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // I am manually creating this object and giving it to Spring
        return new BCryptPasswordEncoder(); 
    }
}
```

Now, anywhere in your app, you can say:
```java
@Autowired
private PasswordEncoder encoder;
```
And Spring will inject the object you created in the `@Bean` method.

---

## 3. Why is this useful? (Dependency Injection)
Imagine `AuthService` needs a `UserRepository`.
*   **Without Spring:**
    `this.userRepo = new PostgresUserRepository();` -> **Tightly Coupled.** You are stuck with Postgres.
*   **With Spring (`@Autowired`):**
    `@Autowired private UserRepository userRepo;`
    Spring looks in its bag of Beans. "Do I have a User Repo? Yes! Here you go."

This makes testing easy. In production, Spring gives you the **Real Repo**. In tests, you give it the **Mock Repo**. The Service code never changes.

## Summary for Interviews
> "I use `@Bean` in `@Configuration` classes to define third-party dependencies like `PasswordEncoder` or `RestTemplate`. For my own classes, I prefer stereotype annotations like `@Service` or `@Component` for automatic detection."
