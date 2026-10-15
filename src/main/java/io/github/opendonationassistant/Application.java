package io.github.opendonationassistant;

import io.github.opendonationassistant.discord.listener.CommandListener;
import io.github.opendonationassistant.rabbit.AMQPConfiguration;
import io.github.opendonationassistant.rabbit.Exchange;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.annotation.Factory;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Map;

@OpenAPIDefinition(
  info = @Info(
    title = "oda-discord-service",
    version = "0.1.0",
    license = @License(
      name = "AGPL-3.0",
      url = "https://www.gnu.org/licenses/agpl-3.0.en.html"
    )
  )
)
@Factory
public class Application {

  @ContextConfigurer
  public static class DefaultEnvironmentConfigurer
    implements ApplicationContextConfigurer {

    @Override
    public void configure(ApplicationContextBuilder builder) {
      builder.defaultEnvironments("standalone");
    }
  }

  public static void main(String[] args) {
    Micronaut.build(args).mainClass(Application.class).banner(false).start();
  }

  @Singleton
  public ChannelInitializer rabbitConfiguration() {
    var bindings = new ArrayList<Exchange>();
    bindings.add(
      Exchange.Exchange(
        "commands",
        Map.of(
          "command.SendDiscordMessageCommand",
          CommandListener.QUEUE
        )
      )
    );
    return new AMQPConfiguration(bindings);
  }
}
