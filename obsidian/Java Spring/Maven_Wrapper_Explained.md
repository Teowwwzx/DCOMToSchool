# What is `mvnw` (Maven Wrapper)?
*Tools of the Trade: Build Automation*

## The Problem
Imagine you join a new team at Ant International.
*   Developer A has **Maven 3.6** installed.
*   Developer B has **Maven 3.9** installed.
*   The CI/CD Server (Jenkins/GitHub Actions) has **Maven 3.2**.

You try to build the project, and it fails because of version differences. This is "dependency hell" for build tools.

## The Solution: Maven Wrapper (`mvnw`)
The `mvnw.cmd` (for Windows) and `mvnw` (for Linux/Mac) scripts are the **Maven Wrapper**.

*   **What it does:** It is a script included *inside* the project. When you run it, it **automatically downloads the exact correct version of Maven** needed for this specific project.
*   **Why use it:**
    *   **Consistency:** Everyone (you, your boss, the server) uses the exact same Maven version.
    *   **Zero Setup:** A new developer doesn't need to install Maven manually. They just clone the repo and run `./mvnw clean install`.

## How to use it
Instead of typing `mvn`, you type `.\mvnw`:

```powershell
# Build the project
.\mvnw clean install

# Run the Spring Boot application
.\mvnw spring-boot:run
```

## "Where is the 'Run' logic defined?"
A common misconception is that `mvnw.cmd` controls *what* runs. It does not.

1.  **`mvnw.cmd`**: Only ensures the correct **Maven tool** is installed and starts it.
2.  **`pom.xml`**: This is the "brain". It contains the **Spring Boot Maven Plugin**:
    ```xml
    <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>
    ```
3.  **The Plugin**: When you run `spring-boot:run`, this plugin scans your code, finds the class with `@SpringBootApplication` and `public static void main`, and starts it.

## Summary for Interviews
If asked, "Why do we include `mvnw` in the repository?":
> "It ensures build reproducibility. It locks the Maven version for the project so that every developer and CI environment builds the code using the exact same toolset, eliminating 'it works on my machine' issues related to build tool versions."
