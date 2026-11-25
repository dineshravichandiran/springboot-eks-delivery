# Runtime image only: the jar is built and tested by Maven first, so the image ships the exact jar that passed the tests.
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}
RUN useradd --system --uid 10001 --no-create-home appuser
WORKDIR /opt/app
COPY target/items-service.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/opt/app/app.jar"]
