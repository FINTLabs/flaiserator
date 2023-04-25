package no.fintlabs.operator.kafka;

import lombok.Data;

@Data
public class KafkaAcl {
    private String topic;
    private String permission;
}
