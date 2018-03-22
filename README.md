# osm-to-netex
Tool for converting OSM XML to NeTex XML

Using java 8 and gradle 4.6

## Run with gradle
```gradle run -PappArgs='-osmFile osm.xml'```

## Build
```gradle build```

## Build fatjar
```gradle fatJar```

## Run from jar
```java -jar build/libs/osm-to-netex-all-1.0-SNAPSHOT.jar -osmFile osm.xml```
