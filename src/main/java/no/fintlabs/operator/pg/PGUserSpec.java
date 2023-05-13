package no.fintlabs.operator.pg;

import lombok.Data;
import no.fintlabs.FlaisSpec;

@Data
public class PGUserSpec implements FlaisSpec {
    private String database;
}