FROM adoptopenjdk/openjdk11:latest
VOLUME /tmp
ADD server.yaml server.yaml
ADD swagger.yaml swagger.yaml
ADD accountingApp.jar /accountingApp.jar
ADD start.sh /start.sh
ADD templates /templates

EXPOSE 8080 8081

# Log line with current date / time
RUN ["echo", "\"[$(uname -n)] $(date)\"", ">> /proc/self/fd/2"]

ENTRYPOINT [ "/start.sh"]
