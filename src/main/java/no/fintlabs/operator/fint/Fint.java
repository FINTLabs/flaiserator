package no.fintlabs.operator.fint;

import lombok.Data;
import no.fintlabs.operator.fint.adapter.FintAdapter;

@Data
public class Fint {
    private FintAdapter adapter = new FintAdapter();
}
