package no.fintlabs.operator.application.kafka;

import lombok.Data;

@Data
public class KafkaAcl {
    private String topic;
    private String permission;
}
