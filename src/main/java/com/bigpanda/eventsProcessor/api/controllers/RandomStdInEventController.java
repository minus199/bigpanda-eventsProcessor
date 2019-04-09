package com.bigpanda.eventsProcessor.api.controllers;

import com.bigpanda.eventsProcessor.data.redis.model.RandomStdInEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/events/stdin")
public class RandomStdInEventController {

	private final ReactiveRedisOperations<String, RandomStdInEvent> stdInEventsOps;
	private final ReactiveRedisMessageListenerContainer messageListenerContainer;
	private final ChannelTopic stdinTopic;
	private final ReactiveRedisTemplate<String, RandomStdInEvent> stdinEventTemplate;

	@Autowired
	public RandomStdInEventController(ReactiveRedisOperations<String, RandomStdInEvent> stdInEventsOps,
	                                  ReactiveRedisMessageListenerContainer messageListenerContainer,
	                                  ChannelTopic stdinTopic,
	                                  @Qualifier("stdinEventTemplate")
			                                  ReactiveRedisTemplate<String, RandomStdInEvent> stdinEventTemplate) {
		this.stdInEventsOps = stdInEventsOps;
		this.messageListenerContainer = messageListenerContainer;
		this.stdinTopic = stdinTopic;
		this.stdinEventTemplate = stdinEventTemplate;
	}

	@GetMapping("/{uuid}")
	public Mono<RandomStdInEvent> get(@PathVariable("uuid") UUID uuid) {
		return stdInEventsOps.opsForValue().get(uuid.toString()).log();
	}

	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/new")
	public Flux<String> receiveCoffeeMessages() {
		return messageListenerContainer
				.receive(stdinTopic)
				.map(ReactiveSubscription.Message::getMessage);
	}

	@GetMapping(produces = MediaType.APPLICATION_STREAM_JSON_VALUE, value = "/all")
	public Flux<RandomStdInEvent> getAll() {
		return stdInEventsOps.scan().flatMap(s -> stdInEventsOps.opsForValue().get(s));
	}
}
