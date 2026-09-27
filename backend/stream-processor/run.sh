#!/usr/bin/env bash
# Runs the stream-processor jar with the JVM flags Flink normally applies
# automatically via bin/flink / start-cluster.sh (flink-conf.yaml's
# env.java.opts.all) on Java 17+/21, but which a bare `java -jar` invocation
# does not pick up on its own.
#
# The shaded jar's manifest already embeds the equivalent Add-Opens/
# Add-Exports attributes (see the maven-shade-plugin config in pom.xml), so
# `java -jar target/stream-processor-*.jar` should work without this script.
# Use this script if that ever proves insufficient (for example, if you run
# the classes via `java -cp target/classes:... Main`, where jar manifest
# attributes do not apply).
set -euo pipefail

JAR="$(ls target/stream-processor-*.jar 2>/dev/null | grep -v original | head -n1)"
if [ -z "${JAR}" ]; then
  echo "No shaded jar found under target/. Run 'mvn package' first." >&2
  exit 1
fi

exec java \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.io=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
  --add-opens=java.base/java.net=ALL-UNNAMED \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-opens=java.base/java.security=ALL-UNNAMED \
  --add-opens=java.base/java.text=ALL-UNNAMED \
  --add-opens=java.base/java.time=ALL-UNNAMED \
  --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
  --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.cs=ALL-UNNAMED \
  --add-opens=java.base/sun.security.action=ALL-UNNAMED \
  --add-opens=java.base/sun.util.calendar=ALL-UNNAMED \
  --add-exports=java.base/sun.net.util=ALL-UNNAMED \
  --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
  --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
  -jar "${JAR}" "$@"
