# Instructions:
# 1. build uber jar:
#       lein.sh do clean, uberjar
# 2. build docker image
#       docker build --rm -t deas/contentreich-ecm-google-ms:latest .
# 3. run docker container
#       docker run --name contentreich-ecm-google-ms -d -p 9090:9090 deas/contentreich-ecm-google-ms:latest
#       docker run -v /conf:/conf -e CONFIG_FILE=/conf/docker.properties --rm -p 9090:9090 deas/contentreich-ecm-google-ms:latest

FROM java:8
MAINTAINER Andreas Steffan <a.steffan@contentreich.de>
# EXPOSE 8080

CMD ["java", "-Dlog_level=info", "-jar", "/contentreich-ecm-google-ms-standalone.jar"]

# instead of logging to stdout, you may log to file in /log. create volume or mount host volume to /log
# RUN mkdir /log && chown daemon /log
# CMD ["java", "-Dlog_level=info", "-Dlog_appender=fileAppender", "-Dlog_location=/log", "-jar", "/contentreich-ecm-google-ms-standalone.jar"]

ADD target/contentreich-ecm-google-ms-*-standalone.jar /contentreich-ecm-google-ms-standalone.jar
