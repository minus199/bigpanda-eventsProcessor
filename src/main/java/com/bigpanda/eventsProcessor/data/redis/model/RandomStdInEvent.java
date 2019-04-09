package com.bigpanda.eventsProcessor.data.redis.model;

import java.util.Map;
import java.util.UUID;

public class RandomStdInEvent {
	private UUID uuid;
	private String eventType;
	private String data;
	private Map<String, Long> uniqueWordCount;
	private Long timestamp;

	public RandomStdInEvent() {
	}

	public RandomStdInEvent(UUID uuid, String eventType, String data, Map<String, Long> uniqueWordCount, Long timestamp) {
		this.uuid = uuid;
		this.eventType = eventType;
		this.data = data;
		this.uniqueWordCount = uniqueWordCount;
		this.timestamp = timestamp;
	}

	public RandomStdInEvent(UUID uuid, String eventType, String eventData, long timestamp) {
		this(uuid, eventType, eventData, null, timestamp);
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getEventType() {
		return eventType;
	}

	public String getData() {
		return data;
	}

	public Map<String, Long> getUniqueWordCount() {
		return uniqueWordCount;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void setUniqueWordCount(Map<String, Long> uniqueWordCount) {
		this.uniqueWordCount = uniqueWordCount;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
}
