package com.bigpanda.eventsProcessor.data.redis;

import com.bigpanda.eventsProcessor.data.redis.model.RandomStdInEvent;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableConfigurationProperties(RedisProperties.class)
@Configuration
public class RedisConfig {
	@Value("${redis.host}")
	private String redisHost;

	@Value("${redis.port}")
	private int redisPort;

	@Bean
	@ConditionalOnMissingBean(name = "clientResources")
	LettucePoolingClientConfiguration lettucePoolConfig(ClientOptions options,
	                                                    @Qualifier("clientResources") ClientResources dcr) {
		return LettucePoolingClientConfiguration.builder()
				.poolConfig(new GenericObjectPoolConfig())
				.clientOptions(options)
				.clientResources(dcr)
				.build();
	}

	@Bean
	@Primary
	public ReactiveRedisConnectionFactory connectionFactory(RedisStandaloneConfiguration redisStandaloneConfiguration,
	                                                        LettucePoolingClientConfiguration lettucePoolConfig) {
		return new LettuceConnectionFactory(redisStandaloneConfiguration, lettucePoolConfig);
	}

	@Bean(destroyMethod = "shutdown")
	RedisClient redisClient(ClientResources clientResources) {
		return RedisClient.create(clientResources, RedisURI.create(redisHost, redisPort));
	}

	@Bean(destroyMethod = "shutdown")
	ClientResources clientResources() {
		return DefaultClientResources.create();
	}

	@Bean
	public RedisStandaloneConfiguration redisStandaloneConfiguration() {
		return new RedisStandaloneConfiguration(redisHost, redisPort);
	}

	@Bean
	public ClientOptions clientOptions() {
		return ClientOptions.builder()
				.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
				.autoReconnect(true)
				.build();
	}

	@Bean
	ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer(ReactiveRedisConnectionFactory connectionFactory) {
		return new ReactiveRedisMessageListenerContainer(connectionFactory);
	}

	@Bean
	ChannelTopic stdinTopic() {
		return new ChannelTopic("newStdinEvent");
	}

	@Bean
	@ConditionalOnMissingBean(name = "redisTemplate")
	@Primary
	public ReactiveRedisTemplate<String, RandomStdInEvent> stdinEventTemplate(ReactiveRedisConnectionFactory factory) {
		Jackson2JsonRedisSerializer<RandomStdInEvent> serializer
				= new Jackson2JsonRedisSerializer<>(RandomStdInEvent.class);

		RedisSerializationContext.RedisSerializationContextBuilder<String, RandomStdInEvent> builder
				= RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

		RedisSerializationContext<String, RandomStdInEvent> context = builder.value(serializer).build();

		return new ReactiveRedisTemplate<>(factory, context);
	}
}