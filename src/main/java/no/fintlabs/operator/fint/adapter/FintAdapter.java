package no.fintlabs.operator.fint.adapter;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FintAdapter {
    private String shortDescription;
    private List<FintAdapterComponents> components = new ArrayList<>();
}
