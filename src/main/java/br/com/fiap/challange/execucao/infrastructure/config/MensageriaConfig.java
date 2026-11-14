package br.com.fiap.challange.execucao.infrastructure.config;

import br.com.fiap.challange.execucao.infrastructure.messaging.MensageriaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MensageriaProperties.class)
public class MensageriaConfig {
}
