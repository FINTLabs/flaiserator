package no.fintlabs.operator.kafka;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Kafka {
    private boolean enabled;
    private List<KafkaAcl> acls = new ArrayList<>();
}
