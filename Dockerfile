FROM bellsoft/liberica-openjdk-debian:17.0.7-7

RUN apt-get update && apt-get upgrade -y
RUN mkdir -p /app/drivers
WORKDIR /app

COPY ./google-chrome-stable_current_amd64.deb /tmp/
RUN apt-get install -y /tmp/google-chrome-stable_current_amd64.deb
RUN rm -f /tmp/google-chrome-stable_current_amd64.deb

COPY ./chromedriver_112 /app/drivers/
COPY ./chromedriver_113 /app/drivers/
COPY ./build/libs/godville-bot-modern-0.0.1-SNAPSHOT.jar /app/

CMD ["java", "-jar", "/app/godville-bot-modern-0.0.1-SNAPSHOT.jar"]