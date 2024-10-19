package no.fintlabs

import io.javaoperatorsdk.operator.Operator
import io.javaoperatorsdk.operator.api.config.ConfigurationServiceOverrider
import java.util.function.Consumer

fun interface OperatorConfigHandler : Consumer<ConfigurationServiceOverrider>
fun interface OperatorPostConfigHandler : Consumer<Operator>