# StreamCheck

A software solution to find the best possible combination of streaming packages to watch your favorite teams and tournaments.

## Overview
StreamCheck helps you optimize your streaming subscriptions by analyzing different package combinations based on your selected teams and tournaments. It provides detailed price and coverage information, helping you make cost-effective decisions.

## Features
- Team and tournament selection
- Package comparison with price and coverage details
- Two optimization strategies:
- Static combination for consistent viewing needs
- Sequential combination for seasonal viewing patterns
- Coverage visualization for both live games and highlights
- Detailed breakdown of covered and uncovered games

## How It Works

### Package Selection Logic
The combination logic employs two approaches:

1. **Static Combination**
   - Uses a greedy algorithm to select multiple packages
   - Optimizes for maximum coverage while considering price efficiency
   - Best for consistent viewing patterns throughout the season

2. **Sequential Combination**
   - Analyzes game distribution across months
   - Calculates variance to identify viewing patterns
   - Recommends periodic package switches when:
     - High variance in game distribution
     - Low average games per month
   - Uses month-by-month optimization

### Possible Improvements
- Depending on the size of the dataset, a linear programming based solution might provide a better theoretical optimum, although it comes with a higher computational cost.
- Add caching for frequently searched combinations
- Enhanced UI/UX experience(Package Details in the results screen, Compact structure etc.)
- Performance optimizations for large datasets(The variance and efficiency thresholds must be adjusted based on the dataset and real-life data)
- Unit Tests

## Getting Started

## Project Structure
The project expects the following structure:

```text
project-root/
├── src/
├── static/
├── pom.xml
├── Dockerfile
└── docker-compose.yaml
```

### Prerequisites
- Docker

### Running Locally
1. Ensure Docker is running
2. Clone the repository and navigate to the project directory
3. Run:
   ```bash
   docker compose up --build
   ```
4. Navigate to **http://localhost:8080/**

