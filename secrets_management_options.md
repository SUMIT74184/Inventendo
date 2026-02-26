That's a very important question. Storing secrets directly in configuration files is not secure. Here are a few options for managing secrets and environment variables, from simple to more advanced:

### 1. Using a `.env` File (Recommended for Local Development)

This is a great approach for local development. You can create a `.env` file in the root of your project to store your secrets. This file should be added to your `.gitignore` file so it's not committed to your code repository.

**How to use it:**

1.  **Create a `.env` file** in the root of your project (`/home/sumit/IdeaProjects/Auth/.env`).
2.  **Add your secrets** to the `.env` file like this:

    ```
    POSTGRES_USER=postgres
    POSTGRES_PASSWORD=a-strong-password
    JWT_SECRET=your-super-secret-key-that-is-long-enough-to-be-secure
    ```

3.  **Update your `docker-compose.yml`** to use these variables. Docker Compose automatically picks up the `.env` file.

    ```yaml
    services:
      postgres:
        environment:
          POSTGRES_USER: ${POSTGRES_USER}
          POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
          ...
      auth-service:
        environment:
          DB_USERNAME: ${POSTGRES_USER}
          DB_PASSWORD: ${POSTGRES_PASSWORD}
          JWT_SECRET: ${JWT_SECRET}
          ...
    ```

4.  **Add `.env` to your `.gitignore` file.**

### 2. Docker Secrets (More Secure, for Swarm)

For production environments using Docker Swarm, you can use Docker Secrets. This is a more secure way to manage secrets as they are encrypted and only accessible to the services that are granted access.

### 3. External Secret Manager (Most Secure, for Production)

For the highest level of security and flexibility, you can use an external secret manager like [HashiCorp Vault](https://www.vaultproject.io/) or [AWS Secrets Manager](https://aws.amazon.com/secrets-manager/). Spring Boot has good integration with these services through Spring Cloud.

---

For your current setup, **I recommend using the `.env` file method**. It's easy to set up for local development and keeps your secrets out of your source code.

Would you like me to help you set up the `.env` file and update the `docker-compose.yml`?
