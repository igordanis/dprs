FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER igordanis@yahoo.com
VOLUME /tmp
ADD ./target/dprs-0.0.1.jar app.jar
RUN sh -c 'touch /app.jar'
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]