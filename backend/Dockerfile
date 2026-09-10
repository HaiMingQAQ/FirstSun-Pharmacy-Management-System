FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY . .
RUN mvn -pl yudao-server -am package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /yudao-server
COPY --from=builder /build/yudao-server/target/yudao-server.jar app.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV ARGS=""

EXPOSE 48080
CMD java ${JAVA_OPTS} -jar app.jar ${ARGS}
