FROM tomcat:9-jre8-alpine

COPY ./build/libs/at-ex.war /usr/local/tomcat/webapps/

