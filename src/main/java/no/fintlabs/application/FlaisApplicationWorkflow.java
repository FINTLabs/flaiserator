package no.fintlabs.application;

import no.fintlabs.FlaisWorkflow;
import no.fintlabs.application.crd.FlaisApplicationCrd;
import no.fintlabs.application.crd.FlaisApplicationSpec;
import org.springframework.stereotype.Component;

@Component
public class FlaisApplicationWorkflow extends FlaisWorkflow<FlaisApplicationCrd, FlaisApplicationSpec> {
}
