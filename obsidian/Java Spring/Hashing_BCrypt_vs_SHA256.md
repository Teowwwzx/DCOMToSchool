# Password Hashing: MessageDigest vs. BCrypt
*Why Fintechs NEVER use MessageDigest for passwords*

## 1. MessageDigest (SHA-256)
*   **What it is:** A general-purpose cryptographic hash function.
*   **Designed for:** Speed. It's meant to verify file integrity (checksums) or digital signatures quickly.
*   **The Problem for Passwords:**
    *   **It's TOO Fast:** A modern GPU can calculate **billions** of SHA-256 hashes per second. If a hacker steals your database, they can brute-force guess every password in minutes.
    *   **No Salt (by default):** If User A and User B both have the password "password123", their hash is identical. Hackers use "Rainbow Tables" (pre-computed lists) to crack these instantly.

## 2. BCrypt
*   **What it is:** A specialized password-hashing function (based on the Blowfish cipher).
*   **Designed for:** **Slowness.**
*   **Why it's better for Passwords:**
    *   **Configurable "Work Factor" (Cost):** You can tell BCrypt to run the algorithm 10 times, 12 times, or 15 times (logarithmic scale). This makes hashing take 0.5 seconds instead of 0.000001 seconds.
    *   **Brute-Force Protection:** This slowness destroys the hacker's speed advantage. Instead of billions of guesses per second, they can only try a few per second.
    *   **Automatic Salting:** BCrypt generates a random "salt" (random string) for *every* password.
        *   User A: "password123" -> `$2a$10$randomSaltA...`
        *   User B: "password123" -> `$2a$10$randomSaltB...`
        *   They look completely different in the database!

## 3. What is a Brute-Force Attack?
**"Brute-forcing"** is basically guessing every possible password until one works.

### How it works (The Math)
Imagine a 4-digit PIN (0000-9999). There are 10,000 combinations.
*   **Human:** Takes hours to try them all.
*   **Computer:** Can try them all in a millisecond.

### Why "Speed" is the Enemy
If your hashing algorithm (SHA-256) is fast, a hacker with a GPU can try **billions** of guesses per second.
*   **SHA-256:** "Is the password 'apple'? No. 'banana'? No..." (Checks 1,000,000,000 in 1 second).
*   **BCrypt:** "Is the password 'apple'? ...[wait 0.2s]... No." (Checks 5 in 1 second).

**Result:** A password that takes **1 minute** to crack with SHA-256 might take **100 years** to crack with BCrypt.

## 4. What is a Rainbow Table?
This is a pre-calculated list of common passwords and their hashes.
1.  Hacker calculates SHA-256("password123") = `ef92b778...` and saves it in a huge table.
2.  Hacker steals your DB and sees `ef92b778...`.
3.  Hacker looks up the hash in their table -> "Aha! It's 'password123'!"

**How Salt fixes this:**
*   BCrypt adds random junk (`salt`) to the password *before* hashing.
*   Input becomes: `password123` + `randomSalt8238`.
*   The hash is now totally unique. The pre-calculated Rainbow Table is useless because the hacker didn't pre-calculate "password123randomSalt8238".

## Summary for Interviews
> "I prefer BCrypt over SHA-256 (MessageDigest) because BCrypt is an **adaptive** hashing function. It includes a work factor to slow down execution intentionally, protecting against brute-force attacks, and it handles salting automatically to prevent Rainbow Table attacks."
