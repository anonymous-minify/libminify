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
COPY ./Evaluation/RQ1/testProjects /usr/app/testProjects
COPY --from=build /root/.m2/repository/LibraryMinimizer/ /root/.m2/repository/LibraryMinimizer/
COPY ./Evaluation/RQ1/run.sh /usr/app/run.sh
WORKDIR /usr/app/

ENTRYPOINT ["/usr/app/run.sh"]