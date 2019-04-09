package com.bigpanda.eventsProcessor;

import com.bigpanda.eventsProcessor.data.model.RandomStdInEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

@Service
public class ProcessingCLI implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(ProcessingCLI.class);

	private final ReactiveRedisOperations<String, RandomStdInEvent> stdInEventsOps;
	private final RedisClient redisClient;
	private final ObjectMapper mapper;

	@Autowired
	public ProcessingCLI(ReactiveRedisOperations<String, RandomStdInEvent> stdInEventsOps,
	                     RedisClient redisClient, ObjectMapper mapper) {
		this.stdInEventsOps = stdInEventsOps;
		this.redisClient = redisClient;
		this.mapper = mapper;
	}

	@Override
	public void run(String... args) {
		read(System.in)
				.subscribeOn(Schedulers.newSingle("starter-thread"))
				.subscribe();
	}

	private Flux<Void> read(InputStream inputStream) {
		return Flux.create(new StdinConsumer(inputStream))
				.parallel().runOn(Schedulers.parallel())
				.map(this::parseEventJson)
				.map(this::createEvent)
				.flatMap(stdInEvent -> countWords(stdInEvent.getData())
						.map(wcByWord -> {
							stdInEvent.setUniqueWordCount(wcByWord);
							return stdInEvent;
						}))
				.flatMap(e -> {
					try {
						String s = mapper.writeValueAsString(e);
						return this.redisClient.connectPubSub().reactive()
								.publish("newStdinEvent", s)
								.and(stdInEventsOps.opsForValue().set(e.getUuid().toString(), e));
					} catch (JsonProcessingException e1) {
						throw new RuntimeException(e1);
					}
				})
				.doOnError(throwable -> log.error("Got error: {}", throwable.getMessage()))
				.sequential();
	}

	private JsonNode parseEventJson(String rawJson) {
		try {
			return mapper.readTree(rawJson);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private RandomStdInEvent createEvent(JsonNode parsedJson) {
		String eventType = parsedJson.get("event_type").asText();
		String eventData = parsedJson.get("data").asText();
		long timestamp = parsedJson.get("timestamp").asLong();
		return new RandomStdInEvent(UUID.randomUUID(), eventType, eventData, timestamp);
	}

	private Mono<HashMap<String, Long>> countWords(String string) {
		return Flux.fromArray(string.split(" "))
				.map(String::toLowerCase)
				.groupBy(String::toString)
				.sort(Comparator.comparing(GroupedFlux::key))
				.flatMap(group -> Mono.zip(Mono.just(Objects.requireNonNull(group.key())), group.count()))
				.reduce(new HashMap<>(), (countByWords, currentWord) -> {
					countByWords.put(currentWord.getT1(), currentWord.getT2());
					return countByWords;
				});
	}

	private static class StdinConsumer implements Consumer<FluxSink<String>>, AutoCloseable {
		private final InputStream stream;

		StdinConsumer(InputStream stream) {
			this.stream = stream;
		}

		@Override
		public void accept(FluxSink<String> sink) {
			Scanner scanner = new Scanner(stream);
			while (scanner.hasNext()) {
				sink.next(scanner.nextLine());
			}
		}

		@Override
		public void close() throws Exception {
			this.stream.close();
		}
	}
}
