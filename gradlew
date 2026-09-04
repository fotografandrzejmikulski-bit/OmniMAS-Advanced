#!/bin/sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P) || exit 1

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    JAVACMD=$JAVA_HOME/jre/sh/java
  else
    JAVACMD=$JAVA_HOME/bin/java
  fi
else
  JAVACMD=java
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1 ; then
  echo >&2 "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
  exit 1
fi

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

set -- "-Dorg.gradle.appname=gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

exec "$JAVACMD" "$@"
