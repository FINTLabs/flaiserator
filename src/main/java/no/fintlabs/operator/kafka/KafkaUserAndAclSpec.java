package no.fintlabs.operator.kafka;

import lombok.*;
import no.fintlabs.FlaisSpec;

import java.util.ArrayList;
import java.util.List;

@Data
public class KafkaUserAndAclSpec implements FlaisSpec {
    private List<KafkaAcl> acls = new ArrayList<>();

}
