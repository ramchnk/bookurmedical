FROM diyfr/openjdk8-alpine-fonts
ADD target/partnernotifserv-1.0-SNAPSHOT.jar target/partnernotifserv-1.0-SNAPSHOT.jar
ADD src src
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "target/partnernotifserv-1.0-SNAPSHOT.jar"]