FROM eclipse-temurin:25-jdk-alpine

RUN apk add --no-cache python3 py3-pip

RUN pip3 install --break-system-packages \
    requests \
    beautifulsoup4 \
    pdfplumber

WORKDIR /app
COPY target/fuelnet-0.0.1.jar app.jar
COPY python/price_scraping.py price_scraping.py

EXPOSE 3015
ENTRYPOINT ["java", "-jar", "app.jar"]
