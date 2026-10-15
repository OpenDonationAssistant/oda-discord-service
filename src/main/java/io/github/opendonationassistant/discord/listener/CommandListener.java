package io.github.opendonationassistant.discord.listener;

import io.github.opendonationassistant.events.MessageProcessor;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Inject;

@RabbitListener(executor = "command-listener")
public class CommandListener {

  public static final String QUEUE_NAME = "discord.command";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);

  private final MessageProcessor processor;

  @Inject
  public CommandListener(MessageProcessor processor) {
    this.processor = processor;
  }

  @Queue(QUEUE_NAME)
  public void listenDiscordCommands(
    @MessageHeader String type,
    byte[] payload,
    RabbitAcknowledgement ack
  ) {
    processor.process(type, payload, ack);
  }
}
