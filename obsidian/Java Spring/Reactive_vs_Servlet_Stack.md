# Reactive vs. Servlet Stack
*Synchronous Blocking (Legacy) vs. Asynchronous Non-Blocking (Modern)*

## 1. The Servlet Stack (What you are using now)
*   **Technology:** Spring MVC (Blocking I/O).
*   **The Model:** **"One Thread per Request."**
    *   User A calls API -> Thread 1 starts -> Waits for DB -> Sends Response -> Thread 1 Closes.
    *   User B calls API -> Thread 2 starts...
*   **The Limit:** A server has a limit on threads (e.g., 200).
    *   If 201 users request data at the exact same millisecond, User 201 waits in a queue.
*   **Pros:** Easy to write, easy to debug (stack traces are clear).
*   **Cons:** Wastes memory (each thread takes ~1MB) while waiting for the database.

---

## 2. The Reactive Stack (Spring WebFlux)
*   **Technology:** Project Reactor, Netty (Non-Blocking I/O).
*   **The Model:** **"Event Loop" (Node.js style).**
    *   A small number of threads (e.g., 4) handle ALL requests.
    *   User A calls API -> Thread 1 accepts -> Sends "Event" to DB -> Thread 1 immediately handles User B.
    *   When DB replies -> Thread 1 wakes up and sends response to User A.
*   **The Advantage:** Can handle **tens of thousands** of concurrent connections with very little RAM.
*   **Pros:** Extreme scalability (High Concurrency). Perfect for streaming data.
*   **Cons:** HARD to debug. "Callback Hell." Complex mental model.

---

## 3. Which one does Fintech use?
**It depends on the Use Case.**

*   **Transactional Systems (Core Banking / Ledger):** Usually **Servlet Stack (Blocking)**.
    *   *Why?* Reliability and "ACID" transactions are more important than raw concurrency. The logic is complex, and Sequential code is safer to write.
*   **Payment Gateways / Real-time Pricing / Notifications:** Often **Reactive Stack (WebFlux)**.
    *   *Why?* They need to handle 50,000 requests/second from mobile apps, and most of the time is spent waiting for 3rd party banks to reply. Reactive saves resources here.

## 4. Ant International Context
Since the JD mentions "High volume, low latency," they likely have a mix.
*   **Your Answer:** "I am implementing the Servlet stack (Spring MVC) currently because standard Transaction Management is critical for Payroll accuracy. However, I am aware that for high-throughput gateways, we might switch to **Spring WebFlux** to handle non-blocking I/O efficiently."
