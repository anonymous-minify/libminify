FROM maven:3.6.0-jdk-8-alpine as build
RUN mkdir -p /usr/app

ENV SCALA_VERSION 2.12.8
ENV SBT_VERSION 1.2.8

ENV PATH /sbt/bin:$PATH

RUN apk add -U bash docker

# Install Scala
## Piping curl directly in tar
RUN \
  wget -O - https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo "export PATH=~/scala-$SCALA_VERSION/bin:$PATH" >> /root/.bashrc

# Install SBT
RUN wget https://piccolo.link/sbt-$SBT_VERSION.tgz && \
  tar -xzvf sbt-$SBT_VERSION.tgz && \
  sbt sbtVersion


COPY ./LibraryMinimizer/ /usr/app/LibraryMinimizer/
RUN cd /usr/app/LibraryMinimizer && \
	sbt publishM2 BranchingTypeAnalysis/publishM2
	

FROM maven:3.6.0-jdk-8-alpine	
ENV _JAVA_OPTIONS "-Xmx8G -Xss1G"
COPY ./MavenPlugin/ /usr/app/MavenPlugin/
COPY ./Evaluation/RQ3/testProjects /usr/app/testProjects
COPY --from=build /root/.m2/repository/LibraryMinimizer/ /root/.m2/repository/LibraryMinimizer/
COPY ./Evaluation/RQ3/run.sh /usr/app/run.sh
RUN apk --no-cache add ca-certificates wget && \
    wget --quiet --output-document=/etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget https://github.com/sgerrand/alpine-pkg-java-openjfx/releases/download/8.151.12-r0/java-openjfx-8.151.12-r0.apk && \
    apk add --no-cache java-openjfx-8.151.12-r0.apk shadow
WORKDIR /usr/app/

ENTRYPOINT /usr/app/run.sh