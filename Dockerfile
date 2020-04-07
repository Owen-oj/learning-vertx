# Extend vert.x image
FROM openjdk:8-jre-alpine

#                                                       (1)

ENV VERTICLE_FILE books-1.0.0-SNAPSHOT-fat.jar
# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

ENV VERTX_OPTS "-Dvertx.options.eventLoopPoolSize=26 -Dvertx.options.deployment.worker=true"
#ENV CLASSPATH "/target/classes/:/usr/verticles/classes/:"


EXPOSE 8888

# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE"]

