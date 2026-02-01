# Spring Boot & Lombok Cheat Sheet
*The "Must-Know" Annotations for Your Interview*

## 1. Core Framework (The Wiring)
| Annotation | Purpose |
| :--- | :--- |
| **`@SpringBootApplication`** | The "On" switch. Used on your main class to start everything. |
| **`@Configuration`** | Marks a class as a source of `@Bean` definitions. |
| **`@Bean`** | Used inside `@Configuration` to manually create a Spring Bean. |
| **`@Autowired`** | Tells Spring: "Find a Bean of this type and plug it in here" (Dependency Injection). |
| **`@Value("${name}")`** | Pulls a value from your `application.properties` file. |

## 2. Stereotypes (The Layers)
| Annotation | Layer | Purpose |
| :--- | :--- | :--- |
| **`@RestController`** | API | Handles HTTP requests and returns JSON. |
| **`@Service`** | Logic | Marks a class as holding Business Logic. |
| **`@Repository`** | Data | Marks a class as the Database Access Layer. |
| **`@Component`** | Generic | Use this if the class doesn't fit the three above. |

## 3. Web & REST (The Path)
| Annotation | Purpose | Example |
| :--- | :--- | :--- |
| **`@RequestMapping`** | Base path for a controller. | `/api/users` |
| **`@GetMapping`** | Read data (HTTP GET). | `/api/users/1` |
| **`@PostMapping`** | Create data (HTTP POST). | `/api/login` |
| **`@RequestBody`** | The JSON data sent *to* the server. | `LoginRequest` |
| **`@RequestParam`** | Variables in the URL query string. | `?id=5` |
| **`@PathVariable`** | Variables in the URL path. | `/users/{id}` |

## 4. Data / JPA (The Database)
| Annotation | Purpose |
| :--- | :--- |
| **`@Entity`** | Marks a Java class as a Database Table. |
| **`@Id`** | Marks the Primary Key column. |
| **`@GeneratedValue`** | Auto-increments the ID (like SERIAL in Postgres). |
| **`@Column`** | Customizes the column name (e.g., `name = "f_name"`). |
| **`@Enumerated`** | Tells JPA to store an Enum as a String in the DB. |

## 5. Lombok (The Shortcuts)
| Annotation | Purpose |
| :--- | :--- |
| **`@Data`** | Generates Getters, Setters, toString, and equals. |
| **`@Builder`** | Enables the `Class.builder()...build()` pattern. |
| **`@NoArgsConstructor`** | Generates a blank constructor (Required for JPA). |
| **`@AllArgsConstructor`** | Generates a constructor with all fields. |
| **`@Slf4j`** | Automatically gives you a `log` object for printing to console. |

## 6. Testing
| Annotation | Purpose |
| :--- | :--- |
| **`@Test`** | Marks a method as a test case. |
| **`@Mock`** | Creates a fake object for unit testing. |
| **`@InjectMocks`** | Creates the real object and inserts the fakes into it. |
