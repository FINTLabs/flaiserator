package no.fintlabs.operator.workflow

annotation class Workflow(val dependents: Array<Dependent> = [])
