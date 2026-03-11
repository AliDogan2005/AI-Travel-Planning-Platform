# AI-Powered Travel Planning Platform

A comprehensive Spring Boot application that helps users create smart travel itineraries with AI-powered recommendations, flight search, and trip management capabilities.

## Features

### Core Functionality
- **User Authentication & Authorization** - JWT-based security with Spring Security
- **Trip Management** - Create, update, and manage travel trips
- **AI-Powered Itinerary Generation** - Google Gemini AI integration for smart travel recommendations
- **Flight Search** - Real-time flight search using Amadeus API
- **Budget Tracking** - Comprehensive budget calculation and tracking

### Technical Features
- **RESTful API Architecture**
- **PostgreSQL Database Integration**
- **JWT Authentication**
- **External API Integrations** (Amadeus, Google Gemini)
- **Validation & Error Handling**
- **Airport Data Management**

## Tech Stack

- **Backend**: Spring Boot 3.2.3, Java 21
- **Database**: PostgreSQL
- **Security**: Spring Security, JWT
- **External APIs**: 
  - Amadeus API (Flight Search)
  - Google Gemini AI (Itinerary Generation)
- **Build Tool**: Maven
- **Documentation**: Spring Boot Actuator

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 12+
- API Keys for:
  - Amadeus API (Flight data)
  - Google Gemini AI (Itinerary generation)

## Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd TravelPlanningPlatform
```

### 2. Database Setup
```sql
-- Create database
CREATE DATABASE travel_planning;

-- Create user (optional)
CREATE USER travel_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE travel_planning TO travel_user;
```

### 3. Configure Environment Variables
Copy the environment template and add your API keys:
```bash
cp .env.template .env
```

Edit `.env` file with your actual API keys:
```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/travel_planning
DATABASE_USERNAME=your_db_username
DATABASE_PASSWORD=your_db_password

# JWT Configuration  
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here_minimum_256_bits
JWT_EXPIRATION=86400000

# Google Gemini AI API
GEMINI_API_KEY=your_google_gemini_api_key_here

# Amadeus Flight API
AMADEUS_API_KEY=your_amadeus_api_key_here
AMADEUS_API_SECRET=your_amadeus_api_secret_here
```

**⚠️ Important: Never commit your `.env` file to version control!**

Alternative: Set environment variables directly in your system:
```bash
export GEMINI_API_KEY="your_google_gemini_api_key"
export AMADEUS_API_KEY="your_amadeus_api_key"
export AMADEUS_API_SECRET="your_amadeus_api_secret"
export JWT_SECRET="your_jwt_secret"
```

### 4. Build and Run
```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Authentication
```
POST /api/auth/register - Register new user
POST /api/auth/login    - Login user
GET  /api/auth/test     - Test endpoint
```

### Trip Management
```
GET    /api/trips           - Get user's trips
POST   /api/trips           - Create new trip
GET    /api/trips/{id}      - Get specific trip
PUT    /api/trips/{id}      - Update trip
DELETE /api/trips/{id}      - Delete trip
```

### Itinerary Management
```
GET  /api/trips/{tripId}/itineraries                    - Get trip itineraries
POST /api/trips/{tripId}/itineraries                    - Create itinerary
POST /api/trips/{tripId}/itineraries/generate           - AI-generate itinerary
PUT  /api/itineraries/{id}                              - Update itinerary
DELETE /api/itineraries/{id}                            - Delete itinerary
```

### Flight Search
```
GET  /api/flights/search                     - Search flights
GET  /api/flights/trips/{tripId}             - Search flights for trip
GET  /api/flights/airports/country/{country} - Get airports by country
POST /api/flights/search                     - Advanced flight search
```

## API Usage Examples

### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "traveler123",
    "email": "traveler@example.com", 
    "password": "securePassword123"
  }'
```

### Create Trip
```bash
curl -X POST http://localhost:8080/api/trips \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "European Adventure",
    "destination": "Paris, France",
    "startDate": "2026-06-01",
    "endDate": "2026-06-10", 
    "totalBudget": 2000.00,
    "currency": "EUR",
    "travelersCount": 2
  }'
```

### Search Flights
```bash
curl "http://localhost:8080/api/flights/search?origin=Istanbul&destination=Paris&departureDate=2026-06-01&directFlightsOnly=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Generate AI Itinerary
```bash
curl -X POST http://localhost:8080/api/trips/1/itineraries/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "tripId": 1,
    "interests": "museums, historical sites, local cuisine",
    "travelStyle": "mid-range comfort",
    "budget": 1000.00,
    "additionalRequirements": "vegetarian-friendly options"
  }'
```

## Project Structure

```
src/
├── main/java/com/travelplanningplatform/
│   ├── client/              # External API clients
│   ├── config/              # Configuration classes
│   ├── controller/          # REST controllers
│   ├── dto/                 # Data Transfer Objects
│   ├── entity/              # JPA entities
│   ├── repository/          # Data repositories
│   ├── security/            # Security configuration
│   ├── service/             # Business logic
│   └── util/                # Utility classes
└── main/resources/
    ├── application.properties
    └── static/
```

## Security

### Environment Variables
This project uses environment variables to store sensitive information like API keys and database credentials. 

**Files to keep secure:**
- `.env` - Contains your actual API keys (never commit this)
- `.env.template` - Template file showing required variables (safe to commit)

**Getting API Keys:**
- **Google Gemini AI**: Get your API key from [Google AI Studio](https://ai.google.dev/)
- **Amadeus API**: Register at [Amadeus for Developers](https://developers.amadeus.com/)

### Production Deployment
For production environments, set environment variables through:
- Heroku: Config Vars in dashboard
- AWS: Environment variables in ECS/Elastic Beanstalk
- Docker: Using `-e` flags or docker-compose environment section
- Linux/Mac: Export commands in startup scripts

## Testing

Run tests with:
```bash
mvn test
```


## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, email aedogan2005@gmail.com.

## Roadmap

- [ ] Hotel booking integration (Booking.com API)
- [ ] Payment processing (Stripe API)
- [ ] Trip sharing functionality
- [ ] Email notifications
- [ ] Map integration
- [ ] Mobile app development
- [ ] Microservices architecture
- [ ] Redis caching
- [ ] Rate limiting
- [ ] Circuit breaker pattern

---

Made with love for travelers around the world

