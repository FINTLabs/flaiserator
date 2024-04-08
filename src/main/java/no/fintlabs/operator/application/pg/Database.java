package no.fintlabs.operator.application.pg;

import lombok.Data;

@Data
public class Database {
    @Deprecated(forRemoval = true)
    private boolean enabled;

    private String database;
}
