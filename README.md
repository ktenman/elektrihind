# Elektrihind App

## Overview

Elektrihind is a service dedicated to providing insights on electricity prices in Estonia. Using scheduled tasks, it
fetches electricity prices at regular intervals and offers users a perspective on the most expensive and cheapest
pricing hours. The service is designed to be containerized using Docker, making it easily deployable across various
environments.

## Features

* Fetches electricity prices at regular intervals
* Provides details on the most expensive and cheapest pricing hours
* Built with Spring Boot, making it easily scalable and maintainable
* Containerized with Docker for simplified deployment

### Telegram Integration

* Sends automated alerts and updates to a designated Telegram chat
* Offers retries in sending messages, ensuring delivery
* Adheres to a daily message limit to prevent spamming
* Displays electricity prices in a user-friendly format on Telegram

## Pre-requisites

* Docker installed
* Docker service running and enabled on startup

```
sudo systemctl enable docker
```

## Building the Docker Image

To build the Docker image for the Elektrihind app:

```
docker build -t elektrihind-app .
```

## Running the Application

### Option 1: Running in Detached Mode

Run Elektrihind app in detached mode (in the background):

```
docker run -d -p 8080:8080 --restart always --name elektrihind-app-container elektrihind-app
```

### Option 2: Running in Foreground

To view logs and messages, run Elektrihind app in the foreground:

```
docker run -p 8080:8080 elektrihind-app
```

## Stopping the Application

To stop and remove the application running in detached mode:

```
docker stop elektrihind-app-container
docker rm elektrihind-app-container
```
