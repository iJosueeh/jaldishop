FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar wrapper de Maven y descriptor pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x mvnw

# Descargar dependencias en caché de capas
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src/ src/

# Compilar empaquetando en JAR (omitiendo tests para agilizar el build)
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el JAR generado desde la etapa de compilación
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto del backend
EXPOSE 8080

# Optimización de memoria JVM para contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
