package no.fintlabs.operator.pg;

import lombok.Data;

@Data
public class Database {
    @Deprecated(forRemoval = true)
    private boolean enabled;

    private String database;
}
