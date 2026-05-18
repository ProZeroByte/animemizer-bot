FROM eclipse-temurin:26-jre-alpine
WORKDIR /app

# Устанавливаем часовой пояс (актуально для Польши)
ENV TZ=Europe/Warsaw
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Создаем непривилегированного пользователя для безопасности
RUN addgroup -S botgroup && adduser -S botuser -G botgroup
USER botuser

COPY *-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
