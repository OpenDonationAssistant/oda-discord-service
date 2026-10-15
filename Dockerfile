FROM fedora:44
WORKDIR /app
COPY target/oda-discord-service /app

CMD ["./oda-discord-service"]
