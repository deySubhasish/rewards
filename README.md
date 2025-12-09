# Rewards Service

A Spring Boot application that calculates and manages customer rewards points based on their transactions.

## Features

- Calculate rewards points for customer transactions
- View monthly rewards breakdown
- Filter transactions by date range
- Caching for improved performance
- RESTful API with Swagger/OpenAPI documentation
- Input validation and error handling
- In-memory H2 database with web console

## Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/deySubhasish/rewards
   cd rewards
   ```

2. **Build the application**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Documentation: http://localhost:8080/swagger-ui.html
   - H2 Database Console: http://localhost:8080/h2-console
     - JDBC URL: jdbc:h2:mem:rewardsdb
     - Username: sa
     - Password: (leave empty)

## API Endpoints

### Get Customer Rewards
```
GET /api/customers/{customerId}/rewards
```

**Query Parameters:**
- `days`: (Optional) Number of days from today to fetch transactions
- `months`: (Optional) Number of months from today to fetch transactions (used when days is not provided)
- `startDate`: (Optional) Start date for filtering transactions (format: yyyy-MM-dd'T'HH:mm:ss)
- `endDate`: (Optional) End date for filtering transactions (defaults to current date/time)

**Example Requests:**
```
GET /api/customers/1/rewards?days=30
GET /api/customers/1/rewards?months=6
GET /api/customers/1/rewards?startDate=2023-01-01T00:00:00&endDate=2023-12-31T23:59:59
```

**Response:**
```json
{
  "customerId": 1,
  "totalRewardPoints": 120,
  "monthlyRewards": [
    {
      "yearMonth": "2023-11",
      "rewardPoints": 90
    },
    {
      "yearMonth": "2023-12",
      "rewardPoints": 30
    }
  ]
}
```

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2023-12-09T11:23:45.123",
  "status": 400,
  "error": "Invalid value 'abc' for parameter 'customerId'. Expected type: Long",
  "path": "/api/customers/abc/rewards"
}
```

### 404 Not Found
```json
{
  "timestamp": "2023-12-09T11:23:45.123",
  "status": 404,
  "error": "Customer not found with ID: 999",
  "path": "/api/customers/999/rewards"
}
```

## Caching

The application uses Caffeine for caching rewards calculations. Cache configuration can be adjusted in `application.yml`.

## Testing

Run the test suite with:
   ```bash
   mvn  test
   ```

## Built With

- Spring Boot 3.5.8
- Spring Web
- Spring Data JPA
- H2 Database
- Caffeine Cache
- OpenAPI 3.0 (Swagger)
- Maven


## Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [OpenAPI](https://swagger.io/specification/)
- [H2 Database](https://www.h2database.com/)
