package no.fintlabs.operator.fint.adapter;

import lombok.Data;
import no.fintlabs.FlaisSpec;

import java.util.ArrayList;
import java.util.List;

@Data
public class FintAdapterSpec implements FlaisSpec {
    private List<FintAdapter> adapter = new ArrayList<>();
}
