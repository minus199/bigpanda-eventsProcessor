# bigpanda-eventsProcessor
Accumulator, Processor and Exposed REST API for incoming stdin events from a separate process

# Instructions:
 - Clone
 - Redis should be available(set your config in `resources/application.properties`)
 - `mvn install && {eventsSpitter executable location}| java -jar target/eventsProcessor-0.0.1-SNAPSHOT.jar`<br>
The server should be up and running @ localhost:8080
# API
 #### <u>/api/events/stdin/new</u><br>
    Creates a ServerSentEvents stream using redis pub/sub.<br> 
    Summary of new events.
 
 `curl 'http://localhost:8080/api/events/stdin/new' \ ` <br>
 `-H 'Accept-Encoding: gzip, deflate, br' \ ` <br>
 `-H 'Accept: text/event-stream' \ ` <br>
 `-H 'Connection: keep-alive' \ ` <br>
 `-H 'Cache-Control: no-cache' \ ` <br>
 `--compressed`
 
 #### <u>/api/events/stdin/all</u><br>
    Creates a json stream of the entire keyspace.<br>
    The response uses a cursor to handle large datasets.<br>
 
`curl http://localhost:8080/api/events/stdin/all`
 #### /api/events/stdin/{uuid of event, assigned by this server}<br>
    Get a single event object by its uuid
 
`curl http://localhost:8080/api/events/stdin/f10ff675-38f0-4ced-b592-23dbb66f3d7b`

# TODOS:
 - Allow connecting additional input sources/streams after the app has been loaded.
    - Detect suitable handler according to stream source
 - Full text search
 - Grep events stream via API